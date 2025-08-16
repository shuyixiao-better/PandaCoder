package com.shuyixiao.bugrecorder.parser;

import com.intellij.openapi.project.Project;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.model.StackTraceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能错误解析器
 * 负责分析捕获的控制台文本，识别并提取结构化的错误信息
 */
public class ErrorParser {

    // 常见异常类型的正则表达式
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "(\\w+(?:\\.\\w+)*(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 堆栈跟踪行的正则表达式
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
            "\\s*at\\s+(\\w+(?:\\.\\w+)*)\\.([\\w$<>]+)\\(([^)]+)\\)(?:\\s*~?\\[(.+?)\\])?",
            Pattern.MULTILINE
    );

    // Caused by 模式
    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile(
            "Caused by:\\s*(\\w+(?:\\.\\w+)*(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // Spring Boot 错误模式
    private static final Pattern SPRING_ERROR_PATTERN = Pattern.compile(
            "org\\.springframework\\.(\\w+)\\..*?(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 数据库连接错误模式 - 扩展支持的数据库类型
    private static final Pattern DB_ERROR_PATTERN = Pattern.compile(
            "(com\\.mysql\\.cj\\.jdbc|org\\.postgresql|oracle\\.jdbc|" +
            "com\\.microsoft\\.sqlserver|org\\.hibernate|org\\.springframework\\.orm|" +
            "org\\.springframework\\.data\\.jpa|org\\.springframework\\.jdbc|" +
            "javax\\.persistence|java\\.sql)\\..*?(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：HTTP相关错误模式
    private static final Pattern HTTP_ERROR_PATTERN = Pattern.compile(
            "(HTTP\\s+(\\d{3})|Feign|RestTemplate|WebClient|OkHttp|Apache\\s+HttpClient|" +
            "org\\.springframework\\.web\\.client|org\\.springframework\\.cloud\\.openfeign)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：JSON序列化错误模式
    private static final Pattern JSON_ERROR_PATTERN = Pattern.compile(
            "(com\\.fasterxml\\.jackson|com\\.google\\.gson|org\\.json|" +
            "javax\\.xml\\.bind|com\\.thoughtworks\\.xstream)\\..*?" +
            "(Json(?:Parse|Mapping)Exception|GsonException|JSONException|JAXBException|XStreamException):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：参数校验错误模式
    private static final Pattern VALIDATION_ERROR_PATTERN = Pattern.compile(
            "(MethodArgumentNotValidException|ConstraintViolationException|" +
            "BindException|ValidationException|org\\.springframework\\.validation)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：Spring Bean/DI错误模式
    private static final Pattern BEAN_ERROR_PATTERN = Pattern.compile(
            "(NoSuchBeanDefinitionException|UnsatisfiedDependencyException|" +
            "BeanCreationException|BeanDefinitionStoreException|" +
            "CircularDependencyException|org\\.springframework\\.beans\\.factory)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：超时错误模式
    private static final Pattern TIMEOUT_ERROR_PATTERN = Pattern.compile(
            "(TimeoutException|SocketTimeoutException|ConnectTimeoutException|" +
            "ReadTimeoutException|java\\.util\\.concurrent\\.TimeoutException)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：缓存错误模式
    private static final Pattern CACHE_ERROR_PATTERN = Pattern.compile(
            "(redis|ehcache|hazelcast|caffeine|guava|org\\.springframework\\.cache)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    // 新增：消息队列错误模式
    private static final Pattern MQ_ERROR_PATTERN = Pattern.compile(
            "(kafka|rabbitmq|activemq|rocketmq|pulsar|" +
            "org\\.springframework\\.amqp|org\\.apache\\.kafka)\\..*?" +
            "(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    /**
     * 解析错误文本并创建Bug记录
     */
    @Nullable
    public BugRecord parseError(@NotNull String errorText, @NotNull Project project) {
        if (errorText == null || errorText.trim().isEmpty()) {
            return null;
        }

        try {
            BugRecord.Builder builder = new BugRecord.Builder()
                    .project(project.getName())
                    .timestamp(LocalDateTime.now())
                    .rawText(errorText.trim());

            // 解析异常类型和消息
            parseExceptionInfo(errorText, builder);

            // 解析堆栈跟踪
            List<StackTraceElement> stackTrace = parseStackTrace(errorText);
            builder.stackTrace(stackTrace);

            // 解析原因链
            List<String> causeChain = parseCauseChain(errorText);
            builder.causeChain(causeChain);

            // 确定顶层堆栈帧
            StackTraceElement topFrame = determineTopFrame(stackTrace);
            builder.topFrame(topFrame);

            // 确定错误类型
            ErrorType errorType = determineErrorType(errorText, stackTrace);
            builder.errorType(errorType);

            // 确定根本原因
            String rootCause = determineRootCause(errorText, causeChain);
            builder.rootCause(rootCause);

            // 生成指纹
            String fingerprint = generateFingerprint(builder.build());
            builder.fingerprint(fingerprint);

            // 提取关键信息
            String summary = generateSummary(builder.build());
            builder.summary(summary);

            return builder.build();

        } catch (Exception e) {
            // 如果解析失败，创建一个基本的Bug记录
            return new BugRecord.Builder()
                    .project(project.getName())
                    .timestamp(LocalDateTime.now())
                    .rawText(errorText.trim())
                    .errorType(ErrorType.UNKNOWN)
                    .summary("解析失败的错误信息")
                    .build();
        }
    }

    /**
     * 解析异常信息
     */
    private void parseExceptionInfo(String errorText, BugRecord.Builder builder) {
        // 按优先级顺序匹配，更具体的模式优先

        // 1. HTTP相关错误（优先级最高）
        Matcher matcher = HTTP_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            String httpStatus = matcher.group(2);
            String exceptionClass = matcher.group(3);
            String errorMessage = matcher.group(4);
            
            builder.exceptionClass(exceptionClass)
                    .errorMessage(errorMessage.trim());
            
            // 如果是5xx错误，标记为HTTP_SERVER，否则为HTTP_CLIENT
            if (httpStatus != null && httpStatus.startsWith("5")) {
                builder.errorType(ErrorType.HTTP_SERVER);
            } else {
                builder.errorType(ErrorType.HTTP_CLIENT);
            }
            return;
        }

        // 2. JSON序列化错误
        matcher = JSON_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim())
                    .errorType(ErrorType.SERIALIZATION);
            return;
        }

        // 3. 参数校验错误
        matcher = VALIDATION_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim())
                    .errorType(ErrorType.VALIDATION);
            return;
        }

        // 4. Spring Bean/DI错误
        matcher = BEAN_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim())
                    .errorType(ErrorType.BEAN);
            return;
        }

        // 5. 超时错误
        matcher = TIMEOUT_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim())
                    .errorType(ErrorType.TIMEOUT);
            return;
        }

        // 6. 缓存错误
        matcher = CACHE_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim())
                    .errorType(ErrorType.CACHE);
            return;
        }

        // 7. 消息队列错误
        matcher = MQ_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim())
                    .errorType(ErrorType.MQ);
            return;
        }

        // 8. 数据库错误
        matcher = DB_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim());
            return;
        }

        // 9. Spring Boot错误
        matcher = SPRING_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim());
            return;
        }

        // 10. Caused by模式
        matcher = CAUSED_BY_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(1))
                    .errorMessage(matcher.group(2).trim());
            return;
        }

        // 11. 通用异常
        matcher = EXCEPTION_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(1))
                    .errorMessage(matcher.group(2).trim());
            return;
        }

        // 如果都没匹配到，尝试提取第一行作为错误信息
        String[] lines = errorText.split("\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            if (!firstLine.isEmpty()) {
                builder.errorMessage(firstLine);
            }
        }
    }

    /**
     * 解析堆栈跟踪
     */
    private List<StackTraceElement> parseStackTrace(String errorText) {
        List<StackTraceElement> stackTrace = new ArrayList<>();

        Matcher matcher = STACK_TRACE_PATTERN.matcher(errorText);
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

        return stackTrace;
    }

    /**
     * 解析原因链
     */
    private List<String> parseCauseChain(String errorText) {
        List<String> causeChain = new ArrayList<>();
        
        // 匹配所有的Caused by行
        Matcher matcher = CAUSED_BY_PATTERN.matcher(errorText);
        while (matcher.find()) {
            String cause = matcher.group(1) + ": " + matcher.group(2).trim();
            causeChain.add(cause);
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
        
        // 查找第一个用户代码的堆栈帧（非java.*、非sun.*、非com.intellij.*等）
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (!className.startsWith("java.") && 
                !className.startsWith("sun.") && 
                !className.startsWith("com.intellij.") &&
                !className.startsWith("org.jetbrains.")) {
                return element;
            }
        }
        
        // 如果没找到用户代码，返回第一个
        return stackTrace.get(0);
    }

    /**
     * 确定根本原因
     */
    private String determineRootCause(String errorText, List<String> causeChain) {
        if (causeChain != null && !causeChain.isEmpty()) {
            // 返回最后一个Caused by，这通常是根本原因
            return causeChain.get(causeChain.size() - 1);
        }
        
        // 如果没有Caused by链，尝试从异常信息中提取
        Matcher matcher = EXCEPTION_PATTERN.matcher(errorText);
        if (matcher.find()) {
            return matcher.group(1) + ": " + matcher.group(2).trim();
        }
        
        return null;
    }

    /**
     * 确定错误类型
     */
    private ErrorType determineErrorType(String errorText, List<StackTraceElement> stackTrace) {
        // 如果已经在parseExceptionInfo中设置了类型，直接返回
        if (errorText.toLowerCase().contains("http 5")) {
            return ErrorType.HTTP_SERVER;
        }
        
        // 首先基于堆栈跟踪进行分类（更准确）
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName().toLowerCase();
            
            // 数据库相关
            if (className.contains("jdbc") || className.contains("hibernate") || 
                className.contains("jpa") || className.contains("sql") ||
                className.contains("mysql") || className.contains("postgresql") ||
                className.contains("oracle") || className.contains("sqlserver")) {
                return ErrorType.DATABASE;
            }
            
            // HTTP相关
            if (className.contains("web.client") || className.contains("openfeign") ||
                className.contains("resttemplate") || className.contains("webclient") ||
                className.contains("okhttp") || className.contains("httpclient")) {
                return ErrorType.HTTP_CLIENT;
            }
            
            // 缓存相关
            if (className.contains("redis") || className.contains("ehcache") ||
                className.contains("hazelcast") || className.contains("caffeine")) {
                return ErrorType.CACHE;
            }
            
            // 消息队列相关
            if (className.contains("kafka") || className.contains("rabbitmq") ||
                className.contains("activemq") || className.contains("rocketmq")) {
                return ErrorType.MQ;
            }
        }

        String lowerText = errorText.toLowerCase();

        // HTTP相关错误
        if (lowerText.contains("http") || lowerText.contains("feign") ||
            lowerText.contains("resttemplate") || lowerText.contains("webclient")) {
            if (lowerText.contains("5") || lowerText.contains("500") || lowerText.contains("502") ||
                lowerText.contains("503") || lowerText.contains("504")) {
                return ErrorType.HTTP_SERVER;
            }
            return ErrorType.HTTP_CLIENT;
        }

        // JSON序列化错误
        if (lowerText.contains("jackson") || lowerText.contains("gson") ||
            lowerText.contains("json") || lowerText.contains("jaxb")) {
            return ErrorType.SERIALIZATION;
        }

        // 参数校验错误
        if (lowerText.contains("validation") || lowerText.contains("constraint") ||
            lowerText.contains("argument") || lowerText.contains("bind")) {
            return ErrorType.VALIDATION;
        }

        // Spring Bean/DI错误
        if (lowerText.contains("bean") || lowerText.contains("dependency") ||
            lowerText.contains("circular") || lowerText.contains("creation")) {
            return ErrorType.BEAN;
        }

        // 超时错误
        if (lowerText.contains("timeout") || lowerText.contains("timed out")) {
            return ErrorType.TIMEOUT;
        }

        // 缓存错误
        if (lowerText.contains("cache") || lowerText.contains("redis")) {
            return ErrorType.CACHE;
        }

        // 消息队列错误
        if (lowerText.contains("kafka") || lowerText.contains("rabbit") ||
            lowerText.contains("queue") || lowerText.contains("message")) {
            return ErrorType.MQ;
        }

        // 数据库相关错误
        if (lowerText.contains("sql") || lowerText.contains("database") ||
                lowerText.contains("mysql") || lowerText.contains("postgresql") ||
                lowerText.contains("oracle") || lowerText.contains("jdbc") ||
                lowerText.contains("hibernate") || lowerText.contains("jpa") ||
                lowerText.contains("persistence")) {
            return ErrorType.DATABASE;
        }

        // 网络相关错误 - 排除数据库连接，只处理纯网络连接
        if ((lowerText.contains("connection") && !lowerText.contains("database") && 
             !lowerText.contains("jdbc") && !lowerText.contains("sql") &&
             !lowerText.contains("mysql") && !lowerText.contains("postgresql") &&
             !lowerText.contains("oracle")) ||
            lowerText.contains("socket") || lowerText.contains("tcp")) {
            return ErrorType.CONNECTIVITY;
        }

        // Spring框架错误
        if (lowerText.contains("springframework") || lowerText.contains("spring boot")) {
            return ErrorType.SPRING_FRAMEWORK;
        }

        // 编译错误
        if (lowerText.contains("compilation") || lowerText.contains("syntax")) {
            return ErrorType.COMPILATION;
        }

        // 运行时错误
        if (lowerText.contains("runtimeexception") || lowerText.contains("nullpointer")) {
            return ErrorType.RUNTIME;
        }

        // 配置错误
        if (lowerText.contains("configuration") || lowerText.contains("properties")) {
            return ErrorType.CONFIGURATION;
        }

        // 内存相关错误
        if (lowerText.contains("outofmemory") || lowerText.contains("heap")) {
            return ErrorType.MEMORY;
        }

        // IO操作错误
        if (lowerText.contains("ioexception") || lowerText.contains("filenotfound") ||
            lowerText.contains("accessdenied")) {
            return ErrorType.IO;
        }

        // 安全相关错误
        if (lowerText.contains("security") || lowerText.contains("authentication") ||
            lowerText.contains("authorization")) {
            return ErrorType.SECURITY;
        }

        // 第三方库错误
        if (lowerText.contains("apache") || lowerText.contains("google") ||
            lowerText.contains("netty") || lowerText.contains("logback") ||
            lowerText.contains("slf4j")) {
            return ErrorType.THIRD_PARTY;
        }

        return ErrorType.UNKNOWN;
    }

    /**
     * 生成错误指纹
     */
    private String generateFingerprint(BugRecord bugRecord) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // 组合关键信息生成指纹
            if (bugRecord.getExceptionClass() != null) {
                sb.append(bugRecord.getExceptionClass());
            }
            
            if (bugRecord.getTopFrame() != null) {
                sb.append(":").append(bugRecord.getTopFrame().getClassName())
                  .append(".").append(bugRecord.getTopFrame().getMethodName());
            }
            
            // 使用归一化的错误消息（去掉数字、ID等变化信息）
            if (bugRecord.getErrorMessage() != null) {
                String normalizedMessage = normalizeMessage(bugRecord.getErrorMessage());
                sb.append(":").append(normalizedMessage);
            }
            
            // 生成SHA-1哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
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
            // 如果SHA-1不可用，使用简单的哈希
            return String.valueOf(sb.toString().hashCode());
        }
    }

    /**
     * 归一化错误消息，去掉数字、ID、时间戳等变化信息
     */
    private String normalizeMessage(String message) {
        if (message == null) return "";
        
        // 去掉常见的数字模式
        String normalized = message
                .replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "VERSION") // 版本号
                .replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\b", "DATE") // 日期
                .replaceAll("\\b\\d{2}:\\d{2}:\\d{2}\\b", "TIME") // 时间
                .replaceAll("\\b\\d{1,5}\\b", "NUM") // 端口号、行号等
                .replaceAll("\\b[0-9a-f]{8,}\\b", "UUID") // UUID
                .replaceAll("\\b\\d+\\.\\d+\\b", "DECIMAL"); // 小数
        
        return normalized;
    }

    /**
     * 生成错误摘要
     */
    private String generateSummary(BugRecord bugRecord) {
        StringBuilder summary = new StringBuilder();

        if (bugRecord.getExceptionClass() != null) {
            summary.append(bugRecord.getExceptionClass());
        }

        if (bugRecord.getErrorMessage() != null) {
            if (summary.length() > 0) {
                summary.append(": ");
            }
            String message = bugRecord.getErrorMessage();
            // 限制摘要长度
            if (message.length() > 100) {
                message = message.substring(0, 97) + "...";
            }
            summary.append(message);
        }

        if (summary.length() == 0) {
            summary.append("未知错误");
        }

        return summary.toString();
    }

    /**
     * 检查是否为数据库相关异常
     */
    private boolean isDatabaseException(String exceptionClass) {
        if (exceptionClass == null) return false;
        
        String lowerClass = exceptionClass.toLowerCase();
        return lowerClass.contains("sql") || lowerClass.contains("jdbc") ||
               lowerClass.contains("hibernate") || lowerClass.contains("jpa") ||
               lowerClass.contains("persistence") || lowerClass.contains("mysql") ||
               lowerClass.contains("postgresql") || lowerClass.contains("oracle") ||
               lowerClass.contains("sqlserver");
    }

    /**
     * 检查是否为数据库连接相关异常
     */
    private boolean isDatabaseConnectionException(String exceptionClass, String errorMessage) {
        if (!isDatabaseException(exceptionClass)) return false;
        
        if (errorMessage == null) return false;
        String lowerMessage = errorMessage.toLowerCase();
        
        return lowerMessage.contains("connection") || lowerMessage.contains("connect") ||
               lowerMessage.contains("timeout") || lowerMessage.contains("refused") ||
               lowerMessage.contains("authentication") || lowerMessage.contains("access denied");
    }

    /**
     * 获取数据库类型
     */
    private String getDatabaseType(String exceptionClass) {
        if (exceptionClass == null) return "Unknown";
        
        String lowerClass = exceptionClass.toLowerCase();
        if (lowerClass.contains("mysql")) return "MySQL";
        if (lowerClass.contains("postgresql") || lowerClass.contains("postgres")) return "PostgreSQL";
        if (lowerClass.contains("oracle")) return "Oracle";
        if (lowerClass.contains("sqlserver") || lowerClass.contains("mssql")) return "SQL Server";
        if (lowerClass.contains("h2")) return "H2";
        if (lowerClass.contains("hsqldb")) return "HSQLDB";
        if (lowerClass.contains("sqlite")) return "SQLite";
        
        return "Unknown";
    }
}