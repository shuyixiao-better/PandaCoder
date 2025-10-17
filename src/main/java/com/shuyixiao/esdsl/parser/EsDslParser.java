package com.shuyixiao.esdsl.parser;

import com.shuyixiao.esdsl.model.EsDslRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ES DSL 解析器
 * 从控制台输出中解析 Elasticsearch 查询 DSL
 */
public class EsDslParser {
    
    // 匹配 Elasticsearch REST 请求日志
    // 例如: [2024-10-17 10:30:45] DEBUG o.e.c.RestClient - request [POST http://localhost:9200/users/_search] ...
    private static final Pattern ES_REQUEST_PATTERN = Pattern.compile(
        "(?i).*?(GET|POST|PUT|DELETE)\\s+(?:https?://)?[^/]+/(\\S+)\\s+\\{(.+?)\\}",
        Pattern.DOTALL
    );
    
    // 匹配 Spring Data Elasticsearch 日志
    private static final Pattern SPRING_DATA_ES_PATTERN = Pattern.compile(
        "(?i).*?Elasticsearch\\s+(?:Request|Query).*?\\{(.+?)\\}",
        Pattern.DOTALL
    );
    
    // 匹配 cURL 格式的请求
    private static final Pattern CURL_PATTERN = Pattern.compile(
        "(?i)curl\\s+-X\\s+(GET|POST|PUT|DELETE)\\s+['\"]?(?:https?://)?[^/]+/(\\S+?)['\"]?\\s+-d\\s*['\"]?(.+?)['\"]?(?:\\s|$)",
        Pattern.DOTALL
    );
    
    // 匹配 JSON 格式的 DSL
    private static final Pattern JSON_DSL_PATTERN = Pattern.compile(
        "\\{\\s*\"(?:query|aggs|aggregations|sort|from|size)\"\\s*:.+?\\}",
        Pattern.DOTALL
    );
    
    // 匹配执行时间
    private static final Pattern EXECUTION_TIME_PATTERN = Pattern.compile(
        "(?i)(?:took|time|duration)[:=]?\\s*(\\d+)\\s*(?:ms|milliseconds?)?",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配 HTTP 状态码
    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile(
        "(?i)(?:status|response|code)[:=]?\\s*(\\d{3})",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 判断文本是否包含 ES DSL 查询
     */
    public static boolean containsEsDsl(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 检查是否包含 Elasticsearch 相关关键词
        String lowerText = text.toLowerCase();
        if (!lowerText.contains("elasticsearch") && 
            !lowerText.contains("_search") && 
            !lowerText.contains("es query") &&
            !lowerText.contains("es request") &&
            !JSON_DSL_PATTERN.matcher(text).find()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 解析 ES DSL 查询
     */
    public static EsDslRecord parseEsDsl(String text, String projectName) {
        if (!containsEsDsl(text)) {
            return null;
        }
        
        try {
            EsDslRecord.Builder builder = EsDslRecord.builder()
                .project(projectName);
            
            // 尝试匹配 REST 请求格式
            Matcher restMatcher = ES_REQUEST_PATTERN.matcher(text);
            if (restMatcher.find()) {
                String method = restMatcher.group(1).toUpperCase();
                String endpoint = restMatcher.group(2);
                String dslQuery = restMatcher.group(3);
                
                builder.method(method)
                       .endpoint(endpoint)
                       .dslQuery(formatJson(dslQuery))
                       .source("RestClient");
                
                // 提取索引名称
                String index = extractIndexFromEndpoint(endpoint);
                if (index != null) {
                    builder.index(index);
                }
            } 
            // 尝试匹配 cURL 格式
            else {
                Matcher curlMatcher = CURL_PATTERN.matcher(text);
                if (curlMatcher.find()) {
                    String method = curlMatcher.group(1).toUpperCase();
                    String endpoint = curlMatcher.group(2);
                    String dslQuery = curlMatcher.group(3);
                    
                    builder.method(method)
                           .endpoint(endpoint)
                           .dslQuery(formatJson(dslQuery))
                           .source("cURL");
                    
                    String index = extractIndexFromEndpoint(endpoint);
                    if (index != null) {
                        builder.index(index);
                    }
                }
                // 尝试匹配 Spring Data Elasticsearch 格式
                else {
                    Matcher springMatcher = SPRING_DATA_ES_PATTERN.matcher(text);
                    if (springMatcher.find()) {
                        String dslQuery = springMatcher.group(1);
                        builder.dslQuery(formatJson(dslQuery))
                               .method("POST")
                               .source("Spring Data Elasticsearch");
                    }
                    // 尝试直接提取 JSON DSL
                    else {
                        Matcher jsonMatcher = JSON_DSL_PATTERN.matcher(text);
                        if (jsonMatcher.find()) {
                            String dslQuery = jsonMatcher.group(0);
                            builder.dslQuery(formatJson(dslQuery))
                                   .method("POST")
                                   .source("Unknown");
                        } else {
                            return null;
                        }
                    }
                }
            }
            
            // 提取执行时间
            Matcher timeMatcher = EXECUTION_TIME_PATTERN.matcher(text);
            if (timeMatcher.find()) {
                try {
                    long executionTime = Long.parseLong(timeMatcher.group(1));
                    builder.executionTime(executionTime);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            
            // 提取 HTTP 状态码
            Matcher statusMatcher = HTTP_STATUS_PATTERN.matcher(text);
            if (statusMatcher.find()) {
                try {
                    int httpStatus = Integer.parseInt(statusMatcher.group(1));
                    builder.httpStatus(httpStatus);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            
            // 如果没有状态码，默认为 200
            EsDslRecord tempRecord = builder.build();
            if (tempRecord.getHttpStatus() == null) {
                builder.httpStatus(200);
            }
            
            return builder.build();
            
        } catch (Exception e) {
            // 解析失败时返回 null
            return null;
        }
    }
    
    /**
     * 从端点中提取索引名称
     */
    private static String extractIndexFromEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return null;
        }
        
        // 去掉查询参数
        endpoint = endpoint.split("\\?")[0];
        
        // 提取第一个路径段作为索引名称
        String[] parts = endpoint.split("/");
        if (parts.length > 0) {
            String index = parts[0];
            // 排除一些保留关键词
            if (!index.equals("_search") && 
                !index.equals("_bulk") && 
                !index.equals("_doc") &&
                !index.equals("_update") &&
                !index.isEmpty()) {
                return index;
            }
        }
        
        return null;
    }
    
    /**
     * 格式化 JSON
     */
    private static String formatJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        try {
            // 简单的 JSON 格式化（添加适当的换行和缩进）
            json = json.trim();
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inQuote = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inQuote = !inQuote;
                }
                
                if (!inQuote) {
                    if (c == '{' || c == '[') {
                        formatted.append(c).append('\n');
                        indent++;
                        formatted.append("  ".repeat(indent));
                    } else if (c == '}' || c == ']') {
                        formatted.append('\n');
                        indent--;
                        formatted.append("  ".repeat(indent));
                        formatted.append(c);
                    } else if (c == ',') {
                        formatted.append(c).append('\n');
                        formatted.append("  ".repeat(indent));
                    } else if (c == ':') {
                        formatted.append(c).append(' ');
                    } else if (!Character.isWhitespace(c)) {
                        formatted.append(c);
                    }
                } else {
                    formatted.append(c);
                }
            }
            
            return formatted.toString();
        } catch (Exception e) {
            // 格式化失败时返回原始字符串
            return json;
        }
    }
}

