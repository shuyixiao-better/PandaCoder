package com.shuyixiao.bugrecorder.model;

/**
 * 错误类型枚举
 * 用于分类不同类型的错误和异常
 */
public enum ErrorType {

    /**
     * 数据库相关错误
     */
    DATABASE("数据库错误", "🗄️", "#FF6B6B"),

    /**
     * 网络连接错误
     */
    NETWORK("网络错误", "🌐", "#4ECDC4"),

    /**
     * Spring框架错误
     */
    SPRING_FRAMEWORK("Spring框架错误", "🍃", "#95E1D3"),

    /**
     * 编译错误
     */
    COMPILATION("编译错误", "⚙️", "#F38BA8"),

    /**
     * 运行时错误
     */
    RUNTIME("运行时错误", "⚡", "#FFD93D"),

    /**
     * 配置错误
     */
    CONFIGURATION("配置错误", "⚙️", "#A8E6CF"),

    /**
     * 内存相关错误
     */
    MEMORY("内存错误", "💾", "#FF8B94"),

    /**
     * IO操作错误
     */
    IO("IO错误", "📁", "#B4A7D6"),

    /**
     * 安全相关错误
     */
    SECURITY("安全错误", "🔒", "#D4A574"),

    /**
     * 第三方库错误
     */
    THIRD_PARTY("第三方库错误", "📦", "#87CEEB"),

    /**
     * 未知类型错误
     */
    UNKNOWN("未知错误", "❓", "#CCCCCC");

    private final String displayName;
    private final String icon;
    private final String color;

    ErrorType(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取图标
     */
    public String getIcon() {
        return icon;
    }

    /**
     * 获取颜色
     */
    public String getColor() {
        return color;
    }

    /**
     * 获取带图标的显示名称
     */
    public String getDisplayNameWithIcon() {
        return icon + " " + displayName;
    }

    /**
     * 根据名称查找错误类型
     */
    public static ErrorType fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return UNKNOWN;
        }

        for (ErrorType type : values()) {
            if (type.name().equalsIgnoreCase(name) ||
                    type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }

        return UNKNOWN;
    }

    /**
     * 判断是否为严重错误
     */
    public boolean isSevere() {
        return this == DATABASE || this == MEMORY || this == SECURITY;
    }

    /**
     * 判断是否为可能自动修复的错误
     */
    public boolean isAutoFixable() {
        return this == CONFIGURATION || this == COMPILATION;
    }
}