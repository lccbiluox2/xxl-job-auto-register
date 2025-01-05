package com.xxl.job.plus.executor.entity;

// 内部类，用于存储变更前后的信息
public  class ChangeInfo {
    private final String oldValue;
    private final String newValue;

    public ChangeInfo(String oldValue, String newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return "ChangeInfo{" +
                "oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                '}';
    }
}
