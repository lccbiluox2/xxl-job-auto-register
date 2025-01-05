package com.xxl.job.plus.executor.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 更新任务信息时，记录哪些字段发生了变化以及变化前后的信息。
 */
public class UpdateResult {
    private boolean needsUpdate;
    private Map<String, ChangeInfo> changedFields;

    public UpdateResult() {
        this.needsUpdate = false;
        this.changedFields = new HashMap<>();
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public void addChangedField(String fieldName, String oldValue, String newValue) {
        if (this.needsUpdate) {
            this.changedFields.put(fieldName, new ChangeInfo(oldValue, newValue));
        }
    }

    public boolean isNeedsUpdate() {
        return needsUpdate;
    }

    public Map<String, ChangeInfo> getChangedFields() {
        return changedFields;
    }

    @Override
    public String toString() {
        return "UpdateResult{" +
                "needsUpdate=" + needsUpdate +
                ", changedFields=" + changedFields +
                '}';
    }


}