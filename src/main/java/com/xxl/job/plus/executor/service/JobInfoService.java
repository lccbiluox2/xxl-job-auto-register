package com.xxl.job.plus.executor.service;

import com.xxl.job.plus.executor.model.XxlJobInfo;

import java.util.List;

public interface JobInfoService {

    List<XxlJobInfo> getJobInfo(Integer jobGroupId, String executorHandler);

    Integer addJobInfo(XxlJobInfo xxlJobInfo);

    Integer updateTask(XxlJobInfo item);

    Integer deleteTask(XxlJobInfo item);
}
