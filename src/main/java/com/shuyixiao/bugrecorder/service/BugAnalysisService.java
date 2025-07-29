package com.shuyixiao.bugrecorder.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.BaiduAPI;
import com.shuyixiao.DomesticAITranslationAPI;
import com.shuyixiao.GoogleCloudTranslationAPI;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * AI辅助分析服务
 * 利用PandaCoder已有的多引擎翻译和大模型能力，对捕获的Bug进行智能分析并提供解决方案建议
 */
@Service
public final class BugAnalysisService {

    private static final Logger LOG = Logger.getInstance(BugAnalysisService.class);

    private final Project project;
    private final DomesticAITranslationAPI domesticAI;
    private final GoogleCloudTranslationAPI googleTranslate;
    private final BaiduAPI baiduTranslate;
    private final BugRecordService bugRecordService;

    public BugAnalysisService(@NotNull Project project) {
        this.project = project;
        this.domesticAI = new DomesticAITranslationAPI();
        this.googleTranslate = new GoogleCloudTranslationAPI();
        this.baiduTranslate = new BaiduAPI();
        this.bugRecordService = project.getService(BugRecordService.class);
    }

    /**
     * 异步分析Bug并提供解决方案
     */
    public CompletableFuture<AnalysisResult> analyzeBugAsync(@NotNull BugRecord bugRecord) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeBug(bugRecord);
            } catch (Exception e) {
                LOG.error("Failed to analyze bug: " + bugRecord.getId(), e);
                return new AnalysisResult(
                        "分析失败：" + e.getMessage(),
                        "无法提供解决方案建议，请手动排查问题。",
                        0.0
                );
            }
        });
    }

    /**
     * 分析Bug并生成分析结果
     */
    @NotNull
    public AnalysisResult analyzeBug(@NotNull BugRecord bugRecord) {
        LOG.info("Starting AI analysis for bug: " + bugRecord.getId());

        try {
            // 构建分析提示词
            String analysisPrompt = buildAnalysisPrompt(bugRecord);

            // 尝试使用不同的AI引擎进行分析
            String analysis = getAIAnalysis(analysisPrompt);
            if (analysis == null || analysis.trim().isEmpty()) {
                return createFallbackAnalysis(bugRecord);
            }

            // 构建解决方案提示词
            String solutionPrompt = buildSolutionPrompt(bugRecord, analysis);
            String solution = getAISolution(solutionPrompt);

            if (solution == null || solution.trim().isEmpty()) {
                solution = generateBasicSolution(bugRecord);
            }

            // 计算置信度
            double confidence = calculateConfidence(bugRecord, analysis, solution);

            LOG.info("AI analysis completed for bug: " + bugRecord.getId());
            return new AnalysisResult(analysis, solution, confidence);

        } catch (Exception e) {
            LOG.error("Error during bug analysis", e);
            return createFallbackAnalysis(bugRecord);
        }
    }

    /**
     * 更新Bug记录的AI分析结果
     */
    public void updateBugWithAnalysis(@NotNull BugRecord bugRecord, @NotNull AnalysisResult result) {
        BugRecord updatedRecord = bugRecord.withAiAnalysis(result.getAnalysis(), result.getSolution());
        bugRecordService.updateBugRecord(updatedRecord);
        LOG.info("Updated bug record with AI analysis: " + bugRecord.getId());
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(@NotNull BugRecord bugRecord) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下Java应用程序错误，并提供详细的错误原因分析：\n\n");

        // 基本信息
        prompt.append("错误类型：").append(bugRecord.getErrorType().getDisplayName()).append("\n");
        if (bugRecord.getExceptionClass() != null) {
            prompt.append("异常类：").append(bugRecord.getExceptionClass()).append("\n");
        }
        if (bugRecord.getErrorMessage() != null) {
            prompt.append("错误消息：").append(bugRecord.getErrorMessage()).append("\n");
        }
        prompt.append("项目：").append(bugRecord.getProject()).append("\n");
        prompt.append("发生时间：").append(bugRecord.getFormattedTimestamp()).append("\n\n");

        // 堆栈跟踪（限制长度）
        if (bugRecord.getStackTrace() != null && !bugRecord.getStackTrace().isEmpty()) {
            prompt.append("关键堆栈跟踪：\n");
            int count = 0;
            for (var stackElement : bugRecord.getStackTrace()) {
                if (count >= 5) break; // 只显示前5行最关键的堆栈跟踪
                if (stackElement.isUserCode()) {
                    prompt.append("  ").append(stackElement.getFormattedString()).append("\n");
                    count++;
                }
            }
            prompt.append("\n");
        }

        // 原始错误文本（截断）
        String rawText = bugRecord.getRawText();
        if (rawText.length() > 1000) {
            rawText = rawText.substring(0, 997) + "...";
        }
        prompt.append("完整错误信息：\n").append(rawText).append("\n\n");

        prompt.append("请用中文分析这个错误的可能原因，包括：\n");
        prompt.append("1. 错误的根本原因\n");
        prompt.append("2. 常见的触发场景\n");
        prompt.append("3. 影响范围评估\n");
        prompt.append("4. 紧急程度判断\n");

        return prompt.toString();
    }

    /**
     * 构建解决方案提示词
     */
    private String buildSolutionPrompt(@NotNull BugRecord bugRecord, @NotNull String analysis) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下错误分析，请提供详细的解决方案：\n\n");
        prompt.append("错误分析：\n").append(analysis).append("\n\n");

        prompt.append("请提供：\n");
        prompt.append("1. 立即的临时解决方案（如果适用）\n");
        prompt.append("2. 长期的根本性解决方案\n");
        prompt.append("3. 预防措施和最佳实践\n");
        prompt.append("4. 相关的代码检查要点\n");
        prompt.append("5. 测试建议\n\n");

        prompt.append("请用中文回答，并提供具体可操作的步骤。");

        return prompt.toString();
    }

    /**
     * 获取AI分析结果
     */
    @Nullable
    private String getAIAnalysis(@NotNull String prompt) {
        // 尝试使用国内大模型
        try {
            String result = domesticAI.translateTextWithAI(prompt, "请分析这个错误");
            if (result != null && !result.trim().isEmpty() && !result.contains("翻译失败")) {
                return result;
            }
        } catch (Exception e) {
            LOG.debug("Domestic AI failed for analysis", e);
        }

        // 降级到其他翻译服务（用于生成基本分析）
        try {
            // 简化prompt用于翻译服务
            String simplePrompt = "分析Java错误：" + prompt.substring(0, Math.min(200, prompt.length()));
            String result = googleTranslate.translateText(simplePrompt, "zh", "en");
            if (result != null) {
                return "基于错误信息的基本分析：这是一个" + getBugRecord().getErrorType().getDisplayName() + "，需要进一步排查。";
            }
        } catch (Exception e) {
            LOG.debug("Google Translate failed for analysis", e);
        }

        return null;
    }

    /**
     * 获取AI解决方案
     */
    @Nullable
    private String getAISolution(@NotNull String prompt) {
        // 尝试使用国内大模型
        try {
            String result = domesticAI.translateTextWithAI(prompt, "请提供解决方案");
            if (result != null && !result.trim().isEmpty() && !result.contains("翻译失败")) {
                return result;
            }
        } catch (Exception e) {
            LOG.debug("Domestic AI failed for solution", e);
        }

        return null;
    }

    /**
     * 生成基本解决方案
     */
    private String generateBasicSolution(@NotNull BugRecord bugRecord) {
        ErrorType errorType = bugRecord.getErrorType();

        switch (errorType) {
            case DATABASE:
                return "数据库连接问题建议：\n" +
                        "1. 检查数据库连接配置\n" +
                        "2. 验证数据库服务是否正常运行\n" +
                        "3. 确认网络连接\n" +
                        "4. 检查数据库用户权限";

            case NETWORK:
                return "网络问题建议：\n" +
                        "1. 检查网络连接状态\n" +
                        "2. 验证防火墙设置\n" +
                        "3. 确认目标服务可访问\n" +
                        "4. 检查超时配置";

            case SPRING_FRAMEWORK:
                return "Spring框架问题建议：\n" +
                        "1. 检查Bean配置\n" +
                        "2. 验证依赖注入\n" +
                        "3. 确认配置文件正确性\n" +
                        "4. 检查启动类和组件扫描";

            case COMPILATION:
                return "编译问题建议：\n" +
                        "1. 检查语法错误\n" +
                        "2. 验证import语句\n" +
                        "3. 确认类路径配置\n" +
                        "4. 重新构建项目";

            case RUNTIME:
                return "运行时问题建议：\n" +
                        "1. 检查空指针引用\n" +
                        "2. 验证变量初始化\n" +
                        "3. 确认方法调用参数\n" +
                        "4. 添加异常处理";

            default:
                return "通用问题排查建议：\n" +
                        "1. 查看完整的错误日志\n" +
                        "2. 检查相关配置文件\n" +
                        "3. 验证依赖项版本\n" +
                        "4. 重启应用程序";
        }
    }

    /**
     * 计算分析结果的置信度
     */
    private double calculateConfidence(@NotNull BugRecord bugRecord, @NotNull String analysis, @NotNull String solution) {
        double confidence = 0.5; // 基础置信度

        // 根据错误类型调整置信度
        if (bugRecord.getErrorType() != ErrorType.UNKNOWN) {
            confidence += 0.2;
        }

        // 根据分析内容质量调整
        if (analysis.length() > 100) {
            confidence += 0.15;
        }

        // 根据解决方案质量调整
        if (solution.length() > 100 && solution.contains("建议")) {
            confidence += 0.15;
        }

        return Math.min(1.0, confidence);
    }

    /**
     * 创建备用分析结果
     */
    private AnalysisResult createFallbackAnalysis(@NotNull BugRecord bugRecord) {
        String basicAnalysis = "这是一个" + bugRecord.getErrorType().getDisplayName() +
                "，发生在" + bugRecord.getFormattedTimestamp() + "。";
        String basicSolution = generateBasicSolution(bugRecord);

        return new AnalysisResult(basicAnalysis, basicSolution, 0.3);
    }

    // 临时方法，需要被实际的bugRecord参数替代
    private BugRecord getBugRecord() {
        return null; // 这个方法需要在实际使用时被移除或修复
    }

    /**
     * AI分析结果
     */
    public static class AnalysisResult {
        private final String analysis;
        private final String solution;
        private final double confidence;

        public AnalysisResult(@NotNull String analysis, @NotNull String solution, double confidence) {
            this.analysis = analysis;
            this.solution = solution;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        }

        @NotNull
        public String getAnalysis() {
            return analysis;
        }

        @NotNull
        public String getSolution() {
            return solution;
        }

        public double getConfidence() {
            return confidence;
        }

        /**
         * 获取置信度等级描述
         */
        @NotNull
        public String getConfidenceLevel() {
            if (confidence >= 0.8) return "高";
            if (confidence >= 0.6) return "中";
            if (confidence >= 0.4) return "低";
            return "极低";
        }

        /**
         * 判断分析结果是否可靠
         */
        public boolean isReliable() {
            return confidence >= 0.6;
        }
    }
}