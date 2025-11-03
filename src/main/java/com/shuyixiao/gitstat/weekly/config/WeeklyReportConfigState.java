package com.shuyixiao.gitstat.weekly.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.shuyixiao.gitstat.weekly.model.WeeklyReportConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 周报配置持久化服务
 * 用于保存和加载周报配置到项目级别的存储
 */
@State(
    name = "GitStatWeeklyReportConfig",
    storages = @Storage("gitStatWeeklyReportConfig.xml")
)
public class WeeklyReportConfigState implements PersistentStateComponent<WeeklyReportConfigState> {
    
    // 配置数据
    private String apiUrl = "https://ai.gitee.com/v1/chat/completions";
    private String apiKey = "";
    private String model = "Qwen3-235B-A22B-Instruct-2507";
    private String promptTemplate = "请根据以下 Git 提交日志，生成一份本周工作周报。要求：\n" +
            "1. 总结本周主要完成的工作内容\n" +
            "2. 按功能模块分类整理\n" +
            "3. 突出重点和亮点\n" +
            "4. 语言简洁专业\n\n" +
            "Git 提交日志：\n{commits}";
    private double temperature = 0.7;
    private int maxTokens = 2000;
    
    /**
     * 获取项目级别的配置实例
     */
    public static WeeklyReportConfigState getInstance(@NotNull Project project) {
        return project.getService(WeeklyReportConfigState.class);
    }
    
    @Nullable
    @Override
    public WeeklyReportConfigState getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull WeeklyReportConfigState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * 转换为配置对象
     */
    public WeeklyReportConfig toConfig() {
        WeeklyReportConfig config = new WeeklyReportConfig();
        config.setApiUrl(this.apiUrl);
        config.setApiKey(this.apiKey);
        config.setModel(this.model);
        config.setPromptTemplate(this.promptTemplate);
        config.setTemperature(this.temperature);
        config.setMaxTokens(this.maxTokens);
        return config;
    }
    
    /**
     * 从配置对象更新状态
     */
    public void fromConfig(WeeklyReportConfig config) {
        this.apiUrl = config.getApiUrl();
        this.apiKey = config.getApiKey();
        this.model = config.getModel();
        this.promptTemplate = config.getPromptTemplate();
        this.temperature = config.getTemperature();
        this.maxTokens = config.getMaxTokens();
    }
    
    // Getters and Setters
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getPromptTemplate() {
        return promptTemplate;
    }
    
    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}

