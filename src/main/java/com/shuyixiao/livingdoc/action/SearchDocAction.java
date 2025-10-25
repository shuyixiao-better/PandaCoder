package com.shuyixiao.livingdoc.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.shuyixiao.livingdoc.analyzer.model.ProjectDocumentation;
import com.shuyixiao.livingdoc.search.SimpleSearchService;
import com.shuyixiao.livingdoc.storage.DocumentStorage;
import com.shuyixiao.livingdoc.ui.LivingDocToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 打开搜索对话框 Action
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class SearchDocAction extends AnAction {
    
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
                IndexProjectAction.performIndexing(project);
            }
            return;
        }
        
        // 显示搜索对话框
        String query = Messages.showInputDialog(
            project,
            "请输入搜索关键词：",
            "搜索活文档",
            Messages.getQuestionIcon()
        );
        
        if (query != null && !query.trim().isEmpty()) {
            performSearch(project, query);
        }
    }
    
    /**
     * 执行搜索
     */
    private void performSearch(Project project, String query) {
        try {
            // 加载文档
            DocumentStorage storage = new DocumentStorage(project);
            ProjectDocumentation doc = storage.loadDocumentation();
            
            if (doc == null) {
                Messages.showErrorDialog(project, "无法加载文档", "活文档");
                return;
            }
            
            // 执行搜索
            SimpleSearchService searchService = new SimpleSearchService();
            List<SimpleSearchService.SearchResultItem> results = searchService.search(doc, query);
            
            if (results.isEmpty()) {
                Messages.showInfoMessage(project, "未找到匹配的接口", "搜索结果");
                return;
            }
            
            // 打开工具窗口并显示结果
            openToolWindowWithResults(project, query, results);
            
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "搜索失败: " + ex.getMessage(), "活文档");
            ex.printStackTrace();
        }
    }
    
    /**
     * 打开工具窗口并显示搜索结果
     */
    private void openToolWindowWithResults(Project project, String query, List<SimpleSearchService.SearchResultItem> results) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("活文档");
        
        if (toolWindow != null) {
            toolWindow.show(() -> {
                Content content = toolWindow.getContentManager().getContent(0);
                if (content != null) {
                    LivingDocToolWindowPanel panel = (LivingDocToolWindowPanel) content.getComponent();
                    panel.displaySearchResults(query, results);
                }
            });
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}
