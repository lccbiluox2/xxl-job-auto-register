package com.xxl.job.plus.executor.entity;

import com.xxl.job.plus.executor.model.XxlJobInfo;

import java.util.ArrayList;
import java.util.List;


public class AddOrUpdateTaskInfo {
    // 需要新增的task
    List<XxlJobInfo> addTask = new ArrayList<>();
    // 需要更新的task
    List<XxlJobInfo> updateTask = new ArrayList<>();
    // 相同的 需要忽略的task
    List<XxlJobInfo> sameTask = new ArrayList<>();

    public AddOrUpdateTaskInfo(List<XxlJobInfo> addTask, List<XxlJobInfo> updateTask) {
        this.addTask = addTask;
        this.updateTask = updateTask;
    }



    public List<XxlJobInfo> getAddTask() {
        return addTask;
    }

    public void setAddTask(List<XxlJobInfo> addTask) {
        this.addTask = addTask;
    }

    public List<XxlJobInfo> getUpdateTask() {
        return updateTask;
    }

    public void setUpdateTask(List<XxlJobInfo> updateTask) {
        this.updateTask = updateTask;
    }

    public List<XxlJobInfo> getSameTask() {
        return sameTask;
    }

    public void setSameTask(List<XxlJobInfo> sameTask) {
        this.sameTask = sameTask;
    }
}
