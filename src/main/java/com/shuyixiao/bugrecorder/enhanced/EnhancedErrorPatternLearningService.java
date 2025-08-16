package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 增强版错误模式学习服务
 * 通过机器学习技术不断优化错误识别准确性
 */
@Service
public final class EnhancedErrorPatternLearningService {

    private static final Logger LOG = Logger.getInstance(EnhancedErrorPatternLearningService.class);

    private final Project project;
    private final BugRecordService bugRecordService;
    
    // 学习到的模式缓存
    private final Map<String, LearnedPattern> learnedPatterns = new ConcurrentHashMap<>();
    
    // 定时器用于定期训练模型
    private final ScheduledExecutorService trainingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ErrorPattern-Training");
        t.setDaemon(true);
        return t;
    });
    
    // 配置参数
    private static final long TRAINING_INTERVAL_HOURS = 6;
    private static final int MIN_SAMPLES_FOR_TRAINING = 50;
    private static final int MAX_PATTERN_AGE_DAYS = 30;

    public EnhancedErrorPatternLearningService(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        
        // 启动定期训练任务
        startTrainingTask();
        
        LOG.info("Enhanced Error Pattern Learning Service initialized for project: " + project.getName());
    }

    /**
     * 从新错误中学习
     */
    public void learnFromNewError(@NotNull BugRecord bugRecord) {
        try {
            String patternKey = generatePatternKey(bugRecord);
            
            // 更新或创建新模式
            LearnedPattern pattern = learnedPatterns.computeIfAbsent(patternKey, 
                k -> new LearnedPattern(bugRecord.getErrorType(), 1));
            
            // 更新统计数据
            pattern.incrementFrequency();
            pattern.updateLastSeen();
            
            LOG.debug("Learned from error pattern: " + patternKey + " (frequency: " + pattern.getFrequency() + ")");
            
        } catch (Exception e) {
            LOG.warn("Error learning from new bug record: " + bugRecord.getId(), e);
        }
    }

    /**
     * 基于学习模式优化错误分类
     */
    public ErrorType optimizeErrorClassification(@NotNull BugRecord bugRecord, @NotNull ErrorType originalType) {
        try {
            String patternKey = generatePatternKey(bugRecord);
            LearnedPattern learnedPattern = learnedPatterns.get(patternKey);
            
            // 如果学习到的模式具有高置信度，则使用学习结果
            if (learnedPattern != null && learnedPattern.getFrequency() >= 5 && 
                learnedPattern.getConfidence() >= 0.8) {
                LOG.debug("Optimizing error classification using learned pattern: " + patternKey + 
                         " -> " + learnedPattern.getErrorType().getDisplayName());
                return learnedPattern.getErrorType();
            }
            
        } catch (Exception e) {
            LOG.warn("Error optimizing classification for bug record: " + bugRecord.getId(), e);
        }
        
        return originalType;
    }

    /**
     * 生成模式键
     */
    private String generatePatternKey(@NotNull BugRecord bugRecord) {
        StringBuilder key = new StringBuilder();
        
        // 异常类名标准化
        if (bugRecord.getExceptionClass() != null) {
            key.append(normalizeClassName(bugRecord.getExceptionClass())).append("|");
        }
        
        // 顶层堆栈帧
        if (bugRecord.getTopFrame() != null) {
            key.append(normalizeClassName(bugRecord.getTopFrame().getClassName()))
               .append(".")
               .append(bugRecord.getTopFrame().getMethodName())
               .append("|");
        }
        
        // 错误消息模式（去除动态内容）
        if (bugRecord.getErrorMessage() != null) {
            key.append(extractMessagePattern(bugRecord.getErrorMessage())).append("|");
        }
        
        return key.toString();
    }

    /**
     * 标准化类名
     */
    private String normalizeClassName(String className) {
        if (className == null) return "";
        
        // 移除包名，只保留类名
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(lastDot + 1);
        }
        return className;
    }

    /**
     * 提取消息模式
     */
    private String extractMessagePattern(String message) {
        if (message == null) return "";
        
        // 移除常见的动态内容
        return message.replaceAll("\\b\\d+\\b", "NUMBER")
                     .replaceAll("\\b[0-9a-f]{8,}\\b", "ID")
                     .replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\b", "DATE")
                     .replaceAll("\\b\\d{2}:\\d{2}:\\d{2}\\b", "TIME");
    }

    /**
     * 启动训练任务
     */
    private void startTrainingTask() {
        trainingScheduler.scheduleWithFixedDelay(() -> {
            try {
                performModelTraining();
            } catch (Exception e) {
                LOG.warn("Error in model training", e);
            }
        }, TRAINING_INTERVAL_HOURS, TRAINING_INTERVAL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 执行模型训练
     */
    private void performModelTraining() {
        try {
            LOG.debug("Starting error pattern model training for project: " + project.getName());
            
            // 获取最近的错误记录进行训练
            List<BugRecord> recentRecords = bugRecordService.getRecentRecords(30); // 最近30天
            
            if (recentRecords.size() < MIN_SAMPLES_FOR_TRAINING) {
                LOG.debug("Insufficient samples for training (" + recentRecords.size() + " < " + 
                         MIN_SAMPLES_FOR_TRAINING + ")");
                return;
            }
            
            // 更新模式置信度
            updatePatternConfidences(recentRecords);
            
            // 清理过期模式
            cleanupExpiredPatterns();
            
            LOG.info("Error pattern model training completed. Patterns: " + learnedPatterns.size());
            
        } catch (Exception e) {
            LOG.error("Error during model training", e);
        }
    }

    /**
     * 更新模式置信度
     */
    private void updatePatternConfidences(List<BugRecord> records) {
        // 按模式分组计算置信度
        Map<String, List<BugRecord>> groupedByPattern = records.stream()
                .filter(r -> r.getFingerprint() != null)
                .collect(Collectors.groupingBy(this::generatePatternKey));
        
        // 更新每个模式的置信度
        for (Map.Entry<String, List<BugRecord>> entry : groupedByPattern.entrySet()) {
            String patternKey = entry.getKey();
            List<BugRecord> patternRecords = entry.getValue();
            
            // 计算一致性（相同错误类型的占比）
            Map<ErrorType, Long> typeCounts = patternRecords.stream()
                    .collect(Collectors.groupingBy(BugRecord::getErrorType, Collectors.counting()));
            
            long totalCount = patternRecords.size();
            long maxCount = typeCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
            
            double consistency = totalCount > 0 ? (double) maxCount / totalCount : 0.0;
            
            // 更新模式
            LearnedPattern pattern = learnedPatterns.get(patternKey);
            if (pattern != null) {
                pattern.updateConfidence(consistency);
            }
        }
    }

    /**
     * 清理过期模式
     */
    private void cleanupExpiredPatterns() {
        long beforeCount = learnedPatterns.size();
        
        learnedPatterns.entrySet().removeIf(entry -> {
            LearnedPattern pattern = entry.getValue();
            // 移除过期的模式（超过30天未出现）
            return pattern.getLastSeenTimestamp().isBefore(LocalDateTime.now().minusDays(MAX_PATTERN_AGE_DAYS));
        });
        
        long removedCount = beforeCount - learnedPatterns.size();
        if (removedCount > 0) {
            LOG.debug("Cleaned up " + removedCount + " expired patterns");
        }
    }

    /**
     * 获取学习到的模式数量
     */
    public int getLearnedPatternCount() {
        return learnedPatterns.size();
    }

    /**
     * 获取高置信度模式数量
     */
    public long getHighConfidencePatternCount() {
        return learnedPatterns.values().stream()
                .filter(p -> p.getConfidence() >= 0.8)
                .count();
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        LOG.info("Shutting down Enhanced Error Pattern Learning Service for project: " + project.getName());
        
        trainingScheduler.shutdown();
        try {
            if (!trainingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                trainingScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            trainingScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        learnedPatterns.clear();
        
        LOG.info("Enhanced Error Pattern Learning Service shutdown completed for project: " + project.getName());
    }

    /**
     * 学习到的模式
     */
    private static class LearnedPattern {
        private final ErrorType errorType;
        private int frequency;
        private double confidence;
        private LocalDateTime lastSeenTimestamp;
        
        public LearnedPattern(@NotNull ErrorType errorType, int initialFrequency) {
            this.errorType = errorType;
            this.frequency = initialFrequency;
            this.confidence = 0.5; // 初始置信度
            this.lastSeenTimestamp = LocalDateTime.now();
        }
        
        public void incrementFrequency() {
            this.frequency++;
            this.lastSeenTimestamp = LocalDateTime.now();
        }
        
        public void updateConfidence(double newConfidence) {
            // 使用指数移动平均平滑置信度更新
            this.confidence = 0.7 * this.confidence + 0.3 * newConfidence;
        }
        
        public void updateLastSeen() {
            this.lastSeenTimestamp = LocalDateTime.now();
        }
        
        @NotNull
        public ErrorType getErrorType() {
            return errorType;
        }
        
        public int getFrequency() {
            return frequency;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        @NotNull
        public LocalDateTime getLastSeenTimestamp() {
            return lastSeenTimestamp;
        }
        
        public boolean isExpired() {
            // 模式如果超过30天未出现，则认为过期
            return lastSeenTimestamp.isBefore(LocalDateTime.now().minusDays(MAX_PATTERN_AGE_DAYS));
        }
    }
}