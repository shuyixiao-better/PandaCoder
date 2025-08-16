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

/**
 * 增强版Bug记录服务
 * 提供更好的性能、可靠性和功能
 */
@Service
public final class EnhancedBugRecordService {

    private static final Logger LOG = Logger.getInstance(EnhancedBugRecordService.class);

    private final Project project;
    private final BugRecordService bugRecordService;
    
    // 线程池优化
    private final ExecutorService recordExecutor;
    private final ScheduledExecutorService cleanupExecutor;
    
    // 配置参数
    private static final int MAX_PENDING_RECORDS = 1000;
    private static final long CLEANUP_INTERVAL_MINUTES = 30;
    private static final int MAX_RECORD_AGE_DAYS = 90;

    public EnhancedBugRecordService(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        
        // 创建专用线程池处理记录保存
        this.recordExecutor = new ThreadPoolExecutor(
                2, 8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = defaultFactory.newThread(r);
                        t.setName("BugRecord-Enhanced-" + t.getName());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 防止任务丢失
        );
        
        // 创建清理定时器
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BugRecord-Cleanup-Enhanced");
            t.setDaemon(true);
            return t;
        });
        
        // 启动定期清理任务
        startCleanupTask();
        
        LOG.info("Enhanced Bug Record Service initialized for project: " + project.getName());
    }

    /**
     * 异步保存Bug记录
     */
    public void saveBugRecordAsync(@NotNull BugRecord bugRecord) {
        if (isRecordValid(bugRecord)) {
            recordExecutor.submit(() -> {
                try {
                    bugRecordService.saveBugRecord(bugRecord);
                    LOG.debug("Bug record saved asynchronously: " + bugRecord.getId() + 
                            " (" + bugRecord.getErrorType().getDisplayName() + ")");
                } catch (Exception e) {
                    LOG.error("Failed to save bug record asynchronously: " + bugRecord.getId(), e);
                }
            });
        }
    }

    /**
     * 验证记录有效性
     */
    private boolean isRecordValid(@NotNull BugRecord bugRecord) {
        // 检查基本字段
        if (bugRecord.getExceptionClass() == null && 
            bugRecord.getErrorMessage() == null &&
            bugRecord.getRawText() == null) {
            LOG.debug("Skipping invalid bug record (no error information)");
            return false;
        }
        
        // 检查时间戳
        if (bugRecord.getTimestamp() == null) {
            LOG.warn("Bug record with null timestamp, setting current time");
            // 修正时间戳
            BugRecord correctedRecord = bugRecord.withTimestamp(LocalDateTime.now());
            // 注意：我们不能直接修改传入的对象，需要在调用处处理
            return true; // 仍然允许保存，service会处理
        }
        
        // 检查记录年龄
        if (bugRecord.getTimestamp().isBefore(LocalDateTime.now().minusDays(MAX_RECORD_AGE_DAYS))) {
            LOG.debug("Skipping old bug record: " + bugRecord.getId());
            return false;
        }
        
        return true;
    }

    /**
     * 创建并保存测试记录
     */
    public void createAndSaveTestRecord(@NotNull String projectName, @NotNull ErrorType errorType, 
                                     @NotNull String exceptionClass, @NotNull String errorMessage) {
        try {
            BugRecord testRecord = new BugRecord.Builder()
                    .project(projectName)
                    .timestamp(LocalDateTime.now())
                    .errorType(errorType)
                    .exceptionClass(exceptionClass)
                    .errorMessage(errorMessage)
                    .summary(errorMessage.length() > 50 ? errorMessage.substring(0, 47) + "..." : errorMessage)
                    .rawText(exceptionClass + ": " + errorMessage)
                    .status(BugStatus.PENDING)
                    .build();
            
            saveBugRecordAsync(testRecord);
            
        } catch (Exception e) {
            LOG.error("Failed to create test bug record", e);
        }
    }

    /**
     * 获取统计信息
     */
    @NotNull
    public BugRecordService.BugStatistics getStatistics(int days) {
        try {
            return bugRecordService.getStatistics(days);
        } catch (Exception e) {
            LOG.error("Failed to get bug statistics", e);
            return new BugRecordService.BugStatistics(0, 0, java.util.Collections.emptyMap());
        }
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(() -> {
            try {
                performCleanup();
            } catch (Exception e) {
                LOG.warn("Error during periodic cleanup", e);
            }
        }, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 执行清理任务
     */
    private void performCleanup() {
        try {
            // 清理过期记录（保留90天）
            bugRecordService.cleanupOldRecords(90);
            
            // 清理已解决的记录（保留30天）
            bugRecordService.cleanupRecordsByStatus(BugStatus.RESOLVED, 30);
            
            LOG.debug("Periodic cleanup completed for project: " + project.getName());
            
        } catch (Exception e) {
            LOG.warn("Error during periodic cleanup for project: " + project.getName(), e);
        }
    }

    /**
     * 获取待处理记录数量
     */
    public int getPendingRecordCount() {
        if (recordExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) recordExecutor).getQueue().size();
        }
        return -1; // 未知
    }

    /**
     * 获取活跃线程数
     */
    public int getActiveThreadCount() {
        if (recordExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) recordExecutor).getActiveCount();
        }
        return -1; // 未知
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        LOG.info("Shutting down Enhanced Bug Record Service for project: " + project.getName());
        
        // 关闭记录执行器
        recordExecutor.shutdown();
        try {
            if (!recordExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                recordExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            recordExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭清理执行器
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOG.info("Enhanced Bug Record Service shutdown completed for project: " + project.getName());
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
}