package com.xxl.job.plus.executor.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.xxl.job.plus.executor.service.JobLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JobLoginServiceImpl 是一个服务类，负责与 xxl-job 管理平台交互，执行登录操作并管理登录凭证（cookie）。
 */
@Service
public class JobLoginServiceImpl implements JobLoginService {

    private static final Logger log = LoggerFactory.getLogger(JobLoginServiceImpl.class);

    // xxl-job 管理平台地址
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // 登录用户名
    @Value("${xxl.job.admin.username}")
    private String username;

    // 登录密码
    @Value("${xxl.job.admin.password}")
    private String password;

    // 用于存储登录凭证（cookie）
    private final Map<String, String> loginCookie = new HashMap<>();

    /**
     * 执行登录操作，获取 xxl-job 的登录凭证（cookie）。
     */
    @Override
    public void login() {
        // 构建请求URL
        String url = adminAddresses + "/login";
        log.info("准备调用登录接口以获取 xxl-job 的 cookie 信息");

        try {
            // 发送POST请求进行登录
            HttpResponse response = HttpRequest.post(url)
                    .form("userName", username)
                    .form("password", password)
                    .execute();

            // 获取响应中的所有 cookies
            List<HttpCookie> cookies = response.getCookies();
            Optional<HttpCookie> cookieOpt = cookies.stream()
                    .filter(cookie -> "XXL_JOB_LOGIN_IDENTITY".equals(cookie.getName()))
                    .findFirst();

            if (!cookieOpt.isPresent()) {
                log.error("未能从响应中找到名为 XXL_JOB_LOGIN_IDENTITY 的 cookie");
                throw new RuntimeException("获取 xxl-job cookie 失败！");
            }

            // 提取所需的 cookie 值
            String value = cookieOpt.get().getValue();
            log.info("成功获取到 xxl-job 的 cookie 信息: {}", value);
            loginCookie.put("XXL_JOB_LOGIN_IDENTITY", value);
        } catch (Exception e) {
            log.error("调用登录接口时发生异常: ", e);
            throw new RuntimeException("登录失败", e);
        }
    }

    /**
     * 获取已存储的登录凭证（cookie），如果不存在则尝试重新登录。
     *
     * @return 包含登录凭证的字符串
     */
    @Override
    public String getCookie() {
        for (int i = 0; i < 3; i++) { // 尝试最多三次获取有效的 cookie
            String cookieStr = loginCookie.get("XXL_JOB_LOGIN_IDENTITY");
            if (cookieStr != null) {
                log.info("返回已存储的 xxl-job cookie: XXL_JOB_LOGIN_IDENTITY={}", cookieStr);
                return "XXL_JOB_LOGIN_IDENTITY=" + cookieStr;
            }

            // 如果没有找到有效的 cookie，则尝试重新登录
            log.warn("未找到有效的 xxl-job cookie，正在尝试重新登录...");
            login();
        }

        // 如果三次尝试后仍然无法获取有效的 cookie，则抛出异常
        log.error("经过多次尝试后仍未能获取有效的 xxl-job cookie");
        throw new RuntimeException("获取 xxl-job cookie 失败！");
    }
}