package com.shuyixiao.esdsl.listener;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.shuyixiao.esdsl.model.EsDslRecord;
import com.shuyixiao.esdsl.parser.EsDslParser;
import com.shuyixiao.esdsl.service.EsDslRecordService;
import org.jetbrains.annotations.NotNull;

/**
 * ES DSL 输出监听器
 * 监听控制台输出，捕获 Elasticsearch 查询 DSL
 */
public class EsDslOutputListener implements ProcessListener {
    
    private static final Logger LOG = Logger.getInstance(EsDslOutputListener.class);
    
    private final Project project;
    private final EsDslRecordService recordService;
    private final StringBuilder buffer = new StringBuilder();
    
    public EsDslOutputListener(@NotNull Project project) {
        this.project = project;
        this.recordService = project.getService(EsDslRecordService.class);
    }
    
    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        LOG.debug("Process started, ES DSL monitoring active");
    }
    
    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        LOG.debug("Process terminated, ES DSL monitoring stopped");
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
            // 将文本添加到缓冲区
            buffer.append(text);
            
            // 检查是否包含完整的 ES DSL 查询
            String bufferedText = buffer.toString();
            
            // 如果缓冲区太大，只保留最后10000个字符
            if (bufferedText.length() > 10000) {
                bufferedText = bufferedText.substring(bufferedText.length() - 10000);
                buffer.setLength(0);
                buffer.append(bufferedText);
            }
            
            // 尝试解析 ES DSL
            if (EsDslParser.containsEsDsl(bufferedText)) {
                EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, project.getName());
                if (record != null) {
                    // 保存记录
                    recordService.addRecord(record);
                    LOG.debug("Captured ES DSL query: " + record.getShortQuery());
                    
                    // 清空缓冲区，避免重复解析
                    buffer.setLength(0);
                }
            }
            
            // 如果检测到换行符，尝试清理缓冲区
            if (text.contains("\n")) {
                // 保留最后1000个字符以处理跨行的DSL
                if (buffer.length() > 1000) {
                    String remaining = buffer.substring(Math.max(0, buffer.length() - 1000));
                    buffer.setLength(0);
                    buffer.append(remaining);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Error processing ES DSL output", e);
        }
    }
}

