package com.shuyixiao.gitstat.ai.detector;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.gitstat.ai.model.AiCodeRecord;
import com.shuyixiao.gitstat.ai.storage.AiCodeRecordStorage;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时 AI 代码检测器
 * 通过监听编辑器事件，实时识别 AI 生成的代码
 * 
 * 性能优化：
 * 1. 只监听代码文件
 * 2. 忽略小的变更（< 10 字符）
 * 3. 异步处理，不阻塞编辑器
 */
public class RealtimeAiCodeDetector implements DocumentListener {
    
    private static final Logger LOG = Logger.getInstance(RealtimeAiCodeDetector.class);
    
    // 记录每个文档的编辑会话
    private final ConcurrentHashMap<Document, EditSession> editSessions = new ConcurrentHashMap<>();
    
    // AI 代码检测记录存储
    private final AiCodeRecordStorage recordStorage;
    
    // 只监听代码文件（性能优化）
    private static final Set<String> CODE_EXTENSIONS = new HashSet<>();
    static {
        CODE_EXTENSIONS.add("java");
        CODE_EXTENSIONS.add("kt");
        CODE_EXTENSIONS.add("groovy");
        CODE_EXTENSIONS.add("scala");
        CODE_EXTENSIONS.add("py");
        CODE_EXTENSIONS.add("js");
        CODE_EXTENSIONS.add("ts");
        CODE_EXTENSIONS.add("jsx");
        CODE_EXTENSIONS.add("tsx");
        CODE_EXTENSIONS.add("go");
        CODE_EXTENSIONS.add("rs");
        CODE_EXTENSIONS.add("c");
        CODE_EXTENSIONS.add("cpp");
        CODE_EXTENSIONS.add("h");
        CODE_EXTENSIONS.add("hpp");
        CODE_EXTENSIONS.add("cs");
        CODE_EXTENSIONS.add("php");
        CODE_EXTENSIONS.add("rb");
        CODE_EXTENSIONS.add("swift");
    }
    
    public RealtimeAiCodeDetector(Project project) {
        this.recordStorage = project.getService(AiCodeRecordStorage.class);
        LOG.info("RealtimeAiCodeDetector initialized for project: " + project.getName());
    }
    
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        // 性能优化 1: 快速过滤 - 只处理代码文件
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        
        if (file == null || !isCodeFile(file)) {
            return; // 非代码文件，直接返回
        }
        
        // 性能优化 2: 快速过滤 - 只处理新增代码
        int newLength = event.getNewFragment().length();
        int oldLength = event.getOldFragment().length();
        int netChange = newLength - oldLength;
        
        if (netChange < 10) {
            return; // 小的变更（< 10 字符），直接返回
        }
        
        // 性能优化 3: 异步处理，不阻塞编辑器
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                processCodeChange(document, event, file);
            } catch (Exception e) {
                LOG.warn("Failed to process code change: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理代码变更
     */
    private void processCodeChange(Document document, DocumentEvent event, VirtualFile file) {
        // 获取或创建编辑会话
        EditSession session = editSessions.computeIfAbsent(document, k -> new EditSession());
        
        // 记录变更信息
        int newLength = event.getNewFragment().length();
        int oldLength = event.getOldFragment().length();
        int netChange = newLength - oldLength;
        
        // 计算时间间隔
        long now = System.currentTimeMillis();
        long duration = now - session.lastEditTime;
        
        // 计算新增行数
        int lineCount = countLines(event.getNewFragment());
        
        // 判断是否为 AI 生成
        int aiProbability = AiCodeDetector.calculateAiProbability(netChange, duration, lineCount);
        
        // 如果 AI 概率 >= 70%，记录为 AI 生成
        if (AiCodeDetector.shouldRecordAsAi(aiProbability)) {
            recordAiCode(file, event, aiProbability, lineCount);
        }
        
        // 更新会话
        session.lastEditTime = now;
    }
    
    /**
     * 记录 AI 生成的代码
     */
    private void recordAiCode(VirtualFile file, DocumentEvent event, int aiProbability, int lineCount) {
        AiCodeRecord record = new AiCodeRecord();
        record.setFilePath(file.getPath());
        record.setTimestamp(System.currentTimeMillis());
        record.setStartOffset(event.getOffset());
        record.setEndOffset(event.getOffset() + event.getNewFragment().length());
        record.setCodeContent(event.getNewFragment().toString());
        record.setAiProbability(aiProbability);
        record.setDetectionMethod(AiCodeRecord.DetectionMethod.REALTIME_SPEED_ANALYSIS);
        record.setLineCount(lineCount);
        
        // 尝试识别 AI 工具
        record.setAiTool(detectAiTool());
        
        // 保存记录（异步）
        recordStorage.saveRecord(record);
        
        LOG.info(String.format("AI code detected: %s, probability=%d%%, tool=%s, lines=%d",
                file.getName(), aiProbability, record.getAiTool(), lineCount));
    }
    
    /**
     * 检测当前活跃的 AI 工具
     */
    private String detectAiTool() {
        // 检查是否安装了 Copilot 插件
        if (isPluginInstalled("com.github.copilot")) {
            return "GitHub Copilot";
        }
        
        // 检查是否安装了 Tabnine 插件
        if (isPluginInstalled("com.tabnine.TabNine")) {
            return "Tabnine";
        }
        
        // 检查是否安装了 CodeWhisperer 插件
        if (isPluginInstalled("amazon.q")) {
            return "Amazon CodeWhisperer";
        }
        
        // 检查是否安装了 Cursor 相关插件
        if (isPluginInstalled("com.cursor")) {
            return "Cursor AI";
        }
        
        return "AI Assistant";
    }
    
    /**
     * 检查插件是否安装
     */
    private boolean isPluginInstalled(String pluginId) {
        try {
            return PluginManager.isPluginInstalled(PluginId.getId(pluginId));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 计算代码行数
     */
    private int countLines(CharSequence text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
    
    /**
     * 判断是否为代码文件
     */
    private boolean isCodeFile(VirtualFile file) {
        String extension = file.getExtension();
        return extension != null && CODE_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * 编辑会话
     * 记录编辑器的连续编辑状态
     */
    private static class EditSession {
        long lastEditTime = System.currentTimeMillis();
    }
    
    /**
     * 清理过期的编辑会话（定期调用）
     */
    public void cleanupOldSessions() {
        long now = System.currentTimeMillis();
        long threshold = 5 * 60 * 1000; // 5 分钟
        
        editSessions.entrySet().removeIf(entry -> 
            now - entry.getValue().lastEditTime > threshold
        );
    }
}

