package com.shuyixiao.sql.model;

import com.shuyixiao.sql.parser.SqlParser;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * SQL 记录模型
 * 用于存储 SQL 查询信息
 */
public class SqlRecord {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String id;
    private final String project;
    private final LocalDateTime timestamp;
    private final String sqlStatement;      // SQL语句
    private final String parameters;        // 参数
    private final String tableName;         // 表名
    private final String operation;         // 操作类型 (SELECT, INSERT, UPDATE, DELETE)
    private final Integer resultCount;      // 结果数量
    private final Long executionTime;       // 执行时间（毫秒）
    private final String source;            // 来源（MyBatis, JPA等）
    private final String apiPath;           // API接口路径
    private final String callerClass;       // 调用SQL的类
    
    private SqlRecord(Builder builder) {
        this.id = builder.id;
        this.project = builder.project;
        this.timestamp = builder.timestamp;
        this.sqlStatement = builder.sqlStatement;
        this.parameters = builder.parameters;
        this.tableName = builder.tableName;
        this.operation = builder.operation;
        this.resultCount = builder.resultCount;
        this.executionTime = builder.executionTime;
        this.source = builder.source;
        this.apiPath = builder.apiPath;
        this.callerClass = builder.callerClass;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getProject() {
        return project;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getSqlStatement() {
        return sqlStatement;
    }
    
    public String getParameters() {
        return parameters;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public Integer getResultCount() {
        return resultCount;
    }
    
    public Long getExecutionTime() {
        return executionTime;
    }
    
    public String getSource() {
        return source;
    }
    
    public String getApiPath() {
        return apiPath;
    }
    
    public String getCallerClass() {
        return callerClass;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }
    
    public String getShortSql() {
        if (sqlStatement == null || sqlStatement.length() <= 100) {
            return sqlStatement;
        }
        return sqlStatement.substring(0, 100) + "...";
    }
    
    public String getOperationColor() {
        if (operation == null) return "#888888";
        
        switch (operation.toUpperCase()) {
            case "SELECT":
                return "#61AFFE";  // 蓝色
            case "INSERT":
                return "#49CC90";  // 绿色
            case "UPDATE":
                return "#FCA130";  // 橙色
            case "DELETE":
                return "#F93E3E";  // 红色
            default:
                return "#888888";  // 灰色
        }
    }
    
    public boolean isSuccess() {
        return resultCount != null && resultCount >= 0;
    }
    
    /**
     * 获取完整的SQL（带参数）
     */
    public String getFullSqlWithParameters() {
        if (sqlStatement == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("-- SQL语句\n");
        sb.append(sqlStatement);
        
        if (parameters != null && !parameters.isEmpty()) {
            sb.append("\n\n-- 参数\n");
            sb.append(parameters);
        }
        
        return sb.toString();
    }
    
    /**
     * 获取可执行的SQL（参数已替换）
     * 使用SqlParser的replaceParameters方法处理复杂的参数（包括大JSON）
     */
    public String getExecutableSql() {
        if (sqlStatement == null) {
            return "";
        }
        
        // 如果没有参数，直接返回SQL
        if (parameters == null || parameters.isEmpty()) {
            return sqlStatement;
        }
        
        try {
            // 使用SqlParser的replaceParameters方法
            return SqlParser.replaceParameters(sqlStatement, parameters);
        } catch (Exception e) {
            // 解析失败，返回原始SQL + 错误信息
            return sqlStatement + "\n-- 参数解析失败\n-- 参数: " + parameters + "\n-- 错误: " + e.getMessage();
        }
    }
    
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String project;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String sqlStatement;
        private String parameters;
        private String tableName;
        private String operation;
        private Integer resultCount;
        private Long executionTime;
        private String source;
        private String apiPath;
        private String callerClass;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder project(String project) {
            this.project = project;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder sqlStatement(String sqlStatement) {
            this.sqlStatement = sqlStatement;
            return this;
        }
        
        public Builder parameters(String parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }
        
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder resultCount(Integer resultCount) {
            this.resultCount = resultCount;
            return this;
        }
        
        public Builder executionTime(Long executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder apiPath(String apiPath) {
            this.apiPath = apiPath;
            return this;
        }
        
        public Builder callerClass(String callerClass) {
            this.callerClass = callerClass;
            return this;
        }
        
        public SqlRecord build() {
            return new SqlRecord(this);
        }
    }
}

