package com.shuyixiao.livingdoc.analyzer.model;

/**
 * API 参数信息
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class Parameter {
    
    /**
     * 参数名
     */
    private String name;
    
    /**
     * 参数类型
     */
    private String type;
    
    /**
     * 参数描述
     */
    private String description;
    
    /**
     * 是否必填
     */
    private boolean required;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 参数来源: query | path | body | header
     */
    private String source;
    
    /**
     * 示例值
     */
    private String example;
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
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
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getExample() {
        return example;
    }
    
    public void setExample(String example) {
        this.example = example;
    }
    
    // Alias methods for compatibility
    public String getIn() {
        return source;
    }
    
    public void setIn(String in) {
        this.source = in;
    }
}

