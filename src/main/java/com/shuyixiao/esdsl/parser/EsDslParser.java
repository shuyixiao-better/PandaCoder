package com.shuyixiao.esdsl.parser;

import com.shuyixiao.esdsl.model.EsDslRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ES DSL 解析器 - 优化版
 * 从控制台输出中解析 Elasticsearch 查询 DSL
 * 支持多种日志格式,包括长 DSL 和 TRACE 级别日志
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
    
    // 匹配 cURL 格式的请求 - 优化版,支持 -iX 和长 DSL
    // 支持格式: curl -iX POST 'url' -d '{"json":"data"}'
    private static final Pattern CURL_PATTERN = Pattern.compile(
        "curl\\s+(?:-[iI]\\s+)?-[Xx]\\s+(GET|POST|PUT|DELETE)\\s+['\"]?(https?://[^\\s'\"]+)['\"]?\\s+-d\\s+['\"](.+?)['\"](?:\\s*#|\\n#|\\s*$)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 匹配 TRACE 级别的详细 curl 日志 - 新增
    // 格式: curl -iX POST 'url' -d '完整JSON'
    //       # HTTP/1.1 200 OK
    //       # 响应内容
    private static final Pattern TRACE_CURL_PATTERN = Pattern.compile(
        "curl\\s+(?:-[iI]\\s+)?-[Xx]\\s+(GET|POST|PUT|DELETE)\\s+['\"]?(https?://[^\\s'\"]+)['\"]?\\s+-d\\s+['\"]\\{(.+?)\\}['\"]\\s*(?:#|\\n)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 匹配 RequestLogger 的 TRACE curl 日志 - 支持POST/PUT (带 -d)
    // 格式: TRACE (RequestLogger.java:90)- curl -iX POST 'url' -d '{"json":"data"}'
    // ✅ 关键改进: -iX可能连在一起，使用 -[iIxX]+ 匹配
    private static final Pattern REQUEST_LOGGER_PATTERN = Pattern.compile(
        "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-?\\s*curl\\s+-[iIxX]+\\s+(POST|PUT|DELETE)\\s+'([^']+)'(?:.*?)-d\\s+'(\\{.+?\\})'",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 匹配 RequestLogger 的 GET 请求 (无 -d 参数)
    // 格式: TRACE (RequestLogger.java:90)- curl -iX GET 'url'
    // ✅ 关键改进: -iX可能连在一起，使用 -[iIxX]+ 匹配
    private static final Pattern REQUEST_LOGGER_GET_PATTERN = Pattern.compile(
        "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-?\\s*curl\\s+-[iIxX]+\\s+GET\\s+'([^']+)'",
        Pattern.CASE_INSENSITIVE
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
        "(?i)(?:status|HTTP/[\\d.]+)\\s+(\\d{3})",
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
        return lowerText.contains("elasticsearch") || 
               lowerText.contains("_search") || 
               lowerText.contains("requestlogger") ||
               lowerText.contains("es query") ||
               lowerText.contains("es request") ||
               (lowerText.contains("curl") && lowerText.contains("-d")) ||
               JSON_DSL_PATTERN.matcher(text).find();
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
            
            // 1. 首先尝试匹配 RequestLogger TRACE 格式 - POST/PUT/DELETE (带 -d)
            // 格式: TRACE (RequestLogger.java:90)- curl -iX POST 'url' -d '{"query":...}'
            Matcher requestLoggerMatcher = REQUEST_LOGGER_PATTERN.matcher(text);
            if (requestLoggerMatcher.find()) {
                String method = requestLoggerMatcher.group(1).toUpperCase();
                String url = requestLoggerMatcher.group(2);
                String dslQuery = requestLoggerMatcher.group(3);  // ✅ 直接使用,不再添加额外的 {}
                
                System.out.println("[DEBUG] 匹配到 RequestLogger TRACE:");
                System.out.println("  Method: " + method);
                System.out.println("  URL: " + url);
                System.out.println("  DSL (前100字符): " + dslQuery.substring(0, Math.min(100, dslQuery.length())));
                
                // 验证 DSL 是否为有效的 JSON
                if (dslQuery.trim().startsWith("{") && dslQuery.trim().endsWith("}")) {
                    builder.method(method)
                           .dslQuery(formatJson(dslQuery))
                           .source("RequestLogger (TRACE)");
                    
                    // 从 URL 提取索引和端点
                    String[] urlParts = extractUrlParts(url);
                    System.out.println("[DEBUG] URL解析结果: " + (urlParts != null ? String.join(" | ", urlParts) : "null"));
                    if (urlParts != null && urlParts.length >= 2) {
                        builder.index(urlParts[0]);
                        builder.endpoint(urlParts[1]);
                    }
                    
                    // 尝试提取 HTTP 状态码
                    extractHttpStatus(text, builder);
                    
                    return buildRecord(builder, text);
                }
                // 如果 JSON 验证失败,继续尝试其他模式匹配
            }
            
            // 2. 尝试匹配 RequestLogger GET 请求 (无 -d 参数)
            // 格式: TRACE (RequestLogger.java:90)- curl -iX GET 'url'
            Matcher requestLoggerGetMatcher = REQUEST_LOGGER_GET_PATTERN.matcher(text);
            if (requestLoggerGetMatcher.find()) {
                String url = requestLoggerGetMatcher.group(1);
                
                System.out.println("[DEBUG] 匹配到 RequestLogger GET:");
                System.out.println("  URL: " + url);
                
                builder.method("GET")
                       .source("RequestLogger (TRACE)");
                
                // 从 URL 提取索引和端点
                String[] urlParts = extractUrlParts(url);
                System.out.println("[DEBUG] URL解析结果: " + (urlParts != null ? String.join(" | ", urlParts) : "null"));
                if (urlParts != null && urlParts.length >= 2) {
                    builder.index(urlParts[0]);
                    builder.endpoint(urlParts[1]);
                }
                
                // 尝试提取 HTTP 状态码
                extractHttpStatus(text, builder);
                
                // ✅ 尝试从响应中提取JSON (即使不完整也显示)
                String responseJson = extractResponseJson(text);
                System.out.println("[DEBUG] 提取的响应JSON: " + (responseJson != null ? 
                    responseJson.substring(0, Math.min(100, responseJson.length())) + "..." : "null"));
                
                if (responseJson != null && !responseJson.isEmpty()) {
                    // 检查JSON是否完整
                    boolean isComplete = responseJson.trim().endsWith("}");
                    String label = isComplete ? "完整响应: " : "部分响应(截断): ";
                    try {
                        builder.dslQuery(label + formatJson(responseJson));
                    } catch (Exception e) {
                        // 格式化失败,使用原始JSON
                        builder.dslQuery(label + responseJson);
                    }
                } else {
                    // 没有响应JSON,只记录请求
                    builder.dslQuery("GET 请求 (无响应数据)");
                }
                
                return buildRecord(builder, text);
            }
            
            // 3. 尝试匹配 TRACE 级别的 curl 日志
            Matcher traceCurlMatcher = TRACE_CURL_PATTERN.matcher(text);
            if (traceCurlMatcher.find()) {
                String method = traceCurlMatcher.group(1).toUpperCase();
                String url = traceCurlMatcher.group(2);
                String dslQuery = "{" + traceCurlMatcher.group(3) + "}";
                
                builder.method(method)
                       .dslQuery(formatJson(dslQuery))
                       .source("cURL (TRACE)");
                
                // 从 URL 提取索引和端点
                String[] urlParts = extractUrlParts(url);
                if (urlParts != null && urlParts.length >= 2) {
                    builder.index(urlParts[0]);
                    builder.endpoint(urlParts[1]);
                }
                
                return buildRecord(builder, text);
            }
            
            // 4. 尝试匹配标准 REST 请求格式
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
                
                return buildRecord(builder, text);
            } 
            
            // 5. 尝试匹配标准 cURL 格式
            Matcher curlMatcher = CURL_PATTERN.matcher(text);
            if (curlMatcher.find()) {
                String method = curlMatcher.group(1).toUpperCase();
                String url = curlMatcher.group(2);
                String dslQuery = curlMatcher.group(3);
                
                builder.method(method)
                       .dslQuery(formatJson(dslQuery))
                       .source("cURL");
                
                // 从 URL 提取索引和端点
                String[] urlParts = extractUrlParts(url);
                if (urlParts != null && urlParts.length >= 2) {
                    builder.index(urlParts[0]);
                    builder.endpoint(urlParts[1]);
                }
                
                return buildRecord(builder, text);
            }
            
            // 6. 尝试匹配 Spring Data Elasticsearch 格式
            Matcher springMatcher = SPRING_DATA_ES_PATTERN.matcher(text);
            if (springMatcher.find()) {
                String dslQuery = springMatcher.group(1);
                builder.dslQuery(formatJson(dslQuery))
                       .method("POST")
                       .source("Spring Data Elasticsearch");
                
                return buildRecord(builder, text);
            }
            
            // 7. 尝试直接提取 JSON DSL
            Matcher jsonMatcher = JSON_DSL_PATTERN.matcher(text);
            if (jsonMatcher.find()) {
                String dslQuery = jsonMatcher.group(0);
                builder.dslQuery(formatJson(dslQuery))
                       .method("POST")
                       .source("Unknown");
                
                return buildRecord(builder, text);
            }
            
            return null;
            
        } catch (Exception e) {
            // 解析失败时返回 null
            return null;
        }
    }
    
    /**
     * 构建记录并提取元数据
     */
    private static EsDslRecord buildRecord(EsDslRecord.Builder builder, String fullText) {
        // 提取执行时间
        Matcher timeMatcher = EXECUTION_TIME_PATTERN.matcher(fullText);
        if (timeMatcher.find()) {
            try {
                long executionTime = Long.parseLong(timeMatcher.group(1));
                builder.executionTime(executionTime);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        
        // 提取 HTTP 状态码
        Matcher statusMatcher = HTTP_STATUS_PATTERN.matcher(fullText);
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
    }
    
    /**
     * 提取 HTTP 状态码
     * 从 TRACE 日志的响应部分提取状态码
     * 例如: # HTTP/1.1 200 OK
     */
    private static void extractHttpStatus(String text, EsDslRecord.Builder builder) {
        try {
            Pattern statusPattern = Pattern.compile("#\\s*HTTP/\\d\\.\\d\\s+(\\d{3})\\s+OK");
            Matcher statusMatcher = statusPattern.matcher(text);
            if (statusMatcher.find()) {
                builder.httpStatus(Integer.parseInt(statusMatcher.group(1)));
            }
        } catch (Exception e) {
            // 忽略提取失败
        }
    }
    
    /**
     * 从TRACE日志响应中提取JSON
     * 例如: # {"cluster_name":"elasticsearch",...}
     */
    private static String extractResponseJson(String text) {
        try {
            // 查找响应JSON (以 # { 开始或 #\n# { 开始)
            // 使用非贪婪匹配,并尝试提取完整的JSON对象
            Pattern jsonPattern = Pattern.compile("#\\s*\\{[^}]*\\}(?:\\}*)", Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(text);
            
            if (jsonMatcher.find()) {
                String json = jsonMatcher.group(0).trim();
                // 移除开头的 #
                json = json.replaceFirst("^#\\s*", "");
                
                // 简单验证是否是有效的JSON开头
                if (json.startsWith("{")) {
                    return json;
                }
            }
            
            // 如果上面的匹配失败,尝试更宽松的匹配
            // 查找 # { 之后的所有内容(即使JSON不完整)
            int jsonStart = text.indexOf("# {");
            if (jsonStart == -1) {
                jsonStart = text.indexOf("#\n# {");
                if (jsonStart != -1) {
                    jsonStart += 3; // 跳过 "#\n#"
                }
            }
            
            if (jsonStart >= 0) {
                String remaining = text.substring(jsonStart).trim();
                // 移除开头的 #
                remaining = remaining.replaceFirst("^#\\s*", "");
                
                if (remaining.startsWith("{")) {
                    // 即使JSON不完整,也返回(用于诊断)
                    return remaining.trim();
                }
            }
        } catch (Exception e) {
            // 忽略提取失败
            System.out.println("[DEBUG] extractResponseJson 异常: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 从完整 URL 中提取索引和端点
     * 例如: http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true
     * 返回: ["dataset_chunk_sharding_24_1536", "dataset_chunk_sharding_24_1536/_search?typed_keys=true"]
     */
    private static String[] extractUrlParts(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // 去掉协议和主机部分
            String path = url;
            if (path.contains("://")) {
                path = path.substring(path.indexOf("://") + 3);
                if (path.contains("/")) {
                    path = path.substring(path.indexOf("/") + 1);
                }
            }
            
            // 提取索引名称(第一个路径段)
            String index = null;
            String endpoint = path;
            
            String[] parts = path.split("/");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                index = parts[0].split("\\?")[0]; // 去掉查询参数
            }
            
            return new String[]{index, endpoint};
        } catch (Exception e) {
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
            // 清理可能的转义字符
            json = json.trim();
            
            // 简单的 JSON 格式化（添加适当的换行和缩进）
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inQuote = false;
            boolean inEscape = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                // 处理转义字符
                if (inEscape) {
                    formatted.append(c);
                    inEscape = false;
                    continue;
                }
                
                if (c == '\\') {
                    formatted.append(c);
                    inEscape = true;
                    continue;
                }
                
                // 处理引号
                if (c == '"' && !inEscape) {
                    inQuote = !inQuote;
                    formatted.append(c);
                    continue;
                }
                
                // 在引号内,直接输出
                if (inQuote) {
                    formatted.append(c);
                    continue;
                }
                
                // 在引号外,处理格式化
                switch (c) {
                    case '{':
                    case '[':
                        formatted.append(c).append('\n');
                        indent++;
                        formatted.append("  ".repeat(indent));
                        break;
                    case '}':
                    case ']':
                        formatted.append('\n');
                        indent--;
                        formatted.append("  ".repeat(Math.max(0, indent)));
                        formatted.append(c);
                        break;
                    case ',':
                        formatted.append(c).append('\n');
                        formatted.append("  ".repeat(indent));
                        break;
                    case ':':
                        formatted.append(c).append(' ');
                        break;
                    default:
                        if (!Character.isWhitespace(c)) {
                            formatted.append(c);
                        }
                        break;
                }
            }
            
            return formatted.toString();
        } catch (Exception e) {
            // 格式化失败时返回原始字符串
            return json;
        }
    }
}
