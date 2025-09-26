package com.shuyixiao.setting;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName PluginSettings.java
 * author 舒一笑不秃头 yixiaoshu88@163.com
 * version 1.0.0
 * Description TODO
 * createTime 2024年09月07日 21:01:00
 */
@State(
    name = "PandaCoderSettings",
    storages = @Storage("pandacoder-settings.xml")
)
public class PluginSettings implements PersistentStateComponent<PluginSettings> {

    private String template = "/**\n" +
            " * Copyright © ${YEAR} integration-projects-maven. All rights reserved.\n" +
            " * ClassName ${NAME}.java\n" +
            " * author 舒一笑不秃头\n" +
            " * version 1.0.0\n" +
            " * Description TODO\n" +
            " * createTime ${TIME}\n" +
            " 技术分享 · 公众号：舒一笑的架构笔记\n" +
            " */\n";

                // 类名前缀配置，多个前缀用逗号分隔
                private String classPrefixes = "Service,Repository,Controller,Component,Util,Manager,Factory,Builder,Handler";

                // 百度翻译API密钥
                private String baiduApiKey = "";

                // 百度翻译应用ID
                private String baiduAppId = "";

    // Google Cloud Translation 配置
    private boolean enableGoogleTranslation = false;
    private String googleApiKey = "";
    private String googleProjectId = "";
    private String googleRegion = "global";

    // 国内大模型配置
    private boolean enableDomesticAI = false;
    private String domesticAIModel = "hunyuan"; // 默认使用腾讯混元，支持从配置文件读取
    private String domesticAIApiKey = "";
    
    // 翻译提示词配置
    private String translationPrompt =
            "你是一位专业软件工程师，负责将技术文档中文本翻译为规范的英文编程术语。请遵循：\n" +
            "1. 【翻译规范】用编程术语表达技术概念，非逐字翻译（例：'配置文件路径'→configPath）\n" +
            "2. 【命名规则】输出直接可用作代码标识符的形式（类名用大驼峰，方法/变量用小驼峰）\n" +
            "3. 【术语处理】专业术语保持行业标准（例：'缓存'→cache而非buffer）\n" +
            "4. 【长文本优化】超过3个技术概念时：\n" +
            "   a) 优先提取核心术语\n" +
            "   b) 保持技术逻辑连贯性\n" +
            "   c) 省略非技术性描述词（'这个'、'一种'等）\n" +
            "5. 【输出要求】只返回最终翻译结果\n\n" +
            "待翻译中文：";
    private boolean useCustomPrompt = false;

    // Bug记录存储配置
    private boolean enableLocalBugStorage = false; // 默认关闭本地存储

    // 单例模式获取实例
    public static PluginSettings getInstance() {
        // 使用新的API代替已弃用的ServiceManager
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(PluginSettings.class);
    }

    // 获取模板，确保不会返回null
    public String getTemplate() {
        if (template == null || template.isEmpty()) {
            template = "/**\n" +
                    " * Copyright © ${YEAR} integration-projects-maven. All rights reserved.\n" +
                    " * ClassName ${NAME}.java\n" +
                    " * author 舒一笑不秃头\n" +
                    " * version 1.0.0\n" +
                    " * Description TODO\n" +
                    " * createTime ${TIME}\n" +
                    " 技术分享 · 公众号：舒一笑的架构笔记\n" +
                    " */\n"; // 设置默认模板
        }
        return template;
    }

    // 设置模板
    public void setTemplate(String template) {
        this.template = template;
    }

    public String getClassPrefixes() {
        if (classPrefixes == null || classPrefixes.isEmpty()) {
            classPrefixes = "Service,Repository,Controller,Component,Util,Manager,Factory,Builder,Handler";
        }
        return classPrefixes;
    }

    public void setClassPrefixes(String classPrefixes) {
        this.classPrefixes = classPrefixes;
    }

    public String getBaiduApiKey() {
        return baiduApiKey;
    }

    public void setBaiduApiKey(String baiduApiKey) {
        this.baiduApiKey = baiduApiKey;
    }

    public String getBaiduAppId() {
        return baiduAppId;
    }

    public void setBaiduAppId(String baiduAppId) {
        this.baiduAppId = baiduAppId;
    }

    public boolean isEnableGoogleTranslation() {
        return enableGoogleTranslation;
    }
    public void setEnableGoogleTranslation(boolean enableGoogleTranslation) {
        this.enableGoogleTranslation = enableGoogleTranslation;
    }
    public String getGoogleApiKey() {
        return googleApiKey;
    }
    public void setGoogleApiKey(String googleApiKey) {
        this.googleApiKey = googleApiKey;
    }
    public String getGoogleProjectId() {
        return googleProjectId;
    }
    public void setGoogleProjectId(String googleProjectId) {
        this.googleProjectId = googleProjectId;
    }
    public String getGoogleRegion() {
        return googleRegion;
    }
    public void setGoogleRegion(String googleRegion) {
        this.googleRegion = googleRegion;
    }
    
    // 国内大模型相关方法
    public boolean isEnableDomesticAI() {
        return enableDomesticAI;
    }
    public void setEnableDomesticAI(boolean enableDomesticAI) {
        this.enableDomesticAI = enableDomesticAI;
    }
    public String getDomesticAIModel() {
        if (domesticAIModel == null || domesticAIModel.isEmpty()) {
            // 从配置文件读取默认模型
            domesticAIModel = com.shuyixiao.config.TranslationModelConfig.getInstance().getDefaultDomesticAIModel();
        }
        return domesticAIModel;
    }
    public void setDomesticAIModel(String domesticAIModel) {
        this.domesticAIModel = domesticAIModel;
    }
    public String getDomesticAIApiKey() {
        return domesticAIApiKey;
    }
    public void setDomesticAIApiKey(String domesticAIApiKey) {
        this.domesticAIApiKey = domesticAIApiKey;
    }

    @Nullable
    @Override
    public PluginSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PluginSettings state) {
        XmlSerializerUtil.copyBean(state, this);
        if (this.template == null || this.template.isEmpty()) {
            this.template = "/**\n" +
                    " * Copyright © ${YEAR} integration-projects-maven. All rights reserved.\n" +
                    " * ClassName ${NAME}.java\n" +
                    " * author 舒一笑不秃头\n" +
                    " * version 1.0.0\n" +
                    " * Description TODO\n" +
                    " * createTime ${TIME}\n" +
                    " 技术分享 · 公众号：舒一笑的架构笔记\n" +
                    " */\n";  // 初始化默认模板
        }

        // 确保classPrefixes不为空
        if (this.classPrefixes == null || this.classPrefixes.isEmpty()) {
            this.classPrefixes = "Service,Repository,Controller,Component,Util,Manager,Factory,Builder,Handler";
        }

        // 确保百度API密钥和应用ID不为null
        if (this.baiduApiKey == null) {
            this.baiduApiKey = "";
        }

        if (this.baiduAppId == null) {
            this.baiduAppId = "";
        }

        if (this.googleApiKey == null) {
            this.googleApiKey = "";
        }
        if (this.googleProjectId == null) {
            this.googleProjectId = "";
        }
        if (this.googleRegion == null) {
            this.googleRegion = "global";
        }
        
        // 确保国内大模型配置不为null
        if (this.domesticAIModel == null || this.domesticAIModel.isEmpty()) {
            this.domesticAIModel = com.shuyixiao.config.TranslationModelConfig.getInstance().getDefaultDomesticAIModel();
        }
        if (this.domesticAIApiKey == null) {
            this.domesticAIApiKey = "";
        }
        
        // 确保提示词配置不为null
        if (this.translationPrompt == null || this.translationPrompt.isEmpty()) {
            this.translationPrompt = "你是一位专业软件工程师，负责将技术文档中文本翻译为规范的英文编程术语。请遵循：\n" +
                    "1. 【翻译规范】用编程术语表达技术概念，非逐字翻译（例：'配置文件路径'→configPath）\n" +
                    "2. 【命名规则】输出直接可用作代码标识符的形式（类名用大驼峰，方法/变量用小驼峰）\n" +
                    "3. 【术语处理】专业术语保持行业标准（例：'缓存'→cache而非buffer）\n" +
                    "4. 【长文本优化】超过3个技术概念时：\n" +
                    "   a) 优先提取核心术语\n" +
                    "   b) 保持技术逻辑连贯性\n" +
                    "   c) 省略非技术性描述词（'这个'、'一种'等）\n" +
                    "5. 【输出要求】只返回最终翻译结果\n\n" +
                    "待翻译中文：";
        }
    }

    // 提示词相关方法
    public String getTranslationPrompt() {
        if (translationPrompt == null || translationPrompt.isEmpty()) {
            translationPrompt = "你是一位专业软件工程师，负责将技术文档中文本翻译为规范的英文编程术语。请遵循：\n" +
                    "1. 【翻译规范】用编程术语表达技术概念，非逐字翻译（例：'配置文件路径'→configPath）\n" +
                    "2. 【命名规则】输出直接可用作代码标识符的形式（类名用大驼峰，方法/变量用小驼峰）\n" +
                    "3. 【术语处理】专业术语保持行业标准（例：'缓存'→cache而非buffer）\n" +
                    "4. 【长文本优化】超过3个技术概念时：\n" +
                    "   a) 优先提取核心术语\n" +
                    "   b) 保持技术逻辑连贯性\n" +
                    "   c) 省略非技术性描述词（'这个'、'一种'等）\n" +
                    "5. 【输出要求】只返回最终翻译结果\n\n" +
                    "待翻译中文：";
        }
        return translationPrompt;
    }
    
    public void setTranslationPrompt(String translationPrompt) {
        this.translationPrompt = translationPrompt;
    }
    
    public boolean isUseCustomPrompt() {
        return useCustomPrompt;
    }
    
    public void setUseCustomPrompt(boolean useCustomPrompt) {
        this.useCustomPrompt = useCustomPrompt;
    }

    // Bug记录存储相关方法
    public boolean isEnableLocalBugStorage() {
        return enableLocalBugStorage;
    }

    public void setEnableLocalBugStorage(boolean enableLocalBugStorage) {
        this.enableLocalBugStorage = enableLocalBugStorage;
    }
}


