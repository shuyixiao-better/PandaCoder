package com.shuyixiao.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PandaCoder 插件设置服务
 * 用于跟踪首次安装、使用次数、里程碑等信息
 * 支持智能推广和用户行为分析
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
@Service(Service.Level.PROJECT)
@State(
    name = "PandaCoderSettings",
    storages = @Storage("pandacoder.xml")
)
public final class PandaCoderSettings implements PersistentStateComponent<PandaCoderSettings.State> {
    
    private State state = new State();
    
    /**
     * 获取项目级别的设置实例
     */
    public static PandaCoderSettings getInstance(@NotNull Project project) {
        return project.getService(PandaCoderSettings.class);
    }
    
    @Nullable
    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
    
    // ==================== 首次安装相关 ====================
    
    /**
     * 是否是首次安装
     */
    public boolean isFirstInstall() {
        return state.firstInstall;
    }
    
    /**
     * 标记首次安装已完成
     */
    public void setFirstInstallComplete() {
        state.firstInstall = false;
    }
    
    /**
     * 获取上次显示欢迎消息的时间
     */
    public long getLastWelcomeTime() {
        return state.lastWelcomeTime;
    }
    
    /**
     * 更新上次显示欢迎消息的时间
     */
    public void updateLastWelcomeTime() {
        state.lastWelcomeTime = System.currentTimeMillis();
    }
    
    // ==================== 使用统计相关 ====================
    
    /**
     * 获取使用次数
     */
    public int getUsageCount() {
        return state.usageCount;
    }
    
    /**
     * 增加使用计数
     */
    public void incrementUsageCount() {
        state.usageCount++;
    }
    
    /**
     * 是否应该显示里程碑提示
     * 在 10、50、100 次使用时显示
     */
    public boolean shouldShowMilestone() {
        int count = state.usageCount;
        return count == 10 || count == 50 || count == 100;
    }
    
    /**
     * 获取上次显示里程碑的使用次数
     */
    public int getLastMilestoneCount() {
        return state.lastMilestoneCount;
    }
    
    /**
     * 更新上次显示里程碑的使用次数
     */
    public void updateLastMilestoneCount(int count) {
        state.lastMilestoneCount = count;
    }
    
    /**
     * 检查是否应该显示里程碑（避免重复显示）
     */
    public boolean shouldShowMilestoneNow() {
        int count = state.usageCount;
        int lastCount = state.lastMilestoneCount;
        
        // 只在达到里程碑且与上次显示的不同时才显示
        return shouldShowMilestone() && count != lastCount;
    }
    
    // ==================== Tool Window 相关 ====================
    
    /**
     * Tool Window 是否已经自动展开过
     */
    public boolean isToolWindowAutoShown() {
        return state.toolWindowAutoShown;
    }
    
    /**
     * 标记 Tool Window 已自动展开过
     */
    public void setToolWindowAutoShown() {
        state.toolWindowAutoShown = true;
    }
    
    // ==================== 持久化状态类 ====================
    
    /**
     * 状态类 - 存储所有需要持久化的数据
     */
    public static class State {
        /**
         * 是否是首次安装
         */
        public boolean firstInstall = true;
        
        /**
         * 使用次数统计
         */
        public int usageCount = 0;
        
        /**
         * 上次显示欢迎消息的时间戳
         */
        public long lastWelcomeTime = 0;
        
        /**
         * 上次显示里程碑的使用次数
         */
        public int lastMilestoneCount = 0;
        
        /**
         * Tool Window 是否已自动展开过
         */
        public boolean toolWindowAutoShown = false;
        
        /**
         * 插件安装时间
         */
        public long installTime = System.currentTimeMillis();
    }
}

