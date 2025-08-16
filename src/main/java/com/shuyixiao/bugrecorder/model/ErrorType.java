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
     * HTTP客户端错误（调用外部服务）
     */
    HTTP_CLIENT("HTTP客户端错误", "📡", "#8EC5FC"),

    /**
     * HTTP服务端错误（本服务或下游返回5xx）
     */
    HTTP_SERVER("HTTP服务端错误", "🛰️", "#E0C3FC"),

    /**
     * 参数校验/数据校验错误
     */
    VALIDATION("校验错误", "✅", "#B8F2E6"),

    /**
     * 序列化/反序列化错误（JSON/XML等）
     */
    SERIALIZATION("序列化错误", "🧩", "#FDE68A"),

    /**
     * Spring Bean/DI相关错误
     */
    BEAN("Bean装配错误", "🫘", "#A7F3D0"),

    /**
     * 超时类错误（细化自网络/数据库等）
     */
    TIMEOUT("超时错误", "⏱️", "#FECACA"),

    /**
     * 连接建立错误（细化自网络）
     */
    CONNECTIVITY("连接错误", "🔌", "#C7D2FE"),

    /**
     * 缓存相关错误（Redis/本地缓存等）
     */
    CACHE("缓存错误", "🧠", "#D1FAE5"),

    /**
     * 消息队列相关错误
     */
    MQ("消息队列错误", "📬", "#FBCFE8"),

    /**
     * DNS解析错误
     */
    DNS("DNS错误", "🗺️", "#FDE68A"),

    /**
     * TLS/SSL错误
     */
    TLS("TLS/SSL错误", "🔐", "#F9A8D4"),

    /**
     * 并发/锁竞争错误
     */
    CONCURRENCY("并发错误", "🧵", "#FECACA"),

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
        return this == DATABASE || this == MEMORY || this == SECURITY || this == HTTP_SERVER || this == TLS;
    }

    /**
     * 判断是否为可能自动修复的错误
     */
    public boolean isAutoFixable() {
        return this == CONFIGURATION || this == COMPILATION || this == VALIDATION || this == SERIALIZATION;
    }
}