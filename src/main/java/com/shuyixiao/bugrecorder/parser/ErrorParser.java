package com.shuyixiao.bugrecorder.parser;

import com.intellij.openapi.project.Project;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.model.StackTraceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

            // 确定错误类型
            ErrorType errorType = determineErrorType(errorText, stackTrace);
            builder.errorType(errorType);

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
        // 首先尝试匹配数据库错误（更具体，优先级最高）
        Matcher matcher = DB_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim());
            return;
        }

        // 尝试匹配Spring Boot错误
        matcher = SPRING_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim());
            return;
        }

        // 尝试匹配Caused by模式
        matcher = CAUSED_BY_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(1))
                    .errorMessage(matcher.group(2).trim());
            return;
        }

        // 最后尝试匹配通用异常
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
     * 确定错误类型
     */
    private ErrorType determineErrorType(String errorText, List<StackTraceElement> stackTrace) {
        // 首先基于堆栈跟踪进行分类（更准确）
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName().toLowerCase();
            if (className.contains("jdbc") || className.contains("hibernate") || 
                className.contains("jpa") || className.contains("sql") ||
                className.contains("mysql") || className.contains("postgresql") ||
                className.contains("oracle") || className.contains("sqlserver")) {
                return ErrorType.DATABASE;
            }
        }

        String lowerText = errorText.toLowerCase();

        // 数据库相关错误 - 优先检查数据库特定的关键词
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
            lowerText.contains("timeout") || lowerText.contains("socket") || 
            lowerText.contains("http") || lowerText.contains("tcp")) {
            return ErrorType.NETWORK;
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

        return ErrorType.UNKNOWN;
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