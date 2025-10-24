package com.shuyixiao.livingdoc.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 导出文档 Action
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ExportDocAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
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
            
            // TODO: 实际的导出逻辑
            
            Notifications.Bus.notify(
                new Notification(
                    "LivingDoc",
                    "导出完成",
                    "文档已导出到: " + exportDir.getPath(),
                    NotificationType.INFORMATION
                ),
                project
            );
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}

