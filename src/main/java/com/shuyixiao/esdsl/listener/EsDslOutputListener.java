package com.shuyixiao.esdsl.listener;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.shuyixiao.esdsl.model.EsDslRecord;
import com.shuyixiao.esdsl.parser.EsDslParser;
import com.shuyixiao.esdsl.service.EsDslRecordService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ES DSL 输出监听器 - 高性能异步版
 * 监听控制台输出，异步捕获 Elasticsearch 查询 DSL
 * 使用合理的缓冲区大小和异步处理，不影响 IDEA 性能
 */
public class EsDslOutputListener implements ProcessListener {
    
    private static final Logger LOG = Logger.getInstance(EsDslOutputListener.class);
    
    // 合理的缓冲区大小：2MB 足够容纳包含大量向量数据的 ES 响应 + API路径上下文
    // ⚠️ 从300KB增加到2MB以支持大型ES响应(包含vector数组可能超过700KB)
    private static final int MAX_BUFFER_SIZE = 2000000;
    
    // 跨行保留的字符数：200K 用于保留API路径等上下文信息（需要保留足够多的历史日志）
    // ⚠️ 从50KB增加到200KB以确保大型响应不会丢失上下文
    private static final int CROSS_LINE_RETAIN_SIZE = 200000;
    
    // 触发解析的最小缓冲区大小 (降低门槛)
    private static final int MIN_PARSE_TRIGGER_SIZE = 200;
    
    // TRACE 日志结束标记（响应 JSON 的结束）
    private static final String TRACE_END_MARKER_1 = "]}}}";
    private static final String TRACE_END_MARKER_2 = "# {\"took\":";
    
    // RequestLogger 日志标记
    private static final String REQUEST_LOGGER_MARKER = "RequestLogger.java";
    private static final String CURL_MARKER = "curl -";
    
    // 调试模式：输出详细日志(优化后减少噪音)
    private static final boolean DEBUG_MODE = true;
    private static final boolean VERBOSE_MODE = true; // 超详细模式(临时启用用于诊断)
    
    private final Project project;
    private final EsDslRecordService recordService;
    private final StringBuilder buffer = new StringBuilder();
    
    // 标记是否正在解析（避免并发解析）
    private final AtomicBoolean isParsing = new AtomicBoolean(false);
    
    // 上次解析时间（避免频繁解析）
    private long lastParseTime = 0;
    private static final long MIN_PARSE_INTERVAL_MS = 50; // 减少到 50ms
    
    public EsDslOutputListener(@NotNull Project project) {
        this.project = project;
        this.recordService = project.getService(EsDslRecordService.class);
    }
    
    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        LOG.warn("===============================================");
        LOG.warn("[ES DSL] 🚀 监听器已启动！");
        LOG.warn("[ES DSL] 项目: " + project.getName());
        LOG.warn("[ES DSL] 项目路径: " + (project.getBasePath() != null ? project.getBasePath() : "Unknown"));
        LOG.warn("[ES DSL] DEBUG 模式: " + DEBUG_MODE);
        LOG.warn("===============================================");
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
            // ✅ 优先处理新的TRACE RequestLogger日志(不通过shouldKeepText检查)
            if (text.contains("TRACE") && text.contains("RequestLogger") && text.contains("curl")) {
                // ✅ 如果缓冲区已有较多内容，先触发解析
                if (buffer.length() > 10000) { // 超过10KB就先解析
                    if (DEBUG_MODE) {
                        LOG.warn("[ES DSL] 🔍 缓冲区较大(" + (buffer.length() / 1024) + "KB)，先解析旧内容");
                    }
                    // 先解析缓冲区中的内容
                    String oldBufferContent = buffer.toString();
                    if (oldBufferContent.length() > 200) {
                        parseAndSave(oldBufferContent);
                    }
                    // 清空缓冲区，准备接收新的TRACE日志
                    buffer.setLength(0);
                }
                
                // ✅ 添加新TRACE日志
                buffer.append(text);
                
                // 调试：如果包含关键词，输出日志
                if (DEBUG_MODE) {
                    LOG.warn("[ES DSL] 📨 检测到 TRACE RequestLogger 日志！");
                    LOG.warn("[ES DSL] 文本长度: " + text.length());
                    LOG.warn("[ES DSL] 当前缓冲区大小: " + (buffer.length() / 1024) + "KB");
                    LOG.warn("[ES DSL] 前150字符: " + text.substring(0, Math.min(150, text.length())));
                }
                
                // ⚠️ 不要立即解析，等待后续的响应数据
                return;  // ✅ 提前返回,不再执行shouldKeepText检查
            }
            
            // ✅ 智能过滤：只保留ES相关的日志行
            if (shouldKeepText(text)) {
                buffer.append(text);
            }
            
            // 快速检查：缓冲区太大时立即清理（避免性能问题）
            if (buffer.length() > MAX_BUFFER_SIZE) {
                // 保留最后的部分
                String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                buffer.setLength(0);
                buffer.append(remaining);
                return;
            }
            
            // 智能触发：只在检测到可能的完整日志时才解析
            if (shouldTriggerParse(text)) {
                // 异步解析，不阻塞主线程
                triggerAsyncParse();
            }
            
            // 轻量级清理：定期清理缓冲区
            if (shouldCleanBuffer(text)) {
                cleanBuffer();
            }
            
        } catch (Exception e) {
            LOG.error("Error processing ES DSL output", e);
        }
    }
    
    
    /**
     * 判断是否应该保留该文本到缓冲区
     * 只保留ES相关的日志,过滤掉Spring Boot启动日志等无关内容
     */
    private boolean shouldKeepText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        // ✅ 明确过滤掉SQL日志（让SQL Monitor处理）
        // SQL日志特征：basejdbclogger, preparing:, parameters:, ==>, <==
        if (lowerText.contains("basejdbclogger") ||
            lowerText.contains("preparing:") ||
            lowerText.contains("parameters:") ||
            (lowerText.contains("==>") && (lowerText.contains("preparing") || lowerText.contains("parameters"))) ||
            (lowerText.contains("<==") && lowerText.contains("total:"))) {
            return false;
        }
        
        // ❌ 明确过滤掉Spring框架日志
        // ⚠️ 注意：不要过滤掉包含API路径的Controller日志和调用ES的Service日志
        if (lowerText.contains("repositoryconfigurationdelegate") ||
            lowerText.contains("tomcatwebserver") ||
            lowerText.contains("dingtalkstreammanager") ||
            lowerText.contains("dingtalkbootstrap") ||
            lowerText.contains("nettyinternallogger") ||
            lowerText.contains("rabbitmqregister") ||
            lowerText.contains("shedlockconfiguration") ||
            lowerText.contains("satoken") ||
            lowerText.contains("redisson") ||
            lowerText.contains("hikaripool") ||
            lowerText.contains("servlet") ||
            lowerText.contains("spring boot") ||
            lowerText.contains("mybatisplus")) {
            return false;
        }
        
        // ✅ 保留包含API路径的日志（Controller、Service等）
        // 但要排除SQL相关的Controller日志
        if ((lowerText.contains("api:") || lowerText.contains("uri:") || 
            lowerText.contains("controller")) && 
            !lowerText.contains("basejdbclogger")) {
            return true;
        }
        
        // ✅ 保留调用ES的Service类日志
        if (lowerText.contains("vectordataretrieverelastic") ||
            lowerText.contains("vectorassistant") ||
            (lowerText.contains("elastic") && !lowerText.contains("basejdbclogger"))) {
            return true;
        }
        
        // ✅ 只保留RequestLogger的TRACE日志(完整行),不保留DEBUG日志
        if (lowerText.contains("requestlogger") && lowerText.contains("trace")) {
            return true;
        }
        
        // ✅ 如果缓冲区已经有RequestLogger内容,保留后续的所有行直到遇到新的日志
        if (buffer.length() > 0) {
            String bufferedText = buffer.toString();
            // 检查缓冲区是否有RequestLogger日志
            if (bufferedText.contains("RequestLogger")) {
                // 保留后续的行(可能是curl命令的continuation、响应头、JSON响应等)
                // 排除明显不相关的新日志行(有时间戳+新的类名)
                if (text.startsWith("#") ||                    // 响应行
                    text.contains("'") ||                       // curl参数
                    text.contains("-d") ||                      // curl data
                    text.contains("{") ||                       // JSON
                    text.trim().isEmpty() ||                    // 空行
                    (!text.matches("^\\d{4}-\\d{2}-\\d{2}.*") && !lowerText.contains("info") && !lowerText.contains("debug"))) {  // 不是新日志行
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否应该触发解析
     * 优化后的触发条件，降低门槛
     */
    private boolean shouldTriggerParse(String text) {
        // 检查时间间隔（避免过于频繁）
        long now = System.currentTimeMillis();
        if (now - lastParseTime < MIN_PARSE_INTERVAL_MS) {
            if (DEBUG_MODE && buffer.length() > 1000) {
                LOG.debug("[ES DSL] 跳过触发：时间间隔太短 (" + (now - lastParseTime) + "ms < " + MIN_PARSE_INTERVAL_MS + "ms)");
            }
            return false;
        }
        
        // 检查缓冲区大小
        if (buffer.length() < MIN_PARSE_TRIGGER_SIZE) {
            return false;
        }
        
        // 检查是否包含关键标记
        String lowerText = text.toLowerCase();
        String bufferedText = buffer.toString();
        String bufferedLower = bufferedText.toLowerCase();
        
        // 1. TRACE 日志完整标记（包含响应）
        if (text.contains(TRACE_END_MARKER_1) || text.contains(TRACE_END_MARKER_2)) {
            if (DEBUG_MODE) {
                LOG.info("[ES DSL] ✅ 触发解析：检测到 TRACE 日志结束标记，缓冲区大小: " + (buffer.length() / 1024) + "K");
            }
            return true;
        }
        
        // 2. RequestLogger 日志（放宽条件）
        if (text.contains(REQUEST_LOGGER_MARKER) || bufferedLower.contains("requestlogger")) {
            if (DEBUG_MODE) {
                LOG.info("[ES DSL] ✅ 触发解析：检测到 RequestLogger 日志，缓冲区大小: " + (buffer.length() / 1024) + "K");
            }
            return true;
        }
        
        // 3. curl 命令（放宽条件：只要有 curl 和 -d）
        if (lowerText.contains(CURL_MARKER)) {
            // 检查缓冲区中是否有 -d 参数
            if (bufferedLower.contains("-d")) {
                // 检查是否有 JSON 结束
                if (bufferedText.contains("'}") || bufferedText.contains("\"}") || 
                    bufferedText.contains("'}\n") || bufferedText.contains("\"}\n")) {
                    if (DEBUG_MODE) {
                        LOG.info("[ES DSL] ✅ 触发解析：检测到完整 curl 命令，缓冲区大小: " + (buffer.length() / 1024) + "K");
                    }
                    return true;
                }
            }
        }
        
        // 4. ES 查询相关关键词 + JSON 结束（缓冲区检查）
        if ((bufferedLower.contains("_search") || bufferedLower.contains("elasticsearch")) 
            && bufferedText.contains("}")) {
            // 确保有 JSON 对象
            if (bufferedText.contains("{\"") || bufferedText.contains("{ \"")) {
                if (DEBUG_MODE) {
                    LOG.info("[ES DSL] ✅ 触发解析：检测到 ES 查询关键词 + JSON，缓冲区大小: " + (buffer.length() / 1024) + "K");
                }
                return true;
            }
        }
        
        // 5. 缓冲区较大时，定期尝试解析（避免遗漏）
        if (buffer.length() > 5000 && text.contains("\n")) {
            if (bufferedLower.contains("elastic") || bufferedLower.contains("_search") || 
                bufferedLower.contains("query") || bufferedLower.contains("curl")) {
                if (DEBUG_MODE) {
                    LOG.info("[ES DSL] ⚠️ 触发解析：缓冲区较大 (" + (buffer.length() / 1024) + "K)，尝试解析");
                }
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
        
        lastParseTime = System.currentTimeMillis();
        
        // 获取当前缓冲区内容的快照
        final String bufferedText = buffer.toString();
        
        // 在后台线程异步解析（不阻塞 IDEA）
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                parseAndSave(bufferedText);
            } finally {
                isParsing.set(false);
            }
        });
    }
    
    /**
     * 解析并保存 DSL
     */
    private void parseAndSave(String bufferedText) {
        try {
            if (DEBUG_MODE) {
                LOG.debug("[ES DSL] 🔍 开始解析，文本长度: " + (bufferedText.length() / 1024) + "K");
            }
            
            // 快速检查
            if (!EsDslParser.containsEsDsl(bufferedText)) {
                if (DEBUG_MODE) {
                    LOG.debug("[ES DSL] ⚠️ 不包含 ES DSL 关键词，跳过");
                }
                // ✅ 即使不包含ES DSL，也要清理缓冲区
                clearBufferInUIThread();
                return;
            }
            
            if (DEBUG_MODE) {
                LOG.debug("[ES DSL] 📝 包含 ES DSL 关键词，开始详细解析...");
            }
            
            // 解析 DSL
            EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, project.getName());
            if (record != null) {
                // 保存记录（在后台线程，带去重）
                recordService.addRecord(record);
                
                // 日志输出
                LOG.info("✅ 成功捕获 ES DSL 查询:");
                LOG.info("  ├─ 索引: " + record.getIndex());
                LOG.info("  ├─ 方法: " + record.getMethod());
                LOG.info("  ├─ 端点: " + record.getEndpoint());
                LOG.info("  ├─ 来源: " + record.getSource());
                LOG.info("  ├─ API路径: " + (record.getApiPath() != null ? record.getApiPath() : "N/A"));
                LOG.info("  ├─ 调用类: " + (record.getCallerClass() != null ? record.getCallerClass() : "N/A"));
                LOG.info("  └─ DSL 长度: " + ((record.getDslQuery() != null ? record.getDslQuery().length() : 0) / 1024) + "K");
                
                // ✅ 立即清理缓冲区（在 UI 线程）
                clearBufferInUIThread();
            } else {
                if (DEBUG_MODE) {
                    LOG.warn("[ES DSL] ❌ 解析失败，返回 null (缓冲区: " + (bufferedText.length() / 1024) + "KB)");
                }
                
                // ✅ 解析失败也要清理缓冲区，避免重复解析
                clearBufferInUIThread();
                
                // ✅ 只在超详细模式下输出完整诊断信息
                if (VERBOSE_MODE && bufferedText.contains("TRACE") && bufferedText.contains("RequestLogger")) {
                    LOG.warn("[ES DSL] 调试信息:");
                    LOG.warn("  - 文本长度: " + bufferedText.length());
                    LOG.warn("  - 包含 'TRACE': " + bufferedText.contains("TRACE"));
                    LOG.warn("  - 包含 'RequestLogger': " + bufferedText.contains("RequestLogger"));
                    LOG.warn("  - 包含 'curl': " + bufferedText.contains("curl"));
                    LOG.warn("  - 包含 '-d': " + bufferedText.contains("-d"));
                    LOG.warn("  - 包含 'GET': " + bufferedText.contains("GET"));
                    
                    // 输出前500字符用于诊断
                    LOG.warn("[ES DSL] 前500字符: " + bufferedText.substring(0, Math.min(500, bufferedText.length())));
                }
            }
        } catch (Exception e) {
            LOG.warn("[ES DSL] ❌ 解析异常", e);
            // ✅ 异常时也要清理缓冲区
            clearBufferInUIThread();
        }
    }
    
    /**
     * 在UI线程中清理缓冲区（保留上下文）
     */
    private void clearBufferInUIThread() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (buffer.length() > CROSS_LINE_RETAIN_SIZE) {
                String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                buffer.setLength(0);
                buffer.append(remaining);
                if (DEBUG_MODE) {
                    LOG.debug("[ES DSL] 🧹 已清理缓冲区，保留 " + (remaining.length() / 1024) + "KB 上下文");
                }
            } else {
                // 如果缓冲区不大，完全清空
                buffer.setLength(0);
                if (DEBUG_MODE) {
                    LOG.debug("[ES DSL] 🧹 已完全清空缓冲区");
                }
            }
        });
    }
    
    /**
     * 判断是否应该清理缓冲区
     */
    private boolean shouldCleanBuffer(String text) {
        // 1. 检测到完整的 TRACE 日志响应结束
        if (text.contains(TRACE_END_MARKER_1) || text.contains(TRACE_END_MARKER_2)) {
            return true;
        }
        
        // 2. 检测到多个连续换行（日志段落结束）
        if (text.contains("\n\n")) {
            return true;
        }
        
        // 3. 缓冲区接近上限
        if (buffer.length() > MAX_BUFFER_SIZE * 0.8) {
            return true;
        }
        
        // 注意: 新TRACE日志的清理已经在 onTextAvailable 中处理了
        
        return false;
    }
    
    /**
     * 清理缓冲区
     */
    private void cleanBuffer() {
        if (buffer.length() > CROSS_LINE_RETAIN_SIZE) {
            String remaining = buffer.substring(Math.max(0, buffer.length() - CROSS_LINE_RETAIN_SIZE));
            buffer.setLength(0);
            buffer.append(remaining);
        }
    }
    
}

