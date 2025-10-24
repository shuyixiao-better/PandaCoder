package com.shuyixiao.livingdoc.analyzer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体模型信息
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class EntityModel {
    
    /**
     * 实体名称
     */
    private String name;
    
    /**
     * 实体描述
     */
    private String description;
    
    /**
     * 字段列表
     */
    private List<EntityField> fields = new ArrayList<>();
    
    /**
     * 包名
     */
    private String packageName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<EntityField> getFields() {
        return fields;
    }
    
    public void setFields(List<EntityField> fields) {
        this.fields = fields;
    }
    
    public void addField(EntityField field) {
        this.fields.add(field);
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public static class EntityField {
        private String name;
        private String type;
        private String description;
        
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
    }
}

