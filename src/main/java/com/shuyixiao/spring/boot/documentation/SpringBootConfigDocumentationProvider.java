package com.shuyixiao.spring.boot.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import com.shuyixiao.spring.boot.config.SpringBootConfigPropertyManager;
import com.shuyixiao.spring.boot.config.SpringBootConfigPropertyManager.ConfigProperty;
import com.shuyixiao.spring.boot.settings.SpringBootHelperSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spring Boot配置文件文档提供器
 * 为配置属性提供文档支持
 */
public class SpringBootConfigDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable String generateDoc(@NotNull PsiElement element, @Nullable PsiElement originalElement) {
        // 检查是否启用了配置文档功能
        SpringBootHelperSettings settings = SpringBootHelperSettings.getInstance();
        if (!settings.isConfigurationDocumentationEnabled()) {
            return null;
        }

        // 检查是否是Spring Boot配置文件
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null || !SpringBootFileDetector.isSpringBootConfigFile(containingFile)) {
            return null;
        }

        // 获取配置属性键
        String propertyKey = extractPropertyKey(element);
        if (propertyKey == null) {
            return null;
        }

        // 获取配置属性信息
        Project project = element.getProject();
        SpringBootConfigPropertyManager propertyManager = SpringBootConfigPropertyManager.getInstance(project);
        ConfigProperty property = propertyManager.getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        // 生成文档HTML
        return generateDocumentationHtml(property);
    }

    @Override
    public @Nullable String getQuickNavigateInfo(@NotNull PsiElement element, @NotNull PsiElement originalElement) {
        // 检查是否启用了配置文档功能
        SpringBootHelperSettings settings = SpringBootHelperSettings.getInstance();
        if (!settings.isConfigurationDocumentationEnabled()) {
            return null;
        }

        // 检查是否是Spring Boot配置文件
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null || !SpringBootFileDetector.isSpringBootConfigFile(containingFile)) {
            return null;
        }

        // 获取配置属性键
        String propertyKey = extractPropertyKey(element);
        if (propertyKey == null) {
            return null;
        }

        // 获取配置属性信息
        Project project = element.getProject();
        SpringBootConfigPropertyManager propertyManager = SpringBootConfigPropertyManager.getInstance(project);
        ConfigProperty property = propertyManager.getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        // 生成快速导航信息
        return generateQuickNavigateInfo(property);
    }

    /**
     * 从PSI元素中提取配置属性键
     */
    @Nullable
    private String extractPropertyKey(@NotNull PsiElement element) {
        if (element instanceof Property) {
            String key = ((Property) element).getKey();
            return key;
        }
        
        // 尝试从父元素中提取
        PsiElement parent = element.getParent();
        if (parent instanceof Property) {
            return ((Property) parent).getKey();
        }
        
        return null;
    }

    /**
     * 生成文档HTML
     */
    @NotNull
    private String generateDocumentationHtml(@NotNull ConfigProperty property) {
        StringBuilder html = new StringBuilder();
        
        html.append("<html><body>");
        
        // 属性名称
        html.append("<h3>").append(escapeHtml(property.getKey())).append("</h3>");
        
        // 属性描述
        if (!property.getDescription().isEmpty()) {
            html.append("<p>").append(escapeHtml(property.getDescription())).append("</p>");
        }
        
        // 属性类型
        html.append("<p><b>类型:</b> ").append(property.getType().toString().toLowerCase()).append("</p>");
        
        // 默认值
        if (!property.getDefaultValue().isEmpty()) {
            html.append("<p><b>默认值:</b> <code>").append(escapeHtml(property.getDefaultValue())).append("</code></p>");
        }
        
        // 枚举值
        if (property.hasEnumValues()) {
            html.append("<p><b>可选值:</b></p>");
            html.append("<ul>");
            for (String enumValue : property.getEnumValues()) {
                html.append("<li><code>").append(escapeHtml(enumValue)).append("</code></li>");
            }
            html.append("</ul>");
        }
        
        // 废弃信息
        if (property.isDeprecated()) {
            html.append("<p><b style=\"color: #ff6b6b;\">已废弃</b>");
            if (property.getReplacementProperty() != null) {
                html.append(" - 请使用: <code>").append(escapeHtml(property.getReplacementProperty())).append("</code>");
            }
            html.append("</p>");
        }
        
        // 使用示例
        String example = generateExample(property);
        if (!example.isEmpty()) {
            html.append("<p><b>示例:</b></p>");
            html.append("<pre><code>").append(escapeHtml(example)).append("</code></pre>");
        }
        
        html.append("</body></html>");
        
        return html.toString();
    }

    /**
     * 生成快速导航信息
     */
    @NotNull
    private String generateQuickNavigateInfo(@NotNull ConfigProperty property) {
        StringBuilder info = new StringBuilder();
        
        info.append(property.getKey());
        info.append(" (").append(property.getType().toString().toLowerCase()).append(")");
        
        if (!property.getDefaultValue().isEmpty()) {
            info.append(" = ").append(property.getDefaultValue());
        }
        
        if (property.isDeprecated()) {
            info.append(" [已废弃]");
        }
        
        return info.toString();
    }

    /**
     * 生成使用示例
     */
    @NotNull
    private String generateExample(@NotNull ConfigProperty property) {
        String key = property.getKey();
        String defaultValue = property.getDefaultValue();
        
        if (defaultValue.isEmpty()) {
            // 根据类型生成示例值
            switch (property.getType()) {
                case STRING:
                    return key + "=your-value";
                case INTEGER:
                    return key + "=123";
                case BOOLEAN:
                    return key + "=true";
                case DURATION:
                    return key + "=30s";
                case DATA_SIZE:
                    return key + "=1MB";
                case STRING_ARRAY:
                    return key + "=value1,value2,value3";
                case ENUM:
                    if (property.hasEnumValues() && !property.getEnumValues().isEmpty()) {
                        return key + "=" + property.getEnumValues().get(0);
                    }
                    return key + "=enum-value";
                default:
                    return key + "=value";
            }
        } else {
            return key + "=" + defaultValue;
        }
    }

    /**
     * 转义HTML特殊字符
     */
    @NotNull
    private String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
} 