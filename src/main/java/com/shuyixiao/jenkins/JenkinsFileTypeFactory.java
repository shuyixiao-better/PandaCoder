package com.shuyixiao.jenkins;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.shuyixiao.jenkins.file.JenkinsFileNameMatcher;
import com.shuyixiao.jenkins.file.JenkinsFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Jenkins文件类型工厂
 * 注册自定义的Jenkins Pipeline文件类型，提供专门的图标和语法高亮支持
 */
public class JenkinsFileTypeFactory extends FileTypeFactory {

    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        // 注册Jenkins文件类型的基本扩展名
        consumer.consume(JenkinsFileType.INSTANCE, JenkinsFileType.DEFAULT_EXTENSION);
        
        // 注册自定义文件名匹配器（处理特殊文件名）
        consumer.consume(JenkinsFileType.INSTANCE, new JenkinsFileNameMatcher());
    }
} 