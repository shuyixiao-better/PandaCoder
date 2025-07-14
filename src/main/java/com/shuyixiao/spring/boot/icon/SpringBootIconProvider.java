package com.shuyixiao.spring.boot.icon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
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

    private static final Logger LOG = Logger.getInstance(SpringBootIconProvider.class);
    private static final Icon SPRING_BOOT_ICON;
    
    static {
        Icon icon = null;
        try {
            icon = IconLoader.getIcon("/icons/springboot.svg", SpringBootIconProvider.class);
            LOG.info("Successfully loaded Spring Boot icon");
        } catch (Exception e) {
            LOG.warn("Failed to load Spring Boot icon", e);
            try {
                // 尝试备用路径
                icon = IconLoader.getIcon("/icons/springboot@2x.svg", SpringBootIconProvider.class);
                LOG.info("Successfully loaded Spring Boot @2x icon as fallback");
            } catch (Exception ex) {
                LOG.error("Failed to load any Spring Boot icon", ex);
            }
        }
        SPRING_BOOT_ICON = icon;
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiFile) {
            PsiFile psiFile = (PsiFile) element;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            
            if (virtualFile != null && SpringBootFileDetector.isSpringBootConfigFile(virtualFile)) {
                LOG.debug("Providing Spring Boot icon for file: " + virtualFile.getName());
                return SPRING_BOOT_ICON;
            }
        }
        
        return null;
    }
} 