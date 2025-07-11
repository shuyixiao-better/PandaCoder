package com.shuyixiao.jenkins.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;

import java.awt.*;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Jenkins Pipeline 语法高亮器
 * 提供更丰富的Jenkins Pipeline特定语法高亮
 */
public class JenkinsSyntaxHighlighter extends SyntaxHighlighterBase {
    
    // Jenkins Pipeline 专用颜色定义 - 使用鲜艳的自定义颜色
    public static final TextAttributesKey JENKINS_KEYWORD = 
            createTextAttributesKey("JENKINS_KEYWORD", createBrightTextAttributes(new Color(0x569CD6), Font.BOLD));
    public static final TextAttributesKey JENKINS_PIPELINE_BLOCK = 
            createTextAttributesKey("JENKINS_PIPELINE_BLOCK", createBrightTextAttributes(new Color(0xC586C0), Font.BOLD));
    public static final TextAttributesKey JENKINS_STAGE_BLOCK = 
            createTextAttributesKey("JENKINS_STAGE_BLOCK", createBrightTextAttributes(new Color(0xDCDCAA), Font.BOLD));
    public static final TextAttributesKey JENKINS_STEP_METHOD = 
            createTextAttributesKey("JENKINS_STEP_METHOD", createBrightTextAttributes(new Color(0x4EC9B0), Font.PLAIN));
    public static final TextAttributesKey JENKINS_VARIABLE = 
            createTextAttributesKey("JENKINS_VARIABLE", createBrightTextAttributes(new Color(0x4FC1FF), Font.PLAIN));
    public static final TextAttributesKey JENKINS_STRING = 
            createTextAttributesKey("JENKINS_STRING", createBrightTextAttributes(new Color(0xCE9178), Font.PLAIN));
    public static final TextAttributesKey JENKINS_COMMENT = 
            createTextAttributesKey("JENKINS_COMMENT", createBrightTextAttributes(new Color(0x6A9955), Font.ITALIC));
    public static final TextAttributesKey JENKINS_NUMBER = 
            createTextAttributesKey("JENKINS_NUMBER", createBrightTextAttributes(new Color(0xB5CEA8), Font.PLAIN));
    public static final TextAttributesKey JENKINS_BRACKET = 
            createTextAttributesKey("JENKINS_BRACKET", createBrightTextAttributes(new Color(0xFFD700), Font.PLAIN));
    public static final TextAttributesKey JENKINS_BRACE = 
            createTextAttributesKey("JENKINS_BRACE", createBrightTextAttributes(new Color(0xFFD700), Font.PLAIN));
    public static final TextAttributesKey JENKINS_OPERATOR = 
            createTextAttributesKey("JENKINS_OPERATOR", createBrightTextAttributes(new Color(0xD4D4D4), Font.PLAIN));
    
    private final GroovySyntaxHighlighter groovyHighlighter = new GroovySyntaxHighlighter();
    
    /**
     * 创建鲜艳的文本属性
     */
    private static TextAttributes createBrightTextAttributes(Color foreground, int fontType) {
        TextAttributes attributes = new TextAttributes();
        attributes.setForegroundColor(foreground);
        attributes.setFontType(fontType);
        return attributes;
    }

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