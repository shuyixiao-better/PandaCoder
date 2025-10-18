package com.shuyixiao.sql.listener;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.parser.SqlParser;
import com.shuyixiao.sql.service.SqlRecordService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQL 输出监听器
 * 监听控制台输出，捕获 SQL 查询日志
 */
public class SqlOutputListener implements ProcessListener {
    
    private static final Logger LOG = Logger.getInstance(SqlOutputListener.class);
    
    // 缓冲区大小：300K 足够容纳SQL日志和上下文
    private static final int MAX_BUFFER_SIZE = 300000;
    
    // 保留上下文大小：100K 用于保留API路径等上下文信息（API日志可能在SQL之前很多行）
    private static final int CROSS_LINE_RETAIN_SIZE = 100000;
    
    // 调试模式
    private static final boolean DEBUG_MODE = true;
    
    private final Project project;
    private final SqlRecordService recordService;
    private final StringBuilder buffer = new StringBuilder();
    
    // 标记是否正在解析（避免并发解析）
    private final AtomicBoolean isParsing = new AtomicBoolean(false);
    
    public SqlOutputListener(@NotNull Project project) {
        this.project = project;
        this.recordService = project.getService(SqlRecordService.class);
    }
    
    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        LOG.warn("===============================================");
        LOG.warn("[SQL Monitor] 🚀 监听器已启动！");
        LOG.warn("[SQL Monitor] 项目: " + project.getName());
        LOG.warn("[SQL Monitor] DEBUG 模式: " + DEBUG_MODE);
        LOG.warn("===============================================");
    }
    
    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        LOG.debug("[SQL Monitor] Process terminated, SQL monitoring stopped");
        // 清空缓冲区
        buffer.setLength(0);
    }
    
    @Override
    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
        // 进程即将终止
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        String text = event.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        
        try {
            // 过滤掉ES相关日志（避免干扰）
            String lowerText = text.toLowerCase();
            if (lowerText.contains("requestlogger") || 
                lowerText.contains("elasticsearch") ||
                lowerText.contains("elastic")) {
                return;
            }
            
            // 只保留SQL相关的日志
            if (shouldKeepText(text)) {
                buffer.append(text);
            }
            
            // 缓冲区太大时清理
            if (buffer.length() > MAX_BUFFER_SIZE) {
                String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                buffer.setLength(0);
                buffer.append(remaining);
                return;
            }
            
            // 检测到Total行，说明一条完整的SQL日志结束
            if (text.contains("<==") && text.contains("Total:")) {
                if (DEBUG_MODE) {
                    LOG.warn("[SQL Monitor] 📊 检测到SQL日志结束标记（Total），缓冲区大小: " + (buffer.length() / 1024) + "KB");
                }
                
                // 异步解析SQL
                triggerAsyncParse();
            }
            
        } catch (Exception e) {
            LOG.error("[SQL Monitor] Error processing SQL output", e);
        }
    }
    
    /**
     * 判断是否应该保留该文本到缓冲区
     */
    private boolean shouldKeepText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        // 保留SQL相关的日志
        if (lowerText.contains("preparing:") || 
            lowerText.contains("parameters:") || 
            lowerText.contains("total:") ||
            lowerText.contains("basejdbclogger")) {
            return true;
        }
        
        // ✅ 保留包含API路径的日志行（多种格式）
        if (lowerText.contains("api:") || 
            lowerText.contains("uri:") || 
            lowerText.contains("/api/") ||
            lowerText.contains("/kl/") ||
            lowerText.contains("/kb/") ||
            lowerText.contains("controller")) {
            return true;
        }
        
        // ✅ 保留包含常见业务日志的行（可能包含API信息）
        if (lowerText.contains("分页查询") || 
            lowerText.contains("查询") ||
            lowerText.contains("根据") ||
            lowerText.contains("page:") ||
            lowerText.contains("code:")) {
            return true;
        }
        
        // 如果缓冲区已经有SQL日志，保留后续的行（可能是参数或结果）
        if (buffer.length() > 0) {
            String bufferedText = buffer.toString();
            if (bufferedText.contains("Preparing:") && !text.trim().isEmpty()) {
                // 保留空行和可能的参数/结果行
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 异步触发解析
     */
    private void triggerAsyncParse() {
        // 避免并发解析
        if (!isParsing.compareAndSet(false, true)) {
            return;
        }
        
        // 获取当前缓冲区内容的快照
        final String bufferedText = buffer.toString();
        
        // 在后台线程异步解析
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                parseAndSave(bufferedText);
            } finally {
                isParsing.set(false);
            }
        });
    }
    
    /**
     * 解析并保存 SQL
     */
    private void parseAndSave(String bufferedText) {
        try {
            if (DEBUG_MODE) {
                LOG.debug("[SQL Monitor] 🔍 开始解析，文本长度: " + (bufferedText.length() / 1024) + "KB");
            }
            
            // 快速检查
            if (!SqlParser.containsSql(bufferedText)) {
                if (DEBUG_MODE) {
                    LOG.debug("[SQL Monitor] ⚠️ 不包含 SQL 关键词，跳过");
                }
                return;
            }
            
            if (DEBUG_MODE) {
                LOG.debug("[SQL Monitor] 📝 包含 SQL 关键词，开始详细解析...");
            }
            
            // 解析 SQL
            SqlRecord record = SqlParser.parseSql(bufferedText, project.getName());
            if (record != null) {
                // 保存记录
                recordService.addRecord(record);
                
                // 日志输出
                LOG.info("✅ 成功捕获 SQL 查询:");
                LOG.info("  ├─ 操作: " + record.getOperation());
                LOG.info("  ├─ 表名: " + record.getTableName());
                LOG.info("  ├─ 结果数: " + record.getResultCount());
                LOG.info("  ├─ API路径: " + (record.getApiPath() != null ? record.getApiPath() : "N/A"));
                LOG.info("  ├─ 调用类: " + (record.getCallerClass() != null ? record.getCallerClass() : "N/A"));
                LOG.info("  └─ SQL长度: " + (record.getSqlStatement() != null ? record.getSqlStatement().length() : 0) + " 字符");
                
                // 清理缓冲区但保留上下文（在 UI 线程）
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (buffer.toString().equals(bufferedText)) {
                        // 保留最后的部分用于API路径提取
                        if (buffer.length() > CROSS_LINE_RETAIN_SIZE) {
                            String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                            buffer.setLength(0);
                            buffer.append(remaining);
                            if (DEBUG_MODE) {
                                LOG.debug("[SQL Monitor] 🧹 已清理缓冲区，保留 " + (remaining.length() / 1024) + "KB 上下文");
                            }
                        }
                    }
                });
            } else {
                if (DEBUG_MODE) {
                    LOG.warn("[SQL Monitor] ❌ 解析失败，返回 null (缓冲区: " + (bufferedText.length() / 1024) + "KB)");
                }
            }
        } catch (Exception e) {
            LOG.warn("[SQL Monitor] ❌ 解析异常", e);
        }
    }
}

