package com.shuyixiao.livingdoc.generator;

import com.shuyixiao.livingdoc.analyzer.model.ApiEndpoint;
import com.shuyixiao.livingdoc.analyzer.model.Parameter;
import com.shuyixiao.livingdoc.analyzer.model.ProjectDocumentation;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Markdown 文档生成器
 * 
 * <p>将项目文档模型转换为 Markdown 格式
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class MarkdownGenerator {
    
    /**
     * 生成完整的项目文档
     */
    public String generate(@NotNull ProjectDocumentation doc) {
        StringBuilder md = new StringBuilder();
        
        // 标题和基本信息
        md.append("# ").append(doc.getProjectName()).append(" - API 文档\n\n");
        md.append("> 📅 生成时间: ").append(getCurrentTime()).append("\n\n");
        md.append("> 📂 项目路径: `").append(doc.getProjectPath()).append("`\n\n");
        md.append("---\n\n");
        
        // 统计信息
        md.append("## 📊 统计信息\n\n");
        md.append("- **总接口数**: ").append(doc.getEndpoints().size()).append("\n");
        
        Map<String, Long> methodCounts = doc.getEndpoints().stream()
            .collect(Collectors.groupingBy(ApiEndpoint::getMethod, Collectors.counting()));
        
        for (Map.Entry<String, Long> entry : methodCounts.entrySet()) {
            md.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n");
        }
        md.append("\n---\n\n");
        
        // 目录
        md.append("## 📑 目录\n\n");
        for (int i = 0; i < doc.getEndpoints().size(); i++) {
            ApiEndpoint endpoint = doc.getEndpoints().get(i);
            md.append(i + 1).append(". [")
                .append(endpoint.getMethod()).append(" ")
                .append(endpoint.getUrl())
                .append("](#").append(generateAnchor(endpoint)).append(")\n");
        }
        md.append("\n---\n\n");
        
        // 接口详情
        md.append("## 📝 接口详情\n\n");
        for (ApiEndpoint endpoint : doc.getEndpoints()) {
            md.append(generateEndpoint(endpoint));
        }
        
        return md.toString();
    }
    
    /**
     * 生成单个接口的文档
     */
    private String generateEndpoint(@NotNull ApiEndpoint endpoint) {
        StringBuilder md = new StringBuilder();
        
        // 接口标题
        md.append("### ").append(endpoint.getMethod()).append(" ").append(endpoint.getUrl()).append("\n\n");
        
        // 描述
        if (endpoint.getDescription() != null && !endpoint.getDescription().isEmpty()) {
            md.append("**描述**: ").append(endpoint.getDescription()).append("\n\n");
        }
        
        // 基本信息
        md.append("**基本信息**:\n");
        md.append("- **方法名**: `").append(endpoint.getMethodName()).append("`\n");
        md.append("- **类名**: `").append(endpoint.getClassName()).append("`\n");
        md.append("- **文件**: `").append(endpoint.getFilePath()).append(":").append(endpoint.getLineNumber()).append("`\n\n");
        
        // 参数
        if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
            md.append("**请求参数**:\n\n");
            md.append("| 参数名 | 类型 | 位置 | 必填 | 说明 |\n");
            md.append("|--------|------|------|------|------|\n");
            
            for (Parameter param : endpoint.getParameters()) {
                md.append("| `").append(param.getName()).append("` ");
                md.append("| ").append(param.getType()).append(" ");
                md.append("| ").append(param.getIn() != null ? param.getIn() : "query").append(" ");
                md.append("| ").append(param.isRequired() ? "✅" : "❌").append(" ");
                md.append("| ").append(param.getDescription() != null ? param.getDescription() : "-").append(" |\n");
            }
            md.append("\n");
        }
        
        // 返回值
        if (endpoint.getResponse() != null) {
            md.append("**响应类型**: `").append(endpoint.getResponse().getType()).append("`\n\n");
        }
        
        md.append("---\n\n");
        
        return md.toString();
    }
    
    /**
     * 生成锚点链接
     */
    private String generateAnchor(@NotNull ApiEndpoint endpoint) {
        return (endpoint.getMethod() + "-" + endpoint.getUrl())
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "-");
    }
    
    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

