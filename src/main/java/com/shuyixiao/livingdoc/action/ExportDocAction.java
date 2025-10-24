package com.shuyixiao.livingdoc.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.livingdoc.storage.DocumentStorage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;

/**
 * 导出文档 Action
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ExportDocAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 检查文档是否存在
        DocumentStorage storage = new DocumentStorage(project);
        if (!storage.exists()) {
            int result = Messages.showYesNoDialog(
                project,
                "未找到文档索引。\n\n是否立即索引项目？",
                "活文档",
                "立即索引",
                "取消",
                Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                new IndexProjectAction().actionPerformed(e);
            }
            return;
        }
        
        // 选择导出目录
        FileChooserDialog dialog = FileChooserFactory.getInstance()
            .createFileChooser(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                project,
                null
            );
        
        VirtualFile[] files = dialog.choose(project);
        if (files.length > 0) {
            VirtualFile exportDir = files[0];
            
            // 导出文档
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    String markdown = storage.loadMarkdown();
                    if (markdown == null) {
                        Messages.showErrorDialog(project, "无法加载文档", "活文档");
                        return;
                    }
                    
                    File exportFile = new File(exportDir.getPath(), project.getName() + "-API文档.md");
                    try (FileWriter writer = new FileWriter(exportFile)) {
                        writer.write(markdown);
                    }
                    
                    Notifications.Bus.notify(
                        new Notification(
                            "LivingDoc",
                            "导出完成",
                            "文档已导出到: " + exportFile.getAbsolutePath(),
                            NotificationType.INFORMATION
                        ),
                        project
                    );
                    
                    // 刷新文件系统
                    exportDir.refresh(false, true);
                    
                } catch (Exception ex) {
                    Messages.showErrorDialog(project, "导出失败: " + ex.getMessage(), "活文档");
                    ex.printStackTrace();
                }
            });
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}
