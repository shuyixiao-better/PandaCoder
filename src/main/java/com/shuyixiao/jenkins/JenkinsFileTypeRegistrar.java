package com.shuyixiao.jenkins;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.shuyixiao.jenkins.file.JenkinsFileType;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Jenkins文件类型注册器
 * 确保在项目启动时正确注册Jenkins文件类型关联
 */
public class JenkinsFileTypeRegistrar implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 在应用程序线程中执行文件类型注册
        ApplicationManager.getApplication().invokeLater(() -> {
            registerJenkinsFileTypes();
        });

        return null;
    }

        /**
     * 注册Jenkins文件类型关联
     */
    private void registerJenkinsFileTypes() {
        try {
            FileTypeManager fileTypeManager = FileTypeManager.getInstance();
            
            // 强制注册文件类型关联，覆盖主题和其他插件的设置
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    // 移除可能存在的冲突关联
                    removeConflictingAssociations(fileTypeManager);
                    
                    // 强制关联 Jenkinsfile 文件名
                    fileTypeManager.associatePattern(JenkinsFileType.INSTANCE, "Jenkinsfile");
                    
                    // 强制关联 Jenkinsfile.* 模式
                    fileTypeManager.associatePattern(JenkinsFileType.INSTANCE, "Jenkinsfile.*");
                    
                    // 强制关联 *.jenkinsfile 模式
                    fileTypeManager.associatePattern(JenkinsFileType.INSTANCE, "*.jenkinsfile");
                    
                } catch (Exception e) {
                    // 忽略异常，可能已经注册过了
                }
            });
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    /**
     * 移除可能的冲突文件类型关联
     */
    private void removeConflictingAssociations(FileTypeManager fileTypeManager) {
        try {
            // 尝试移除Jenkinsfile可能的其他关联
            // 这里我们可以添加逻辑来处理主题插件可能添加的关联
        } catch (Exception e) {
            // 静默忽略
        }
    }
}
