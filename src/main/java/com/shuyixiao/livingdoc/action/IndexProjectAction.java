package com.shuyixiao.livingdoc.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.shuyixiao.livingdoc.analyzer.JavaDocAnalyzer;
import com.shuyixiao.livingdoc.analyzer.model.ProjectDocumentation;
import com.shuyixiao.livingdoc.generator.MarkdownGenerator;
import com.shuyixiao.livingdoc.storage.DocumentStorage;
import org.jetbrains.annotations.NotNull;

/**
 * 索引项目文档 Action
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class IndexProjectAction extends AnAction {
    
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
        
        // 后台任务索引
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "索引项目文档", true) {
            private String markdownPath;
            private int endpointCount;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("正在分析项目代码...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.0);
                    
                    // 1. 分析代码（在 Read Action 中执行 PSI 操作）
                    ProjectDocumentation doc = ReadAction.compute(() -> {
                        JavaDocAnalyzer analyzer = new JavaDocAnalyzer(project);
                        return analyzer.analyze();
                    });
                    
                    endpointCount = doc.getEndpoints().size();
                    indicator.setFraction(0.4);
                    
                    // 2. 生成 Markdown
                    indicator.setText("正在生成文档...");
                    MarkdownGenerator generator = new MarkdownGenerator();
                    String markdown = generator.generate(doc);
                    indicator.setFraction(0.7);
                    
                    // 3. 保存文档
                    indicator.setText("正在保存文档...");
                    DocumentStorage storage = new DocumentStorage(project);
                    storage.saveDocumentation(doc);
                    storage.saveMarkdown(markdown);
                    markdownPath = storage.getMarkdownPath();
                    indicator.setFraction(1.0);
                    
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Notifications.Bus.notify(
                            new Notification(
                                "LivingDoc",
                                "索引失败",
                                "错误: " + ex.getMessage(),
                                NotificationType.ERROR
                            ),
                            project
                        );
                    });
                    ex.printStackTrace();
                }
            }
            
            @Override
            public void onSuccess() {
                String message = String.format("✅ 索引完成！发现 %d 个接口", endpointCount);
                
                Notifications.Bus.notify(
                    new Notification(
                        "LivingDoc",
                        "索引完成",
                        message + "\n文档已保存到: " + markdownPath,
                        NotificationType.INFORMATION
                    ),
                    project
                );
                
                int result = Messages.showYesNoDialog(
                    project,
                    message + "\n\n是否立即打开文档？",
                    "活文档",
                    "打开文档",
                    "关闭",
                    Messages.getInformationIcon()
                );
                
                if (result == Messages.YES) {
                    openMarkdownFile(project, markdownPath);
                }
            }
        });
    }
    
    /**
     * 打开 Markdown 文件
     */
    private void openMarkdownFile(Project project, String path) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                com.intellij.openapi.vfs.VirtualFile file = 
                    com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
                if (file != null) {
                    com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(file, true);
                }
            } catch (Exception ex) {
                Messages.showErrorDialog(project, "无法打开文件: " + ex.getMessage(), "活文档");
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}
