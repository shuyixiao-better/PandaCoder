package com.shuyixiao.spring.boot.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import com.shuyixiao.spring.boot.config.SpringBootConfigPropertyManager;
import com.shuyixiao.spring.boot.config.SpringBootConfigPropertyManager.ConfigProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Spring Boot配置文件智能补全提供器
 * 为application.properties和application.yml文件提供智能补全
 */
public class SpringBootConfigCompletionContributor extends CompletionContributor {

    public SpringBootConfigCompletionContributor() {
        // Properties文件的补全
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withName(
                PlatformPatterns.string().contains("application")
            )),
            new SpringBootConfigCompletionProvider()
        );
    }

    /**
     * 配置文件补全提供器（支持Properties和YAML）
     */
    private static class SpringBootConfigCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
            
            PsiFile file = parameters.getOriginalFile();
            
            // 检查是否为Spring Boot配置文件（支持Properties和YAML）
            if (!isSpringBootConfigFile(file)) {
                return;
            }

            Project project = file.getProject();
            SpringBootConfigPropertyManager propertyManager = SpringBootConfigPropertyManager.getInstance(project);
            
            // 获取当前位置（如需）
            // PsiElement position = parameters.getPosition();
            String currentText = getCurrentText(parameters);
            
            // 获取当前输入的前缀
            String prefix = getPrefix(currentText, parameters);
            
            // 为配置属性提供补全
            List<ConfigProperty> properties = propertyManager.getPropertiesByPrefix(prefix);
            
            for (ConfigProperty property : properties) {
                LookupElement element = createLookupElement(property, file);
                result.addElement(element);
            }
            
            // 如果没有找到匹配的属性，提供所有属性
            if (properties.isEmpty() && prefix.length() < 3) {
                for (ConfigProperty property : propertyManager.getAllProperties()) {
                    LookupElement element = createLookupElement(property, file);
                    result.addElement(element);
                }
            }
        }
        
        /**
         * 检查是否为Spring Boot配置文件（支持Properties和YAML）
         */
        private boolean isSpringBootConfigFile(@NotNull PsiFile file) {
            // 首先检查是否为Spring Boot配置文件
            if (!SpringBootFileDetector.isSpringBootConfigFile(file)) {
                return false;
            }
            
            // 检查文件类型：Properties文件或YAML文件
            String fileName = file.getName();
            return file instanceof PropertiesFile || 
                   fileName.endsWith(".yml") || 
                   fileName.endsWith(".yaml");
        }
    }

    /**
     * 创建补全元素（根据文件类型调整格式）
     */
    private static LookupElement createLookupElement(ConfigProperty property, PsiFile file) {
        if (file instanceof PropertiesFile) {
            return createPropertiesLookupElement(property);
        } else {
            return createYamlLookupElement(property);
        }
    }

    /**
     * 创建Properties格式的补全元素
     */
    private static LookupElement createPropertiesLookupElement(ConfigProperty property) {
        String key = property.getKey();
        String description = property.getDescription();
        String defaultValue = property.getDefaultValue();
        String typeInfo = property.getType().toString().toLowerCase();
        
        LookupElementBuilder builder = LookupElementBuilder.create(key)
                .withTypeText(typeInfo)
                .withTailText(" = " + (defaultValue.isEmpty() ? "..." : defaultValue), true);
        
        if (description != null && !description.isEmpty()) {
            builder = builder.withPresentableText(key + " (" + description + ")");
        }
        
        return builder;
    }
    
    /**
     * 创建YAML格式的补全元素
     */
    private static LookupElement createYamlLookupElement(ConfigProperty property) {
        String key = property.getKey();
        String description = property.getDescription();
        String defaultValue = property.getDefaultValue();
        String typeInfo = property.getType().toString().toLowerCase();
        
        // 将点分隔的key转换为YAML格式
        String yamlKey = convertToYamlKey(key);
        
        LookupElementBuilder builder = LookupElementBuilder.create(yamlKey)
                .withTypeText(typeInfo)
                .withTailText(": " + (defaultValue.isEmpty() ? "..." : defaultValue), true);
        
        if (description != null && !description.isEmpty()) {
            builder = builder.withPresentableText(yamlKey + " (" + description + ")");
        }
        
        return builder;
    }
    
    /**
     * 将点分隔的属性键转换为YAML格式
     * 例如：server.port -> server:\n  port
     */
    private static String convertToYamlKey(String key) {
        // 简单实现：对于YAML，我们可以直接使用原始key
        // 更复杂的实现可以处理嵌套结构
        return key;
    }

    /**
     * 获取当前文本
     */
    private static String getCurrentText(CompletionParameters parameters) {
        Editor editor = parameters.getEditor();
        Document document = editor.getDocument();
        int offset = parameters.getOffset();
        
        // 获取当前行的文本
        int lineStartOffset = document.getLineStartOffset(document.getLineNumber(offset));
        int lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset));
        
        return document.getText(new TextRange(lineStartOffset, lineEndOffset));
    }

    /**
     * 获取Properties文件的前缀
     */
    private static String getPrefix(String currentText, CompletionParameters parameters) {
        // 简单的前缀提取逻辑
        String[] parts = currentText.split("=");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        return "";
    }
} 