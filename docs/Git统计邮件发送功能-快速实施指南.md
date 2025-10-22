# Git 统计邮件发送功能 - 快速实施指南

## 🚀 快速开始

本指南提供了具体的代码实现示例，帮助您快速实施邮件发送功能。

---

## 📦 第一步：添加依赖

### 修改 `build.gradle`

```gradle
dependencies {
    // ... 现有依赖 ...
    
    // JavaMail API - 邮件发送核心库
    implementation 'com.sun.mail:javax.mail:1.6.2'
    
    // Jakarta Activation - JavaMail 依赖
    implementation 'com.sun.activation:jakarta.activation:1.2.2'
}
```

执行 Gradle 同步:
```bash
./gradlew build --refresh-dependencies
```

---

## 📁 第二步：创建项目结构

创建以下包和文件结构:

```
src/main/java/com/shuyixiao/gitstat/
├── email/                          # 新建邮件功能包
│   ├── model/                      # 数据模型
│   │   ├── GitStatEmailConfig.java
│   │   ├── GitStatEmailContent.java
│   │   └── GitStatEmailRecord.java
│   ├── service/                    # 服务层
│   │   ├── GitStatEmailService.java
│   │   └── EmailTemplateService.java
│   ├── ui/                         # UI 界面
│   │   ├── EmailConfigPanel.java
│   │   └── EmailHistoryPanel.java
│   └── util/                       # 工具类
│       ├── PasswordEncryptor.java
│       └── SmtpConfigValidator.java
└── ...

src/main/resources/
└── email/                          # 邮件模板资源
    ├── templates/
    │   ├── daily-report.html
    │   └── daily-report.txt
    └── presets/
        └── smtp-presets.json       # SMTP 预设配置
```

---

## 💻 第三步：核心代码实现

### 1. GitStatEmailConfig.java - 配置模型

```java
package com.shuyixiao.gitstat.email.model;

public class GitStatEmailConfig {
    // SMTP 配置
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private boolean enableTLS = true;
    private boolean enableSSL = false;
    
    // 认证信息
    private String senderEmail = "";
    private String senderPassword = "";  // 加密存储
    private String senderName = "Git Statistics";
    private String recipientEmail = "";
    
    // 定时配置
    private boolean enableScheduled = false;
    private String scheduledTime = "18:00";
    
    // 统计配置
    private String filterAuthor = null;  // null 表示所有作者
    private boolean includeTrends = true;
    private boolean sendHtml = true;
    private String emailSubject = "📊 Git 统计日报 - {DATE}";
    
    // Getters and Setters
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }
    
    public boolean isEnableTLS() { return enableTLS; }
    public void setEnableTLS(boolean enableTLS) { this.enableTLS = enableTLS; }
    
    public boolean isEnableSSL() { return enableSSL; }
    public void setEnableSSL(boolean enableSSL) { this.enableSSL = enableSSL; }
    
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    
    public String getSenderPassword() { return senderPassword; }
    public void setSenderPassword(String senderPassword) { this.senderPassword = senderPassword; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    
    public boolean isEnableScheduled() { return enableScheduled; }
    public void setEnableScheduled(boolean enableScheduled) { this.enableScheduled = enableScheduled; }
    
    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    
    public String getFilterAuthor() { return filterAuthor; }
    public void setFilterAuthor(String filterAuthor) { this.filterAuthor = filterAuthor; }
    
    public boolean isIncludeTrends() { return includeTrends; }
    public void setIncludeTrends(boolean includeTrends) { this.includeTrends = includeTrends; }
    
    public boolean isSendHtml() { return sendHtml; }
    public void setSendHtml(boolean sendHtml) { this.sendHtml = sendHtml; }
    
    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }
    
    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        return smtpHost != null && !smtpHost.isEmpty()
            && smtpPort > 0
            && senderEmail != null && !senderEmail.isEmpty()
            && senderPassword != null && !senderPassword.isEmpty()
            && recipientEmail != null && !recipientEmail.isEmpty();
    }
}
```

### 2. GitStatEmailContent.java - 邮件内容模型

```java
package com.shuyixiao.gitstat.email.model;

import com.shuyixiao.gitstat.model.GitDailyStat;
import java.time.LocalDate;
import java.util.Map;

public class GitStatEmailContent {
    private String authorName;
    private String authorEmail;
    private LocalDate statisticsDate;
    
    // 当日统计
    private int todayCommits;
    private int todayAdditions;
    private int todayDeletions;
    private int todayNetChanges;
    
    // 趋势数据
    private Map<LocalDate, GitDailyStat> last7Days;
    private Map<LocalDate, GitDailyStat> last30Days;
    
    // 排名信息
    private int rankByCommits = -1;
    private int rankByAdditions = -1;
    private int totalAuthors = 0;
    
    // Getters and Setters
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    
    public LocalDate getStatisticsDate() { return statisticsDate; }
    public void setStatisticsDate(LocalDate statisticsDate) { this.statisticsDate = statisticsDate; }
    
    public int getTodayCommits() { return todayCommits; }
    public void setTodayCommits(int todayCommits) { this.todayCommits = todayCommits; }
    
    public int getTodayAdditions() { return todayAdditions; }
    public void setTodayAdditions(int todayAdditions) { this.todayAdditions = todayAdditions; }
    
    public int getTodayDeletions() { return todayDeletions; }
    public void setTodayDeletions(int todayDeletions) { this.todayDeletions = todayDeletions; }
    
    public int getTodayNetChanges() { return todayNetChanges; }
    public void setTodayNetChanges(int todayNetChanges) { this.todayNetChanges = todayNetChanges; }
    
    public Map<LocalDate, GitDailyStat> getLast7Days() { return last7Days; }
    public void setLast7Days(Map<LocalDate, GitDailyStat> last7Days) { this.last7Days = last7Days; }
    
    public Map<LocalDate, GitDailyStat> getLast30Days() { return last30Days; }
    public void setLast30Days(Map<LocalDate, GitDailyStat> last30Days) { this.last30Days = last30Days; }
    
    public int getRankByCommits() { return rankByCommits; }
    public void setRankByCommits(int rankByCommits) { this.rankByCommits = rankByCommits; }
    
    public int getRankByAdditions() { return rankByAdditions; }
    public void setRankByAdditions(int rankByAdditions) { this.rankByAdditions = rankByAdditions; }
    
    public int getTotalAuthors() { return totalAuthors; }
    public void setTotalAuthors(int totalAuthors) { this.totalAuthors = totalAuthors; }
}
```

### 3. GitStatEmailService.java - 邮件服务核心

```java
package com.shuyixiao.gitstat.email.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.gitstat.email.model.GitStatEmailConfig;
import com.shuyixiao.gitstat.email.model.GitStatEmailContent;
import com.shuyixiao.gitstat.email.util.PasswordEncryptor;
import com.shuyixiao.gitstat.model.GitAuthorDailyStat;
import com.shuyixiao.gitstat.model.GitDailyStat;
import com.shuyixiao.gitstat.service.GitStatService;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service(Service.Level.PROJECT)
public final class GitStatEmailService {
    
    private static final Logger LOG = Logger.getInstance(GitStatEmailService.class);
    
    private final Project project;
    private final GitStatService gitStatService;
    private final EmailTemplateService templateService;
    
    private GitStatEmailConfig config;
    private ScheduledExecutorService scheduler;
    
    public GitStatEmailService(Project project) {
        this.project = project;
        this.gitStatService = project.getService(GitStatService.class);
        this.templateService = new EmailTemplateService();
        this.config = new GitStatEmailConfig();
    }
    
    /**
     * 手动发送今日统计邮件
     */
    public boolean sendTodayEmail() {
        return sendEmail(LocalDate.now());
    }
    
    /**
     * 发送指定日期的统计邮件
     */
    public boolean sendEmail(LocalDate date) {
        if (!config.isValid()) {
            LOG.warn("Email configuration is not valid");
            return false;
        }
        
        try {
            // 1. 收集统计数据
            GitStatEmailContent content = collectStatistics(date);
            
            // 2. 生成邮件内容
            String emailBody = templateService.generateEmailBody(content, config);
            
            // 3. 发送邮件
            sendViaSMTP(emailBody, date);
            
            LOG.info("Email sent successfully for date: " + date);
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to send email", e);
            return false;
        }
    }
    
    /**
     * 收集统计数据
     */
    private GitStatEmailContent collectStatistics(LocalDate date) {
        GitStatEmailContent content = new GitStatEmailContent();
        content.setStatisticsDate(date);
        
        String filterAuthor = config.getFilterAuthor();
        
        if (filterAuthor != null && !filterAuthor.isEmpty()) {
            // 筛选特定作者
            GitAuthorDailyStat authorStat = gitStatService.getAuthorDailyStats()
                .stream()
                .filter(s -> s.getDate().equals(date))
                .filter(s -> s.getAuthorName().equals(filterAuthor))
                .findFirst()
                .orElse(null);
            
            if (authorStat != null) {
                content.setAuthorName(authorStat.getAuthorName());
                content.setAuthorEmail(authorStat.getAuthorEmail());
                content.setTodayCommits(authorStat.getCommits());
                content.setTodayAdditions(authorStat.getAdditions());
                content.setTodayDeletions(authorStat.getDeletions());
                content.setTodayNetChanges(authorStat.getNetChanges());
            }
        } else {
            // 统计所有作者
            GitDailyStat dailyStat = gitStatService.getDailyStats()
                .stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst()
                .orElse(null);
            
            if (dailyStat != null) {
                content.setAuthorName("所有开发者");
                content.setAuthorEmail("");
                content.setTodayCommits(dailyStat.getCommits());
                content.setTodayAdditions(dailyStat.getAdditions());
                content.setTodayDeletions(dailyStat.getDeletions());
                content.setTodayNetChanges(dailyStat.getNetChanges());
            }
        }
        
        // 如果需要趋势分析
        if (config.isIncludeTrends()) {
            content.setLast7Days(getLast7DaysStats(date));
            content.setLast30Days(getLast30DaysStats(date));
        }
        
        // 计算排名（如果筛选了特定作者）
        if (filterAuthor != null && !filterAuthor.isEmpty()) {
            calculateRanking(content, filterAuthor);
        }
        
        return content;
    }
    
    /**
     * 获取最近7天统计
     */
    private Map<LocalDate, GitDailyStat> getLast7DaysStats(LocalDate endDate) {
        Map<LocalDate, GitDailyStat> stats = new LinkedHashMap<>();
        List<GitDailyStat> allStats = gitStatService.getDailyStats();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            GitDailyStat stat = allStats.stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst()
                .orElse(new GitDailyStat(date));
            stats.put(date, stat);
        }
        
        return stats;
    }
    
    /**
     * 获取最近30天统计
     */
    private Map<LocalDate, GitDailyStat> getLast30DaysStats(LocalDate endDate) {
        Map<LocalDate, GitDailyStat> stats = new LinkedHashMap<>();
        List<GitDailyStat> allStats = gitStatService.getDailyStats();
        
        for (int i = 29; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            GitDailyStat stat = allStats.stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst()
                .orElse(new GitDailyStat(date));
            stats.put(date, stat);
        }
        
        return stats;
    }
    
    /**
     * 计算作者排名
     */
    private void calculateRanking(GitStatEmailContent content, String authorName) {
        // 按提交次数排名
        List<String> authorsByCommits = new ArrayList<>();
        gitStatService.getAuthorStatsSortedByCommits().forEach(a -> 
            authorsByCommits.add(a.getAuthorName())
        );
        content.setRankByCommits(authorsByCommits.indexOf(authorName) + 1);
        
        // 按代码量排名
        List<String> authorsByAdditions = new ArrayList<>();
        gitStatService.getAuthorStatsSortedByAdditions().forEach(a -> 
            authorsByAdditions.add(a.getAuthorName())
        );
        content.setRankByAdditions(authorsByAdditions.indexOf(authorName) + 1);
        
        // 总作者数
        content.setTotalAuthors(gitStatService.getAuthorStats().size());
    }
    
    /**
     * 通过 SMTP 发送邮件
     */
    private void sendViaSMTP(String emailBody, LocalDate date) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.auth", "true");
        
        if (config.isEnableTLS()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (config.isEnableSSL()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        
        // 创建会话
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String decryptedPassword = PasswordEncryptor.decrypt(
                    config.getSenderPassword(), 
                    project
                );
                return new PasswordAuthentication(
                    config.getSenderEmail(), 
                    decryptedPassword
                );
            }
        });
        
        // 创建消息
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(
            config.getSenderEmail(), 
            config.getSenderName()
        ));
        message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(config.getRecipientEmail())
        );
        
        // 设置主题（替换日期占位符）
        String subject = config.getEmailSubject()
            .replace("{DATE}", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        message.setSubject(subject);
        
        // 设置内容
        if (config.isSendHtml()) {
            message.setContent(emailBody, "text/html; charset=utf-8");
        } else {
            message.setText(emailBody);
        }
        
        // 发送
        Transport.send(message);
    }
    
    /**
     * 启动定时任务
     */
    public void startScheduledTask() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (!config.isEnableScheduled() || !config.isValid()) {
            return;
        }
        
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 计算首次执行延迟
        long initialDelay = calculateInitialDelay();
        long period = TimeUnit.DAYS.toMillis(1);
        
        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    // 发送昨天的统计
                    sendEmail(LocalDate.now().minusDays(1));
                } catch (Exception e) {
                    LOG.error("Scheduled email task failed", e);
                }
            },
            initialDelay,
            period,
            TimeUnit.MILLISECONDS
        );
        
        LOG.info("Scheduled email task started, first run in " + 
                 (initialDelay / 1000 / 60) + " minutes");
    }
    
    /**
     * 停止定时任务
     */
    public void stopScheduledTask() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
            LOG.info("Scheduled email task stopped");
        }
    }
    
    /**
     * 计算首次执行延迟（毫秒）
     */
    private long calculateInitialDelay() {
        String[] parts = config.getScheduledTime().split(":");
        int targetHour = Integer.parseInt(parts[0]);
        int targetMinute = Integer.parseInt(parts[1]);
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime nextRun = now
            .withHour(targetHour)
            .withMinute(targetMinute)
            .withSecond(0)
            .withNano(0);
        
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        
        return java.time.Duration.between(now, nextRun).toMillis();
    }
    
    /**
     * 测试 SMTP 连接
     */
    public boolean testConnection() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getSmtpHost());
            props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
            props.put("mail.smtp.auth", "true");
            
            if (config.isEnableTLS()) {
                props.put("mail.smtp.starttls.enable", "true");
            }
            if (config.isEnableSSL()) {
                props.put("mail.smtp.ssl.enable", "true");
            }
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String decryptedPassword = PasswordEncryptor.decrypt(
                        config.getSenderPassword(), 
                        project
                    );
                    return new PasswordAuthentication(
                        config.getSenderEmail(), 
                        decryptedPassword
                    );
                }
            });
            
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            
            return true;
        } catch (Exception e) {
            LOG.error("SMTP connection test failed", e);
            return false;
        }
    }
    
    // Getters and Setters
    public GitStatEmailConfig getConfig() {
        return config;
    }
    
    public void setConfig(GitStatEmailConfig config) {
        this.config = config;
    }
}
```

### 4. PasswordEncryptor.java - 密码加密工具

```java
package com.shuyixiao.gitstat.email.util;

import com.intellij.openapi.project.Project;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class PasswordEncryptor {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * 生成项目唯一密钥
     */
    private static SecretKey getKey(Project project) {
        String seed = project.getBasePath() + "GitStatEmail";
        byte[] keyBytes = Arrays.copyOf(
            seed.getBytes(StandardCharsets.UTF_8), 
            16
        );
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 加密密码
     */
    public static String encrypt(String password, Project project) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(
                password.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }
    
    /**
     * 解密密码
     */
    public static String decrypt(String encryptedPassword, Project project) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }
        
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(
                Base64.getDecoder().decode(encryptedPassword)
            );
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }
}
```

---

## 📧 第四步：邮件模板

### HTML 模板简化版（`src/main/resources/email/templates/daily-report.html`）

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; overflow: hidden; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 30px; text-align: center; }
        .content { padding: 30px; }
        .stat-card { background: #f8f9fa; border-radius: 6px; padding: 20px; margin-bottom: 20px; }
        .stat-row { display: flex; justify-content: space-around; margin: 15px 0; }
        .stat-item { text-align: center; }
        .stat-label { color: #666; font-size: 14px; }
        .stat-value { font-size: 28px; font-weight: bold; color: #333; margin-top: 8px; }
        .positive { color: #28a745; }
        .negative { color: #dc3545; }
        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 Git 统计日报</h1>
            <p>{{DATE}} | {{AUTHOR_NAME}}</p>
        </div>
        <div class="content">
            <div class="stat-card">
                <h2 style="margin-top: 0;">🎯 今日概览</h2>
                <div class="stat-row">
                    <div class="stat-item">
                        <div class="stat-label">提交次数</div>
                        <div class="stat-value">{{COMMITS}}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">新增代码</div>
                        <div class="stat-value positive">+{{ADDITIONS}}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">删除代码</div>
                        <div class="stat-value negative">-{{DELETIONS}}</div>
                    </div>
                </div>
                <div style="text-align: center; margin-top: 20px;">
                    <strong>净变化: {{NET_CHANGES}} 行</strong>
                </div>
            </div>
            {{TREND_SECTION}}
            {{RANKING_SECTION}}
        </div>
        <div class="footer">
            <p>此邮件由 PandaCoder Git 统计工具自动生成</p>
            <p>{{TIMESTAMP}}</p>
        </div>
    </div>
</body>
</html>
```

### 纯文本模板（`src/main/resources/email/templates/daily-report.txt`）

```text
========================================
📊 Git 统计日报
========================================
日期: {{DATE}}
作者: {{AUTHOR_NAME}}

🎯 今日概览
----------------------------------------
提交次数: {{COMMITS}}
新增代码: +{{ADDITIONS}} 行
删除代码: -{{DELETIONS}} 行
净变化:   {{NET_CHANGES}} 行

{{TREND_SECTION}}

{{RANKING_SECTION}}

========================================
此邮件由 PandaCoder Git 统计工具自动生成
{{TIMESTAMP}}
========================================
```

---

## 🎨 第五步：UI 界面集成

### 在 GitStatToolWindow 中添加邮件报告标签页

修改 `GitStatToolWindow.java`:

```java
// 在 initializeUI() 方法中添加
tabbedPane.addTab("📧 邮件报告", createEmailReportPanel());

/**
 * 创建邮件报告面板
 */
private JComponent createEmailReportPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(JBUI.Borders.empty(10));
    
    // 创建配置区域
    JPanel configPanel = createEmailConfigPanel();
    
    // 创建操作按钮区域
    JPanel actionPanel = createEmailActionPanel();
    
    // 组合布局
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(configPanel, BorderLayout.CENTER);
    topPanel.add(actionPanel, BorderLayout.SOUTH);
    
    panel.add(topPanel, BorderLayout.NORTH);
    
    return panel;
}

/**
 * 创建邮件配置面板
 */
private JPanel createEmailConfigPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(JBUI.Borders.empty(10));
    
    // SMTP 配置
    JTextField smtpHostField = new JTextField(20);
    JTextField smtpPortField = new JTextField(5);
    JTextField senderEmailField = new JTextField(20);
    JPasswordField senderPasswordField = new JPasswordField(20);
    JTextField recipientEmailField = new JTextField(20);
    
    JCheckBox tlsCheckBox = new JCheckBox("启用 TLS", true);
    JCheckBox sslCheckBox = new JCheckBox("启用 SSL", false);
    
    // 定时发送配置
    JCheckBox enableScheduledCheckBox = new JCheckBox("启用每日定时发送", false);
    JTextField scheduledTimeField = new JTextField("18:00", 5);
    
    // 添加组件到面板
    panel.add(createLabeledField("SMTP服务器:", smtpHostField));
    panel.add(createLabeledField("端口:", smtpPortField));
    panel.add(createLabeledField("发送者邮箱:", senderEmailField));
    panel.add(createLabeledField("SMTP密码:", senderPasswordField));
    panel.add(createLabeledField("接收者邮箱:", recipientEmailField));
    
    JPanel checksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    checksPanel.add(tlsCheckBox);
    checksPanel.add(sslCheckBox);
    panel.add(checksPanel);
    
    panel.add(Box.createVerticalStrut(10));
    panel.add(enableScheduledCheckBox);
    panel.add(createLabeledField("发送时间:", scheduledTimeField));
    
    return panel;
}

/**
 * 创建邮件操作按钮面板
 */
private JPanel createEmailActionPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    
    JButton testButton = new JButton("测试连接");
    JButton saveButton = new JButton("保存配置");
    JButton sendButton = new JButton("📧 立即发送");
    JButton previewButton = new JButton("👀 预览邮件");
    
    testButton.addActionListener(e -> testEmailConnection());
    saveButton.addActionListener(e -> saveEmailConfig());
    sendButton.addActionListener(e -> sendEmailNow());
    previewButton.addActionListener(e -> previewEmail());
    
    panel.add(testButton);
    panel.add(saveButton);
    panel.add(sendButton);
    panel.add(previewButton);
    
    return panel;
}

private JPanel createLabeledField(String label, JComponent field) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.add(new JLabel(label));
    panel.add(field);
    return panel;
}
```

---

## 🔧 第六步：常见邮箱配置

### Gmail 配置示例

```java
// SMTP 服务器配置
smtpHost = "smtp.gmail.com"
smtpPort = 587
enableTLS = true
enableSSL = false

// 重要提示:
// 1. 需要在 Google 账户中启用"两步验证"
// 2. 生成"应用专用密码"用于 SMTP 认证
// 3. 不要使用 Gmail 登录密码
```

### QQ 邮箱配置示例

```java
// SMTP 服务器配置
smtpHost = "smtp.qq.com"
smtpPort = 587  // 或 465（SSL）
enableTLS = true  // 587 端口使用
enableSSL = false // 或 true（465 端口使用）

// 重要提示:
// 1. 登录 QQ 邮箱网页版
// 2. 设置 -> 账户 -> 开启 SMTP 服务
// 3. 生成授权码（不是QQ密码）
```

### 163 邮箱配置示例

```java
// SMTP 服务器配置
smtpHost = "smtp.163.com"
smtpPort = 465
enableTLS = false
enableSSL = true

// 重要提示:
// 1. 登录 163 邮箱网页版
// 2. 设置 -> POP3/SMTP/IMAP -> 开启 SMTP 服务
// 3. 设置客户端授权密码
```

---

## ✅ 第七步：测试和验证

### 测试步骤

1. **配置 SMTP 信息**
   - 填写正确的 SMTP 服务器和端口
   - 设置发送者和接收者邮箱
   - 输入授权码/应用专用密码

2. **测试连接**
   - 点击"测试连接"按钮
   - 验证 SMTP 配置是否正确

3. **预览邮件**
   - 点击"预览邮件"查看生成的邮件内容
   - 确认统计数据正确

4. **手动发送**
   - 点击"立即发送"测试邮件发送功能
   - 检查接收邮箱是否收到邮件

5. **定时任务**
   - 启用定时发送
   - 等待指定时间验证自动发送

---

## 🐛 常见问题排查

### 1. 发送失败：Authentication failed

**原因**: SMTP 密码错误或未启用授权

**解决**:
- Gmail: 使用应用专用密码，非 Gmail 登录密码
- QQ: 使用授权码，非 QQ 密码
- 163: 使用客户端授权密码

### 2. 发送失败：Connection timeout

**原因**: 网络问题或端口被防火墙阻止

**解决**:
- 检查网络连接
- 尝试不同端口（587 TLS / 465 SSL）
- 检查防火墙设置

### 3. 邮件内容乱码

**原因**: 编码问题

**解决**:
```java
message.setContent(emailBody, "text/html; charset=utf-8");
```

### 4. 定时任务不执行

**原因**: IDE 关闭或服务未启动

**解决**:
- 确保 IDE 保持运行
- 检查定时任务是否正确配置
- 查看日志确认任务状态

---

## 📚 下一步扩展

完成基础功能后，可以考虑以下扩展:

1. ✅ 添加周报/月报功能
2. ✅ 支持多个接收者
3. ✅ 添加附件（CSV格式数据）
4. ✅ 支持自定义邮件模板
5. ✅ 添加邮件发送历史记录
6. ✅ 集成钉钉/企业微信 Webhook

---

## 🎯 快速检查清单

- [ ] 添加 JavaMail 依赖
- [ ] 创建邮件相关包和类
- [ ] 实现邮件配置模型
- [ ] 实现邮件发送服务
- [ ] 创建 HTML 和纯文本模板
- [ ] 添加密码加密功能
- [ ] 集成到工具窗口 UI
- [ ] 实现定时任务
- [ ] 测试各大邮箱服务商
- [ ] 添加错误处理和日志

---

**最后更新**: 2025-10-22
**版本**: 1.0

