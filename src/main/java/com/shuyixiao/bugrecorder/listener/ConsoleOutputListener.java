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

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    // 使用线程安全的队列来缓存输出文本，避免阻塞控制台
    private final ConcurrentLinkedQueue<String> outputBuffer = new ConcurrentLinkedQueue<>();
    private final StringBuilder currentErrorBuffer = new StringBuilder();

    // 用于识别错误输出的关键词
    private static final String[] ERROR_INDICATORS = {
            "Exception", "Error", "Caused by", "at ", "java.lang",
            "org.springframework", "com.mysql", "org.hibernate"
    };

    public ConsoleOutputListener(Project project) {
        this.project = project;
        this.errorParser = new ErrorParser();
        this.bugRecordService = project.getService(BugRecordService.class);
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        String text = event.getText();

        // 只处理标准错误输出和包含错误关键词的标准输出
        if (ProcessOutputTypes.STDERR.equals(outputType) || containsErrorIndicator(text)) {
            // 异步处理文本，避免阻塞控制台输出
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                processErrorText(text);
            });
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
            currentErrorBuffer.append(text);

            // 检查是否是一个完整的错误信息
            if (isCompleteError(currentErrorBuffer.toString())) {
                String completeError = currentErrorBuffer.toString();
                currentErrorBuffer.setLength(0); // 清空缓冲区

                // 解析错误并创建Bug记录
                BugRecord bugRecord = errorParser.parseError(completeError, project);
                if (bugRecord != null) {
                    bugRecordService.saveBugRecord(bugRecord);
                    LOG.info("Bug record created: " + bugRecord.getErrorType());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to process error text", e);
        }
    }

    /**
     * 判断是否是一个完整的错误信息
     * 简单实现：如果文本以异常结尾或者包含完整的堆栈跟踪
     */
    private boolean isCompleteError(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 如果文本包含多行且最后一行不是以"at "开头，可能是完整的错误
        String[] lines = text.split("\n");
        if (lines.length > 1) {
            String lastLine = lines[lines.length - 1].trim();
            // 如果最后一行不是堆栈跟踪行，认为错误完整
            return !lastLine.startsWith("at ") && !lastLine.startsWith("Caused by");
        }

        return false;
    }

    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        LOG.debug("Process started, beginning console monitoring");
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        LOG.debug("Process terminated, stopping console monitoring");
        // 处理缓冲区中剩余的文本
        if (currentErrorBuffer.length() > 0) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                processErrorText("\n"); // 添加换行符触发最终处理
            });
        }
    }
}