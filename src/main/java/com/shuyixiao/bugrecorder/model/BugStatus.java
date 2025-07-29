package com.shuyixiao.bugrecorder.model;

/**
 * Bug状态枚举
 */
public enum BugStatus {
    PENDING("待处理", "待处理的Bug"),
    IN_PROGRESS("处理中", "正在处理的Bug"),
    RESOLVED("已解决", "已解决的Bug"),
    IGNORED("已忽略", "已忽略的Bug");

    private final String displayName;
    private final String description;

    BugStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
} 