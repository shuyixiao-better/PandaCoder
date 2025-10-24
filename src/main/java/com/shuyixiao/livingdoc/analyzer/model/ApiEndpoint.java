package com.shuyixiao.livingdoc.analyzer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * API 端点信息
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ApiEndpoint {
    
    /**
     * API路径
     */
    private String path;
    
    /**
     * HTTP方法: GET, POST, PUT, DELETE等
     */
    private String httpMethod;
    
    /**
     * API描述
     */
    private String description;
    
    /**
     * 请求参数列表
     */
    private List<Parameter> parameters = new ArrayList<>();
    
    /**
     * 响应模型
     */
    private ResponseModel response;
    
    /**
     * 所属Controller类名
     */
    private String controller;
    
    /**
     * 方法名
     */
    private String methodName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 行号
     */
    private int lineNumber;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 完整注释
     */
    private String javadoc;
    
    /**
     * 请求示例
     */
    private String requestExample;
    
    /**
     * 响应示例
     */
    private String responseExample;
    
    /**
     * 标签（用于分类）
     */
    private List<String> tags = new ArrayList<>();
    
    // Getters and Setters
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }
    
    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }
    
    public ResponseModel getResponse() {
        return response;
    }
    
    public void setResponse(ResponseModel response) {
        this.response = response;
    }
    
    public String getController() {
        return controller;
    }
    
    public void setController(String controller) {
        this.controller = controller;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getJavadoc() {
        return javadoc;
    }
    
    public void setJavadoc(String javadoc) {
        this.javadoc = javadoc;
    }
    
    public String getRequestExample() {
        return requestExample;
    }
    
    public void setRequestExample(String requestExample) {
        this.requestExample = requestExample;
    }
    
    public String getResponseExample() {
        return responseExample;
    }
    
    public void setResponseExample(String responseExample) {
        this.responseExample = responseExample;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public void addTag(String tag) {
        this.tags.add(tag);
    }
    
    // Alias methods for compatibility
    public String getUrl() {
        return path;
    }
    
    public void setUrl(String url) {
        this.path = url;
    }
    
    public String getMethod() {
        return httpMethod;
    }
    
    public void setMethod(String method) {
        this.httpMethod = method;
    }
    
    public String getClassName() {
        return controller;
    }
    
    public void setClassName(String className) {
        this.controller = className;
    }
    
    /**
     * 获取唯一ID
     */
    public String getId() {
        return controller + "#" + methodName;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s - %s", httpMethod, path, description);
    }
}

