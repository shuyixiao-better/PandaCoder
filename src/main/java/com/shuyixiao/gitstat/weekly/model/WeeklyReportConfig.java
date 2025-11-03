package com.shuyixiao.gitstat.weekly.model;

/**
 * 周报配置模型
 * 用于存储 AI API 配置和提示词模板
 */
public class WeeklyReportConfig {
    
    // AI API 配置
    private String apiUrl = "https://ai.gitee.com/v1/chat/completions";
    private String apiKey = "";
    private String model = "Qwen3-235B-A22B-Instruct-2507";
    
    // 提示词模板
    private String promptTemplate = "请根据以下 Git 提交日志，生成一份本周工作周报。要求：\n" +
            "1. 总结本周主要完成的工作内容\n" +
            "2. 按功能模块分类整理\n" +
            "3. 突出重点和亮点\n" +
            "4. 语言简洁专业\n\n" +
            "Git 提交日志：\n{commits}";
    
    // 温度参数（控制生成的随机性）
    private double temperature = 0.7;
    
    // 最大 token 数
    private int maxTokens = 2000;
    
    public WeeklyReportConfig() {
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

