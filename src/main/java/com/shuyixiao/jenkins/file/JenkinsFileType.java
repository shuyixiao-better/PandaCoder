package com.shuyixiao.jenkins.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.GroovyLanguage;

import javax.swing.*;

/**
 * Jenkins Pipeline 文件类型
 * 提供专门的图标和语法高亮支持
 */
public class JenkinsFileType extends LanguageFileType {

    public static final JenkinsFileType INSTANCE = new JenkinsFileType();
    public static final String DEFAULT_EXTENSION = "jenkinsfile";

    private JenkinsFileType() {
        super(GroovyLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Jenkins Pipeline";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Jenkins Pipeline script file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        try {
            // 强制加载我们的自定义图标，不允许被主题覆盖
            Icon icon = IconLoader.getIcon("/icons/jenkinsfile.svg", JenkinsFileType.class);
            if (icon != null) {
                return icon;
            }
        } catch (Exception e) {
            // 如果加载失败，记录日志但不抛出异常
        }
        
        // 备用方案：如果主图标加载失败，返回null让系统使用默认图标
        return null;
    }

    @Override
    public boolean isSecondary() {
        return false;
    }
}
