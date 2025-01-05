package com.xxl.job.plus.executor.entity;

import com.xxl.job.plus.executor.model.XxlJobInfo;

import java.util.ArrayList;
import java.util.List;


public class AddOrUpdateTaskInfo {
    // 需要新增的task
    private List<XxlJobInfo> addTask = new ArrayList<>();
    // 需要更新的task
    private  List<XxlJobInfo> updateTask = new ArrayList<>();
    // 相同的 需要忽略的task
    private  List<XxlJobInfo> sameTask = new ArrayList<>();
    // 应该删除的任务
    private  List<XxlJobInfo> deleteTask = new ArrayList<>();


    public AddOrUpdateTaskInfo(List<XxlJobInfo> addTask, List<XxlJobInfo> updateTask, List<XxlJobInfo> deleteTask) {
        this.addTask = addTask;
        this.updateTask = updateTask;
        this.deleteTask = deleteTask;
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

    public List<XxlJobInfo> getDeleteTask() {
        return deleteTask;
    }

    public void setDeleteTask(List<XxlJobInfo> deleteTask) {
        this.deleteTask = deleteTask;
    }
}
