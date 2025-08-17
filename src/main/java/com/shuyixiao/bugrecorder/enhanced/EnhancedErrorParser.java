package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.model.StackTraceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版错误解析器
 * 提供更丰富的错误信息解析和上下文捕获
 */
public class EnhancedErrorParser {

    private static final Logger LOG = Logger.getInstance(EnhancedErrorParser.class);
    
    // 扩展的错误模式匹配
    private static final Map<String, Pattern> ERROR_PATTERNS = new ConcurrentHashMap<>();
    
    // 错误上下文收集器
    private final EnhancedContextCaptureService contextService;
    
    // 错误模式缓存
    private final Map<String, ErrorPattern> errorPatternCache = new ConcurrentHashMap<>();
    
    static {
        initializeErrorPatterns();
    }
    
    public EnhancedErrorParser(@NotNull Project project) {
        this.contextService = project.getService(EnhancedContextCaptureService.class);
    }
    
    /**
     * 初始化错误模式
     */
    private static void initializeErrorPatterns() {
        // 基础异常模式
        ERROR_PATTERNS.put("exception", Pattern.compile(
            "(\\w+(?:\\.\\w+)*(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 堆栈跟踪模式
        ERROR_PATTERNS.put("stack_trace", Pattern.compile(
            "\\s*at\\s+(\\w+(?:\\.\\w+)*)\\.([\\w$<>]+)\\(([^)]+)\\)(?:\\s*~?\\[(.+?)\\])?",
            Pattern.MULTILINE
        ));
        
        // Caused by 模式
        ERROR_PATTERNS.put("caused_by", Pattern.compile(
            "Caused by:\\s*(\\w+(?:\\.\\w+)*(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // Spring Boot 错误模式
        ERROR_PATTERNS.put("spring_error", Pattern.compile(
            "org\\.springframework\\.(\\w+)\\..*?(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 数据库错误模式
        ERROR_PATTERNS.put("database_error", Pattern.compile(
            "(com\\.mysql\\.cj\\.jdbc|org\\.postgresql|oracle\\.jdbc|" +
            "com\\.microsoft\\.sqlserver|org\\.hibernate|org\\.springframework\\.orm|" +
            "org\\.springframework\\.data\\.jpa|org\\.springframework\\.jdbc|" +
            "javax\\.persistence|java\\.sql)\\..*?(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // HTTP错误模式
        ERROR_PATTERNS.put("http_error", Pattern.compile(
            "(HTTP\\s+(\\d{3})|Feign|RestTemplate|WebClient|OkHttp|Apache\\s+HttpClient|" +
            "org\\.springframework\\.web\\.client|org\\.springframework\\.cloud\\.openfeign)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // JSON序列化错误模式
        ERROR_PATTERNS.put("json_error", Pattern.compile(
            "(com\\.fasterxml\\.jackson|com\\.google\\.gson|org\\.json|" +
            "javax\\.xml\\.bind|com\\.thoughtworks\\.xstream)\\..*?" +
            "(Json(?:Parse|Mapping)Exception|GsonException|JSONException|JAXBException|XStreamException):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 参数校验错误模式
        ERROR_PATTERNS.put("validation_error", Pattern.compile(
            "(MethodArgumentNotValidException|ConstraintViolationException|" +
            "BindException|ValidationException|org\\.springframework\\.validation)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // Spring Bean/DI错误模式
        ERROR_PATTERNS.put("bean_error", Pattern.compile(
            "(NoSuchBeanDefinitionException|UnsatisfiedDependencyException|" +
            "BeanCreationException|BeanDefinitionStoreException|" +
            "CircularDependencyException|org\\.springframework\\.beans\\.factory)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 超时错误模式
        ERROR_PATTERNS.put("timeout_error", Pattern.compile(
            "(TimeoutException|SocketTimeoutException|ConnectTimeoutException|" +
            "ReadTimeoutException|java\\.util\\.concurrent\\.TimeoutException)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 缓存错误模式
        ERROR_PATTERNS.put("cache_error", Pattern.compile(
            "(redis|ehcache|hazelcast|caffeine|org\\.springframework\\.cache)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 消息队列错误模式
        ERROR_PATTERNS.put("mq_error", Pattern.compile(
            "(kafka|rabbitmq|activemq|rocketmq|org\\.springframework\\.amqp|" +
            "org\\.springframework\\.kafka)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 网络连接错误模式
        ERROR_PATTERNS.put("network_error", Pattern.compile(
            "(ConnectionException|ConnectException|SocketException|" +
            "java\\.net\\.ConnectException|java\\.net\\.SocketException)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 安全相关错误模式
        ERROR_PATTERNS.put("security_error", Pattern.compile(
            "(SecurityException|AuthenticationException|AuthorizationException|" +
            "AccessDeniedException|org\\.springframework\\.security)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 编译错误模式
        ERROR_PATTERNS.put("compilation_error", Pattern.compile(
            "(CompilationException|SyntaxException|ParseException|" +
            "javax\\.tools\\.Diagnostic|com\\.sun\\.tools\\.javac)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 内存相关错误模式
        ERROR_PATTERNS.put("memory_error", Pattern.compile(
            "(OutOfMemoryError|StackOverflowError|" +
            "java\\.lang\\.OutOfMemoryError|java\\.lang\\.StackOverflowError)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 文件IO错误模式
        ERROR_PATTERNS.put("file_io_error", Pattern.compile(
            "(FileNotFoundException|IOException|FileSystemException|" +
            "java\\.io\\.FileNotFoundException|java\\.io\\.IOException)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
        
        // 并发相关错误模式
        ERROR_PATTERNS.put("concurrency_error", Pattern.compile(
            "(ConcurrentModificationException|IllegalStateException|" +
            "java\\.util\\.ConcurrentModificationException|java\\.lang\\.IllegalStateException)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
        ));
    }
    
    /**
     * 解析错误文本，生成增强的Bug记录
     */
    public BugRecord parseEnhancedError(@NotNull String errorText, @NotNull Project project) {
        try {
            // 创建错误模式对象
            ErrorPattern pattern = analyzeErrorPattern(errorText);
            
            // 构建Bug记录
            BugRecord.Builder builder = new BugRecord.Builder()
                .project(project.getName())
                .timestamp(LocalDateTime.now())
                .rawText(errorText)
                .summary(generateSummary(errorText, pattern))
                .resolved(false)
                .status(com.shuyixiao.bugrecorder.model.BugStatus.PENDING);
            
            // 解析异常信息
            parseExceptionInfo(errorText, builder, pattern);
            
            // 解析堆栈跟踪
            List<StackTraceElement> stackTrace = parseStackTrace(errorText);
            builder.stackTrace(stackTrace);
            
            // 解析原因链
            List<String> causeChain = parseCauseChain(errorText);
            builder.causeChain(causeChain);
            
            // 确定顶层堆栈帧
            StackTraceElement topFrame = determineTopFrame(stackTrace);
            builder.topFrame(topFrame);
            
            // 确定根本原因
            String rootCause = determineRootCause(errorText, causeChain);
            builder.rootCause(rootCause);
            
            // 生成指纹
            String fingerprint = generateFingerprint(errorText, pattern);
            builder.fingerprint(fingerprint);
            
            // 设置发生次数
            builder.occurrenceCount(1);
            
            // 添加增强的上下文信息
            addEnhancedContext(builder, pattern, errorText);
            
            return builder.build();
            
        } catch (Exception e) {
            LOG.warn("Failed to parse enhanced error", e);
            return null;
        }
    }
    
    /**
     * 分析错误模式
     */
    private ErrorPattern analyzeErrorPattern(String errorText) {
        ErrorPattern pattern = new ErrorPattern();
        
        // 检查各种错误类型
        for (Map.Entry<String, Pattern> entry : ERROR_PATTERNS.entrySet()) {
            String patternType = entry.getKey();
            Pattern regex = entry.getValue();
            
            Matcher matcher = regex.matcher(errorText);
            if (matcher.find()) {
                pattern.addPattern(patternType, matcher.group(0));
                pattern.setPrimaryType(patternType);
            }
        }
        
        // 分析错误严重程度
        pattern.setSeverity(analyzeSeverity(errorText));
        
        // 分析错误类别
        pattern.setCategory(analyzeCategory(errorText));
        
        return pattern;
    }
    
    /**
     * 分析错误严重程度
     */
    private String analyzeSeverity(String errorText) {
        String lowerText = errorText.toLowerCase();
        
        if (lowerText.contains("fatal") || lowerText.contains("outofmemory") || 
            lowerText.contains("stackoverflow") || lowerText.contains("critical")) {
            return "CRITICAL";
        } else if (lowerText.contains("error") || lowerText.contains("exception")) {
            return "ERROR";
        } else if (lowerText.contains("warning") || lowerText.contains("warn")) {
            return "WARNING";
        } else if (lowerText.contains("info") || lowerText.contains("debug")) {
            return "INFO";
        } else {
            return "UNKNOWN";
        }
    }
    
    /**
     * 分析错误类别
     */
    private String analyzeCategory(String errorText) {
        String lowerText = errorText.toLowerCase();
        
        if (lowerText.contains("database") || lowerText.contains("jdbc") || 
            lowerText.contains("hibernate") || lowerText.contains("sql")) {
            return "DATABASE";
        } else if (lowerText.contains("http") || lowerText.contains("web") || 
                   lowerText.contains("rest") || lowerText.contains("feign")) {
            return "HTTP";
        } else if (lowerText.contains("spring") || lowerText.contains("bean") || 
                   lowerText.contains("dependency")) {
            return "SPRING";
        } else if (lowerText.contains("validation") || lowerText.contains("constraint")) {
            return "VALIDATION";
        } else if (lowerText.contains("timeout") || lowerText.contains("connection")) {
            return "NETWORK";
        } else if (lowerText.contains("memory") || lowerText.contains("heap")) {
            return "RESOURCE";
        } else {
            return "GENERAL";
        }
    }
    
    /**
     * 生成错误摘要
     */
    private String generateSummary(String errorText, ErrorPattern pattern) {
        try {
            // 提取第一行作为基础摘要
            String[] lines = errorText.split("\n");
            String firstLine = lines[0].trim();
            
            // 如果第一行太长，截断它
            if (firstLine.length() > 200) {
                firstLine = firstLine.substring(0, 200) + "...";
            }
            
            // 添加模式信息
            StringBuilder summary = new StringBuilder();
            summary.append("[").append(pattern.getPrimaryType().toUpperCase()).append("] ");
            summary.append(firstLine);
            
            // 添加严重程度
            if (pattern.getSeverity() != null) {
                summary.append(" (Severity: ").append(pattern.getSeverity()).append(")");
            }
            
            return summary.toString();
            
        } catch (Exception e) {
            return "Error parsing failed: " + e.getMessage();
        }
    }
    
    /**
     * 解析异常信息
     */
    private void parseExceptionInfo(String errorText, BugRecord.Builder builder, ErrorPattern pattern) {
        try {
            // 根据主要错误类型进行解析
            String primaryType = pattern.getPrimaryType();
            
            if (primaryType != null) {
                Pattern regex = ERROR_PATTERNS.get(primaryType);
                if (regex != null) {
                    Matcher matcher = regex.matcher(errorText);
                    if (matcher.find()) {
                        if (matcher.groupCount() >= 2) {
                            builder.exceptionClass(matcher.group(1));
                            builder.errorMessage(matcher.group(2).trim());
                        }
                    }
                }
            }
            
            // 设置错误类型
            ErrorType errorType = mapPatternToErrorType(primaryType);
            if (errorType != null) {
                builder.errorType(errorType);
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to parse exception info", e);
        }
    }
    
    /**
     * 将模式类型映射到ErrorType
     */
    private ErrorType mapPatternToErrorType(String patternType) {
        if (patternType == null) return ErrorType.UNKNOWN;
        
        switch (patternType) {
            case "database_error": return ErrorType.DATABASE;
            case "http_error": return ErrorType.HTTP_CLIENT;
            case "spring_error": return ErrorType.SPRING_FRAMEWORK;
            case "validation_error": return ErrorType.VALIDATION;
            case "bean_error": return ErrorType.BEAN;
            case "timeout_error": return ErrorType.TIMEOUT;
            case "cache_error": return ErrorType.CACHE;
            case "mq_error": return ErrorType.MQ;
            case "json_error": return ErrorType.SERIALIZATION;
            case "network_error": return ErrorType.NETWORK;
            case "security_error": return ErrorType.SECURITY;
            case "compilation_error": return ErrorType.COMPILATION;
            case "memory_error": return ErrorType.MEMORY;
            case "file_io_error": return ErrorType.IO;
            case "concurrency_error": return ErrorType.CONCURRENCY;
            default: return ErrorType.UNKNOWN;
        }
    }
    
    /**
     * 解析堆栈跟踪
     */
    private List<StackTraceElement> parseStackTrace(String errorText) {
        List<StackTraceElement> stackTrace = new ArrayList<>();
        
        try {
            Pattern pattern = ERROR_PATTERNS.get("stack_trace");
            Matcher matcher = pattern.matcher(errorText);
            
            while (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                String location = matcher.group(3);
                String jarFile = matcher.group(4);
                
                StackTraceElement element = new StackTraceElement(
                    className, methodName, location, jarFile
                );
                stackTrace.add(element);
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse stack trace", e);
        }
        
        return stackTrace;
    }
    
    /**
     * 解析原因链
     */
    private List<String> parseCauseChain(String errorText) {
        List<String> causeChain = new ArrayList<>();
        
        try {
            Pattern pattern = ERROR_PATTERNS.get("caused_by");
            Matcher matcher = pattern.matcher(errorText);
            
            while (matcher.find()) {
                String cause = matcher.group(1) + ": " + matcher.group(2).trim();
                causeChain.add(cause);
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse cause chain", e);
        }
        
        return causeChain;
    }
    
    /**
     * 确定顶层堆栈帧
     */
    private StackTraceElement determineTopFrame(List<StackTraceElement> stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return null;
        }
        
        // 查找第一个用户代码的堆栈帧
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (!className.startsWith("java.") && 
                !className.startsWith("sun.") && 
                !className.startsWith("com.intellij.") &&
                !className.startsWith("org.jetbrains.")) {
                return element;
            }
        }
        
        return stackTrace.get(0);
    }
    
    /**
     * 确定根本原因
     */
    private String determineRootCause(String errorText, List<String> causeChain) {
        if (causeChain != null && !causeChain.isEmpty()) {
            return causeChain.get(causeChain.size() - 1);
        }
        
        // 尝试从异常信息中提取
        Pattern pattern = ERROR_PATTERNS.get("exception");
        Matcher matcher = pattern.matcher(errorText);
        if (matcher.find()) {
            return matcher.group(1) + ": " + matcher.group(2).trim();
        }
        
        return null;
    }
    
    /**
     * 生成错误指纹
     */
    private String generateFingerprint(String errorText, ErrorPattern pattern) {
        try {
            String fingerprintData = pattern.getPrimaryType() + ":" + 
                                   pattern.getSeverity() + ":" + 
                                   pattern.getCategory();
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(fingerprintData.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("MD5 algorithm not available", e);
            return "fingerprint-" + System.currentTimeMillis();
        }
    }
    
    /**
     * 添加增强的上下文信息
     */
    private void addEnhancedContext(BugRecord.Builder builder, ErrorPattern pattern, String errorText) {
        try {
            // 获取系统上下文
            Map<String, Object> systemContext = contextService.getSystemContext();
            
            // 获取项目上下文
            Map<String, Object> projectContext = contextService.getProjectContext();
            
            // 获取运行时上下文
            Map<String, Object> runtimeContext = contextService.getRuntimeContext();
            
            // 创建上下文摘要
            Map<String, Object> contextSummary = new HashMap<>();
            contextSummary.put("system", createContextSummary(systemContext));
            contextSummary.put("project", createContextSummary(projectContext));
            contextSummary.put("runtime", createContextSummary(runtimeContext));
            contextSummary.put("error_pattern", pattern.toMap());
            
            // 将上下文信息添加到AI分析字段（临时存储）
            builder.aiAnalysis("Context: " + contextSummary.toString());
            
        } catch (Exception e) {
            LOG.warn("Failed to add enhanced context", e);
        }
    }
    
    /**
     * 创建上下文摘要
     */
    private Map<String, Object> createContextSummary(Map<String, Object> context) {
        Map<String, Object> summary = new HashMap<>();
        
        // 只保留关键信息，避免数据过大
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 过滤掉敏感信息和大对象
            if (!key.contains("password") && !key.contains("secret") && 
                !key.contains("token") && !key.contains("key") &&
                value != null && value.toString().length() < 1000) {
                summary.put(key, value);
            }
        }
        
        return summary;
    }
    
    /**
     * 错误模式内部类
     */
    private static class ErrorPattern {
        private String primaryType;
        private String severity;
        private String category;
        private final Map<String, String> patterns = new HashMap<>();
        
        public void addPattern(String type, String pattern) {
            patterns.put(type, pattern);
        }
        
        public void setPrimaryType(String primaryType) {
            this.primaryType = primaryType;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getPrimaryType() {
            return primaryType;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public String getCategory() {
            return category;
        }
        
        public Map<String, String> getPatterns() {
            return patterns;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("primaryType", primaryType);
            map.put("severity", severity);
            map.put("category", category);
            map.put("patterns", patterns);
            return map;
        }
    }
} 