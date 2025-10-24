package com.shuyixiao.advice;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName: PluginAdviceService.java
 * author: 舒一笑不秃头
 * version: 2.2.0
 * Description: 插件建议反馈邮件服务，负责发送用户的意见、建议和Bug反馈到作者邮箱
 * createTime: 2025-10-24
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class PluginAdviceService {
    
    private static final Logger LOG = Logger.getInstance(PluginAdviceService.class);
    
    private static final String SMTP_HOST = "smtp.163.com";
    private static final int SMTP_PORT = 465; // 使用SSL端口
    private static final boolean ENABLE_SSL = true;
    
    private final String senderEmail;
    private final String receiverEmail;
    private final String smtpPassword;
    
    public PluginAdviceService() {
        // 从配置文件加载SMTP配置
        Properties config = loadEmailConfig();
        this.senderEmail = config.getProperty("advice.email.sender", "yixiaoshu88@163.com");
        this.receiverEmail = config.getProperty("advice.email.receiver", "yixiaoshu88@163.com");
        this.smtpPassword = config.getProperty("advice.email.smtp.password", "");
    }
    
    /**
     * 加载邮件配置
     */
    private Properties loadEmailConfig() {
        Properties config = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("advice-email.properties")) {
            if (input != null) {
                config.load(input);
            }
        } catch (IOException e) {
            LOG.warn("Failed to load email config, using default values", e);
        }
        return config;
    }
    
    /**
     * 发送反馈邮件
     * @param feedbackType 反馈类型（功能建议、Bug反馈、使用体验等）
     * @param content 反馈内容
     * @param contactInfo 联系方式（可选）
     * @param project 当前项目
     */
    public void sendFeedback(String feedbackType, String content, String contactInfo, Project project) 
            throws MessagingException {
        
        // 检查限流
        if (!FeedbackRateLimiter.canSendToday()) {
            throw new MessagingException("今日反馈次数已达上限（" + FeedbackRateLimiter.getTodaySendCount() + "/" + 6 + "）");
        }
        
        // 配置SMTP属性
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        
        if (ENABLE_SSL) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(SMTP_PORT));
        }
        
        // 设置超时
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.connectiontimeout", "30000");
        
        // 创建会话
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, smtpPassword);
            }
        });
        
        // 创建邮件
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(senderEmail, "PandaCoder 用户", "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(senderEmail));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiverEmail));
        
        // 邮件主题
        String subject = String.format("【PandaCoder %s】%s", 
                feedbackType, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        message.setSubject(subject);
        
        // 邮件内容（HTML格式）
        String htmlContent = generateEmailHtml(feedbackType, content, contactInfo, project);
        message.setContent(htmlContent, "text/html; charset=utf-8");
        
        // 发送邮件
        Transport.send(message);
        
        // 记录发送
        FeedbackRateLimiter.recordSend();
        
        LOG.info("Feedback email sent successfully: " + feedbackType);
    }
    
    /**
     * 生成邮件HTML内容
     */
    private String generateEmailHtml(String feedbackType, String content, 
                                     String contactInfo, Project project) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='utf-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 20px auto; padding: 20px; background: #f9f9f9; border-radius: 8px; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }");
        html.append(".content { background: white; padding: 25px; border-radius: 0 0 8px 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(".label { color: #667eea; font-weight: bold; margin-top: 15px; margin-bottom: 5px; }");
        html.append(".value { background: #f5f7fa; padding: 12px; border-left: 3px solid #667eea; margin-bottom: 15px; white-space: pre-wrap; word-wrap: break-word; }");
        html.append(".footer { text-align: center; margin-top: 20px; color: #888; font-size: 12px; }");
        html.append(".emoji { font-size: 24px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        // 头部
        html.append("<div class='header'>");
        html.append("<div class='emoji'>").append(getEmojiByType(feedbackType)).append("</div>");
        html.append("<h2 style='margin: 10px 0;'>PandaCoder 用户反馈</h2>");
        html.append("<p style='margin: 5px 0; opacity: 0.9;'>").append(feedbackType).append("</p>");
        html.append("</div>");
        
        // 内容
        html.append("<div class='content'>");
        
        html.append("<div class='label'>📝 反馈内容：</div>");
        html.append("<div class='value'>").append(escapeHtml(content)).append("</div>");
        
        if (contactInfo != null && !contactInfo.trim().isEmpty()) {
            html.append("<div class='label'>📞 联系方式：</div>");
            html.append("<div class='value'>").append(escapeHtml(contactInfo)).append("</div>");
        }
        
        html.append("<div class='label'>⏰ 反馈时间：</div>");
        html.append("<div class='value'>").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"))).append("</div>");
        
        if (project != null && project.getBasePath() != null) {
            html.append("<div class='label'>📁 项目信息：</div>");
            html.append("<div class='value'>").append(escapeHtml(project.getName())).append("</div>");
        }
        
        html.append("</div>");
        
        // 底部
        html.append("<div class='footer'>");
        html.append("此邮件由 PandaCoder 插件自动发送<br/>");
        html.append("感谢您的反馈，让 PandaCoder 变得更好！💪");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 根据反馈类型获取表情符号
     */
    private String getEmojiByType(String feedbackType) {
        switch (feedbackType) {
            case "功能建议": return "💡";
            case "Bug反馈": return "🐛";
            case "使用体验": return "⭐";
            case "其他": return "💬";
            default: return "📨";
        }
    }
    
    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("\n", "<br/>");
    }
    
    /**
     * 测试SMTP连接
     */
    public boolean testConnection() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");
            
            if (ENABLE_SSL) {
                props.put("mail.smtp.ssl.enable", "true");
            }
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, smtpPassword);
                }
            });
            
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            
            LOG.info("SMTP connection test successful");
            return true;
        } catch (Exception e) {
            LOG.error("SMTP connection test failed", e);
            return false;
        }
    }
    
    /**
     * 获取接收者邮箱
     */
    public String getReceiverEmail() {
        return receiverEmail;
    }
}

