package com.shuyixiao.livingdoc.vector;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 向量文档实体
 * 
 * <p>包含文档的所有信息：ID、向量、内容、元数据等
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class VectorDocument {
    
    /**
     * 文档唯一标识
     */
    private String id;
    
    /**
     * 向量表示
     */
    private float[] vector;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 元数据（用于过滤和展示）
     */
    private Map<String, Object> metadata;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    public VectorDocument() {
        this.metadata = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public VectorDocument(String id, float[] vector, String content) {
        this();
        this.id = id;
        this.vector = vector;
        this.content = content;
    }
    
    public VectorDocument(String id, float[] vector, String content, Map<String, Object> metadata) {
        this(id, vector, content);
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public float[] getVector() {
        return vector;
    }
    
    public void setVector(float[] vector) {
        this.vector = vector;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 添加元数据
     */
    public VectorDocument addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    /**
     * 获取元数据（带默认值）
     */
    public Object getMetadata(String key, Object defaultValue) {
        return this.metadata.getOrDefault(key, defaultValue);
    }
    
    @Override
    public String toString() {
        return "VectorDocument{" +
                "id='" + id + '\'' +
                ", content='" + (content != null && content.length() > 50 ? 
                    content.substring(0, 50) + "..." : content) + '\'' +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                '}';
    }
}

