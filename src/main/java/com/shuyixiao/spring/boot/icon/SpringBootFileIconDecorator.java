package com.shuyixiao.spring.boot.icon;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Spring Boot文件图标装饰器
 * 最高优先级的图标提供器，确保Spring Boot配置文件图标不被任何主题覆盖
 */
public class SpringBootFileIconDecorator implements FileIconProvider {

    private static final Icon SPRING_BOOT_ICON = IconLoader.getIcon("/icons/springboot.svg", SpringBootFileIconDecorator.class);

    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        if (SpringBootFileDetector.isSpringBootConfigFile(file)) {
            return SPRING_BOOT_ICON;
        }
        return null;
    }
} 