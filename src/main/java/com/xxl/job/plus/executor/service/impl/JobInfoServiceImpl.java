package com.xxl.job.plus.executor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xxl.job.plus.executor.model.XxlJobInfo;
import com.xxl.job.plus.executor.service.JobInfoService;
import com.xxl.job.plus.executor.service.JobLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JobInfoServiceImpl 是一个服务类，负责与 xxl-job 管理平台交互，获取任务信息列表以及添加新的任务信息。
 */
@Service
public class JobInfoServiceImpl implements JobInfoService {

    private static final Logger log = LoggerFactory.getLogger(JobInfoServiceImpl.class);

    // xxl-job 管理平台地址
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // 登录服务，用于获取访问管理平台所需的 cookie
    @Autowired
    private JobLoginService jobLoginService;

    /**
     * 根据执行器组ID和处理器名称获取任务信息列表。
     *
     * @param jobGroupId 执行器组ID
     * @param executorHandler 任务处理器名称
     * @return 任务信息列表
     */
    @Override
    public List<XxlJobInfo> getJobInfo(Integer jobGroupId, String executorHandler) {
        // 构建请求URL
        String url = adminAddresses + "/jobinfo/pageList";
        log.info("准备调用接口获取任务信息列表:{}", url);

        try {
            // 发送POST请求以获取任务信息列表
            HttpResponse response = HttpRequest.post(url)
                    .form("jobGroup", jobGroupId)
                    .form("executorHandler", executorHandler)
                    .form("triggerStatus", -1) // -1 表示不筛选触发状态
                    .cookie(jobLoginService.getCookie())
                    .execute();

            // 解析响应体
            String body = response.body();
            JSONArray array = JSONUtil.parse(body).getByPath("data", JSONArray.class);
            List<XxlJobInfo> list = array.stream()
                    .map(o -> JSONUtil.toBean((JSONObject) o, XxlJobInfo.class))
                    .collect(Collectors.toList());

            if (list == null || list.isEmpty()) {
                log.warn("未找到符合条件的任务信息。jobGroupId: {}, executorHandler: {}", jobGroupId, executorHandler);
            } else {
                log.info("成功获取到 {} 个任务信息。", list.size());
            }

            return list;
        } catch (Exception e) {
            log.error("获取任务信息列表失败: ", e);
            throw new RuntimeException("获取任务信息列表失败", e);
        }
    }

    /**
     * 添加一个新的任务信息。
     *
     * @param xxlJobInfo 要添加的任务信息对象
     * @return 新增任务的ID
     */
    @Override
    public Integer addJobInfo(XxlJobInfo xxlJobInfo) {
        // 构建请求URL
        String url = adminAddresses + "/jobinfo/add";
        log.info("准备调用接口添加任务信息:{}", url);

        try {
            // 将任务信息对象转换为Map，以便于HTTP POST请求时使用
            Map<String, Object> paramMap = BeanUtil.beanToMap(xxlJobInfo);
            log.debug("准备发送的任务信息参数: {}", paramMap);

            // 发送POST请求以添加任务信息
            HttpResponse response = HttpRequest.post(url)
                    .form(paramMap)
                    .cookie(jobLoginService.getCookie())
                    .execute();

            // 解析响应体
            JSONObject json = JSONUtil.parseObj(response.body());
            Object code = json.getByPath("code");

            if (code.equals(200)) {
                Integer jobId = Convert.toInt(json.getByPath("content"));
                log.info("成功添加任务信息，新任务ID: {}", jobId);
                return jobId;
            } else {
                log.error("添加任务信息失败，响应码: {}, 响应内容: {}", code, json.getByPath("msg"));
                throw new RuntimeException("添加任务信息失败");
            }
        } catch (Exception e) {
            log.error("添加任务信息过程中出现异常: ", e);
            throw new RuntimeException("添加任务信息失败", e);
        }
    }

    /**
     * 界面查看的时候，更新参数如下
     *
     * jobGroup: 1
     * jobDesc: 测试job1
     * author: hydra
     * alarmEmail:
     * scheduleType: CRON
     * scheduleConf: 0 0 0 * * ? *
     * cronGen_display: 0 0 0 * * ? *
     * schedule_conf_CRON: 0 0 0 * * ? *
     * schedule_conf_FIX_RATE:
     * schedule_conf_FIX_DELAY:
     * executorHandler: testJob
     * executorParam:
     * executorRouteStrategy: ROUND
     * childJobId:
     * misfireStrategy: DO_NOTHING
     * executorBlockStrategy: SERIAL_EXECUTION
     * executorTimeout: 0
     * executorFailRetryCount: 0
     * id: 6
     *
     * id 应该是必须的
     * @param item
     * @return
     */
    @Override
    public Integer updateTask(XxlJobInfo item) {
        // 构建请求URL
        String url = adminAddresses + "/jobinfo/update";
        log.info("准备调用接口更新任务信息:{}", url);

        try {
            // 将任务信息对象转换为Map，以便于HTTP POST请求时使用
            Map<String, Object> paramMap = BeanUtil.beanToMap(item);
            log.info("准备发送的任务信息参数: {}", paramMap);

            // 发送POST请求以更新任务信息
            HttpResponse response = HttpRequest.post(url)
                    .form(paramMap)
                    .cookie(jobLoginService.getCookie())
                    .execute();

            // 解析响应体
            JSONObject json = JSONUtil.parseObj(response.body());
            Object code = json.getByPath("code");

            if (Objects.equals(code, 200)) { // 使用 Objects.equals 处理 null
                Integer jobId = Convert.toInt(json.getByPath("content"));
                log.info("成功更新任务信息，任务ID: {}", jobId);
                return jobId;
            } else {
                log.error("更新任务信息失败，响应码: {}, 响应内容: {}", code, json.getByPath("msg"));
                throw new RuntimeException("更新任务信息失败");
            }
        } catch (Exception e) {
            log.error("更新任务信息过程中出现异常: ", e);
            throw new RuntimeException("更新任务信息失败", e);
        }
    }
}