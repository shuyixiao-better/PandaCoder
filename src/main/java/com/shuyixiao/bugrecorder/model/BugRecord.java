package com.shuyixiao.bugrecorder.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Bug记录数据模型
 * 存储从控制台捕获的错误信息的结构化数据
 */
public class BugRecord {

    private final String id;
    private final String project;
    private final LocalDateTime timestamp;
    private final ErrorType errorType;
    private final String exceptionClass;
    private final String errorMessage;
    private final String summary;
    private final List<StackTraceElement> stackTrace;
    private final String rawText;
    private final boolean resolved;
    private final String aiAnalysis;
    private final String solution;
    private final BugStatus status;

    private BugRecord(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.project = builder.project;
        this.timestamp = builder.timestamp;
        this.errorType = builder.errorType;
        this.exceptionClass = builder.exceptionClass;
        this.errorMessage = builder.errorMessage;
        this.summary = builder.summary;
        this.stackTrace = builder.stackTrace;
        this.rawText = builder.rawText;
        this.resolved = builder.resolved;
        this.aiAnalysis = builder.aiAnalysis;
        this.solution = builder.solution;
        this.status = builder.status;
    }

    // Getters
    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getProject() {
        return project;
    }

    @NotNull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @NotNull
    public ErrorType getErrorType() {
        return errorType;
    }

    @Nullable
    public String getExceptionClass() {
        return exceptionClass;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    @Nullable
    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    @NotNull
    public String getRawText() {
        return rawText;
    }

    public boolean isResolved() {
        return resolved;
    }

    @NotNull
    public BugStatus getStatus() {
        return status != null ? status : BugStatus.PENDING;
    }

    @Nullable
    public String getAiAnalysis() {
        return aiAnalysis;
    }

    @Nullable
    public String getSolution() {
        return solution;
    }

    /**
     * 获取格式化的时间字符串
     */
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取简短的错误描述
     */
    public String getShortDescription() {
        if (summary != null && !summary.isEmpty()) {
            return summary.length() > 50 ? summary.substring(0, 47) + "..." : summary;
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage.length() > 50 ? errorMessage.substring(0, 47) + "..." : errorMessage;
        }
        return "未知错误";
    }

    /**
     * 创建已解决的Bug记录副本
     */
    public BugRecord withResolved(boolean resolved) {
        return new Builder(this).resolved(resolved).build();
    }

    /**
     * 创建包含AI分析的Bug记录副本
     */
    public BugRecord withAiAnalysis(String aiAnalysis, String solution) {
        return new Builder(this).aiAnalysis(aiAnalysis).solution(solution).build();
    }

    public BugRecord withStatus(BugStatus status) {
        return new Builder(this).status(status).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BugRecord bugRecord = (BugRecord) o;
        return Objects.equals(id, bugRecord.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BugRecord{" +
                "id='" + id + '\'' +
                ", project='" + project + '\'' +
                ", timestamp=" + getFormattedTimestamp() +
                ", errorType=" + errorType +
                ", summary='" + summary + '\'' +
                ", resolved=" + resolved +
                '}';
    }

    /**
     * Builder模式构建BugRecord
     */
    public static class Builder {
        private String id;
        private String project;
        private LocalDateTime timestamp;
        private ErrorType errorType = ErrorType.UNKNOWN;
        private String exceptionClass;
        private String errorMessage;
        private String summary;
        private List<StackTraceElement> stackTrace;
        private String rawText;
        private boolean resolved = false;
        private String aiAnalysis;
        private String solution;
        private BugStatus status = BugStatus.PENDING;

        public Builder() {}

        public Builder(BugRecord bugRecord) {
            this.id = bugRecord.id;
            this.project = bugRecord.project;
            this.timestamp = bugRecord.timestamp;
            this.errorType = bugRecord.errorType;
            this.exceptionClass = bugRecord.exceptionClass;
            this.errorMessage = bugRecord.errorMessage;
            this.summary = bugRecord.summary;
            this.stackTrace = bugRecord.stackTrace;
            this.rawText = bugRecord.rawText;
            this.resolved = bugRecord.resolved;
            this.aiAnalysis = bugRecord.aiAnalysis;
            this.solution = bugRecord.solution;
            this.status = bugRecord.status;
        }

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

        public Builder errorType(ErrorType errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder exceptionClass(String exceptionClass) {
            this.exceptionClass = exceptionClass;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder stackTrace(List<StackTraceElement> stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder rawText(String rawText) {
            this.rawText = rawText;
            return this;
        }

        public Builder resolved(boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        public Builder status(BugStatus status) {
            this.status = status;
            return this;
        }

        public Builder aiAnalysis(String aiAnalysis) {
            this.aiAnalysis = aiAnalysis;
            return this;
        }

        public Builder solution(String solution) {
            this.solution = solution;
            return this;
        }

        public BugRecord build() {
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }
            if (rawText == null) {
                rawText = "";
            }
            return new BugRecord(this);
        }
    }
}