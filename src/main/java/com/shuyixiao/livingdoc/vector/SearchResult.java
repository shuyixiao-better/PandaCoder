package com.shuyixiao.livingdoc.vector;

import java.util.Map;

/**
 * 向量搜索结果
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class SearchResult {
    
    /**
     * 文档ID
     */
    private String id;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 相似度分数（0-1之间，越大越相似）
     */
    private double score;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 向量（可选）
     */
    private float[] vector;
    
    public SearchResult() {
    }
    
    public SearchResult(String id, String content, double score) {
        this.id = id;
        this.content = content;
        this.score = score;
    }
    
    public SearchResult(String id, String content, double score, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.score = score;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public float[] getVector() {
        return vector;
    }
    
    public void setVector(float[] vector) {
        this.vector = vector;
    }
    
    /**
     * 获取元数据字段
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 获取元数据字段（带默认值）
     */
    public Object getMetadataValue(String key, Object defaultValue) {
        return metadata != null ? metadata.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
                "id='" + id + '\'' +
                ", score=" + String.format("%.4f", score) +
                ", content='" + (content != null && content.length() > 50 ? 
                    content.substring(0, 50) + "..." : content) + '\'' +
                ", metadata=" + metadata +
                '}';
    }
    
    /**
     * Builder模式
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String content;
        private double score;
        private Map<String, Object> metadata;
        private float[] vector;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder score(double score) {
            this.score = score;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder vector(float[] vector) {
            this.vector = vector;
            return this;
        }
        
        public SearchResult build() {
            SearchResult result = new SearchResult(id, content, score, metadata);
            result.setVector(vector);
            return result;
        }
    }
}

