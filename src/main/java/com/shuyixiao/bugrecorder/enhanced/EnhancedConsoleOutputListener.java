package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.parser.ErrorParser;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 增强版控制台输出监听器
 * 提供更好的性能、准确性和功能
 */
public class EnhancedConsoleOutputListener implements ProcessListener {

    private static final Logger LOG = Logger.getInstance(EnhancedConsoleOutputListener.class);

    private final Project project;
    private final BugRecordService bugRecordService;
    private final ErrorParser errorParser;

    // 使用更高效的缓冲区实现
    private final StringBuilder errorBuffer = new StringBuilder();
    private final Object bufferLock = new Object();
    
    // 状态管理
    private final AtomicBoolean isActive = new AtomicBoolean(true);
    private final AtomicInteger bufferLength = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastUpdateTime = new AtomicReference<>(LocalDateTime.now());
    
    // 线程池优化
    private final ExecutorService parsingExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    
    // 配置参数
    private static final int MAX_BUFFER_SIZE = 50000; // 减小缓冲区以提高响应速度
    private static final int BUFFER_TIMEOUT_MS = 3000; // 缩短超时时间
    private static final int MAX_CONCURRENT_PARSING = 3; // 限制并发解析数
    
    // 错误检测优化
    private static final String[] ERROR_START_MARKERS = {
        // 基础异常关键词
        "Exception", "Error", "Caused by:", "SEVERE:", "FATAL:",
        
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
    
    private static final String[] ERROR_CONTINUATION_MARKERS = {
        "at ", "\tat ", "Caused by:", "Suppressed:", "... "
    };

    public EnhancedConsoleOutputListener(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        this.errorParser = new ErrorParser();
        
        // 创建专用线程池处理记录保存
        this.parsingExecutor = new ThreadPoolExecutor(
                1, MAX_CONCURRENT_PARSING,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = defaultFactory.newThread(r);
                        t.setName("BugRecord-Enhanced-" + t.getName());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 防止任务丢失
        );
        
        // 创建清理定时器
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BugRecord-Timeout");
            t.setDaemon(true);
            return t;
        });
        
        // 启动超时检查
        startTimeoutChecker();
        
        LOG.info("Enhanced Console Output Listener initialized for project: " + project.getName());
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (!isActive.get()) {
            return;
        }

        String text = event.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        // 只处理标准错误输出和包含错误关键词的标准输出
        if (ProcessOutputTypes.STDERR.equals(outputType) || 
            containsErrorIndicators(text)) {
            
            // 异步处理以避免阻塞控制台
            parsingExecutor.submit(() -> processTextChunk(text));
        }
    }

    /**
     * 检查文本是否包含错误标识
     */
    private boolean containsErrorIndicators(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        
        // 检查开头标识符
        for (String marker : ERROR_START_MARKERS) {
            if (lowerText.contains(marker.toLowerCase())) {
                return true;
            }
        }
        
        // 如果已经有错误在缓冲区中，检查延续标识符
        synchronized (bufferLock) {
            if (bufferLength.get() > 0) {
                for (String marker : ERROR_CONTINUATION_MARKERS) {
                    if (lowerText.contains(marker.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * 处理文本块
     */
    private void processTextChunk(String text) {
        try {
            synchronized (bufferLock) {
                // 更新缓冲区
                errorBuffer.append(text);
                bufferLength.addAndGet(text.length());
                lastUpdateTime.set(LocalDateTime.now());
                
                // 检查缓冲区大小限制
                if (bufferLength.get() > MAX_BUFFER_SIZE) {
                    LOG.warn("Error buffer size exceeded limit, processing immediately");
                    processCurrentBuffer();
                    return;
                }
                
                // 检查是否形成完整错误
                if (isCompleteError(errorBuffer.toString())) {
                    processCurrentBuffer();
                }
            }
        } catch (Exception e) {
            LOG.warn("Error processing text chunk", e);
        }
    }

    /**
     * 判断是否是完整错误
     */
    private boolean isCompleteError(String bufferContent) {
        if (bufferContent == null || bufferContent.isEmpty()) {
            return false;
        }
        
        String[] lines = bufferContent.split("\n");
        if (lines.length < 3) {
            return false; // 至少需要几行才能构成完整错误
        }
        
        // 检查最后几行是否表明错误结束
        for (int i = Math.max(0, lines.length - 3); i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 错误结束标志：空行、新行开始、或者不是堆栈跟踪行
            if (line.isEmpty() || 
                (line.startsWith("[") && line.contains("]")) || // 日志时间戳
                (line.startsWith("INFO:") || line.startsWith("WARN:") || line.startsWith("DEBUG:"))) {
                return true;
            }
        }
        
        // 检查是否有明显的错误结束模式
        String lastLine = lines[lines.length - 1].trim();
        return lastLine.isEmpty() || 
               (lastLine.startsWith("[") && lastLine.contains("]")) ||
               lastLine.startsWith("---") ||
               lastLine.startsWith("====") ||
               (lastLine.contains(" completed") && lastLine.contains("ms"));
    }

    /**
     * 处理当前缓冲区内容
     */
    private void processCurrentBuffer() {
        if (bufferLength.get() == 0) {
            return;
        }
        
        String errorContent = errorBuffer.toString();
        errorBuffer.setLength(0);
        bufferLength.set(0);
        
        // 异步解析并在单独线程中处理
        parsingExecutor.submit(() -> {
            try {
                BugRecord bugRecord = errorParser.parseError(errorContent, project);
                if (bugRecord != null) {
                    bugRecordService.saveBugRecord(bugRecord);
                    LOG.info("Bug record saved: " + bugRecord.getId() + 
                            " (" + bugRecord.getErrorType().getDisplayName() + ")");
                }
            } catch (Exception e) {
                LOG.error("Failed to process error content", e);
            }
        });
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
        if (!isActive.get() || bufferLength.get() == 0) {
            return;
        }
        
        LocalDateTime lastUpdate = lastUpdateTime.get();
        if (lastUpdate != null && 
            LocalDateTime.now().isAfter(lastUpdate.plusSeconds(BUFFER_TIMEOUT_MS / 1000))) {
            
            synchronized (bufferLock) {
                // 双重检查
                if (bufferLength.get() > 0) {
                    LOG.debug("Processing buffer due to timeout");
                    processCurrentBuffer();
                }
            }
        }
    }

    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        LOG.debug("Process started, enhanced console monitoring activated");
        isActive.set(true);
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        LOG.debug("Process terminated, cleaning up resources");
        dispose();
    }

    @Override
    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
        LOG.debug("Process will terminate, preparing cleanup");
    }

    /**
     * 清理资源
     */
    public void dispose() {
        isActive.set(false);
        
        // 关闭线程池
        parsingExecutor.shutdown();
        try {
            if (!parsingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                parsingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            parsingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭定时器
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 处理剩余缓冲区内容
        synchronized (bufferLock) {
            if (bufferLength.get() > 0) {
                processCurrentBuffer();
            }
        }
        
        LOG.info("Enhanced Console Output Listener disposed for project: " + project.getName());
    }

    /**
     * 检查监听器是否活跃
     */
    public boolean isActive() {
        return isActive.get();
    }

    /**
     * 获取缓冲区长度
     */
    public int getBufferLength() {
        return bufferLength.get();
    }

    /**
     * 获取最后更新时间
     */
    @NotNull
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime.get();
    }
}