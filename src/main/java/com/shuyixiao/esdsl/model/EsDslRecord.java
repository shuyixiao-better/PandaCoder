package com.shuyixiao.esdsl.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * ES DSL 记录模型
 * 用于存储 Elasticsearch 查询 DSL 信息
 */
public class EsDslRecord {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String id;
    private final String project;
    private final LocalDateTime timestamp;
    private final String dslQuery;
    private final String index;
    private final String method;  // GET, POST, PUT, DELETE
    private final String endpoint; // 完整的请求端点
    private final String response;
    private final Long executionTime; // 执行时间（毫秒）
    private final Integer httpStatus; // HTTP状态码
    private final String source;  // 来源（RestHighLevelClient, RestClient等）
    
    private EsDslRecord(Builder builder) {
        this.id = builder.id;
        this.project = builder.project;
        this.timestamp = builder.timestamp;
        this.dslQuery = builder.dslQuery;
        this.index = builder.index;
        this.method = builder.method;
        this.endpoint = builder.endpoint;
        this.response = builder.response;
        this.executionTime = builder.executionTime;
        this.httpStatus = builder.httpStatus;
        this.source = builder.source;
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
    
    public String getDslQuery() {
        return dslQuery;
    }
    
    public String getIndex() {
        return index;
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public String getResponse() {
        return response;
    }
    
    public Long getExecutionTime() {
        return executionTime;
    }
    
    public Integer getHttpStatus() {
        return httpStatus;
    }
    
    public String getSource() {
        return source;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }
    
    public String getShortQuery() {
        if (dslQuery == null || dslQuery.length() <= 100) {
            return dslQuery;
        }
        return dslQuery.substring(0, 100) + "...";
    }
    
    public String getMethodColor() {
        switch (method.toUpperCase()) {
            case "GET":
                return "#61AFFE";  // 蓝色
            case "POST":
                return "#49CC90";  // 绿色
            case "PUT":
                return "#FCA130";  // 橙色
            case "DELETE":
                return "#F93E3E";  // 红色
            default:
                return "#888888";  // 灰色
        }
    }
    
    public boolean isSuccess() {
        return httpStatus != null && httpStatus >= 200 && httpStatus < 300;
    }
    
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String project;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String dslQuery;
        private String index;
        private String method;
        private String endpoint;
        private String response;
        private Long executionTime;
        private Integer httpStatus;
        private String source;
        
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
        
        public Builder dslQuery(String dslQuery) {
            this.dslQuery = dslQuery;
            return this;
        }
        
        public Builder index(String index) {
            this.index = index;
            return this;
        }
        
        public Builder method(String method) {
            this.method = method;
            return this;
        }
        
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public Builder response(String response) {
            this.response = response;
            return this;
        }
        
        public Builder executionTime(Long executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder httpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public EsDslRecord build() {
            return new EsDslRecord(this);
        }
    }
}

