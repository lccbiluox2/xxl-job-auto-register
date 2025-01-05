package com.xxl.job.plus.executor.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xxl.job.plus.executor.model.XxlJobGroup;
import com.xxl.job.plus.executor.service.JobGroupService;
import com.xxl.job.plus.executor.service.JobLoginService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JobGroupServiceImpl 是一个服务类，负责与 xxl-job 管理平台交互，获取执行器列表、自动注册执行器以及精确检查执行器的存在。
 */
@Service
public class JobGroupServiceImpl implements JobGroupService {

    private static final Logger log = LoggerFactory.getLogger(JobGroupServiceImpl.class);

    // xxl-job 管理平台地址
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // 执行器应用名称
    @Value("${xxl.job.executor.appname}")
    private String appName;

    // 执行器标题（用于区分不同执行器）
    @Value("${xxl.job.executor.title}")
    private String title;

    /*
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    @Value("${xxl.job.executor.addressType:0}")
    private Integer addressType;

    /*
     * 执行器地址列表，多地址逗号分隔(仅当 addressType 为 1 时有效)
     */
    @Value("${xxl.job.executor.addressList:}")
    private String addressList;

    // 登录服务，用于获取访问管理平台所需的 cookie
    @Autowired
    private JobLoginService jobLoginService;

    /**
     * 获取所有执行器的列表信息。
     *
     * @return 执行器列表
     */
    @Override
    public List<XxlJobGroup> getJobGroup() {
        // 构建请求URL
        String url = adminAddresses + "/jobgroup/pageList";
        log.info("准备调用接口获取执行器列表信息:{}", url);

        try {
            // 发送POST请求以获取执行器列表
            HttpResponse response = HttpRequest.post(url)
                    .form("appname", appName)
                    .form("title", title)
                    .cookie(jobLoginService.getCookie())
                    .execute();

            // 解析响应体
            String body = response.body();
            JSONArray array = JSONUtil.parse(body).getByPath("data", JSONArray.class);
            List<XxlJobGroup> list = array.stream()
                    .map(o -> JSONUtil.toBean((JSONObject) o, XxlJobGroup.class))
                    .collect(Collectors.toList());

            if (list == null || list.isEmpty()) {
                log.warn("执行器列表为空，可能是参数不对或未创建任务执行器。appName: {}, title: {}", appName, title);
            } else {
                log.info("成功获取到 {} 个执行器信息。", list.size());
            }

            return list;
        } catch (Exception e) {
            log.error("获取执行器列表失败: ", e);
            throw new RuntimeException("获取执行器列表失败", e);
        }
    }

    /**
     * 自动注册一个新的执行器。
     *
     * @return 如果注册成功返回 true，否则返回 false
     */
    @Override
    public boolean autoRegisterGroup() {
        // 构建请求URL
        String url = adminAddresses + "/jobgroup/save";
        log.info("准备调用接口自动注册执行器:{}", url);

        try {
            // 创建HTTP POST请求
            HttpRequest httpRequest = HttpRequest.post(url)
                    .form("appname", appName)
                    .form("title", title);

            // 设置执行器地址类型及相应参数
            httpRequest.form("addressType", addressType);
            if (addressType.equals(1)) {
                if (Strings.isBlank(addressList)) {
                    log.error("手动录入模式下,执行器地址列表不能为空");
                    throw new RuntimeException("手动录入模式下,执行器地址列表不能为空");
                }
                httpRequest.form("addressList", addressList);
            }

            // 发送请求并处理响应
            HttpResponse response = httpRequest.cookie(jobLoginService.getCookie())
                    .execute();

            Object code = JSONUtil.parse(response.body()).getByPath("code");

            if (code.equals(200)) {
                log.info("自动注册执行器成功！");
                return true;
            } else {
                log.error("自动注册执行器失败，响应码: {}", code);
                return false;
            }
        } catch (Exception e) {
            log.error("自动注册执行器过程中出现异常: ", e);
            throw new RuntimeException("自动注册执行器失败", e);
        }
    }

    /**
     * 检查当前配置的应用是否已经存在。
     *
     * @return 如果存在返回 true，否则返回 false
     */
    @Override
    public boolean preciselyCheck() {
        log.info("开始精确检查执行器是否存在...");

        try {
            // 获取所有执行器列表
            List<XxlJobGroup> jobGroup = getJobGroup();

            // 查找是否有匹配的应用名和标题的执行器
            Optional<XxlJobGroup> has = jobGroup.stream()
                    .filter(xxlJobGroup -> xxlJobGroup.getAppname().equals(appName)
                            && xxlJobGroup.getTitle().equals(title))
                    .findAny();

            if (has.isPresent()) {
                log.info("找到匹配的执行器: appName={}, title={}", appName, title);
            } else {
                log.info("未找到匹配的执行器: appName={}, title={}", appName, title);
            }

            return has.isPresent();
        } catch (Exception e) {
            log.error("精确检查执行器存在性失败: ", e);
            throw new RuntimeException("精确检查执行器存在性失败", e);
        }
    }
}