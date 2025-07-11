package com.shuyixiao.jenkins.icon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.shuyixiao.jenkins.file.JenkinsFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Jenkins图标提供器
 * 确保Jenkins文件始终显示自定义图标，不被主题覆盖
 */
public class JenkinsIconProvider extends IconProvider implements DumbAware {

    private static final Icon JENKINS_ICON = IconLoader.getIcon("/icons/jenkinsfile.svg", JenkinsIconProvider.class);

    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiFile) {
            PsiFile psiFile = (PsiFile) element;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            
            if (virtualFile != null && isJenkinsFile(virtualFile)) {
                return JENKINS_ICON;
            }
        }
        
        return null;
    }

    /**
     * 判断是否为Jenkins文件
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
        
        // 检查文件类型
        if (file.getFileType() instanceof JenkinsFileType) {
            return true;
        }
        
        return false;
    }
} 