package com.shuyixiao.bugrecorder.listener;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.shuyixiao.bugrecorder.parser.ErrorParser;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 控制台输出监听器
 * 负责实时捕获IntelliJ IDEA运行/调试控制台的输出内容
 * 并识别其中的错误和异常信息
 */
public class ConsoleOutputListener implements ProcessListener {

    private static final Logger LOG = Logger.getInstance(ConsoleOutputListener.class);

    private final Project project;
    private final ErrorParser errorParser;
    private final BugRecordService bugRecordService;

    // 线程安全地拼接错误文本
    private final StringBuilder currentErrorBuffer = new StringBuilder();
    private final Object errorBufferLock = new Object();

    // 错误缓冲配置
    private static final int MAX_BUFFER_SIZE = 10000; // 最大缓冲大小
    private static final int BUFFER_TIMEOUT_MS = 5000; // 缓冲超时时间（毫秒）
    private long lastBufferUpdateTime = System.currentTimeMillis();

    // 定时器用于处理超时
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    // 用于识别错误输出的关键词 - 扩展支持更多错误类型
    private static final String[] ERROR_INDICATORS = {
            // 基础异常关键词
            "Exception", "Error", "Caused by", "at ", "java.lang",
            
            // Spring相关
            "org.springframework", "springframework", "spring boot",
            
            // 数据库相关
            "com.mysql", "org.hibernate", "jdbc", "sql", "database",
            
            // HTTP相关
            "http", "feign", "resttemplate", "webclient", "okhttp", "apache httpclient",
            
            // JSON/序列化相关
            "jackson", "gson", "json", "jaxb", "xstream",
            
            // 校验相关
            "validation", "constraint", "argument", "bind",
            
            // Bean/DI相关
            "bean", "dependency", "circular", "creation",
            
            // 超时相关
            "timeout", "timed out", "connection refused",
            
            // 缓存相关
            "redis", "ehcache", "hazelcast", "caffeine",
            
            // 消息队列相关
            "kafka", "rabbitmq", "activemq", "rocketmq",
            
            // 网络相关
            "connection", "socket", "tcp", "connect",
            
            // 安全相关
            "security", "authentication", "authorization",
            
            // 编译相关
            "compilation", "syntax", "compile",
            
            // 内存相关
            "outofmemory", "heap", "memory"
    };

    public ConsoleOutputListener(Project project) {
        this.project = project;
        this.errorParser = new ErrorParser();
        this.bugRecordService = project.getService(BugRecordService.class);
        
        // 启动超时检查任务
        startTimeoutChecker();
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        String text = event.getText();

        // 只处理标准错误输出和包含错误关键词的标准输出
        if (ProcessOutputTypes.STDERR.equals(outputType) || containsErrorIndicator(text)) {
            // 异步处理文本，避免阻塞控制台输出
            ApplicationManager.getApplication().executeOnPooledThread(() -> processErrorText(text));
        }
    }

    /**
     * 检查文本是否包含错误指示词
     */
    private boolean containsErrorIndicator(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        for (String indicator : ERROR_INDICATORS) {
            if (lowerText.contains(indicator.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理错误文本
     */
    private void processErrorText(String text) {
        try {
            // 将文本添加到当前错误缓冲区
            String completeError = null;
            synchronized (errorBufferLock) {
                currentErrorBuffer.append(text);
                lastBufferUpdateTime = System.currentTimeMillis();
                
                // 检查缓冲区大小限制
                if (currentErrorBuffer.length() > MAX_BUFFER_SIZE) {
                    LOG.warn("Error buffer exceeded maximum size, processing current content");
                    completeError = currentErrorBuffer.toString();
                    currentErrorBuffer.setLength(0);
                } else if (isCompleteError(currentErrorBuffer.toString())) {
                    completeError = currentErrorBuffer.toString();
                    currentErrorBuffer.setLength(0);
                }
            }

            // 如果形成了完整错误，进行解析与保存
            if (completeError != null) {
                BugRecord bugRecord = errorParser.parseError(completeError, project);
                if (bugRecord != null) {
                    bugRecordService.saveBugRecord(bugRecord);
                    LOG.info("Bug record created: " + bugRecord.getErrorType() + 
                            " (fingerprint: " + bugRecord.getFingerprint() + ")");
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to process error text", e);
        }
    }

    /**
     * 启动超时检查器
     */
    private void startTimeoutChecker() {
        timeoutExecutor.scheduleWithFixedDelay(() -> {
            try {
                checkBufferTimeout();
            } catch (Exception e) {
                LOG.warn("Error in timeout checker", e);
            }
        }, BUFFER_TIMEOUT_MS, BUFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查缓冲区超时
     */
    private void checkBufferTimeout() {
        synchronized (errorBufferLock) {
            if (currentErrorBuffer.length() > 0 && 
                System.currentTimeMillis() - lastBufferUpdateTime > BUFFER_TIMEOUT_MS) {
                
                LOG.debug("Processing error buffer due to timeout");
                String completeError = currentErrorBuffer.toString();
                currentErrorBuffer.setLength(0);
                
                // 异步处理超时的错误内容
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        BugRecord bugRecord = errorParser.parseError(completeError, project);
                        if (bugRecord != null) {
                            bugRecordService.saveBugRecord(bugRecord);
                            LOG.info("Bug record created from timeout buffer: " + bugRecord.getErrorType());
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed to process timeout error text", e);
                    }
                });
            }
        }
    }

    /**
     * 判断是否是一个完整的错误信息
     * 改进实现：基于多种条件判断错误是否完整
     */
    private boolean isCompleteError(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String[] lines = text.split("\n");
        if (lines.length < 2) {
            return false; // 至少需要2行才能形成完整错误
        }

        String lastLine = lines[lines.length - 1].trim();
        String secondLastLine = lines.length > 1 ? lines[lines.length - 2].trim() : "";

        // 条件1：最后一行不是堆栈跟踪行
        boolean notStackFrame = !lastLine.startsWith("at ") && !lastLine.startsWith("Caused by");
        
        // 条件2：最后一行不是空行
        boolean notEmptyLine = !lastLine.isEmpty();
        
        // 条件3：最后一行不是异常类的开始（通常异常类后面跟着冒号和消息）
        boolean notExceptionStart = !lastLine.contains("Exception:") && !lastLine.contains("Error:");
        
        // 条件4：倒数第二行是堆栈跟踪行，最后一行不是（表示堆栈结束）
        boolean stackEnded = secondLastLine.startsWith("at ") && notStackFrame;
        
        // 条件5：包含完整的异常信息（有异常类、消息和堆栈）
        boolean hasCompleteInfo = text.contains("Exception:") || text.contains("Error:");
        
        // 综合判断：满足多个条件时认为错误完整
        return (notStackFrame && notEmptyLine && notExceptionStart) || 
               (stackEnded && hasCompleteInfo) ||
               (hasCompleteInfo && lines.length >= 3); // 至少3行且包含异常信息
    }

    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        LOG.debug("Process started, beginning console monitoring");
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        LOG.debug("Process terminated, stopping console monitoring");
        
        // 处理缓冲区中剩余的文本
        synchronized (errorBufferLock) {
            if (currentErrorBuffer.length() > 0) {
                LOG.debug("Processing remaining error buffer on process termination");
                String remainingText = currentErrorBuffer.toString();
                currentErrorBuffer.setLength(0);
                
                // 直接触发一次检测
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        BugRecord bugRecord = errorParser.parseError(remainingText, project);
                        if (bugRecord != null) {
                            bugRecordService.saveBugRecord(bugRecord);
                            LOG.info("Final bug record created: " + bugRecord.getErrorType());
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed to process final error text", e);
                    }
                });
            }
        }
        
        // 关闭超时检查器
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}