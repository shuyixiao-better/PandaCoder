package com.shuyixiao.bugrecorder.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import com.shuyixiao.bugrecorder.service.ConsoleMonitoringService;
import com.shuyixiao.bugrecorder.service.BugAnalysisService;
import org.jetbrains.annotations.NotNull;

/**
 * Bug记录器启动活动
 * 在项目启动时初始化Bug记录器相关服务
 */
public class BugRecorderStartupActivity implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(BugRecorderStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing Bug Recorder for project: " + project.getName());

        try {
            // 初始化服务
            BugRecordService bugRecordService = project.getService(BugRecordService.class);
            ConsoleMonitoringService monitoringService = project.getService(ConsoleMonitoringService.class);
            BugAnalysisService analysisService = project.getService(BugAnalysisService.class);

            // 启用控制台监听（默认启用）
            monitoringService.enableMonitoring();

            // 清理超过60天的旧记录
            bugRecordService.cleanupOldRecords(60);

            LOG.info("Bug Recorder initialized successfully for project: " + project.getName());

        } catch (Exception e) {
            LOG.error("Failed to initialize Bug Recorder", e);
        }
    }
}