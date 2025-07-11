package com.shuyixiao.jenkins.highlight;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.jenkins.file.JenkinsFileType;
import com.shuyixiao.jenkins.util.JenkinsFileDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;

/**
 * Jenkins语法高亮器工厂
 * 为Jenkins文件提供专门的语法高亮器
 */
public class JenkinsSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        // 检查是否为Jenkins文件
        if (virtualFile != null) {
            // 检查文件类型
            if (virtualFile.getFileType() instanceof JenkinsFileType) {
                return new JenkinsSyntaxHighlighter();
            }
            
            // 检查文件名模式
            String fileName = virtualFile.getName();
            if (isJenkinsFile(fileName)) {
                return new JenkinsSyntaxHighlighter();
            }
            
            // 检查文件内容（如果需要）
            if (project != null) {
                try {
                    String content = new String(virtualFile.contentsToByteArray());
                    if (content.contains("pipeline {") || content.contains("node {") || content.contains("@Library")) {
                        return new JenkinsSyntaxHighlighter();
                    }
                } catch (Exception e) {
                    // 忽略异常，使用默认高亮器
                }
            }
        }
        
        // 回退到默认的Groovy语法高亮器
        return new GroovySyntaxHighlighter();
    }
    
    /**
     * 检查文件名是否为Jenkins文件
     */
    private boolean isJenkinsFile(@NotNull String fileName) {
        return "Jenkinsfile".equals(fileName) ||
               fileName.startsWith("Jenkinsfile.") ||
               fileName.toLowerCase().endsWith(".jenkinsfile");
    }
} 