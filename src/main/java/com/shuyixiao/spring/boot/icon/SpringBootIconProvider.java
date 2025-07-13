package com.shuyixiao.spring.boot.icon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Spring Boot图标提供器
 * 为Spring Boot配置文件提供图标显示
 */
public class SpringBootIconProvider extends IconProvider implements DumbAware {

    private static final Icon SPRING_BOOT_ICON = IconLoader.getIcon("/icons/springboot.svg", SpringBootIconProvider.class);

    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiFile) {
            PsiFile psiFile = (PsiFile) element;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            
            if (virtualFile != null && SpringBootFileDetector.isSpringBootConfigFile(virtualFile)) {
                return SPRING_BOOT_ICON;
            }
        }
        
        return null;
    }
} 