package com.shuyixiao.bugrecorder.enhanced;

/**
 * Bug状态枚举
 * 定义Bug记录的不同状态
 */
public enum BugStatus {
    NEW("新建", "🆕"),
    IN_PROGRESS("处理中", "🔧"),
    RESOLVED("已解决", "✅"),
    CLOSED("已关闭", "🔒"),
    REOPENED("重新打开", "🔄"),
    IGNORED("已忽略", "🙈"),
    DUPLICATE("重复", "🔁"),
    WONT_FIX("不会修复", "🙅"),
    NEEDS_REVIEW("需要审查", "🧐");

    private final String displayName;
    private final String emoji;

    BugStatus(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    @Override
    public String toString() {
        return emoji + " " + displayName;
    }

    /**
     * 根据名称查找Bug状态
     */
    public static BugStatus fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return NEW;
        }

        for (BugStatus status : values()) {
            if (status.name().equalsIgnoreCase(name) ||
                status.displayName.equalsIgnoreCase(name)) {
                return status;
            }
        }

        return NEW;
    }

    /**
     * 获取带表情符号的显示名称
     */
    public String getDisplayNameWithEmoji() {
        return emoji + " " + displayName;
    }

    /**
     * 判断是否为已完成状态
     */
    public boolean isCompleted() {
        return this == RESOLVED || this == CLOSED || this == IGNORED || this == WONT_FIX;
    }

    /**
     * 判断是否为活动状态
     */
    public boolean isActive() {
        return this == NEW || this == IN_PROGRESS || this == REOPENED || this == NEEDS_REVIEW;
    }

    /**
     * 判断是否为未解决状态
     */
    public boolean isUnresolved() {
        return !isCompleted();
    }
}