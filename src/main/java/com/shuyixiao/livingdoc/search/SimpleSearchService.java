package com.shuyixiao.livingdoc.search;

import com.shuyixiao.livingdoc.analyzer.model.ApiEndpoint;
import com.shuyixiao.livingdoc.analyzer.model.ProjectDocumentation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简单搜索服务
 * 
 * <p>基于文本匹配的简单搜索，不依赖向量数据库
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class SimpleSearchService {
    
    /**
     * 搜索结果类
     */
    public static class SearchResultItem {
        private final ApiEndpoint endpoint;
        private final double score;
        private final String matchedField;
        
        public SearchResultItem(ApiEndpoint endpoint, double score, String matchedField) {
            this.endpoint = endpoint;
            this.score = score;
            this.matchedField = matchedField;
        }
        
        public ApiEndpoint getEndpoint() {
            return endpoint;
        }
        
        public double getScore() {
            return score;
        }
        
        public String getMatchedField() {
            return matchedField;
        }
    }
    
    /**
     * 搜索 API 接口
     * 
     * @param doc 项目文档
     * @param query 搜索关键词
     * @return 搜索结果列表，按相关度排序
     */
    public List<SearchResultItem> search(@NotNull ProjectDocumentation doc, @NotNull String query) {
        if (query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        List<SearchResultItem> results = new ArrayList<>();
        
        for (ApiEndpoint endpoint : doc.getEndpoints()) {
            double score = calculateScore(endpoint, normalizedQuery);
            if (score > 0) {
                String matchedField = findMatchedField(endpoint, normalizedQuery);
                results.add(new SearchResultItem(endpoint, score, matchedField));
            }
        }
        
        // 按分数排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return results;
    }
    
    /**
     * 计算匹配分数
     */
    private double calculateScore(@NotNull ApiEndpoint endpoint, @NotNull String query) {
        double score = 0.0;
        
        // URL 匹配（权重 3.0）
        if (endpoint.getUrl() != null && contains(endpoint.getUrl(), query)) {
            score += 3.0;
        }
        
        // 方法名匹配（权重 2.5）
        if (endpoint.getMethodName() != null && contains(endpoint.getMethodName(), query)) {
            score += 2.5;
        }
        
        // 描述匹配（权重 2.0）
        if (endpoint.getDescription() != null && contains(endpoint.getDescription(), query)) {
            score += 2.0;
        }
        
        // HTTP 方法匹配（权重 1.5）
        if (endpoint.getMethod() != null && contains(endpoint.getMethod(), query)) {
            score += 1.5;
        }
        
        // 类名匹配（权重 1.0）
        if (endpoint.getClassName() != null && contains(endpoint.getClassName(), query)) {
            score += 1.0;
        }
        
        // 参数匹配（权重 1.0）
        if (endpoint.getParameters() != null) {
            for (var param : endpoint.getParameters()) {
                if (param.getName() != null && contains(param.getName(), query)) {
                    score += 1.0;
                }
                if (param.getDescription() != null && contains(param.getDescription(), query)) {
                    score += 0.5;
                }
            }
        }
        
        return score;
    }
    
    /**
     * 查找匹配的字段
     */
    private String findMatchedField(@NotNull ApiEndpoint endpoint, @NotNull String query) {
        if (endpoint.getUrl() != null && contains(endpoint.getUrl(), query)) {
            return "URL";
        }
        if (endpoint.getMethodName() != null && contains(endpoint.getMethodName(), query)) {
            return "方法名";
        }
        if (endpoint.getDescription() != null && contains(endpoint.getDescription(), query)) {
            return "描述";
        }
        if (endpoint.getMethod() != null && contains(endpoint.getMethod(), query)) {
            return "HTTP方法";
        }
        if (endpoint.getClassName() != null && contains(endpoint.getClassName(), query)) {
            return "类名";
        }
        return "参数";
    }
    
    /**
     * 检查文本是否包含查询词（不区分大小写）
     */
    private boolean contains(String text, String query) {
        return text.toLowerCase().contains(query);
    }
}

