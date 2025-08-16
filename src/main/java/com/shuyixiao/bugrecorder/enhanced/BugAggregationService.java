package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.BugStatus;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 增强版Bug记录聚合服务
 * 提供更好的性能、准确性和功能
 */
@Service
public final class BugAggregationService {

    private static final Logger LOG = Logger.getInstance(BugAggregationService.class);

    private final Project project;
    private final BugRecordService bugRecordService;
    
    // 聚合数据缓存
    private final Map<String, AggregatedData> aggregatedDataCache = new ConcurrentHashMap<>();
    
    // 定时器用于定期刷新聚合数据
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BugAggregation-Refresh");
        t.setDaemon(true);
        return t;
    });
    
    // 配置参数
    private static final long REFRESH_INTERVAL_MINUTES = 15;
    private static final long CACHE_EXPIRATION_MS = 10 * 60 * 1000; // 10分钟

    public BugAggregationService(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        
        // 启动定期刷新任务
        startRefreshTask();
        
        LOG.info("Bug Aggregation Service initialized for project: " + project.getName());
    }

    /**
     * 获取错误类型分布
     */
    public ErrorTypeDistribution getErrorTypeDistribution(int days) {
        String cacheKey = "errorTypeDistribution_" + days;
        
        AggregatedData cached = aggregatedDataCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (ErrorTypeDistribution) cached.getData();
        }
        
        try {
            List<BugRecord> records = bugRecordService.getRecentRecords(days);
            
            Map<ErrorType, Long> distribution = records.stream()
                    .collect(Collectors.groupingBy(
                            BugRecord::getErrorType,
                            Collectors.counting()
                    ));
            
            ErrorTypeDistribution result = new ErrorTypeDistribution(distribution);
            aggregatedDataCache.put(cacheKey, new AggregatedData(result));
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Failed to get error type distribution", e);
            return new ErrorTypeDistribution(java.util.Collections.emptyMap());
        }
    }

    /**
     * 获取高频错误列表
     */
    public List<FrequentError> getFrequentErrors(int days, int limit) {
        String cacheKey = "frequentErrors_" + days + "_" + limit;
        
        AggregatedData cached = aggregatedDataCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<FrequentError>) cached.getData();
        }
        
        try {
            List<BugRecord> records = bugRecordService.getRecentRecords(days);
            
            Map<String, List<BugRecord>> groupedByFingerprint = records.stream()
                    .filter(r -> r.getFingerprint() != null)
                    .collect(Collectors.groupingBy(BugRecord::getFingerprint));
            
            List<FrequentError> frequentErrors = groupedByFingerprint.entrySet().stream()
                    .map(entry -> {
                        List<BugRecord> group = entry.getValue();
                        BugRecord sample = group.get(0);
                        return new FrequentError(
                                sample.getFingerprint(),
                                sample.getSummary(),
                                sample.getErrorType(),
                                group.size(),
                                group.stream().mapToLong(BugRecord::getOccurrenceCount).sum(),
                                group.stream().map(BugRecord::getTimestamp).min(LocalDateTime::compareTo).orElse(LocalDateTime.now())
                        );
                    })
                    .sorted((a, b) -> Long.compare(b.getTotalOccurrences(), a.getTotalOccurrences()))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            aggregatedDataCache.put(cacheKey, new AggregatedData(frequentErrors));
            
            return frequentErrors;
            
        } catch (Exception e) {
            LOG.error("Failed to get frequent errors", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 获取趋势分析
     */
    public TrendAnalysis getTrendAnalysis(int days) {
        String cacheKey = "trendAnalysis_" + days;
        
        AggregatedData cached = aggregatedDataCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (TrendAnalysis) cached.getData();
        }
        
        try {
            List<BugRecord> records = bugRecordService.getRecentRecords(days);
            
            // 按日期分组统计
            Map<String, Long> dailyCounts = records.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getTimestamp().toLocalDate().toString(),
                            Collectors.counting()
                    ));
            
            // 计算平均值和趋势
            long total = dailyCounts.values().stream().mapToLong(Long::longValue).sum();
            double average = total / (double) days;
            
            // 计算最近趋势（最后7天vs之前）
            long recentTotal = 0;
            long previousTotal = 0;
            int dayCount = 0;
            
            for (Map.Entry<String, Long> entry : dailyCounts.entrySet()) {
                if (dayCount < 7) {
                    recentTotal += entry.getValue();
                } else {
                    previousTotal += entry.getValue();
                }
                dayCount++;
            }
            
            TrendDirection trend = TrendDirection.STABLE;
            if (recentTotal > previousTotal * 1.2) {
                trend = TrendDirection.INCREASING;
            } else if (recentTotal < previousTotal * 0.8) {
                trend = TrendDirection.DECREASING;
            }
            
            TrendAnalysis result = new TrendAnalysis(dailyCounts, average, trend);
            aggregatedDataCache.put(cacheKey, new AggregatedData(result));
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Failed to get trend analysis", e);
            return new TrendAnalysis(java.util.Collections.emptyMap(), 0, TrendDirection.STABLE);
        }
    }

    /**
     * 获取解决率统计
     */
    public ResolutionRate getResolutionRate(int days) {
        String cacheKey = "resolutionRate_" + days;
        
        AggregatedData cached = aggregatedDataCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (ResolutionRate) cached.getData();
        }
        
        try {
            List<BugRecord> records = bugRecordService.getRecentRecords(days);
            
            long total = records.size();
            long resolved = records.stream()
                    .mapToLong(r -> r.isResolved() ? 1 : 0)
                    .sum();
            
            double rate = total > 0 ? (double) resolved / total : 0.0;
            
            ResolutionRate result = new ResolutionRate(total, resolved, rate);
            aggregatedDataCache.put(cacheKey, new AggregatedData(result));
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Failed to get resolution rate", e);
            return new ResolutionRate(0, 0, 0.0);
        }
    }

    /**
     * 启动刷新任务
     */
    private void startRefreshTask() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshAggregatedData();
            } catch (Exception e) {
                LOG.warn("Error during aggregated data refresh", e);
            }
        }, REFRESH_INTERVAL_MINUTES, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 刷新聚合数据
     */
    private void refreshAggregatedData() {
        try {
            LOG.debug("Refreshing aggregated bug data for project: " + project.getName());
            
            // 清除过期的缓存数据
            aggregatedDataCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            
            LOG.debug("Aggregated data refresh completed. Cache size: " + aggregatedDataCache.size());
            
        } catch (Exception e) {
            LOG.warn("Error during aggregated data refresh", e);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        aggregatedDataCache.clear();
        LOG.debug("Aggregated data cache cleared for project: " + project.getName());
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        LOG.info("Shutting down Bug Aggregation Service for project: " + project.getName());
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        aggregatedDataCache.clear();
        
        LOG.info("Bug Aggregation Service shutdown completed for project: " + project.getName());
    }

    /**
     * 获取项目
     */
    @NotNull
    public Project getProject() {
        return project;
    }

    /**
     * 获取Bug记录服务
     */
    @NotNull
    public BugRecordService getBugRecordService() {
        return bugRecordService;
    }

    /**
     * 聚合数据包装类
     */
    private static class AggregatedData {
        private final Object data;
        private final long timestamp;
        private static final long CACHE_EXPIRATION_MS = 10 * 60 * 1000; // 10分钟

        public AggregatedData(@NotNull Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        @NotNull
        public Object getData() {
            return data;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_MS;
        }
    }

    /**
     * 错误类型分布
     */
    public static class ErrorTypeDistribution {
        private final Map<ErrorType, Long> distribution;

        public ErrorTypeDistribution(@NotNull Map<ErrorType, Long> distribution) {
            this.distribution = distribution;
        }

        @NotNull
        public Map<ErrorType, Long> getDistribution() {
            return distribution;
        }

        public long getTotalCount() {
            return distribution.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    /**
     * 高频错误
     */
    public static class FrequentError {
        private final String fingerprint;
        private final String summary;
        private final ErrorType errorType;
        private final int uniqueOccurrences;
        private final long totalOccurrences;
        private final LocalDateTime firstSeen;

        public FrequentError(@NotNull String fingerprint, @NotNull String summary, 
                           @NotNull ErrorType errorType, int uniqueOccurrences, 
                           long totalOccurrences, @NotNull LocalDateTime firstSeen) {
            this.fingerprint = fingerprint;
            this.summary = summary;
            this.errorType = errorType;
            this.uniqueOccurrences = uniqueOccurrences;
            this.totalOccurrences = totalOccurrences;
            this.firstSeen = firstSeen;
        }

        @NotNull
        public String getFingerprint() {
            return fingerprint;
        }

        @NotNull
        public String getSummary() {
            return summary;
        }

        @NotNull
        public ErrorType getErrorType() {
            return errorType;
        }

        public int getUniqueOccurrences() {
            return uniqueOccurrences;
        }

        public long getTotalOccurrences() {
            return totalOccurrences;
        }

        @NotNull
        public LocalDateTime getFirstSeen() {
            return firstSeen;
        }
    }

    /**
     * 趋势分析
     */
    public static class TrendAnalysis {
        private final Map<String, Long> dailyCounts;
        private final double average;
        private final TrendDirection trend;

        public TrendAnalysis(@NotNull Map<String, Long> dailyCounts, double average, @NotNull TrendDirection trend) {
            this.dailyCounts = dailyCounts;
            this.average = average;
            this.trend = trend;
        }

        @NotNull
        public Map<String, Long> getDailyCounts() {
            return dailyCounts;
        }

        public double getAverage() {
            return average;
        }

        @NotNull
        public TrendDirection getTrend() {
            return trend;
        }
    }

    /**
     * 趋势方向
     */
    public enum TrendDirection {
        INCREASING, DECREASING, STABLE
    }

    /**
     * 解决率统计
     */
    public static class ResolutionRate {
        private final long totalCount;
        private final long resolvedCount;
        private final double rate;

        public ResolutionRate(long totalCount, long resolvedCount, double rate) {
            this.totalCount = totalCount;
            this.resolvedCount = resolvedCount;
            this.rate = rate;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public long getResolvedCount() {
            return resolvedCount;
        }

        public long getPendingCount() {
            return totalCount - resolvedCount;
        }

        public double getRate() {
            return rate;
        }

        @NotNull
        public String getRatePercentage() {
            return String.format("%.1f%%", rate * 100);
        }
    }
}