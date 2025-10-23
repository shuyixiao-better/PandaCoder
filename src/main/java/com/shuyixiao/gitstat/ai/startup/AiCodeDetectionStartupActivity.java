package com.shuyixiao.gitstat.ai.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.shuyixiao.gitstat.ai.config.GitStatAiConfigState;
import com.shuyixiao.gitstat.ai.detector.RealtimeAiCodeDetector;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

/**
 * AI 代码检测启动活动
 * 在项目启动时自动注册编辑器监听器
 */
public class AiCodeDetectionStartupActivity implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(AiCodeDetectionStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        // 检查是否启用 AI 统计
        GitStatAiConfigState config = GitStatAiConfigState.getInstance();
        if (!config.enableAiStats || !config.enableRealtimeMonitoring) {
            LOG.info("AI code detection is disabled for project: " + project.getName());
            return;
        }
        
        try {
            // 创建并注册实时检测器
            RealtimeAiCodeDetector detector = new RealtimeAiCodeDetector(project);
            
            // 注册为全局文档监听器
            EditorFactory.getInstance()
                    .getEventMulticaster()
                    .addDocumentListener(detector, project);
            
            // 定期清理过期的编辑会话（每5分钟）
            Timer cleanupTimer = new Timer("AiCodeDetector-Cleanup-" + project.getName(), true);
            cleanupTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    detector.cleanupOldSessions();
                }
            }, 5 * 60 * 1000, 5 * 60 * 1000);
            
            LOG.info("AI 代码实时检测已启动 for project: " + project.getName());
            
        } catch (Exception e) {
            LOG.error("Failed to start AI code detection", e);
        }
    }
}

