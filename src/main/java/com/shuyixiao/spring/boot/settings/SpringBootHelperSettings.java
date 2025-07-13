package com.shuyixiao.spring.boot.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spring Boot Helper设置状态类
 * 用于保存和管理Spring Boot Helper的配置信息
 */
@State(
    name = "SpringBootHelperSettings",
    storages = @Storage("springboot-helper-settings.xml")
)
@Service(Service.Level.APP)
public class SpringBootHelperSettings implements PersistentStateComponent<SpringBootHelperSettings> {

    // 配置文件智能补全是否启用
    private boolean configurationCompletionEnabled = true;
    
    // 注解智能补全是否启用
    private boolean annotationCompletionEnabled = true;
    
    // 配置文件值验证是否启用
    private boolean configurationValidationEnabled = true;
    
    // 注解文档提示是否启用
    private boolean annotationDocumentationEnabled = true;
    
    // 配置文件文档提示是否启用
    private boolean configurationDocumentationEnabled = true;
    
    // 配置文件格式化是否启用
    private boolean configurationFormattingEnabled = true;
    
    // 自动导入Spring Boot依赖是否启用
    private boolean autoImportEnabled = true;
    
    // 废弃属性警告是否启用
    private boolean deprecationWarningsEnabled = true;
    
    // 配置文件环境特定补全是否启用
    private boolean environmentSpecificCompletionEnabled = true;
    
    // 配置文件类型提示是否启用
    private boolean configurationTypeHintsEnabled = true;
    
    // 注解参数验证是否启用
    private boolean annotationParameterValidationEnabled = true;
    
    // 配置文件重复键检测是否启用
    private boolean duplicateKeyDetectionEnabled = true;

    // 默认构造函数
    public SpringBootHelperSettings() {
    }

    @Nullable
    @Override
    public SpringBootHelperSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SpringBootHelperSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getter和Setter方法
    public boolean isConfigurationCompletionEnabled() {
        return configurationCompletionEnabled;
    }

    public void setConfigurationCompletionEnabled(boolean configurationCompletionEnabled) {
        this.configurationCompletionEnabled = configurationCompletionEnabled;
    }

    public boolean isAnnotationCompletionEnabled() {
        return annotationCompletionEnabled;
    }

    public void setAnnotationCompletionEnabled(boolean annotationCompletionEnabled) {
        this.annotationCompletionEnabled = annotationCompletionEnabled;
    }

    public boolean isConfigurationValidationEnabled() {
        return configurationValidationEnabled;
    }

    public void setConfigurationValidationEnabled(boolean configurationValidationEnabled) {
        this.configurationValidationEnabled = configurationValidationEnabled;
    }

    public boolean isAnnotationDocumentationEnabled() {
        return annotationDocumentationEnabled;
    }

    public void setAnnotationDocumentationEnabled(boolean annotationDocumentationEnabled) {
        this.annotationDocumentationEnabled = annotationDocumentationEnabled;
    }

    public boolean isConfigurationDocumentationEnabled() {
        return configurationDocumentationEnabled;
    }

    public void setConfigurationDocumentationEnabled(boolean configurationDocumentationEnabled) {
        this.configurationDocumentationEnabled = configurationDocumentationEnabled;
    }

    public boolean isConfigurationFormattingEnabled() {
        return configurationFormattingEnabled;
    }

    public void setConfigurationFormattingEnabled(boolean configurationFormattingEnabled) {
        this.configurationFormattingEnabled = configurationFormattingEnabled;
    }

    public boolean isAutoImportEnabled() {
        return autoImportEnabled;
    }

    public void setAutoImportEnabled(boolean autoImportEnabled) {
        this.autoImportEnabled = autoImportEnabled;
    }

    public boolean isDeprecationWarningsEnabled() {
        return deprecationWarningsEnabled;
    }

    public void setDeprecationWarningsEnabled(boolean deprecationWarningsEnabled) {
        this.deprecationWarningsEnabled = deprecationWarningsEnabled;
    }

    public boolean isEnvironmentSpecificCompletionEnabled() {
        return environmentSpecificCompletionEnabled;
    }

    public void setEnvironmentSpecificCompletionEnabled(boolean environmentSpecificCompletionEnabled) {
        this.environmentSpecificCompletionEnabled = environmentSpecificCompletionEnabled;
    }

    public boolean isConfigurationTypeHintsEnabled() {
        return configurationTypeHintsEnabled;
    }

    public void setConfigurationTypeHintsEnabled(boolean configurationTypeHintsEnabled) {
        this.configurationTypeHintsEnabled = configurationTypeHintsEnabled;
    }

    public boolean isAnnotationParameterValidationEnabled() {
        return annotationParameterValidationEnabled;
    }

    public void setAnnotationParameterValidationEnabled(boolean annotationParameterValidationEnabled) {
        this.annotationParameterValidationEnabled = annotationParameterValidationEnabled;
    }

    public boolean isDuplicateKeyDetectionEnabled() {
        return duplicateKeyDetectionEnabled;
    }

    public void setDuplicateKeyDetectionEnabled(boolean duplicateKeyDetectionEnabled) {
        this.duplicateKeyDetectionEnabled = duplicateKeyDetectionEnabled;
    }

    /**
     * 获取应用程序级别的设置实例
     */
    public static SpringBootHelperSettings getInstance() {
        return ApplicationManager.getApplication().getService(SpringBootHelperSettings.class);
    }

    /**
     * 重置所有设置为默认值
     */
    public void resetToDefaults() {
        configurationCompletionEnabled = true;
        annotationCompletionEnabled = true;
        configurationValidationEnabled = true;
        annotationDocumentationEnabled = true;
        configurationDocumentationEnabled = true;
        configurationFormattingEnabled = true;
        autoImportEnabled = true;
        deprecationWarningsEnabled = true;
        environmentSpecificCompletionEnabled = true;
        configurationTypeHintsEnabled = true;
        annotationParameterValidationEnabled = true;
        duplicateKeyDetectionEnabled = true;
    }

    /**
     * 检查是否启用了任何功能
     */
    public boolean isAnyFeatureEnabled() {
        return configurationCompletionEnabled ||
               annotationCompletionEnabled ||
               configurationValidationEnabled ||
               annotationDocumentationEnabled ||
               configurationDocumentationEnabled ||
               configurationFormattingEnabled ||
               autoImportEnabled ||
               deprecationWarningsEnabled ||
               environmentSpecificCompletionEnabled ||
               configurationTypeHintsEnabled ||
               annotationParameterValidationEnabled ||
               duplicateKeyDetectionEnabled;
    }
} 