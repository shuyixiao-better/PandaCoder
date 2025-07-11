package com.shuyixiao.jenkins.file;

import com.intellij.openapi.fileTypes.FileNameMatcher;
import org.jetbrains.annotations.NotNull;

/**
 * Jenkins文件名匹配器
 * 用于识别没有扩展名的Jenkinsfile文件
 */
public class JenkinsFileNameMatcher implements FileNameMatcher {
    
    @Override
    public boolean accept(@NotNull String fileName) {
        // 精确匹配 Jenkinsfile（最重要的情况）
        if ("Jenkinsfile".equals(fileName)) {
            return true;
        }
        
        // 匹配 Jenkinsfile.* 模式（如 Jenkinsfile.dev, Jenkinsfile.prod）
        if (fileName.startsWith("Jenkinsfile.")) {
            return true;
        }
        
        // 匹配 *.jenkinsfile 模式（不区分大小写）
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jenkinsfile")) {
            return true;
        }
        
        // 匹配其他常见的Jenkins文件模式
        if (lowerFileName.equals("jenkins") || 
            lowerFileName.equals("pipeline")) {
            return true;
        }
        
        return false;
    }

    @NotNull
    @Override
    public String getPresentableString() {
        return "Jenkinsfile";
    }
} 