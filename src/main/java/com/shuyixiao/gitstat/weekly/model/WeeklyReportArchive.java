package com.shuyixiao.gitstat.weekly.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 周报归档数据模型
 * 用于存储周报的完整信息，包括周报内容、生成时间、作者等
 * 
 * @author PandaCoder Team
 * @since 2.2.0
 */
public class WeeklyReportArchive {
    
    // 周报唯一标识（可选，MongoDB会自动生成_id）
    private String id;
    
    // 周报内容
    private String reportContent;
    
    // Git提交日志（原始数据）
    private String commits;
    
    // 生成时间
    private LocalDateTime generatedTime;
    
    // 周范围 - 开始日期
    private LocalDate weekStartDate;
    
    // 周范围 - 结束日期
    private LocalDate weekEndDate;
    
    // 作者筛选条件（null表示全部作者）
    private String authorFilter;
    
    // 项目名称
    private String projectName;
    
    // AI模型信息
    private String aiModel;
    
    // API地址
    private String apiUrl;
    
    // 提交统计信息
    private int totalCommits;
    
    // 作者数量
    private int totalAuthors;

    // ==================== 用户身份信息 ====================

    // 设备唯一标识（基于MAC地址的SHA-256哈希值）
    private String deviceId;

    // 用户自定义用户名
    private String userName;

    // 用户自定义编码（工号、员工编号等）
    private String userCode;

    // 用户邮箱（可选）
    private String userEmail;

    // 用户部门（可选）
    private String userDepartment;

    // 扩展字段（用于存储其他自定义信息）
    private Map<String, Object> metadata;
    
    public WeeklyReportArchive() {
        this.generatedTime = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    /**
     * 转换为MongoDB文档格式
     */
    public Map<String, Object> toDocument() {
        Map<String, Object> doc = new HashMap<>();
        
        if (id != null) {
            doc.put("_id", id);
        }
        
        doc.put("reportContent", reportContent);
        doc.put("commits", commits);
        doc.put("generatedTime", generatedTime.toString());
        
        if (weekStartDate != null) {
            doc.put("weekStartDate", weekStartDate.toString());
        }
        if (weekEndDate != null) {
            doc.put("weekEndDate", weekEndDate.toString());
        }
        
        doc.put("authorFilter", authorFilter);
        doc.put("projectName", projectName);
        doc.put("aiModel", aiModel);
        doc.put("apiUrl", apiUrl);
        doc.put("totalCommits", totalCommits);
        doc.put("totalAuthors", totalAuthors);

        // 用户身份信息
        doc.put("deviceId", deviceId);
        doc.put("userName", userName);
        doc.put("userCode", userCode);
        doc.put("userEmail", userEmail);
        doc.put("userDepartment", userDepartment);

        if (metadata != null && !metadata.isEmpty()) {
            doc.put("metadata", metadata);
        }
        
        return doc;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getReportContent() {
        return reportContent;
    }
    
    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }
    
    public String getCommits() {
        return commits;
    }
    
    public void setCommits(String commits) {
        this.commits = commits;
    }
    
    public LocalDateTime getGeneratedTime() {
        return generatedTime;
    }
    
    public void setGeneratedTime(LocalDateTime generatedTime) {
        this.generatedTime = generatedTime;
    }
    
    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }
    
    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }
    
    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }
    
    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }
    
    public String getAuthorFilter() {
        return authorFilter;
    }
    
    public void setAuthorFilter(String authorFilter) {
        this.authorFilter = authorFilter;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getAiModel() {
        return aiModel;
    }
    
    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public int getTotalCommits() {
        return totalCommits;
    }
    
    public void setTotalCommits(int totalCommits) {
        this.totalCommits = totalCommits;
    }
    
    public int getTotalAuthors() {
        return totalAuthors;
    }
    
    public void setTotalAuthors(int totalAuthors) {
        this.totalAuthors = totalAuthors;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    // ==================== 用户身份信息的 Getters and Setters ====================

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserDepartment() {
        return userDepartment;
    }

    public void setUserDepartment(String userDepartment) {
        this.userDepartment = userDepartment;
    }
}

