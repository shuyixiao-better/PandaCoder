package com.shuyixiao.livingdoc.analyzer.model;

/**
 * API 响应模型
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ResponseModel {
    
    /**
     * 响应类型
     */
    private String type;
    
    /**
     * 响应描述
     */
    private String description;
    
    /**
     * 响应示例
     */
    private String example;
    
    // Getters and Setters
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getExample() {
        return example;
    }
    
    public void setExample(String example) {
        this.example = example;
    }
}

