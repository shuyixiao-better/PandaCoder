package com.shuyixiao.esdsl.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.shuyixiao.esdsl.service.EsDslMonitoringService;
import org.jetbrains.annotations.NotNull;

/**
 * ES DSL 启动活动
 * 在项目启动时初始化 ES DSL 监控服务
 */
public class EsDslStartupActivity implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(EsDslStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        try {
            // 初始化 ES DSL 监控服务
            EsDslMonitoringService monitoringService = project.getService(EsDslMonitoringService.class);
            
            if (monitoringService != null) {
                LOG.info("ES DSL monitoring service initialized for project: " + project.getName());
            } else {
                LOG.warn("Failed to initialize ES DSL monitoring service");
            }
            
        } catch (Exception e) {
            LOG.error("Error initializing ES DSL monitoring service", e);
        }
    }
}

