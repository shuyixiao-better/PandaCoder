package com.shuyixiao.livingdoc.analyzer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目文档信息
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ProjectDocumentation {
    
    /**
     * 项目名称
     */
    private String projectName;
    
    /**
     * 项目路径
     */
    private String projectPath;
    
    /**
     * 项目描述
     */
    private String projectDescription;
    
    /**
     * 项目版本
     */
    private String version;
    
    /**
     * API端点列表
     */
    private List<ApiEndpoint> apis = new ArrayList<>();
    
    /**
     * 实体模型列表
     */
    private List<EntityModel> entities = new ArrayList<>();
    
    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
    
    public ProjectDocumentation() {
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getProjectPath() {
        return projectPath;
    }
    
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }
    
    public String getProjectDescription() {
        return projectDescription;
    }
    
    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public List<ApiEndpoint> getApis() {
        return apis;
    }
    
    public void setApis(List<ApiEndpoint> apis) {
        this.apis = apis;
    }
    
    public void addApi(ApiEndpoint api) {
        this.apis.add(api);
    }
    
    public void addApis(List<ApiEndpoint> apis) {
        this.apis.addAll(apis);
    }
    
    // Alias methods for compatibility
    public List<ApiEndpoint> getEndpoints() {
        return apis;
    }
    
    public void setEndpoints(List<ApiEndpoint> endpoints) {
        this.apis = endpoints;
    }
    
    public List<EntityModel> getEntities() {
        return entities;
    }
    
    public void setEntities(List<EntityModel> entities) {
        this.entities = entities;
    }
    
    public void addEntity(EntityModel entity) {
        this.entities.add(entity);
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

