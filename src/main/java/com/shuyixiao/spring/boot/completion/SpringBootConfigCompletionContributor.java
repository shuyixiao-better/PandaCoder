package com.shuyixiao.spring.boot.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
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
            new SpringBootPropertiesCompletionProvider()
        );
    }

    /**
     * Properties文件补全提供器
     */
    private static class SpringBootPropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
            
            PsiFile file = parameters.getOriginalFile();
            if (!(file instanceof PropertiesFile) || !SpringBootFileDetector.isSpringBootConfigFile(file)) {
                return;
            }

            Project project = file.getProject();
            SpringBootConfigPropertyManager propertyManager = SpringBootConfigPropertyManager.getInstance(project);
            
            PsiElement position = parameters.getPosition();
            String currentText = getCurrentText(parameters);
            
            // 获取当前输入的前缀
            String prefix = getPrefix(currentText, parameters);
            
            // 为配置属性提供补全
            List<ConfigProperty> properties = propertyManager.getPropertiesByPrefix(prefix);
            
            for (ConfigProperty property : properties) {
                LookupElement element = createPropertiesLookupElement(property);
                result.addElement(element);
            }
            
            // 如果没有找到匹配的属性，提供所有属性
            if (properties.isEmpty() && prefix.length() < 3) {
                for (ConfigProperty property : propertyManager.getAllProperties()) {
                    LookupElement element = createPropertiesLookupElement(property);
                    result.addElement(element);
                }
            }
        }
    }



    /**
     * 创建Properties格式的补全元素
     */
    private static LookupElement createPropertiesLookupElement(ConfigProperty property) {
        String key = property.getKey();
        String description = property.getDescription();
        String defaultValue = property.getDefaultValue();
        
        LookupElementBuilder builder = LookupElementBuilder.create(key)
            .withTypeText(property.getType().toString().toLowerCase())
            .withTailText(defaultValue.isEmpty() ? "" : " (默认: " + defaultValue + ")")
            .withPresentableText(key);
        
        if (!description.isEmpty()) {
            builder = builder.withTypeText(description, true);
        }
        
        // 如果有枚举值，添加到描述中
        if (property.hasEnumValues()) {
            String enumText = "可选值: " + String.join(", ", property.getEnumValues());
            builder = builder.withTailText(" - " + enumText, true);
        }
        
        // 如果属性已废弃，添加标记
        if (property.isDeprecated()) {
            builder = builder.withStrikeoutness(true);
            if (property.getReplacementProperty() != null) {
                builder = builder.withTailText(" (已废弃，请使用: " + property.getReplacementProperty() + ")", true);
            }
        }
        
        return builder;
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