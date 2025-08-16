package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.BaiduAPI;
import com.shuyixiao.DomesticAITranslationAPI;
import com.shuyixiao.GoogleCloudTranslationAPI;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.service.BugAnalysisService;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 增强版AI分析管理器
 * 提供更智能的错误分析和解决方案建议
 */
@Service
public final class EnhancedBugAnalysisManager {

    private static final Logger LOG = Logger.getInstance(EnhancedBugAnalysisManager.class);

    private final Project project;
    private final BugRecordService bugRecordService;
    private final BugAnalysisService bugAnalysisService;
    
    // AI引擎
    private final DomesticAITranslationAPI domesticAI;
    private final GoogleCloudTranslationAPI googleTranslate;
    private final BaiduAPI baiduTranslate;
    
    // 线程池优化
    private final ExecutorService analysisExecutor;
    private final ScheduledExecutorService batchExecutor;
    
    // 配置参数
    private static final int MAX_CONCURRENT_ANALYSIS = 2;
    private static final long BATCH_ANALYSIS_DELAY_SECONDS = 10;
    private static final int MAX_ANALYSIS_QUEUE_SIZE = 20;

    public EnhancedBugAnalysisManager(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        this.bugAnalysisService = project.getService(BugAnalysisService.class);
        
        // 初始化AI引擎
        this.domesticAI = new DomesticAITranslationAPI();
        this.googleTranslate = new GoogleCloudTranslationAPI();
        this.baiduTranslate = new BaiduAPI();
        
        // 创建专用线程池处理AI分析
        this.analysisExecutor = new ThreadPoolExecutor(
                1, MAX_CONCURRENT_ANALYSIS,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_ANALYSIS_QUEUE_SIZE),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = defaultFactory.newThread(r);
                        t.setName("BugAnalysis-Manager-" + t.getName());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建批处理定时器
        this.batchExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BugAnalysis-Batch");
            t.setDaemon(true);
            return t;
        });
        
        LOG.info("Enhanced Bug Analysis Manager initialized for project: " + project.getName());
    }

    /**
     * 异步分析Bug记录
     */
    public void analyzeBugRecordAsync(@NotNull BugRecord bugRecord) {
        analysisExecutor.submit(() -> {
            try {
                performAnalysis(bugRecord);
            } catch (Exception e) {
                LOG.error("Failed to analyze bug record: " + bugRecord.getId(), e);
            }
        });
    }

    /**
     * 执行分析
     */
    private void performAnalysis(@NotNull BugRecord bugRecord) {
        try {
            LOG.info("Starting enhanced AI analysis for bug: " + bugRecord.getId() + 
                    " (" + bugRecord.getErrorType().getDisplayName() + ")");
            
            // 使用现有的分析服务进行分析
            BugAnalysisService.AnalysisResult result = bugAnalysisService.analyzeBug(bugRecord);
            
            // 更新Bug记录的AI分析结果
            bugAnalysisService.updateBugWithAnalysis(bugRecord, result);
            
            LOG.info("Enhanced AI analysis completed for bug: " + bugRecord.getId() + 
                    " (Confidence: " + String.format("%.2f", result.getConfidence()) + ")");
            
        } catch (Exception e) {
            LOG.error("Failed to perform enhanced analysis for bug: " + bugRecord.getId(), e);
            
            // 创建失败的分析结果
            BugAnalysisService.AnalysisResult fallbackResult = new BugAnalysisService.AnalysisResult(
                    "分析失败：" + e.getMessage(),
                    "无法提供解决方案建议，请手动排查问题。",
                    0.0
            );
            
            try {
                bugAnalysisService.updateBugWithAnalysis(bugRecord, fallbackResult);
            } catch (Exception updateException) {
                LOG.error("Failed to update bug record with fallback analysis: " + bugRecord.getId(), updateException);
            }
        }
    }

    /**
     * 批量分析Bug记录
     */
    public void analyzeBugRecordsBatch(@NotNull List<BugRecord> bugRecords) {
        if (bugRecords.isEmpty()) {
            return;
        }
        
        // 延迟执行以合并更多的记录
        batchExecutor.schedule(() -> {
            try {
                performBatchAnalysis(bugRecords);
            } catch (Exception e) {
                LOG.error("Failed to perform batch analysis for " + bugRecords.size() + " records", e);
            }
        }, BATCH_ANALYSIS_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 执行批量分析
     */
    private void performBatchAnalysis(@NotNull List<BugRecord> bugRecords) {
        LOG.info("Starting batch analysis for " + bugRecords.size() + " bug records");
        
        // 按错误类型分组以优化分析
        Map<ErrorType, List<BugRecord>> groupedRecords = 
                bugRecords.stream().collect(Collectors.groupingBy(BugRecord::getErrorType));
        
        // 对每组记录进行分析
        for (Map.Entry<ErrorType, List<BugRecord>> entry : groupedRecords.entrySet()) {
            ErrorType errorType = entry.getKey();
            List<BugRecord> records = entry.getValue();
            
            LOG.debug("Analyzing " + records.size() + " records of type: " + errorType.getDisplayName());
            
            // 提交每个记录进行分析
            for (BugRecord record : records) {
                analyzeBugRecordAsync(record);
            }
        }
        
        LOG.info("Batch analysis submission completed for " + bugRecords.size() + " records");
    }

    /**
     * 获取待处理分析任务数量
     */
    public int getPendingAnalysisCount() {
        if (analysisExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) analysisExecutor).getQueue().size();
        }
        return -1; // 未知
    }

    /**
     * 获取活跃分析线程数
     */
    public int getActiveAnalysisCount() {
        if (analysisExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) analysisExecutor).getActiveCount();
        }
        return -1; // 未知
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        LOG.info("Shutting down Enhanced Bug Analysis Manager for project: " + project.getName());
        
        // 关闭分析执行器
        analysisExecutor.shutdown();
        try {
            if (!analysisExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                analysisExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            analysisExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭批处理执行器
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOG.info("Enhanced Bug Analysis Manager shutdown completed for project: " + project.getName());
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
     * 获取Bug分析服务
     */
    @NotNull
    public BugAnalysisService getBugAnalysisService() {
        return bugAnalysisService;
    }
}