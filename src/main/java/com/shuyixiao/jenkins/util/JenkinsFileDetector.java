package com.shuyixiao.jenkins.util;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Jenkins文件识别工具类
 * 用于识别Jenkinsfile和Jenkins Pipeline脚本
 */
public class JenkinsFileDetector {
    
    /**
     * 判断是否为Jenkins Pipeline文件
     * 
     * @param file PSI文件对象
     * @return 如果是Jenkins文件返回true，否则返回false
     */
    public static boolean isJenkinsFile(@NotNull PsiFile file) {
        String fileName = file.getName();
        String fileContent = file.getText();
        
        // 检查文件名
        if (isJenkinsFileName(fileName)) {
            return true;
        }
        
        // 检查文件内容
        return containsJenkinsPipelineContent(fileContent);
    }
    
    /**
     * 检查文件名是否为Jenkins文件名模式
     */
    private static boolean isJenkinsFileName(@NotNull String fileName) {
        return "Jenkinsfile".equals(fileName) || 
               fileName.startsWith("Jenkinsfile.") ||
               fileName.toLowerCase().contains("jenkins");
    }
    
    /**
     * 检查文件内容是否包含Jenkins Pipeline语法
     */
    private static boolean containsJenkinsPipelineContent(@NotNull String content) {
        // 声明式Pipeline
        if (content.contains("pipeline {") || 
            content.contains("pipeline{")) {
            return true;
        }
        
        // 脚本式Pipeline
        if (content.contains("node {") || 
            content.contains("node(") ||
            content.contains("node{")) {
            return true;
        }
        
        // 其他Jenkins特征
        return content.contains("@Library") ||
               content.contains("agent ") ||
               content.contains("stages {") ||
               content.contains("stage(") ||
               content.contains("steps {") ||
               content.contains("post {") ||
               content.contains("environment {") ||
               content.contains("parameters {") ||
               content.contains("tools {") ||
               content.contains("options {");
    }
    
    /**
     * 检查是否为声明式Pipeline
     */
    public static boolean isDeclarativePipeline(@NotNull PsiFile file) {
        String content = file.getText();
        return content.contains("pipeline {") || content.contains("pipeline{");
    }
    
    /**
     * 检查是否为脚本式Pipeline
     */
    public static boolean isScriptedPipeline(@NotNull PsiFile file) {
        String content = file.getText();
        return (content.contains("node {") || content.contains("node(") || content.contains("node{")) &&
               !isDeclarativePipeline(file);
    }
} 