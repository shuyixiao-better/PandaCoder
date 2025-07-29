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

    // 数据库连接错误模式
    private static final Pattern DB_ERROR_PATTERN = Pattern.compile(
            "(com\\.mysql\\.cj\\.jdbc|org\\.postgresql|oracle\\.jdbc)\\..*?(\\w+(?:Exception|Error)):\\s*(.+?)(?=\n|$)",
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
        // 首先尝试匹配主异常
        Matcher matcher = EXCEPTION_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(1))
                    .errorMessage(matcher.group(2).trim());
            return;
        }

        // 尝试匹配Spring Boot错误
        matcher = SPRING_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim());
            return;
        }

        // 尝试匹配数据库错误
        matcher = DB_ERROR_PATTERN.matcher(errorText);
        if (matcher.find()) {
            builder.exceptionClass(matcher.group(2))
                    .errorMessage(matcher.group(3).trim());
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
        String lowerText = errorText.toLowerCase();

        // 数据库相关错误
        if (lowerText.contains("sql") || lowerText.contains("database") ||
                lowerText.contains("connection") || lowerText.contains("mysql") ||
                lowerText.contains("postgresql") || lowerText.contains("oracle")) {
            return ErrorType.DATABASE;
        }

        // 网络相关错误
        if (lowerText.contains("connection") || lowerText.contains("timeout") ||
                lowerText.contains("socket") || lowerText.contains("http")) {
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
}