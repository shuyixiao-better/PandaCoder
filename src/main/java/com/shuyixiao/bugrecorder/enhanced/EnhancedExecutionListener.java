package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 增强版执行监听器
 * 负责协调控制台监听器的生命周期
 */
@Service
public final class EnhancedExecutionListener {

    private static final Logger LOG = Logger.getInstance(EnhancedExecutionListener.class);

    private final Project project;
    private final EnhancedConsoleMonitoringService monitoringService;
    
    // 进程ID到监听器的映射
    private final Map<String, EnhancedConsoleOutputListener> processListeners = new ConcurrentHashMap<>();
    
    // 状态标志
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    
    // 统计信息
    private final AtomicInteger totalStartedProcesses = new AtomicInteger(0);
    private final AtomicInteger totalTerminatedProcesses = new AtomicInteger(0);
    private final AtomicInteger totalFailedStarts = new AtomicInteger(0);

    public EnhancedExecutionListener(@NotNull Project project) {
        this.project = project;
        this.monitoringService = project.getService(EnhancedConsoleMonitoringService.class);
        
        // 初始化执行监听器
        initializeExecutionListener();
    }

    /**
     * 初始化执行监听器
     */
    private void initializeExecutionListener() {
        if (!isInitialized.compareAndSet(false, true)) {
            return;
        }

        try {
            ApplicationManager.getApplication().getMessageBus()
                    .connect(project)
                    .subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
                        @Override
                        public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                            handleProcessStartScheduled(executorId, env);
                        }

                        @Override
                        public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, 
                                                 @NotNull ProcessHandler handler) {
                            handleProcessStarted(executorId, env, handler);
                        }

                        @Override
                        public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                            handleProcessNotStarted(executorId, env);
                        }

                        @Override
                        public void processTerminating(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                                     @NotNull ProcessHandler handler) {
                            handleProcessTerminating(executorId, env, handler);
                        }

                        @Override
                        public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                                    @NotNull ProcessHandler handler, int exitCode) {
                            handleProcessTerminated(executorId, env, handler, exitCode);
                        }
                    });

            LOG.info("Enhanced Execution Listener initialized for project: " + project.getName());
        } catch (Exception e) {
            LOG.error("Failed to initialize Enhanced Execution Listener", e);
        }
    }

    /**
     * 处理进程启动调度
     */
    private void handleProcessStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (isDisposed.get()) {
            return;
        }

        LOG.debug("Process start scheduled: " + executorId + " in project: " + project.getName());
    }

    /**
     * 处理进程启动
     */
    private void handleProcessStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                   @NotNull ProcessHandler handler) {
        if (isDisposed.get() || !monitoringService.isMonitoringEnabled()) {
            return;
        }

        try {
            String processId = generateProcessId(handler);
            
            // 创建增强版监听器
            EnhancedConsoleOutputListener listener = new EnhancedConsoleOutputListener(project);
            
            // 注册监听器
            handler.addProcessListener(listener);
            monitoringService.addListener(processId, listener);
            processListeners.put(processId, listener);
            
            totalStartedProcesses.incrementAndGet();
            
            LOG.debug("Enhanced console listener attached to process: " + processId +
                     " (total started: " + totalStartedProcesses.get() + ")");

        } catch (Exception e) {
            LOG.error("Failed to attach enhanced console listener to process", e);
        }
    }

    /**
     * 处理进程未启动
     */
    private void handleProcessNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (isDisposed.get()) {
            return;
        }

        totalFailedStarts.incrementAndGet();
        LOG.debug("Process not started: " + executorId + " in project: " + project.getName() + 
                 " (total failed: " + totalFailedStarts.get() + ")");
    }

    /**
     * 处理进程终止中
     */
    private void handleProcessTerminating(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                        @NotNull ProcessHandler handler) {
        if (isDisposed.get()) {
            return;
        }

        try {
            String processId = generateProcessId(handler);
            
            // 清理监听器
            EnhancedConsoleOutputListener listener = processListeners.remove(processId);
            if (listener != null) {
                monitoringService.removeListener(processId);
                LOG.debug("Enhanced console listener cleaned up for terminating process: " + processId);
            }
        } catch (Exception e) {
            LOG.warn("Error cleaning up listener for terminating process", e);
        }
    }

    /**
     * 处理进程终止
     */
    private void handleProcessTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                       @NotNull ProcessHandler handler, int exitCode) {
        if (isDisposed.get()) {
            return;
        }

        try {
            String processId = generateProcessId(handler);
            
            // 清理监听器
            EnhancedConsoleOutputListener listener = processListeners.remove(processId);
            if (listener != null) {
                listener.dispose();
                monitoringService.removeListener(processId);
                totalTerminatedProcesses.incrementAndGet();
                LOG.debug("Enhanced console listener disposed for terminated process: " + processId + 
                         " (exit code: " + exitCode + ", total terminated: " + totalTerminatedProcesses.get() + ")");
            }
        } catch (Exception e) {
            LOG.warn("Error disposing listener for terminated process", e);
        }
    }

    /**
     * 生成进程ID
     */
    private String generateProcessId(@NotNull ProcessHandler handler) {
        return "process_" + handler.hashCode() + "_" + System.currentTimeMillis();
    }

    /**
     * 获取活动监听器数量
     */
    public int getActiveListenerCount() {
        return processListeners.size();
    }

    /**
     * 清理所有监听器
     */
    public void clearAllListeners() {
        LOG.debug("Clearing all enhanced process listeners (" + processListeners.size() + " active)");
        
        for (Map.Entry<String, EnhancedConsoleOutputListener> entry : processListeners.entrySet()) {
            try {
                entry.getValue().dispose();
                monitoringService.removeListener(entry.getKey());
            } catch (Exception e) {
                LOG.warn("Error disposing listener for process: " + entry.getKey(), e);
            }
        }
        
        processListeners.clear();
    }

    /**
     * 处理服务销毁
     */
    public void dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            LOG.info("Disposing Enhanced Execution Listener for project: " + project.getName() +
                    " (started: " + totalStartedProcesses.get() + 
                    ", terminated: " + totalTerminatedProcesses.get() + 
                    ", failed: " + totalFailedStarts.get() + ")");
            clearAllListeners();
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized.get();
    }

    /**
     * 检查是否已销毁
     */
    public boolean isDisposed() {
        return isDisposed.get();
    }

    /**
     * 获取统计信息
     */
    @NotNull
    public ExecutionStatistics getStatistics() {
        return new ExecutionStatistics(
                totalStartedProcesses.get(),
                totalTerminatedProcesses.get(),
                totalFailedStarts.get(),
                processListeners.size()
        );
    }

    /**
     * 执行统计信息
     */
    public static class ExecutionStatistics {
        private final int totalStartedProcesses;
        private final int totalTerminatedProcesses;
        private final int totalFailedStarts;
        private final int activeListeners;

        public ExecutionStatistics(int totalStartedProcesses, int totalTerminatedProcesses, 
                                 int totalFailedStarts, int activeListeners) {
            this.totalStartedProcesses = totalStartedProcesses;
            this.totalTerminatedProcesses = totalTerminatedProcesses;
            this.totalFailedStarts = totalFailedStarts;
            this.activeListeners = activeListeners;
        }

        public int getTotalStartedProcesses() {
            return totalStartedProcesses;
        }

        public int getTotalTerminatedProcesses() {
            return totalTerminatedProcesses;
        }

        public int getTotalFailedStarts() {
            return totalFailedStarts;
        }

        public int getActiveListeners() {
            return activeListeners;
        }

        public int getRunningProcesses() {
            return totalStartedProcesses - totalTerminatedProcesses - totalFailedStarts;
        }
    }
}