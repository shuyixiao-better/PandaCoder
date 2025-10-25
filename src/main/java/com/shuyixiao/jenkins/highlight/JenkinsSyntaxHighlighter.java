package com.shuyixiao.jenkins.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;

/**
 * Jenkins Pipeline 语法高亮器
 * 提供更丰富的Jenkins Pipeline特定语法高亮
 */
public class JenkinsSyntaxHighlighter extends SyntaxHighlighterBase {
    
    // Jenkins Pipeline 专用颜色定义 - 直接使用默认颜色，让IDE主题可以适配
    // 使用默认颜色键避免弃用警告
    public static final TextAttributesKey JENKINS_KEYWORD = DefaultLanguageHighlighterColors.KEYWORD;
    public static final TextAttributesKey JENKINS_PIPELINE_BLOCK = DefaultLanguageHighlighterColors.KEYWORD;
    public static final TextAttributesKey JENKINS_STAGE_BLOCK = DefaultLanguageHighlighterColors.KEYWORD;
    public static final TextAttributesKey JENKINS_STEP_METHOD = DefaultLanguageHighlighterColors.FUNCTION_CALL;
    public static final TextAttributesKey JENKINS_VARIABLE = DefaultLanguageHighlighterColors.IDENTIFIER;
    public static final TextAttributesKey JENKINS_STRING = DefaultLanguageHighlighterColors.STRING;
    public static final TextAttributesKey JENKINS_COMMENT = DefaultLanguageHighlighterColors.LINE_COMMENT;
    public static final TextAttributesKey JENKINS_NUMBER = DefaultLanguageHighlighterColors.NUMBER;
    public static final TextAttributesKey JENKINS_BRACKET = DefaultLanguageHighlighterColors.BRACKETS;
    public static final TextAttributesKey JENKINS_BRACE = DefaultLanguageHighlighterColors.BRACES;
    public static final TextAttributesKey JENKINS_OPERATOR = DefaultLanguageHighlighterColors.OPERATION_SIGN;
    
    private final GroovySyntaxHighlighter groovyHighlighter = new GroovySyntaxHighlighter();

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return groovyHighlighter.getHighlightingLexer();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        // 使用Groovy默认高亮器作为基础，增强颜色效果
        TextAttributesKey[] groovyKeys = groovyHighlighter.getTokenHighlights(tokenType);
        
        // 如果Groovy高亮器返回了结果，我们基于它进行增强
        if (groovyKeys.length > 0) {
            TextAttributesKey groovyKey = groovyKeys[0];
            
            // 增强特定类型的颜色 - 使用Jenkins专用的更鲜艳颜色
            if (groovyKey != null) {
                if (groovyKey.equals(DefaultLanguageHighlighterColors.KEYWORD)) {
                    return new TextAttributesKey[]{JENKINS_KEYWORD};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.STRING)) {
                    return new TextAttributesKey[]{JENKINS_STRING};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.LINE_COMMENT) || 
                           groovyKey.equals(DefaultLanguageHighlighterColors.BLOCK_COMMENT)) {
                    return new TextAttributesKey[]{JENKINS_COMMENT};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.NUMBER)) {
                    return new TextAttributesKey[]{JENKINS_NUMBER};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.BRACKETS)) {
                    return new TextAttributesKey[]{JENKINS_BRACKET};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.BRACES)) {
                    return new TextAttributesKey[]{JENKINS_BRACE};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.OPERATION_SIGN)) {
                    return new TextAttributesKey[]{JENKINS_OPERATOR};
                } else if (groovyKey.equals(DefaultLanguageHighlighterColors.IDENTIFIER)) {
                    // 对于标识符，我们使用Jenkins变量颜色来让它更鲜艳
                    return new TextAttributesKey[]{JENKINS_VARIABLE};
                }
            }
        }
        
        // 回退到增强的颜色而不是原始的Groovy颜色
        if (groovyKeys.length > 0) {
            // 为了确保所有文本都有颜色，我们至少返回Jenkins关键字颜色
            return new TextAttributesKey[]{JENKINS_KEYWORD};
        }
        
        return groovyKeys;
    }
} 