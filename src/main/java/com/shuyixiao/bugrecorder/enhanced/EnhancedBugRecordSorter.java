package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.diagnostic.Logger;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.BugStatus;
import com.shuyixiao.bugrecorder.model.ErrorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 增强版Bug记录排序器
 * 提供多种排序策略和自定义排序功能
 */
public class EnhancedBugRecordSorter {

    private static final Logger LOG = Logger.getInstance(EnhancedBugRecordSorter.class);

    // 预定义的排序器
    public static final Comparator<BugRecord> BY_TIMESTAMP_DESC = 
            Comparator.comparing(BugRecord::getTimestamp).reversed();

    public static final Comparator<BugRecord> BY_TIMESTAMP_ASC = 
            Comparator.comparing(BugRecord::getTimestamp);

    public static final Comparator<BugRecord> BY_ERROR_TYPE = 
            Comparator.comparing(BugRecord::getErrorType);

    public static final Comparator<BugRecord> BY_STATUS = 
            Comparator.comparing(BugRecord::getStatus);

    public static final Comparator<BugRecord> BY_CONFIDENCE_DESC = 
            Comparator.comparing(BugRecord::getOccurrenceCount).reversed();

    public static final Comparator<BugRecord> BY_OCCURRENCE_COUNT_DESC = 
            Comparator.comparing(BugRecord::getOccurrenceCount).reversed();

    public static final Comparator<BugRecord> BY_PROJECT = 
            Comparator.comparing(BugRecord::getProject);

    // 复合排序器
    public static final Comparator<BugRecord> BY_PRIORITY = 
            BY_STATUS.thenComparing(BY_OCCURRENCE_COUNT_DESC).reversed();

    public static final Comparator<BugRecord> BY_RECENT_AND_FREQUENT = 
            BY_TIMESTAMP_DESC.thenComparing(BY_OCCURRENCE_COUNT_DESC);

    public static final Comparator<BugRecord> BY_SEVERITY = 
            Comparator.comparing(EnhancedBugRecordSorter::getErrorSeverity)
                    .thenComparing(BY_CONFIDENCE_DESC)
                    .reversed();

    /**
     * 对Bug记录列表进行排序
     */
    @NotNull
    public static List<BugRecord> sort(@NotNull List<BugRecord> records, @NotNull SortCriteria criteria) {
        try {
            if (records.isEmpty()) {
                return records;
            }

            Comparator<BugRecord> comparator = buildComparator(criteria);
            records.sort(comparator);

            LOG.debug("Sorted " + records.size() + " bug records using criteria: " + criteria);

            return records;

        } catch (Exception e) {
            LOG.warn("Error sorting bug records, returning original list", e);
            return records;
        }
    }

    /**
     * 构建比较器
     */
    @NotNull
    private static Comparator<BugRecord> buildComparator(@NotNull SortCriteria criteria) {
        Comparator<BugRecord> comparator = (o1, o2) -> 0; // 默认比较器（相等）

        // 应用主要排序字段
        switch (criteria.getPrimaryField()) {
            case TIMESTAMP:
                comparator = criteria.isAscending() ? BY_TIMESTAMP_ASC : BY_TIMESTAMP_DESC;
                break;
            case ERROR_TYPE:
                comparator = criteria.isAscending() ? BY_ERROR_TYPE : BY_ERROR_TYPE.reversed();
                break;
            case STATUS:
                comparator = criteria.isAscending() ? BY_STATUS : BY_STATUS.reversed();
                break;
            case CONFIDENCE:
                comparator = criteria.isAscending() ? BY_CONFIDENCE_DESC.reversed() : BY_CONFIDENCE_DESC;
                break;
            case OCCURRENCE_COUNT:
                comparator = criteria.isAscending() ? BY_OCCURRENCE_COUNT_DESC.reversed() : BY_OCCURRENCE_COUNT_DESC;
                break;
            case PROJECT:
                comparator = criteria.isAscending() ? BY_PROJECT : BY_PROJECT.reversed();
                break;
            case PRIORITY:
                comparator = criteria.isAscending() ? BY_PRIORITY.reversed() : BY_PRIORITY;
                break;
            case RECENT_AND_FREQUENT:
                comparator = criteria.isAscending() ? BY_RECENT_AND_FREQUENT.reversed() : BY_RECENT_AND_FREQUENT;
                break;
            case SEVERITY:
                comparator = criteria.isAscending() ? BY_SEVERITY.reversed() : BY_SEVERITY;
                break;
        }

        // 应用次要排序字段
        if (criteria.getSecondaryField() != null) {
            Comparator<BugRecord> secondaryComparator = null;
            
            switch (criteria.getSecondaryField()) {
                case TIMESTAMP:
                    secondaryComparator = criteria.isSecondaryAscending() ? BY_TIMESTAMP_ASC : BY_TIMESTAMP_DESC;
                    break;
                case ERROR_TYPE:
                    secondaryComparator = criteria.isSecondaryAscending() ? BY_ERROR_TYPE : BY_ERROR_TYPE.reversed();
                    break;
                case STATUS:
                    secondaryComparator = criteria.isSecondaryAscending() ? BY_STATUS : BY_STATUS.reversed();
                    break;
                case CONFIDENCE:
                    secondaryComparator = criteria.isSecondaryAscending() ? BY_CONFIDENCE_DESC.reversed() : BY_CONFIDENCE_DESC;
                    break;
                case OCCURRENCE_COUNT:
                    secondaryComparator = criteria.isSecondaryAscending() ? BY_OCCURRENCE_COUNT_DESC.reversed() : BY_OCCURRENCE_COUNT_DESC;
                    break;
                case PROJECT:
                    secondaryComparator = criteria.isSecondaryAscending() ? BY_PROJECT : BY_PROJECT.reversed();
                    break;
            }
            
            if (secondaryComparator != null) {
                comparator = comparator.thenComparing(secondaryComparator);
            }
        }

        return comparator;
    }

    /**
     * 获取错误严重级别（用于排序）
     */
    private static int getErrorSeverity(@NotNull BugRecord record) {
        // 基于错误类型确定严重级别
        switch (record.getErrorType()) {
            case DATABASE:
            case NETWORK:
            case HTTP_SERVER:
                return 5; // 最高优先级
                
            case SPRING_FRAMEWORK:
            case SECURITY:
            case MEMORY:
                return 4; // 高优先级
                
            case HTTP_CLIENT:
            case VALIDATION:
            case COMPILATION:
                return 3; // 中等优先级
                
            case RUNTIME:
            case TIMEOUT:
            case CACHE:
                return 2; // 低优先级
                
            case IO:
            case CONFIGURATION:
            case THIRD_PARTY:
                return 1; // 最低优先级
                
            default:
                return 0; // 未知类型
        }
    }

    /**
     * 获取状态优先级（用于排序）
     */
    private static int getStatusPriority(@NotNull BugRecord record) {
        // 基于状态确定优先级
        switch (record.getStatus()) {
            case PENDING:
                return 4; // 待处理问题优先级最高
            case IN_PROGRESS:
                return 3; // 处理中问题优先级较高
            case RESOLVED:
                return 2; // 已解决问题优先级中等
            case IGNORED:
                return 1; // 已忽略问题优先级最低
            default:
                return 0; // 未知状态
        }
    }

    /**
     * 获取置信度权重（用于排序）
     */
    private static double getConfidenceWeight(@NotNull BugRecord record) {
        // 基于置信度和发生次数计算权重
        double confidence = getConfidenceWeight(record);
        int occurrenceCount = record.getOccurrenceCount();
        
        // 置信度权重 + 发生次数权重（发生次数越多，权重越高）
        return confidence + (Math.log(occurrenceCount + 1) / 10.0);
    }

    /**
     * 排序条件
     */
    public static class SortCriteria {
        private final SortField primaryField;
        private final boolean ascending;
        private final SortField secondaryField;
        private final boolean secondaryAscending;

        private SortCriteria(Builder builder) {
            this.primaryField = builder.primaryField != null ? builder.primaryField : SortField.TIMESTAMP;
            this.ascending = builder.ascending;
            this.secondaryField = builder.secondaryField;
            this.secondaryAscending = builder.secondaryAscending;
        }

        @NotNull
        public SortField getPrimaryField() {
            return primaryField;
        }

        public boolean isAscending() {
            return ascending;
        }

        @Nullable
        public SortField getSecondaryField() {
            return secondaryField;
        }

        public boolean isSecondaryAscending() {
            return secondaryAscending;
        }

        /**
         * 构建器模式
         */
        public static class Builder {
            private SortField primaryField;
            private boolean ascending = false;
            private SortField secondaryField;
            private boolean secondaryAscending = false;

            public Builder primaryField(@Nullable SortField primaryField) {
                this.primaryField = primaryField;
                return this;
            }

            public Builder ascending(boolean ascending) {
                this.ascending = ascending;
                return this;
            }

            public Builder secondaryField(@Nullable SortField secondaryField) {
                this.secondaryField = secondaryField;
                return this;
            }

            public Builder secondaryAscending(boolean secondaryAscending) {
                this.secondaryAscending = secondaryAscending;
                return this;
            }

            public SortCriteria build() {
                return new SortCriteria(this);
            }
        }
    }

    /**
     * 排序字段枚举
     */
    public enum SortField {
        TIMESTAMP("时间戳"),
        ERROR_TYPE("错误类型"),
        STATUS("状态"),
        CONFIDENCE("置信度"),
        OCCURRENCE_COUNT("发生次数"),
        PROJECT("项目"),
        PRIORITY("优先级"),
        RECENT_AND_FREQUENT("最近且频繁"),
        SEVERITY("严重程度");

        private final String displayName;

        SortField(@NotNull String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 预定义的排序器
     */
    public static class PredefinedSorters {
        /**
         * 按时间戳降序排序
         */
        @NotNull
        public static SortCriteria byTimestampDesc() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.TIMESTAMP)
                    .ascending(false)
                    .build();
        }

        /**
         * 按时间戳升序排序
         */
        @NotNull
        public static SortCriteria byTimestampAsc() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.TIMESTAMP)
                    .ascending(true)
                    .build();
        }

        /**
         * 按错误类型排序
         */
        @NotNull
        public static SortCriteria byErrorType() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.ERROR_TYPE)
                    .ascending(true)
                    .build();
        }

        /**
         * 按状态排序
         */
        @NotNull
        public static SortCriteria byStatus() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.STATUS)
                    .ascending(true)
                    .build();
        }

        /**
         * 按置信度降序排序
         */
        @NotNull
        public static SortCriteria byConfidenceDesc() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.CONFIDENCE)
                    .ascending(false)
                    .build();
        }

        /**
         * 按发生次数降序排序
         */
        @NotNull
        public static SortCriteria byOccurrenceCountDesc() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.OCCURRENCE_COUNT)
                    .ascending(false)
                    .build();
        }

        /**
         * 按项目排序
         */
        @NotNull
        public static SortCriteria byProject() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.PROJECT)
                    .ascending(true)
                    .build();
        }

        /**
         * 按优先级排序
         */
        @NotNull
        public static SortCriteria byPriority() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.PRIORITY)
                    .ascending(false)
                    .build();
        }

        /**
         * 按最近且频繁排序
         */
        @NotNull
        public static SortCriteria byRecentAndFrequent() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.RECENT_AND_FREQUENT)
                    .ascending(false)
                    .build();
        }

        /**
         * 按严重程度排序
         */
        @NotNull
        public static SortCriteria bySeverity() {
            return new SortCriteria.Builder()
                    .primaryField(SortField.SEVERITY)
                    .ascending(false)
                    .build();
        }
    }
}