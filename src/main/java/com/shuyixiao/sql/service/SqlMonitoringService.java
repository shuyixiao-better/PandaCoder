package com.shuyixiao.sql.service;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.sql.listener.SqlOutputListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL 监控服务
 * 负责管理 SQL 输出监听器的生命周期
 */
@Service
public final class SqlMonitoringService {
    
    private static final Logger LOG = Logger.getInstance(SqlMonitoringService.class);
    
    private final Project project;
    private final Map<ProcessHandler, SqlOutputListener> activeListeners = new ConcurrentHashMap<>();
    private boolean monitoringEnabled = true;
    
    public SqlMonitoringService(@NotNull Project project) {
        this.project = project;
        
        // 监听运行配置的启动和停止
        setupExecutionListener();
    }
    
    /**
     * 启用 SQL 监听
     */
    public void enableMonitoring() {
        monitoringEnabled = true;
        LOG.info("SQL monitoring enabled for project: " + project.getName());
    }
    
    /**
     * 禁用 SQL 监听
     */
    public void disableMonitoring() {
        monitoringEnabled = false;
        // 清理所有活动的监听器
        clearAllListeners();
        LOG.info("SQL monitoring disabled for project: " + project.getName());
    }
    
    /**
     * 检查监听是否启用
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
    
    /**
     * 获取活动监听器数量
     */
    public int getActiveListenerCount() {
        return activeListeners.size();
    }
    
    /**
     * 为进程处理器添加监听器
     */
    public void attachListener(@NotNull ProcessHandler processHandler) {
        if (!monitoringEnabled) {
            return;
        }
        
        try {
            // 检查是否已经存在监听器
            if (activeListeners.containsKey(processHandler)) {
                LOG.debug("SQL listener already exists for process: " + processHandler);
                return;
            }
            
            // 创建新的监听器
            SqlOutputListener listener = new SqlOutputListener(project);
            processHandler.addProcessListener(listener);
            activeListeners.put(processHandler, listener);
            
            LOG.debug("Attached SQL listener to process: " + processHandler);
            
            // 监听进程终止，自动清理监听器
            processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    removeListener(processHandler);
                }
            });
            
        } catch (Exception e) {
            LOG.error("Failed to attach SQL listener", e);
        }
    }
    
    /**
     * 移除进程处理器的监听器
     */
    public void removeListener(@NotNull ProcessHandler processHandler) {
        SqlOutputListener listener = activeListeners.remove(processHandler);
        if (listener != null) {
            try {
                processHandler.removeProcessListener(listener);
                LOG.debug("Removed SQL listener from process: " + processHandler);
            } catch (Exception e) {
                LOG.warn("Failed to remove SQL listener", e);
            }
        }
    }
    
    /**
     * 清理所有监听器
     */
    public void clearAllListeners() {
        for (Map.Entry<ProcessHandler, SqlOutputListener> entry : activeListeners.entrySet()) {
            try {
                entry.getKey().removeProcessListener(entry.getValue());
            } catch (Exception e) {
                LOG.warn("Failed to remove listener during cleanup", e);
            }
        }
        activeListeners.clear();
        LOG.debug("Cleared all SQL listeners");
    }
    
    /**
     * 设置执行监听器
     */
    private void setupExecutionListener() {
        ApplicationManager.getApplication().getMessageBus()
                .connect(project)
                .subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
                    @Override
                    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                        // 进程即将启动时的处理
                    }
                    
                    @Override
                    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                               @NotNull ProcessHandler handler) {
                        // 进程启动时自动附加监听器
                        ApplicationManager.getApplication().invokeLater(() -> {
                            attachListener(handler);
                        });
                    }
                    
                    @Override
                    public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                        // 进程启动失败时的处理
                    }
                    
                    @Override
                    public void processTerminating(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                                   @NotNull ProcessHandler handler) {
                        // 进程即将终止时的处理
                    }
                    
                    @Override
                    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                                  @NotNull ProcessHandler handler, int exitCode) {
                        // 进程终止时自动移除监听器
                        removeListener(handler);
                    }
                });
    }
    
    /**
     * 简化的进程适配器
     */
    private static abstract class ProcessAdapter implements com.intellij.execution.process.ProcessListener {
        @Override
        public void startNotified(@NotNull com.intellij.execution.process.ProcessEvent event) {}
        
        @Override
        public void processWillTerminate(@NotNull com.intellij.execution.process.ProcessEvent event, boolean willBeDestroyed) {}
        
        @Override
        @SuppressWarnings("rawtypes")
        public void onTextAvailable(@NotNull com.intellij.execution.process.ProcessEvent event, @NotNull com.intellij.openapi.util.Key outputType) {}
    }
}

