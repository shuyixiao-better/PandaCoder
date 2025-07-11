package com.shuyixiao.jenkins.icon;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Jenkins文件图标装饰器
 * 最高优先级的图标提供器，确保Jenkins文件图标不被任何主题覆盖
 */
public class JenkinsFileIconDecorator implements FileIconProvider {

    private static final Icon JENKINS_ICON = IconLoader.getIcon("/icons/jenkinsfile.svg", JenkinsFileIconDecorator.class);

    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        if (isJenkinsFile(file)) {
            return JENKINS_ICON;
        }
        return null;
    }

    /**
     * 检查是否为Jenkins文件
     */
    private boolean isJenkinsFile(@NotNull VirtualFile file) {
        String fileName = file.getName();
        
        // 精确匹配 Jenkinsfile
        if ("Jenkinsfile".equals(fileName)) {
            return true;
        }
        
        // 匹配 Jenkinsfile.* 模式
        if (fileName.startsWith("Jenkinsfile.")) {
            return true;
        }
        
        // 匹配 *.jenkinsfile 模式
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jenkinsfile")) {
            return true;
        }
        
        // 匹配其他Jenkins文件模式
        if (lowerFileName.equals("jenkins") || lowerFileName.equals("pipeline")) {
            return true;
        }
        
        return false;
    }
} 