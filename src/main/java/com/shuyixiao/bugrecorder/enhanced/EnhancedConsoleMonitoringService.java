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
 * 增强版控制台监控服务
 * 提供更好的资源管理和性能优化
 */
@Service
public final class EnhancedConsoleMonitoringService {

    private static final Logger LOG = Logger.getInstance(EnhancedConsoleMonitoringService.class);

    private final Project project;
    
    // 活动监听器映射
    private final Map<String, EnhancedConsoleOutputListener> activeListeners = new ConcurrentHashMap<>();
    
    // 服务状态
    private final AtomicBoolean isEnabled = new AtomicBoolean(true);
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    
    // 统计信息
    private final AtomicInteger totalAttachedListeners = new AtomicInteger(0);
    private final AtomicInteger totalDetachedListeners = new AtomicInteger(0);
    
    // 配置参数
    private static final int MAX_ACTIVE_LISTENERS = 50;

    public EnhancedConsoleMonitoringService(@NotNull Project project) {
        this.project = project;
        
        LOG.info("Enhanced Console Monitoring Service initialized for project: " + project.getName());
    }

    /**
     * 启用监控
     */
    public void enableMonitoring() {
        if (isDisposed.get()) {
            return;
        }
        
        isEnabled.set(true);
        LOG.info("Enhanced console monitoring enabled for project: " + project.getName());
    }

    /**
     * 禁用监控
     */
    public void disableMonitoring() {
        isEnabled.set(false);
        clearAllListeners();
        LOG.info("Enhanced console monitoring disabled for project: " + project.getName());
    }

    /**
     * 检查监控是否启用
     */
    public boolean isMonitoringEnabled() {
        return isEnabled.get() && !isDisposed.get();
    }

    /**
     * 获取活动监听器数量
     */
    public int getActiveListenerCount() {
        return activeListeners.size();
    }

    /**
     * 添加监听器
     */
    public void addListener(@NotNull String processId, @NotNull EnhancedConsoleOutputListener listener) {
        if (!isMonitoringEnabled()) {
            return;
        }

        // 检查监听器数量限制
        if (activeListeners.size() >= MAX_ACTIVE_LISTENERS) {
            LOG.warn("Maximum active listeners reached (" + MAX_ACTIVE_LISTENERS + "), removing oldest listener");
            // 移除最老的监听器
            String oldestProcessId = activeListeners.keySet().iterator().next();
            removeListener(oldestProcessId);
        }

        // 移除已存在的监听器
        EnhancedConsoleOutputListener existing = activeListeners.put(processId, listener);
        if (existing != null) {
            existing.dispose();
            LOG.debug("Replaced existing listener for process: " + processId);
        } else {
            LOG.debug("Added enhanced console listener for process: " + processId);
        }
        
        totalAttachedListeners.incrementAndGet();
    }

    /**
     * 移除监听器
     */
    public void removeListener(@NotNull String processId) {
        EnhancedConsoleOutputListener listener = activeListeners.remove(processId);
        if (listener != null) {
            try {
                listener.dispose();
                totalDetachedListeners.incrementAndGet();
                LOG.debug("Removed enhanced console listener for process: " + processId);
            } catch (Exception e) {
                LOG.warn("Failed to dispose listener for process: " + processId, e);
            }
        }
    }

    /**
     * 清理所有监听器
     */
    public void clearAllListeners() {
        LOG.debug("Clearing all enhanced console listeners (" + activeListeners.size() + " active)");
        
        for (Map.Entry<String, EnhancedConsoleOutputListener> entry : activeListeners.entrySet()) {
            try {
                entry.getValue().dispose();
                totalDetachedListeners.incrementAndGet();
            } catch (Exception e) {
                LOG.warn("Failed to dispose listener for process: " + entry.getKey(), e);
            }
        }
        
        activeListeners.clear();
    }

    /**
     * 获取监听器
     */
    public EnhancedConsoleOutputListener getListener(@NotNull String processId) {
        return activeListeners.get(processId);
    }

    /**
     * 检查是否存在监听器
     */
    public boolean hasListener(@NotNull String processId) {
        return activeListeners.containsKey(processId);
    }

    /**
     * 处理服务销毁
     */
    public void dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            LOG.info("Disposing Enhanced Console Monitoring Service for project: " + project.getName() +
                    " (Attached: " + totalAttachedListeners.get() + 
                    ", Detached: " + totalDetachedListeners.get() + ")");
            clearAllListeners();
        }
    }

    /**
     * 获取项目
     */
    @NotNull
    public Project getProject() {
        return project;
    }

    /**
     * 获取总附加监听器数
     */
    public int getTotalAttachedListeners() {
        return totalAttachedListeners.get();
    }

    /**
     * 获取总分离监听器数
     */
    public int getTotalDetachedListeners() {
        return totalDetachedListeners.get();
    }
}