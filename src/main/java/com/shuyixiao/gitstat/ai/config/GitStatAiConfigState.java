package com.shuyixiao.gitstat.ai.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 识别配置状态
 * 保存用户的 AI 代码识别相关配置
 */
@State(
    name = "GitStatAiConfig",
    storages = @Storage("gitStatAiConfig.xml")
)
public class GitStatAiConfigState implements PersistentStateComponent<GitStatAiConfigState> {
    
    // AI 识别关键词列表（用户可自定义）
    public List<String> aiKeywords = new ArrayList<>();
    
    // AI 工具映射（用户可自定义）
    public List<AiToolMapping> aiToolMappings = new ArrayList<>();
    
    // 是否启用 AI 统计
    public boolean enableAiStats = true;
    
    // 是否启用实时监控
    public boolean enableRealtimeMonitoring = true;
    
    // 是否启用启发式识别（Git Diff 分析）
    public boolean enableHeuristicDetection = true;
    
    // 启发式识别阈值（多少行代码一次性提交视为 AI 生成）
    public int heuristicLinesThreshold = 100;
    
    // AI 识别敏感度（70-95，越高越严格）
    public int aiDetectionThreshold = 70;
    
    // 是否在总览中显示 AI 统计
    public boolean showAiStatsInOverview = true;
    
    // 是否在编辑器中高亮 AI 代码
    public boolean showAiHighlight = false;
    
    // 是否在提交时提示 AI 代码
    public boolean promptAiCodeOnCommit = true;
    
    public GitStatAiConfigState() {
        // 初始化默认关键词
        aiKeywords.add("[AI]");
        aiKeywords.add("[ai]");
        aiKeywords.add("[Copilot]");
        aiKeywords.add("[Cursor]");
        aiKeywords.add("[ChatGPT]");
        aiKeywords.add("[GPT]");
        aiKeywords.add("[Claude]");
        aiKeywords.add("AI:");
        aiKeywords.add("AI Generated");
        aiKeywords.add("AI 生成");
        aiKeywords.add("AI 辅助");
        
        // 初始化默认工具映射
        aiToolMappings.add(new AiToolMapping("copilot", "GitHub Copilot"));
        aiToolMappings.add(new AiToolMapping("cursor", "Cursor AI"));
        aiToolMappings.add(new AiToolMapping("chatgpt", "ChatGPT"));
        aiToolMappings.add(new AiToolMapping("gpt", "ChatGPT"));
        aiToolMappings.add(new AiToolMapping("claude", "Claude"));
        aiToolMappings.add(new AiToolMapping("tabnine", "Tabnine"));
        aiToolMappings.add(new AiToolMapping("codewhisperer", "Amazon CodeWhisperer"));
    }
    
    @Nullable
    @Override
    public GitStatAiConfigState getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull GitStatAiConfigState state) {
        this.aiKeywords = state.aiKeywords;
        this.aiToolMappings = state.aiToolMappings;
        this.enableAiStats = state.enableAiStats;
        this.enableRealtimeMonitoring = state.enableRealtimeMonitoring;
        this.enableHeuristicDetection = state.enableHeuristicDetection;
        this.heuristicLinesThreshold = state.heuristicLinesThreshold;
        this.aiDetectionThreshold = state.aiDetectionThreshold;
        this.showAiStatsInOverview = state.showAiStatsInOverview;
        this.showAiHighlight = state.showAiHighlight;
        this.promptAiCodeOnCommit = state.promptAiCodeOnCommit;
    }
    
    public static GitStatAiConfigState getInstance() {
        return ApplicationManager.getApplication().getService(GitStatAiConfigState.class);
    }
    
    /**
     * AI 工具映射
     */
    public static class AiToolMapping {
        public String keyword;
        public String toolName;
        
        public AiToolMapping() {}
        
        public AiToolMapping(String keyword, String toolName) {
            this.keyword = keyword;
            this.toolName = toolName;
        }
    }
}

