package com.shuyixiao.gitstat.email.model;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName SmtpPreset.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description SMTP邮箱服务预设配置类，提供Gmail、QQ邮箱、163邮箱等12种常见邮箱服务的预设配置，支持一键快速配置SMTP参数
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SmtpPreset that = (SmtpPreset) obj;

        if (smtpPort != that.smtpPort) return false;
        if (enableTLS != that.enableTLS) return false;
        if (enableSSL != that.enableSSL) return false;
        if (!name.equals(that.name)) return false;
        return smtpHost.equals(that.smtpHost);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + smtpHost.hashCode();
        result = 31 * result + smtpPort;
        result = 31 * result + (enableTLS ? 1 : 0);
        result = 31 * result + (enableSSL ? 1 : 0);
        return result;
    }
}

