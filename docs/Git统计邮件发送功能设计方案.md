# Git 统计邮件发送功能设计方案

## 📋 目录

1. [功能概述](#功能概述)
2. [技术架构](#技术架构)
3. [详细设计](#详细设计)
4. [实现步骤](#实现步骤)
5. [配置界面](#配置界面)
6. [邮件模板](#邮件模板)
7. [安全性考虑](#安全性考虑)
8. [扩展性设计](#扩展性设计)

---

## 功能概述

### 核心功能
为 Git 统计工具窗口增加**邮件发送功能**，支持自动统计每日代码编写情况并通过邮件发送报告。

### 主要特性
1. ✅ **定时发送**：支持每日定时发送（可配置发送时间）
2. ✅ **手动发送**：工具窗口提供"立即发送邮件"按钮
3. ✅ **统计内容**：
   - 当日提交次数、新增行数、删除行数
   - 当日净代码变化量
   - 近7天/30天趋势统计
   - 个人贡献排名
4. ✅ **邮件配置**：支持 SMTP 服务器配置（Gmail、QQ邮箱、163邮箱等）
5. ✅ **多格式支持**：HTML 精美邮件 + 纯文本备选
6. ✅ **发送历史**：记录邮件发送历史，可查看过往报告
7. ✅ **筛选功能**：支持按作者筛选，只统计指定作者的数据

---

## 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                   Git 统计工具窗口 UI                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │  统计展示    │  │  邮件配置    │  │  发送历史       │  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │  手动发送    │  │  定时设置    │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                 GitStatEmailService (核心服务)               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  • 数据收集与格式化                                   │  │
│  │  • 邮件内容生成（HTML模板引擎）                      │  │
│  │  • SMTP 邮件发送                                      │  │
│  │  • 定时任务调度                                       │  │
│  │  • 发送历史记录                                       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    数据层 & 外部服务                         │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ GitStatService│  │ SMTP Server  │  │  本地存储       │  │
│  │  (已有服务)   │  │ (Gmail/QQ等) │  │  (发送历史)     │  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 技术选型

| 组件 | 技术选择 | 说明 |
|------|---------|------|
| **邮件发送** | JavaMail API | Java 标准邮件发送库 |
| **定时任务** | IntelliJ Scheduled Tasks | IDE 内置定时任务框架 |
| **HTML 模板** | 内置模板引擎 | 简单的字符串替换或 Velocity |
| **配置存储** | IntelliJ PersistentStateComponent | IDE 配置持久化 |
| **密码加密** | AES 加密 | 保护 SMTP 密码安全 |
| **历史记录** | JSON 文件 | 轻量级本地存储 |

---

## 详细设计

### 1. 数据模型

#### GitStatEmailConfig（邮件配置）

```java
public class GitStatEmailConfig {
    // SMTP 服务器配置
    private String smtpHost;           // 如：smtp.gmail.com
    private int smtpPort;              // 如：587（TLS）或 465（SSL）
    private boolean enableTLS;         // 是否启用 TLS
    private boolean enableSSL;         // 是否启用 SSL
    
    // 发送者信息
    private String senderEmail;        // 发送者邮箱
    private String senderPassword;     // SMTP 密码（加密存储）
    private String senderName;         // 发送者名称
    
    // 接收者信息
    private String recipientEmail;     // 接收者邮箱
    
    // 定时发送配置
    private boolean enableScheduled;   // 是否启用定时发送
    private String scheduledTime;      // 发送时间（如：18:00）
    
    // 统计配置
    private String filterAuthor;       // 筛选作者（为空则统计所有）
    private int statisticsDays;        // 统计天数（默认1，当日）
    private boolean includeTrends;     // 是否包含趋势分析（7天/30天）
    
    // 邮件内容配置
    private boolean sendHtml;          // 是否发送 HTML 邮件
    private String emailSubject;       // 邮件主题模板
}
```

#### GitStatEmailRecord（发送记录）

```java
public class GitStatEmailRecord {
    private String recordId;           // 记录ID
    private LocalDateTime sendTime;    // 发送时间
    private String recipient;          // 接收者
    private String subject;            // 邮件主题
    private boolean success;           // 是否成功
    private String errorMessage;       // 错误信息（如有）
    
    // 统计数据快照
    private int commits;               // 提交次数
    private int additions;             // 新增行数
    private int deletions;             // 删除行数
    private int netChanges;            // 净变化
}
```

#### GitStatEmailContent（邮件内容）

```java
public class GitStatEmailContent {
    private String authorName;         // 作者名称
    private LocalDate statisticsDate;  // 统计日期
    
    // 当日数据
    private int todayCommits;
    private int todayAdditions;
    private int todayDeletions;
    private int todayNetChanges;
    
    // 趋势数据（可选）
    private Map<LocalDate, GitDailyStat> last7Days;
    private Map<LocalDate, GitDailyStat> last30Days;
    
    // 排名信息
    private int rankByCommits;         // 提交次数排名
    private int rankByAdditions;       // 代码量排名
}
```

---

### 2. 核心服务

#### GitStatEmailService

```java
@Service(Service.Level.PROJECT)
public final class GitStatEmailService {
    
    private final Project project;
    private final GitStatService gitStatService;
    private ScheduledExecutorService scheduler;
    
    /**
     * 初始化服务
     */
    public void initialize() {
        // 加载配置
        loadConfig();
        
        // 启动定时任务（如果已配置）
        if (config.isEnableScheduled()) {
            scheduleDaily();
        }
    }
    
    /**
     * 手动发送邮件
     */
    public boolean sendEmailManually() {
        return sendEmail(LocalDate.now());
    }
    
    /**
     * 发送指定日期的统计邮件
     */
    public boolean sendEmail(LocalDate date) {
        try {
            // 1. 收集统计数据
            GitStatEmailContent content = collectStatistics(date);
            
            // 2. 生成邮件内容
            String emailBody = generateEmailContent(content);
            
            // 3. 发送邮件
            sendViaSMTP(emailBody);
            
            // 4. 记录发送历史
            recordSendHistory(content, true, null);
            
            return true;
        } catch (Exception e) {
            recordSendHistory(null, false, e.getMessage());
            return false;
        }
    }
    
    /**
     * 收集统计数据
     */
    private GitStatEmailContent collectStatistics(LocalDate date) {
        GitStatEmailContent content = new GitStatEmailContent();
        
        // 获取当日统计
        if (config.getFilterAuthor() != null) {
            // 筛选特定作者
            GitAuthorDailyStat authorStat = gitStatService
                .getAuthorDailyStats()
                .stream()
                .filter(s -> s.getDate().equals(date))
                .filter(s -> s.getAuthorName().equals(config.getFilterAuthor()))
                .findFirst()
                .orElse(null);
            
            if (authorStat != null) {
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
        
        return content;
    }
    
    /**
     * 生成邮件内容（HTML格式）
     */
    private String generateEmailContent(GitStatEmailContent content) {
        if (config.isSendHtml()) {
            return generateHtmlEmail(content);
        } else {
            return generatePlainTextEmail(content);
        }
    }
    
    /**
     * 通过 SMTP 发送邮件
     */
    private void sendViaSMTP(String emailBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());
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
                return new PasswordAuthentication(
                    config.getSenderEmail(),
                    decryptPassword(config.getSenderPassword())
                );
            }
        });
        
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(
            config.getSenderEmail(), 
            config.getSenderName()
        ));
        message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(config.getRecipientEmail())
        );
        message.setSubject(config.getEmailSubject());
        
        if (config.isSendHtml()) {
            message.setContent(emailBody, "text/html; charset=utf-8");
        } else {
            message.setText(emailBody);
        }
        
        Transport.send(message);
    }
    
    /**
     * 定时任务调度
     */
    private void scheduleDaily() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 计算首次执行延迟
        LocalTime scheduledTime = LocalTime.parse(config.getScheduledTime());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(scheduledTime.getHour())
                                   .withMinute(scheduledTime.getMinute())
                                   .withSecond(0);
        
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        
        long initialDelay = Duration.between(now, nextRun).toMillis();
        long period = TimeUnit.DAYS.toMillis(1);
        
        scheduler.scheduleAtFixedRate(
            () -> sendEmail(LocalDate.now().minusDays(1)),
            initialDelay,
            period,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * 记录发送历史
     */
    private void recordSendHistory(GitStatEmailContent content, 
                                   boolean success, 
                                   String errorMessage) {
        GitStatEmailRecord record = new GitStatEmailRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setSendTime(LocalDateTime.now());
        record.setRecipient(config.getRecipientEmail());
        record.setSubject(config.getEmailSubject());
        record.setSuccess(success);
        record.setErrorMessage(errorMessage);
        
        if (content != null) {
            record.setCommits(content.getTodayCommits());
            record.setAdditions(content.getTodayAdditions());
            record.setDeletions(content.getTodayDeletions());
            record.setNetChanges(content.getTodayNetChanges());
        }
        
        // 保存到本地 JSON 文件
        saveRecordToFile(record);
    }
}
```

---

### 3. HTML 邮件模板

#### 精美 HTML 模板设计

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background-color: #f5f5f5;
            margin: 0;
            padding: 20px;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 28px;
        }
        .header p {
            margin: 10px 0 0 0;
            opacity: 0.9;
        }
        .content {
            padding: 30px;
        }
        .stat-card {
            background-color: #f8f9fa;
            border-radius: 6px;
            padding: 20px;
            margin-bottom: 20px;
        }
        .stat-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 15px;
        }
        .stat-label {
            color: #666;
            font-size: 14px;
        }
        .stat-value {
            font-size: 24px;
            font-weight: bold;
            color: #333;
        }
        .stat-value.positive {
            color: #28a745;
        }
        .stat-value.negative {
            color: #dc3545;
        }
        .chart-container {
            margin: 20px 0;
        }
        .bar-chart {
            display: flex;
            align-items: flex-end;
            height: 150px;
            gap: 8px;
        }
        .bar {
            flex: 1;
            background: linear-gradient(to top, #667eea, #764ba2);
            border-radius: 4px 4px 0 0;
            position: relative;
        }
        .bar-label {
            position: absolute;
            bottom: -25px;
            width: 100%;
            text-align: center;
            font-size: 11px;
            color: #666;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        .badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
        }
        .badge-success {
            background-color: #d4edda;
            color: #155724;
        }
        .badge-info {
            background-color: #d1ecf1;
            color: #0c5460;
        }
    </style>
</head>
<body>
    <div class="container">
        <!-- Header -->
        <div class="header">
            <h1>📊 Git 统计日报</h1>
            <p>{{DATE}} | {{AUTHOR_NAME}}</p>
        </div>
        
        <!-- Content -->
        <div class="content">
            <!-- Today's Summary -->
            <div class="stat-card">
                <h2 style="margin-top: 0; color: #333;">🎯 今日概览</h2>
                <div class="stat-row">
                    <div>
                        <div class="stat-label">提交次数</div>
                        <div class="stat-value">{{COMMITS}}</div>
                    </div>
                    <div>
                        <div class="stat-label">新增代码</div>
                        <div class="stat-value positive">+{{ADDITIONS}}</div>
                    </div>
                    <div>
                        <div class="stat-label">删除代码</div>
                        <div class="stat-value negative">-{{DELETIONS}}</div>
                    </div>
                </div>
                <div style="text-align: center; margin-top: 20px;">
                    <span class="badge badge-success">
                        净变化: {{NET_CHANGES}} 行
                    </span>
                </div>
            </div>
            
            <!-- 7-Day Trend -->
            <div class="stat-card">
                <h2 style="margin-top: 0; color: #333;">📈 近7天趋势</h2>
                <div class="chart-container">
                    <div class="bar-chart">
                        {{7_DAY_BARS}}
                    </div>
                </div>
            </div>
            
            <!-- Achievements -->
            <div class="stat-card">
                <h2 style="margin-top: 0; color: #333;">🏆 统计成就</h2>
                <p>
                    <span class="badge badge-info">提交排名: 第 {{RANK_COMMITS}} 名</span>
                    <span class="badge badge-info">代码量排名: 第 {{RANK_ADDITIONS}} 名</span>
                </p>
            </div>
        </div>
        
        <!-- Footer -->
        <div class="footer">
            <p>此邮件由 PandaCoder Git 统计工具自动生成</p>
            <p>{{TIMESTAMP}}</p>
        </div>
    </div>
</body>
</html>
```

#### 纯文本邮件模板

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

📈 近7天趋势
----------------------------------------
{{7_DAY_TEXT_CHART}}

🏆 统计排名
----------------------------------------
提交次数排名: 第 {{RANK_COMMITS}} 名
代码量排名:   第 {{RANK_ADDITIONS}} 名

========================================
此邮件由 PandaCoder Git 统计工具自动生成
{{TIMESTAMP}}
========================================
```

---

### 4. UI 界面设计

#### 工具窗口新增功能

在现有的 `GitStatToolWindow` 中添加"邮件报告"标签页：

```java
// 在 initializeUI() 方法中添加
tabbedPane.addTab("📧 邮件报告", createEmailReportPanel());
```

#### 邮件报告面板布局

```
┌─────────────────────────────────────────────────────────────┐
│  📧 邮件报告                                                  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─ 邮件配置 ─────────────────────────────────────────────┐ │
│  │                                                          │ │
│  │  SMTP 服务器:  [smtp.gmail.com        ] 端口: [587]    │ │
│  │  发送者邮箱:   [your@gmail.com         ]                │ │
│  │  SMTP 密码:    [••••••••••••••         ]                │ │
│  │  接收者邮箱:   [recipient@email.com    ]                │ │
│  │                                                          │ │
│  │  ☑ 启用 TLS    ☐ 启用 SSL                              │ │
│  │                                                          │ │
│  │  [测试连接]  [保存配置]                                 │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 定时发送 ─────────────────────────────────────────────┐ │
│  │                                                          │ │
│  │  ☑ 启用每日定时发送                                     │ │
│  │  发送时间: [18:00]  (每天的 18:00 发送昨日统计)        │ │
│  │                                                          │ │
│  │  筛选作者: [张三          ▼]  (留空=所有作者)          │ │
│  │  包含趋势: ☑ 7天趋势  ☑ 30天趋势                       │ │
│  │                                                          │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 手动发送 ─────────────────────────────────────────────┐ │
│  │                                                          │ │
│  │  选择日期: [2025-10-21    📅]                          │ │
│  │                                                          │ │
│  │  [📧 立即发送邮件]  [👀 预览邮件]                       │ │
│  │                                                          │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 发送历史 ─────────────────────────────────────────────┐ │
│  │ 时间           │ 接收者           │ 状态  │ 提交  │ 代码 │ │
│  │────────────────┼──────────────────┼───────┼───────┼──────│ │
│  │ 2025-10-21 18:00│ user@email.com  │ ✅成功│ 12    │ +450 │ │
│  │ 2025-10-20 18:00│ user@email.com  │ ✅成功│ 8     │ +320 │ │
│  │ 2025-10-19 18:00│ user@email.com  │ ❌失败│ -     │ -    │ │
│  │                                                          │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

### 5. 配置管理

#### 配置存储（使用 IntelliJ PersistentStateComponent）

```java
@State(
    name = "GitStatEmailConfig",
    storages = @Storage("gitStatEmailConfig.xml")
)
public class GitStatEmailConfigState implements PersistentStateComponent<GitStatEmailConfigState> {
    
    // 配置字段
    public String smtpHost = "smtp.gmail.com";
    public int smtpPort = 587;
    public boolean enableTLS = true;
    public boolean enableSSL = false;
    
    public String senderEmail = "";
    public String senderPassword = "";  // 加密存储
    public String senderName = "Git Stats";
    
    public String recipientEmail = "";
    
    public boolean enableScheduled = false;
    public String scheduledTime = "18:00";
    
    public String filterAuthor = "";
    public boolean includeTrends = true;
    public boolean sendHtml = true;
    public String emailSubject = "📊 Git 统计日报 - {DATE}";
    
    @Override
    public GitStatEmailConfigState getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull GitStatEmailConfigState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    public static GitStatEmailConfigState getInstance(Project project) {
        return project.getService(GitStatEmailConfigState.class);
    }
}
```

---

### 6. 安全性设计

#### SMTP 密码加密

```java
public class PasswordEncryptor {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // 使用项目唯一的密钥（基于项目路径生成）
    private static SecretKey getKey(Project project) throws Exception {
        String projectPath = project.getBasePath();
        byte[] keyBytes = Arrays.copyOf(
            projectPath.getBytes(StandardCharsets.UTF_8), 
            16
        );
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 加密密码
     */
    public static String encrypt(String password, Project project) {
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }
    
    /**
     * 解密密码
     */
    public static String decrypt(String encryptedPassword, Project project) {
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }
}
```

---

## 实现步骤

### 阶段一：基础邮件发送（1-2天）

1. ✅ 添加 JavaMail 依赖到 `build.gradle`
2. ✅ 创建 `GitStatEmailConfig` 模型
3. ✅ 创建 `GitStatEmailService` 服务
4. ✅ 实现基础 SMTP 邮件发送
5. ✅ 实现纯文本邮件模板
6. ✅ 添加配置 UI 界面
7. ✅ 实现"立即发送"功能

### 阶段二：HTML 邮件和模板（1天）

1. ✅ 设计 HTML 邮件模板
2. ✅ 实现模板变量替换
3. ✅ 添加趋势图表生成
4. ✅ 实现邮件预览功能

### 阶段三：定时任务（1天）

1. ✅ 实现定时任务调度
2. ✅ 添加定时配置 UI
3. ✅ 实现启动时恢复定时任务
4. ✅ 添加任务状态显示

### 阶段四：历史记录和优化（1天）

1. ✅ 实现发送历史记录
2. ✅ 添加历史查看 UI
3. ✅ 实现密码加密存储
4. ✅ 添加错误处理和日志
5. ✅ 性能优化和测试

### 阶段五：高级功能（可选，1-2天）

1. ✅ 支持多接收者
2. ✅ 支持附件（CSV 格式导出）
3. ✅ 支持自定义邮件模板
4. ✅ 添加邮件发送成功通知
5. ✅ 支持邮件模板预设（Gmail、QQ、163 等）

---

## 配置示例

### Gmail 配置

```
SMTP 服务器: smtp.gmail.com
端口: 587
启用 TLS: ✅
启用 SSL: ☐
发送者邮箱: your@gmail.com
SMTP 密码: 应用专用密码（不是Gmail密码）
```

> **注意**: Gmail 需要开启"允许安全性较低的应用"或使用"应用专用密码"

### QQ 邮箱配置

```
SMTP 服务器: smtp.qq.com
端口: 587 或 465（SSL）
启用 TLS: ✅（587端口）
启用 SSL: ✅（465端口）
发送者邮箱: your@qq.com
SMTP 密码: QQ邮箱授权码（不是QQ密码）
```

### 163 邮箱配置

```
SMTP 服务器: smtp.163.com
端口: 465
启用 TLS: ☐
启用 SSL: ✅
发送者邮箱: your@163.com
SMTP 密码: 163邮箱授权码
```

---

## 扩展性设计

### 未来可扩展功能

1. **多人报告**: 支持发送团队统计报告给多个接收者
2. **周报/月报**: 支持周报、月报定时发送
3. **自定义图表**: 支持更丰富的数据可视化
4. **对比分析**: 支持与上周/上月数据对比
5. **项目维度**: 支持多项目统计汇总
6. **Webhook 集成**: 支持推送到钉钉、企业微信等
7. **PDF 导出**: 支持生成 PDF 格式报告
8. **数据分析**: 基于历史数据进行趋势分析和预测

---

## 总结

本方案设计了一个完整的 Git 统计邮件发送功能，包括：

✅ **核心功能**: 手动发送 + 定时发送
✅ **精美模板**: HTML 邮件 + 纯文本备选
✅ **安全可靠**: 密码加密 + 错误处理
✅ **易于使用**: 直观的配置界面 + 预设模板
✅ **可扩展**: 模块化设计，便于后续扩展

**预计开发时间**: 4-6天（核心功能）

---

## 附录：依赖配置

### build.gradle 需要添加的依赖

```gradle
dependencies {
    // 现有依赖...
    
    // JavaMail API - 邮件发送
    implementation 'com.sun.mail:javax.mail:1.6.2'
    
    // 可选：Velocity 模板引擎（如果需要复杂模板）
    // implementation 'org.apache.velocity:velocity-engine-core:2.3'
}
```

---

**文档创建时间**: 2025-10-22
**设计者**: PandaCoder AI Assistant
**版本**: 1.0

