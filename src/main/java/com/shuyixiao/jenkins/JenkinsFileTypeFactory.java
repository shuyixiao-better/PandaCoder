package com.shuyixiao.jenkins;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyFileType;

/**
 * Jenkins文件类型工厂
 * 将Jenkinsfile相关文件关联到Groovy文件类型，从而获得语法高亮支持
 */
public class JenkinsFileTypeFactory extends FileTypeFactory {

    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        // 将 Jenkinsfile 文件关联到 Groovy 文件类型
        consumer.consume(GroovyFileType.GROOVY_FILE_TYPE, "Jenkinsfile");
        
        // 也可以关联其他常见的Jenkins文件模式
        // 注意：这里只能关联扩展名，不能关联具体的文件名模式
    }
} 