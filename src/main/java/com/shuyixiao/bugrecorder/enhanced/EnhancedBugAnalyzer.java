package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.BaiduAPI;
import com.shuyixiao.DomesticAITranslationAPI;
import com.shuyixiao.GoogleCloudTranslationAPI;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 增强版Bug记录分析器
 * 提供更智能的错误分析和解决方案建议
 */
public class EnhancedBugAnalyzer {

    private static final Logger LOG = Logger.getInstance(EnhancedBugAnalyzer.class);

    private final Project project;
    
    // AI引擎
    private final DomesticAITranslationAPI domesticAI;
    private final GoogleCloudTranslationAPI googleTranslate;
    private final BaiduAPI baiduTranslate;
    
    // 线程池优化
    private final ExecutorService analysisExecutor;
    
    // 配置参数
    private static final int MAX_CONCURRENT_ANALYSIS = 3;
    private static final long ANALYSIS_TIMEOUT_MS = 30000; // 30秒超时

    public EnhancedBugAnalyzer(@NotNull Project project) {
        this.project = project;
        
        // 初始化AI引擎
        this.domesticAI = new DomesticAITranslationAPI();
        this.googleTranslate = new GoogleCloudTranslationAPI();
        this.baiduTranslate = new BaiduAPI();
        
        // 创建专用线程池处理AI分析
        this.analysisExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_ANALYSIS, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r, "BugAnalyzer-Enhanced-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        
        LOG.info("Enhanced Bug Analyzer initialized for project: " + project.getName());
    }

    /**
     * 异步分析Bug记录
     */
    @NotNull
    public CompletableFuture<EnhancedAnalysisResult> analyzeBugAsync(@NotNull BugRecord bugRecord) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeBug(bugRecord);
            } catch (Exception e) {
                LOG.error("Failed to analyze bug record: " + bugRecord.getId(), e);
                return createFallbackAnalysis(bugRecord, e);
            }
        }, analysisExecutor).orTimeout(ANALYSIS_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 分析Bug记录
     */
    @NotNull
    public EnhancedAnalysisResult analyzeBug(@NotNull BugRecord bugRecord) {
        try {
            LOG.info("Starting enhanced analysis for bug: " + bugRecord.getId() + 
                    " (" + bugRecord.getErrorType().getDisplayName() + ")");
            
            // 构建分析提示词
            String analysisPrompt = buildAnalysisPrompt(bugRecord);
            
            // 尝试使用不同的AI引擎进行分析
            String analysis = getAIAnalysis(analysisPrompt);
            if (analysis == null || analysis.trim().isEmpty()) {
                return createBasicAnalysis(bugRecord);
            }
            
            // 构建解决方案提示词
            String solutionPrompt = buildSolutionPrompt(bugRecord, analysis);
            String solution = getAISolution(solutionPrompt);
            
            if (solution == null || solution.trim().isEmpty()) {
                solution = generateBasicSolution(bugRecord);
            }
            
            // 计算置信度
            double confidence = calculateConfidence(bugRecord, analysis, solution);
            
            // 创建增强分析结果
            EnhancedAnalysisResult result = EnhancedAnalysisResult.newBuilder()
                    .analysis(analysis)
                    .solution(solution)
                    .confidence(confidence)
                    .level(determineAnalysisLevel(bugRecord))
                    .rootCause(extractRootCause(analysis))
                    .impactScope(determineImpactScope(bugRecord))
                    .mitigationSteps(extractMitigationSteps(solution))
                    .preventionMeasures(extractPreventionMeasures(solution))
                    .analyzedTime(LocalDateTime.now())
                    .analyzer("EnhancedBugAnalyzer")
                    .build();
            
            LOG.info("Enhanced analysis completed for bug: " + bugRecord.getId() + 
                    " (Confidence: " + String.format("%.2f", confidence) + ")");
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Failed to analyze bug record: " + bugRecord.getId(), e);
            return createFallbackAnalysis(bugRecord, e);
        }
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
        if (rawText != null && rawText.length() > 1000) {
            rawText = rawText.substring(0, 997) + "...";
        }
        if (rawText != null) {
            prompt.append("完整错误信息：\n").append(rawText).append("\n\n");
        }

        prompt.append("请用中文分析这个错误的可能原因，包括：\n");
        prompt.append("1. 错误的根本原因\n");
        prompt.append("2. 常见的触发场景\n");
        prompt.append("3. 影响范围评估\n");
        prompt.append("4. 紧急程度判断\n");
        prompt.append("5. 相关的技术栈信息\n");

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

        // 降级到Google翻译
        try {
            String result = googleTranslate.translateText(prompt, "zh", "en");
            if (result != null && !result.trim().isEmpty()) {
                return "基于翻译的分析结果：" + result;
            }
        } catch (Exception e) {
            LOG.debug("Google Translate failed for analysis", e);
        }

        // 最后尝试百度翻译
        try {
            String result = baiduTranslate.translate(prompt);
            if (result != null && !result.trim().isEmpty()) {
                return "基于百度翻译的分析结果：" + result;
            }
        } catch (Exception e) {
            LOG.debug("Baidu Translate failed for analysis", e);
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
     * 创建基本分析
     */
    private EnhancedAnalysisResult createBasicAnalysis(@NotNull BugRecord bugRecord) {
        String basicAnalysis = "这是一个" + bugRecord.getErrorType().getDisplayName() +
                "，发生在" + bugRecord.getFormattedTimestamp() + "。";
        String basicSolution = generateBasicSolution(bugRecord);

        return EnhancedAnalysisResult.newBuilder()
                .analysis(basicAnalysis)
                .solution(basicSolution)
                .confidence(0.3)
                .level(EnhancedAnalysisResult.AnalysisLevel.BASIC)
                .analyzedTime(LocalDateTime.now())
                .analyzer("BasicAnalyzer")
                .build();
    }

    /**
     * 创建备用分析
     */
    private EnhancedAnalysisResult createFallbackAnalysis(@NotNull BugRecord bugRecord, @NotNull Exception e) {
        String fallbackAnalysis = "分析失败：" + e.getMessage();
        String fallbackSolution = "无法提供解决方案建议，请手动排查问题。";

        return EnhancedAnalysisResult.newBuilder()
                .analysis(fallbackAnalysis)
                .solution(fallbackSolution)
                .confidence(0.0)
                .level(EnhancedAnalysisResult.AnalysisLevel.BASIC)
                .analyzedTime(LocalDateTime.now())
                .analyzer("FallbackAnalyzer")
                .build();
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
     * 确定分析等级
     */
    private EnhancedAnalysisResult.AnalysisLevel determineAnalysisLevel(@NotNull BugRecord bugRecord) {
        // 基于错误类型的复杂度确定分析等级
        switch (bugRecord.getErrorType()) {
            case DATABASE:
            case NETWORK:
            case SPRING_FRAMEWORK:
                return EnhancedAnalysisResult.AnalysisLevel.INTERMEDIATE;
            case COMPILATION:
            case RUNTIME:
            case VALIDATION:
                return EnhancedAnalysisResult.AnalysisLevel.BASIC;
            default:
                return EnhancedAnalysisResult.AnalysisLevel.BASIC;
        }
    }

    /**
     * 提取根本原因
     */
    private String extractRootCause(@NotNull String analysis) {
        // 简单提取根本原因（实际实现中可能需要更复杂的自然语言处理）
        if (analysis.contains("根本原因")) {
            int startIndex = analysis.indexOf("根本原因");
            int endIndex = analysis.indexOf("\n", startIndex);
            if (endIndex == -1) {
                endIndex = Math.min(startIndex + 100, analysis.length());
            }
            return analysis.substring(startIndex, endIndex).trim();
        }
        return "未明确识别根本原因";
    }

    /**
     * 确定影响范围
     */
    private String determineImpactScope(@NotNull BugRecord bugRecord) {
        // 基于错误类型确定影响范围
        switch (bugRecord.getErrorType()) {
            case DATABASE:
                return "数据库连接层面，可能影响所有数据库操作";
            case NETWORK:
                return "网络通信层面，可能影响外部服务调用";
            case SPRING_FRAMEWORK:
                return "应用框架层面，可能影响应用启动和运行";
            case COMPILATION:
                return "编译阶段，影响项目构建";
            case RUNTIME:
                return "运行时阶段，影响具体功能执行";
            default:
                return "局部影响";
        }
    }

    /**
     * 提取缓解步骤
     */
    private String extractMitigationSteps(@NotNull String solution) {
        // 简单提取缓解步骤
        if (solution.contains("临时解决方案")) {
            int startIndex = solution.indexOf("临时解决方案");
            int endIndex = solution.indexOf("长期的根本性解决方案");
            if (endIndex == -1) {
                endIndex = solution.length();
            }
            return solution.substring(startIndex, endIndex).trim();
        }
        return "请参考完整解决方案";
    }

    /**
     * 提取预防措施
     */
    private String extractPreventionMeasures(@NotNull String solution) {
        // 简单提取预防措施
        if (solution.contains("预防措施")) {
            int startIndex = solution.indexOf("预防措施");
            int endIndex = solution.indexOf("测试建议");
            if (endIndex == -1) {
                endIndex = solution.length();
            }
            return solution.substring(startIndex, endIndex).trim();
        }
        return "请参考完整解决方案";
    }

    /**
     * 关闭分析器
     */
    public void shutdown() {
        LOG.info("Shutting down Enhanced Bug Analyzer for project: " + project.getName());
        
        analysisExecutor.shutdown();
        try {
            if (!analysisExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                analysisExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            analysisExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOG.info("Enhanced Bug Analyzer shutdown completed for project: " + project.getName());
    }

    /**
     * 获取项目
     */
    @NotNull
    public Project getProject() {
        return project;
    }
}