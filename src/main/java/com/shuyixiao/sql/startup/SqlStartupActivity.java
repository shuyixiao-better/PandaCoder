package com.shuyixiao.sql.startup;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.shuyixiao.sql.service.SqlMonitoringService;
import org.jetbrains.annotations.NotNull;

/**
 * SQL Monitor 启动活动
 * 在项目启动时初始化 SQL 监控服务，并为已运行的进程附加监听器
 */
public class SqlStartupActivity implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(SqlStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                LOG.warn("==============================================");
                LOG.warn("[SQL Monitor] 🚀 启动活动开始执行");
                LOG.warn("[SQL Monitor] 项目: " + project.getName());
                LOG.warn("==============================================");
                
                // 获取 SQL 监控服务
                SqlMonitoringService monitoringService = project.getService(SqlMonitoringService.class);
                
                if (monitoringService == null) {
                    LOG.error("[SQL Monitor] ❌ 无法获取 SqlMonitoringService");
                    return;
                }
                
                // 启用监控
                monitoringService.enableMonitoring();
                LOG.info("[SQL Monitor] ✅ SQL 监控已启用");
                
                // 为已运行的进程附加监听器
                ProcessHandler[] runningProcesses = ExecutionManager.getInstance(project).getRunningProcesses();
                LOG.warn("[SQL Monitor] 🔍 检测到 " + runningProcesses.length + " 个正在运行的进程");
                
                for (ProcessHandler handler : runningProcesses) {
                    if (!handler.isProcessTerminated() && !handler.isProcessTerminating()) {
                        monitoringService.attachListener(handler);
                        LOG.warn("[SQL Monitor] ✅ 已为运行中的进程附加监听器: " + handler);
                    }
                }
                
                LOG.warn("[SQL Monitor] ✅ 启动活动执行完成");
                LOG.warn("==============================================");
                
            } catch (Exception e) {
                LOG.error("[SQL Monitor] ❌ 启动活动执行失败", e);
            }
        });
    }
}

