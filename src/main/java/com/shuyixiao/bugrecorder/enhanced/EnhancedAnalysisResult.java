package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 增强版Bug记录分析结果
 * 提供更详细的分析信息和解决方案建议
 */
public class EnhancedAnalysisResult {

    private static final Logger LOG = Logger.getInstance(EnhancedAnalysisResult.class);

    private final String analysis;
    private final String solution;
    private final double confidence;
    private final AnalysisLevel level;
    private final String rootCause;
    private final String impactScope;
    private final String mitigationSteps;
    private final String preventionMeasures;
    private final LocalDateTime analyzedTime;
    private final String analyzer;

    private EnhancedAnalysisResult(Builder builder) {
        this.analysis = builder.analysis != null ? builder.analysis : "";
        this.solution = builder.solution != null ? builder.solution : "";
        this.confidence = Math.max(0.0, Math.min(1.0, builder.confidence));
        this.level = builder.level != null ? builder.level : AnalysisLevel.BASIC;
        this.rootCause = builder.rootCause;
        this.impactScope = builder.impactScope;
        this.mitigationSteps = builder.mitigationSteps;
        this.preventionMeasures = builder.preventionMeasures;
        this.analyzedTime = builder.analyzedTime != null ? builder.analyzedTime : LocalDateTime.now();
        this.analyzer = builder.analyzer != null ? builder.analyzer : "Unknown";
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

    @NotNull
    public AnalysisLevel getLevel() {
        return level;
    }

    @Nullable
    public String getRootCause() {
        return rootCause;
    }

    @Nullable
    public String getImpactScope() {
        return impactScope;
    }

    @Nullable
    public String getMitigationSteps() {
        return mitigationSteps;
    }

    @Nullable
    public String getPreventionMeasures() {
        return preventionMeasures;
    }

    @NotNull
    public LocalDateTime getAnalyzedTime() {
        return analyzedTime;
    }

    @NotNull
    public String getAnalyzer() {
        return analyzer;
    }

    /**
     * 获取置信度等级描述
     */
    @NotNull
    public String getConfidenceLevel() {
        if (confidence >= 0.9) return "极高";
        if (confidence >= 0.8) return "高";
        if (confidence >= 0.7) return "中高";
        if (confidence >= 0.6) return "中等";
        if (confidence >= 0.5) return "中低";
        if (confidence >= 0.4) return "低";
        return "极低";
    }

    /**
     * 判断分析结果是否可靠
     */
    public boolean isReliable() {
        return confidence >= 0.7;
    }

    /**
     * 获取分析结果摘要
     */
    @NotNull
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        // 添加分析内容摘要
        if (analysis != null && !analysis.isEmpty()) {
            String trimmedAnalysis = analysis.trim();
            summary.append(trimmedAnalysis.length() > 100 ? 
                trimmedAnalysis.substring(0, 97) + "..." : trimmedAnalysis);
        }
        
        // 添加置信度信息
        summary.append(" (置信度: ").append(String.format("%.1f", confidence * 100)).append("%)");
        
        return summary.toString();
    }

    /**
     * 合并另一个分析结果
     */
    @NotNull
    public EnhancedAnalysisResult mergeWith(@NotNull EnhancedAnalysisResult other) {
        try {
            Builder builder = new Builder(this);
            
            // 合并分析内容（选择置信度更高的）
            if (other.confidence > this.confidence) {
                builder.analysis(other.analysis)
                       .solution(other.solution)
                       .confidence(other.confidence)
                       .level(other.level);
            }
            
            // 合并其他信息（优先使用非空值）
            if (other.rootCause != null && !other.rootCause.isEmpty()) {
                builder.rootCause(other.rootCause);
            }
            
            if (other.impactScope != null && !other.impactScope.isEmpty()) {
                builder.impactScope(other.impactScope);
            }
            
            if (other.mitigationSteps != null && !other.mitigationSteps.isEmpty()) {
                builder.mitigationSteps(other.mitigationSteps);
            }
            
            if (other.preventionMeasures != null && !other.preventionMeasures.isEmpty()) {
                builder.preventionMeasures(other.preventionMeasures);
            }
            
            // 更新分析时间和分析器
            builder.analyzedTime(LocalDateTime.now())
                   .analyzer("Merged[" + this.analyzer + "+" + other.analyzer + "]");
            
            return builder.build();
            
        } catch (Exception e) {
            LOG.warn("Error merging analysis results", e);
            return this; // 返回当前结果作为回退
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancedAnalysisResult that = (EnhancedAnalysisResult) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               Objects.equals(analysis, that.analysis) &&
               Objects.equals(solution, that.solution) &&
               level == that.level &&
               Objects.equals(rootCause, that.rootCause) &&
               Objects.equals(impactScope, that.impactScope) &&
               Objects.equals(mitigationSteps, that.mitigationSteps) &&
               Objects.equals(preventionMeasures, that.preventionMeasures) &&
               Objects.equals(analyzedTime, that.analyzedTime) &&
               Objects.equals(analyzer, that.analyzer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analysis, solution, confidence, level, rootCause, impactScope, 
                           mitigationSteps, preventionMeasures, analyzedTime, analyzer);
    }

    @Override
    public String toString() {
        return "EnhancedAnalysisResult{" +
                "analysis='" + analysis + '\'' +
                ", solution='" + solution + '\'' +
                ", confidence=" + confidence +
                ", level=" + level +
                ", rootCause='" + rootCause + '\'' +
                ", impactScope='" + impactScope + '\'' +
                ", mitigationSteps='" + mitigationSteps + '\'' +
                ", preventionMeasures='" + preventionMeasures + '\'' +
                ", analyzedTime=" + analyzedTime +
                ", analyzer='" + analyzer + '\'' +
                '}';
    }

    /**
     * 创建构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 从现有结果创建构建器
     */
    public static Builder newBuilder(@NotNull EnhancedAnalysisResult result) {
        return new Builder(result);
    }

    /**
     * 分析等级
     */
    public enum AnalysisLevel {
        BASIC("基础分析"),
        INTERMEDIATE("中级分析"),
        ADVANCED("高级分析"),
        EXPERT("专家级分析");

        private final String displayName;

        AnalysisLevel(@NotNull String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private String analysis;
        private String solution;
        private double confidence = 0.5;
        private AnalysisLevel level = AnalysisLevel.BASIC;
        private String rootCause;
        private String impactScope;
        private String mitigationSteps;
        private String preventionMeasures;
        private LocalDateTime analyzedTime;
        private String analyzer;

        public Builder() {}

        public Builder(@NotNull EnhancedAnalysisResult result) {
            this.analysis = result.analysis;
            this.solution = result.solution;
            this.confidence = result.confidence;
            this.level = result.level;
            this.rootCause = result.rootCause;
            this.impactScope = result.impactScope;
            this.mitigationSteps = result.mitigationSteps;
            this.preventionMeasures = result.preventionMeasures;
            this.analyzedTime = result.analyzedTime;
            this.analyzer = result.analyzer;
        }

        public Builder analysis(@Nullable String analysis) {
            this.analysis = analysis;
            return this;
        }

        public Builder solution(@Nullable String solution) {
            this.solution = solution;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder level(@Nullable AnalysisLevel level) {
            this.level = level;
            return this;
        }

        public Builder rootCause(@Nullable String rootCause) {
            this.rootCause = rootCause;
            return this;
        }

        public Builder impactScope(@Nullable String impactScope) {
            this.impactScope = impactScope;
            return this;
        }

        public Builder mitigationSteps(@Nullable String mitigationSteps) {
            this.mitigationSteps = mitigationSteps;
            return this;
        }

        public Builder preventionMeasures(@Nullable String preventionMeasures) {
            this.preventionMeasures = preventionMeasures;
            return this;
        }

        public Builder analyzedTime(@Nullable LocalDateTime analyzedTime) {
            this.analyzedTime = analyzedTime;
            return this;
        }

        public Builder analyzer(@Nullable String analyzer) {
            this.analyzer = analyzer;
            return this;
        }

        public EnhancedAnalysisResult build() {
            return new EnhancedAnalysisResult(this);
        }
    }
}