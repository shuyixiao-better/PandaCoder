package com.shuyixiao.gitstat.email.model;

/**
 * SMTP 邮箱服务预设配置
 */
public class SmtpPreset {
    
    private final String name;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean enableTLS;
    private final boolean enableSSL;
    private final String description;
    
    public SmtpPreset(String name, String smtpHost, int smtpPort, 
                     boolean enableTLS, boolean enableSSL, String description) {
        this.name = name;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.enableTLS = enableTLS;
        this.enableSSL = enableSSL;
        this.description = description;
    }
    
    // 预设配置列表
    public static SmtpPreset[] getPresets() {
        return new SmtpPreset[] {
            new SmtpPreset(
                "Gmail",
                "smtp.gmail.com",
                587,
                true,
                false,
                "需要应用专用密码"
            ),
            new SmtpPreset(
                "QQ邮箱 (TLS)",
                "smtp.qq.com",
                587,
                true,
                false,
                "需要授权码"
            ),
            new SmtpPreset(
                "QQ邮箱 (SSL)",
                "smtp.qq.com",
                465,
                false,
                true,
                "需要授权码"
            ),
            new SmtpPreset(
                "163邮箱",
                "smtp.163.com",
                465,
                false,
                true,
                "需要客户端授权密码"
            ),
            new SmtpPreset(
                "126邮箱",
                "smtp.126.com",
                465,
                false,
                true,
                "需要客户端授权密码"
            ),
            new SmtpPreset(
                "Outlook/Hotmail",
                "smtp.office365.com",
                587,
                true,
                false,
                "需要应用密码"
            ),
            new SmtpPreset(
                "Yahoo邮箱",
                "smtp.mail.yahoo.com",
                587,
                true,
                false,
                "需要应用密码"
            ),
            new SmtpPreset(
                "新浪邮箱",
                "smtp.sina.com",
                587,
                true,
                false,
                "需要客户端授权密码"
            ),
            new SmtpPreset(
                "搜狐邮箱",
                "smtp.sohu.com",
                465,
                false,
                true,
                "需要客户端授权密码"
            ),
            new SmtpPreset(
                "阿里云邮箱",
                "smtp.aliyun.com",
                465,
                false,
                true,
                "需要授权密码"
            ),
            new SmtpPreset(
                "腾讯企业邮箱",
                "smtp.exmail.qq.com",
                465,
                false,
                true,
                "需要授权码"
            ),
            new SmtpPreset(
                "自定义",
                "",
                587,
                true,
                false,
                "手动配置"
            )
        };
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public String getSmtpHost() {
        return smtpHost;
    }
    
    public int getSmtpPort() {
        return smtpPort;
    }
    
    public boolean isEnableTLS() {
        return enableTLS;
    }
    
    public boolean isEnableSSL() {
        return enableSSL;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

