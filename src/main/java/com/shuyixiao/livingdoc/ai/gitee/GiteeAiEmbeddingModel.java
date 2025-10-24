package com.shuyixiao.livingdoc.ai.gitee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Gitee AI (模力方舟) Embedding Model 实现
 * 
 * <p>支持的模型：
 * <ul>
 *   <li>text-embedding-v3 - 通用向量化模型（推荐，1024维）</li>
 *   <li>bge-large-zh-v1.5 - 中文优化向量模型（1024维）</li>
 *   <li>m3e-base - 轻量级向量模型（768维）</li>
 * </ul>
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class GiteeAiEmbeddingModel {
    
    private static final Logger log = LoggerFactory.getLogger(GiteeAiEmbeddingModel.class);
    
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int dimensions;
    
    public GiteeAiEmbeddingModel(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
        this.dimensions = getDimensionsForModel(model);
    }
    
    /**
     * 获取单个文本的向量表示
     * 
     * @param text 文本内容
     * @return 向量数组
     */
    public float[] embed(String text) {
        List<float[]> embeddings = embedBatch(Collections.singletonList(text));
        return embeddings.isEmpty() ? new float[0] : embeddings.get(0);
    }
    
    /**
     * 批量获取文本的向量表示
     * 
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", texts);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/embeddings"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(60))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("API call failed: " + response.statusCode() + " - " + response.body());
            }
            
            // 解析响应
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            
            if (data == null || data.isEmpty()) {
                throw new RuntimeException("No embeddings in response");
            }
            
            // 提取向量
            List<float[]> embeddings = new ArrayList<>();
            for (Map<String, Object> item : data) {
                List<Double> embedding = (List<Double>) item.get("embedding");
                if (embedding != null) {
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = embedding.get(i).floatValue();
                    }
                    embeddings.add(vector);
                }
            }
            
            // 记录Token使用量
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
            if (usage != null) {
                log.debug("Embedding token usage - Total: {}", usage.get("total_tokens"));
            }
            
            log.debug("Successfully generated {} embeddings", embeddings.size());
            return embeddings;
            
        } catch (Exception e) {
            log.error("Failed to call Gitee AI embedding API", e);
            throw new RuntimeException("Failed to call Gitee AI embedding API", e);
        }
    }
    
    /**
     * 计算两个向量的余弦相似度
     * 
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度（0-1之间）
     */
    public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 获取向量维度
     * 
     * @return 向量维度
     */
    public int getDimensions() {
        return dimensions;
    }
    
    /**
     * 根据模型名称获取向量维度
     */
    private int getDimensionsForModel(String modelName) {
        return switch (modelName) {
            case "text-embedding-v3", "bge-large-zh-v1.5" -> 1024;
            case "m3e-base" -> 768;
            default -> 1024;  // 默认
        };
    }
    
    /**
     * 获取模型名称
     */
    public String getModel() {
        return model;
    }
}

