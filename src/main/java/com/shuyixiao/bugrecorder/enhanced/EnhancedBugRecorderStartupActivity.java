package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import com.shuyixiao.bugrecorder.service.ConsoleMonitoringService;
import org.jetbrains.annotations.NotNull;

/**
 * 增强版Bug记录器启动活动
 * 在项目启动时初始化增强版Bug记录器相关服务
 */
public class EnhancedBugRecorderStartupActivity implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(EnhancedBugRecorderStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing Enhanced Bug Recorder for project: " + project.getName());

        try {
            // 初始化增强版服务
            EnhancedConsoleMonitoringService monitoringService = project.getService(EnhancedConsoleMonitoringService.class);
            EnhancedExecutionListener executionListener = project.getService(EnhancedExecutionListener.class);
            EnhancedBugRecordManager recordManager = project.getService(EnhancedBugRecordManager.class);
            EnhancedBugAnalysisService analysisService = project.getService(EnhancedBugAnalysisService.class);
            BugAggregationService aggregationService = project.getService(BugAggregationService.class);
            ErrorPatternLearningService learningService = project.getService(ErrorPatternLearningService.class);
            
            // 启用控制台监听（默认启用）
            monitoringService.enableMonitoring();
            
            // 清理超过60天的旧记录
            BugRecordService bugRecordService = project.getService(BugRecordService.class);
            bugRecordService.cleanupOldRecords(60);
            
            // 验证所有记录状态
            bugRecordService.validateAllRecords();

            LOG.info("Enhanced Bug Recorder initialized successfully for project: " + project.getName() +
                    " (Monitoring: " + monitoringService.isMonitoringEnabled() + 
                    ", Listeners: " + executionListener.getActiveListenerCount() + ")");

        } catch (Exception e) {
            LOG.error("Failed to initialize Enhanced Bug Recorder", e);
        }
    }
}