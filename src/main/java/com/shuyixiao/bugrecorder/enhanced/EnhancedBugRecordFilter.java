package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.diagnostic.Logger;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 增强版Bug记录过滤器
 * 提供更灵活和精确的Bug记录过滤功能
 */
public class EnhancedBugRecordFilter {

    private static final Logger LOG = Logger.getInstance(EnhancedBugRecordFilter.class);

    // 预编译的正则表达式模式以提高性能
    private static final Pattern ERROR_INDICATOR_PATTERN = Pattern.compile(
            "\\b(Exception|Error|Caused by:|SEVERE:|FATAL:)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IGNORED_PATTERN = Pattern.compile(
            "\\b(DEBUG|TRACE|INFO|WARN)\\b|" +
            "\\b(at\\s+sun\\.|at\\s+java\\.|at\\s+com\\.intellij\\.)\\b|" +
            "\\b\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2},\\d{3}\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 过滤条件
    private final Predicate<BugRecord> filterPredicate;
    private final FilterCriteria criteria;

    private EnhancedBugRecordFilter(@NotNull FilterCriteria criteria) {
        this.criteria = criteria;
        this.filterPredicate = buildFilterPredicate(criteria);
    }

    /**
     * 创建过滤器实例
     */
    @NotNull
    public static EnhancedBugRecordFilter create(@NotNull FilterCriteria criteria) {
        return new EnhancedBugRecordFilter(criteria);
    }

    /**
     * 创建默认过滤器（不过滤任何内容）
     */
    @NotNull
    public static EnhancedBugRecordFilter createDefault() {
        return new EnhancedBugRecordFilter(FilterCriteria.DEFAULT);
    }

    /**
     * 构建过滤谓词
     */
    @NotNull
    private Predicate<BugRecord> buildFilterPredicate(@NotNull FilterCriteria criteria) {
        Predicate<BugRecord> predicate = record -> true; // 默认接受所有记录

        // 按错误类型过滤
        if (criteria.getErrorTypes() != null && !criteria.getErrorTypes().isEmpty()) {
            predicate = predicate.and(record -> criteria.getErrorTypes().contains(record.getErrorType()));
        }

        // 按时间范围过滤
        if (criteria.getStartTime() != null) {
            predicate = predicate.and(record -> !record.getTimestamp().isBefore(criteria.getStartTime()));
        }

        // 按结束时间过滤
        if (criteria.getEndTime() != null) {
            predicate = predicate.and(record -> !record.getTimestamp().isAfter(criteria.getEndTime()));
        }

        // 按关键字过滤
        if (criteria.getKeywords() != null && !criteria.getKeywords().isEmpty()) {
            predicate = predicate.and(record -> containsAnyKeyword(record, criteria.getKeywords()));
        }

        // 按项目过滤
        if (criteria.getProjectNames() != null && !criteria.getProjectNames().isEmpty()) {
            predicate = predicate.and(record -> criteria.getProjectNames().contains(record.getProject()));
        }

        // 按状态过滤
        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            predicate = predicate.and(record -> criteria.getStatuses().contains(record.getStatus()));
        }

        // 按解决状态过滤
        if (criteria.getResolvedOnly() != null) {
            predicate = predicate.and(record -> record.isResolved() == criteria.getResolvedOnly());
        }

        // 按最小置信度过滤
        if (criteria.getMinConfidence() != null) {
            predicate = predicate.and(record -> calculateConfidence(record) >= criteria.getMinConfidence());
        }

        // 按最大置信度过滤
        if (criteria.getMaxConfidence() != null) {
            predicate = predicate.and(record -> calculateConfidence(record) <= criteria.getMaxConfidence());
        }

        return predicate;
    }

    /**
     * 检查记录是否包含任一关键字
     */
    private boolean containsAnyKeyword(@NotNull BugRecord record, @NotNull java.util.List<String> keywords) {
        String lowerSummary = record.getSummary() != null ? record.getSummary().toLowerCase() : "";
        String lowerMessage = record.getErrorMessage() != null ? record.getErrorMessage().toLowerCase() : "";
        String lowerClass = record.getExceptionClass() != null ? record.getExceptionClass().toLowerCase() : "";
        String lowerRawText = record.getRawText() != null ? record.getRawText().toLowerCase() : "";

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            if (lowerSummary.contains(lowerKeyword) ||
                lowerMessage.contains(lowerKeyword) ||
                lowerClass.contains(lowerKeyword) ||
                lowerRawText.contains(lowerKeyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 过滤Bug记录
     */
    public boolean accept(@NotNull BugRecord record) {
        try {
            return filterPredicate.test(record);
        } catch (Exception e) {
            LOG.warn("Error filtering bug record: " + record.getId(), e);
            return false; // 默认拒绝有错误的记录
        }
    }

    /**
     * 检查文本是否可能是错误信息
     */
    public static boolean isLikelyErrorText(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String trimmedText = text.trim();

        // 忽略纯调试、跟踪、信息或警告级别的日志
        if (IGNORED_PATTERN.matcher(trimmedText).find()) {
            return false;
        }

        // 检查是否包含错误指示词
        return ERROR_INDICATOR_PATTERN.matcher(trimmedText).find();
    }

    /**
     * 检查文本是否应该是忽略的内容
     */
    public static boolean shouldBeIgnored(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }

        String trimmedText = text.trim();

        // 忽略纯调试、跟踪、信息或警告级别的日志
        return IGNORED_PATTERN.matcher(trimmedText).find();
    }

    /**
     * 计算记录的置信度
     */
    private double calculateConfidence(@NotNull BugRecord record) {
        // 基于发生次数计算置信度（发生次数越多，置信度越高）
        double occurrenceConfidence = Math.min(1.0, record.getOccurrenceCount() / 10.0);
        
        // 基于错误类型计算置信度
        double typeConfidence = 0.5; // 默认置信度
        switch (record.getErrorType()) {
            case DATABASE:
            case NETWORK:
            case HTTP_SERVER:
                typeConfidence = 0.9; // 服务器端错误置信度高
                break;
            case SPRING_FRAMEWORK:
            case HTTP_CLIENT:
            case VALIDATION:
                typeConfidence = 0.7; // 框架和客户端错误置信度中等
                break;
            case COMPILATION:
            case RUNTIME:
            case TIMEOUT:
                typeConfidence = 0.6; // 运行时错误置信度一般
                break;
            case CACHE:
            case MQ:
            case SECURITY:
                typeConfidence = 0.8; // 缓存、消息队列、安全错误置信度较高
                break;
            case IO:
            case CONFIGURATION:
            case THIRD_PARTY:
                typeConfidence = 0.4; // IO、配置、第三方库错误置信度较低
                break;
        }
        
        // 综合计算置信度
        return 0.7 * occurrenceConfidence + 0.3 * typeConfidence;
    }

    /**
     * 获取过滤条件
     */
    @NotNull
    public FilterCriteria getCriteria() {
        return criteria;
    }

    /**
     * 过滤条件
     */
    public static class FilterCriteria {
        // 默认过滤条件（不过滤任何内容）
        public static final FilterCriteria DEFAULT = new FilterCriteria.Builder().build();

        private final java.util.Set<ErrorType> errorTypes;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final java.util.List<String> keywords;
        private final java.util.Set<String> projectNames;
        private final java.util.Set<BugStatus> statuses;
        private final Boolean resolvedOnly;
        private final Double minConfidence;
        private final Double maxConfidence;

        private FilterCriteria(Builder builder) {
            this.errorTypes = builder.errorTypes != null ? 
                new java.util.HashSet<>(builder.errorTypes) : 
                java.util.Collections.emptySet();
            this.startTime = builder.startTime;
            this.endTime = builder.endTime;
            this.keywords = builder.keywords != null ? 
                new java.util.ArrayList<>(builder.keywords) : 
                java.util.Collections.emptyList();
            this.projectNames = builder.projectNames != null ? 
                new java.util.HashSet<>(builder.projectNames) : 
                java.util.Collections.emptySet();
            this.statuses = builder.statuses != null ? 
                new java.util.HashSet<>(builder.statuses) : 
                java.util.Collections.emptySet();
            this.resolvedOnly = builder.resolvedOnly;
            this.minConfidence = builder.minConfidence;
            this.maxConfidence = builder.maxConfidence;
        }

        @NotNull
        public java.util.Set<ErrorType> getErrorTypes() {
            return errorTypes;
        }

        @Nullable
        public LocalDateTime getStartTime() {
            return startTime;
        }

        @Nullable
        public LocalDateTime getEndTime() {
            return endTime;
        }

        @NotNull
        public java.util.List<String> getKeywords() {
            return keywords;
        }

        @NotNull
        public java.util.Set<String> getProjectNames() {
            return projectNames;
        }

        @NotNull
        public java.util.Set<BugStatus> getStatuses() {
            return statuses;
        }

        @Nullable
        public Boolean getResolvedOnly() {
            return resolvedOnly;
        }

        @Nullable
        public Double getMinConfidence() {
            return minConfidence;
        }

        @Nullable
        public Double getMaxConfidence() {
            return maxConfidence;
        }

        /**
         * 构建器模式
         */
        public static class Builder {
            private java.util.Set<ErrorType> errorTypes;
            private LocalDateTime startTime;
            private LocalDateTime endTime;
            private java.util.List<String> keywords;
            private java.util.Set<String> projectNames;
            private java.util.Set<BugStatus> statuses;
            private Boolean resolvedOnly;
            private Double minConfidence;
            private Double maxConfidence;

            public Builder errorTypes(@Nullable java.util.Set<ErrorType> errorTypes) {
                this.errorTypes = errorTypes;
                return this;
            }

            public Builder startTime(@Nullable LocalDateTime startTime) {
                this.startTime = startTime;
                return this;
            }

            public Builder endTime(@Nullable LocalDateTime endTime) {
                this.endTime = endTime;
                return this;
            }

            public Builder keywords(@Nullable java.util.List<String> keywords) {
                this.keywords = keywords;
                return this;
            }

            public Builder projectNames(@Nullable java.util.Set<String> projectNames) {
                this.projectNames = projectNames;
                return this;
            }

            public Builder statuses(@Nullable java.util.Set<BugStatus> statuses) {
                this.statuses = statuses;
                return this;
            }

            public Builder resolvedOnly(@Nullable Boolean resolvedOnly) {
                this.resolvedOnly = resolvedOnly;
                return this;
            }

            public Builder minConfidence(@Nullable Double minConfidence) {
                this.minConfidence = minConfidence;
                return this;
            }

            public Builder maxConfidence(@Nullable Double maxConfidence) {
                this.maxConfidence = maxConfidence;
                return this;
            }

            public FilterCriteria build() {
                return new FilterCriteria(this);
            }
        }
    }

    /**
     * 预定义的过滤器
     */
    public static class PredefinedFilters {
        /**
         * 创建今天记录的过滤器
         */
        @NotNull
        public static EnhancedBugRecordFilter forToday() {
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            
            FilterCriteria criteria = new FilterCriteria.Builder()
                    .startTime(todayStart)
                    .endTime(todayEnd)
                    .build();
            
            return EnhancedBugRecordFilter.create(criteria);
        }

        /**
         * 创建本周记录的过滤器
         */
        @NotNull
        public static EnhancedBugRecordFilter forThisWeek() {
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            
            FilterCriteria criteria = new FilterCriteria.Builder()
                    .startTime(weekStart)
                    .build();
            
            return EnhancedBugRecordFilter.create(criteria);
        }

        /**
         * 创建未解决问题的过滤器
         */
        @NotNull
        public static EnhancedBugRecordFilter forUnresolved() {
            FilterCriteria criteria = new FilterCriteria.Builder()
                    .resolvedOnly(false)
                    .build();
            
            return EnhancedBugRecordFilter.create(criteria);
        }

        /**
         * 创建高置信度记录的过滤器
         */
        @NotNull
        public static EnhancedBugRecordFilter forHighConfidence() {
            FilterCriteria criteria = new FilterCriteria.Builder()
                    .minConfidence(0.8)
                    .build();
            
            return EnhancedBugRecordFilter.create(criteria);
        }

        /**
         * 创建数据库相关错误的过滤器
         */
        @NotNull
        public static EnhancedBugRecordFilter forDatabaseErrors() {
            java.util.Set<ErrorType> dbTypes = java.util.Set.of(
                ErrorType.DATABASE, 
                ErrorType.NETWORK
            );
            
            FilterCriteria criteria = new FilterCriteria.Builder()
                    .errorTypes(dbTypes)
                    .build();
            
            return EnhancedBugRecordFilter.create(criteria);
        }

        /**
         * 创建网络相关错误的过滤器
         */
        @NotNull
        public static EnhancedBugRecordFilter forNetworkErrors() {
            java.util.Set<ErrorType> networkTypes = java.util.Set.of(
                ErrorType.NETWORK, 
                ErrorType.HTTP_CLIENT, 
                ErrorType.HTTP_SERVER
            );
            
            FilterCriteria criteria = new FilterCriteria.Builder()
                    .errorTypes(networkTypes)
                    .build();
            
            return EnhancedBugRecordFilter.create(criteria);
        }
    }
}