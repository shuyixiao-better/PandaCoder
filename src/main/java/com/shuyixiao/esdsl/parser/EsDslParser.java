package com.shuyixiao.esdsl.parser;

import com.shuyixiao.esdsl.model.EsDslRecord;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ES DSL 解析器 - 智能版
 * 采用多阶段解析策略，不依赖特定日志格式
 * 核心思路：先提取JSON，再关联上下文，最后语义验证
 */
public class EsDslParser {

    // ==================== 第一阶段：JSON提取 ====================

    /**
     * 智能提取所有可能的JSON块（支持嵌套）
     * 使用栈匹配算法，而不是正则表达式
     */
    private static List<JsonBlock> extractJsonBlocks(String text) {
        List<JsonBlock> blocks = new ArrayList<>();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            if (text.charAt(i) == '{') {
                // 找到JSON起始位置，使用栈匹配
                int start = i;
                int braceCount = 0;
                boolean inString = false;
                boolean escaped = false;

                for (int j = i; j < len; j++) {
                    char c = text.charAt(j);

                    if (escaped) {
                        escaped = false;
                        continue;
                    }

                    if (c == '\\') {
                        escaped = true;
                        continue;
                    }

                    if (c == '"') {
                        inString = !inString;
                        continue;
                    }

                    if (!inString) {
                        if (c == '{') {
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                // 找到完整的JSON块
                                String json = text.substring(start, j + 1);
                                blocks.add(new JsonBlock(json, start, j + 1));
                                i = j; // 跳过已处理的部分
                                break;
                            }
                        }
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * JSON块数据结构
     */
    private static class JsonBlock {
        String content;
        int startPos;
        int endPos;

        JsonBlock(String content, int startPos, int endPos) {
            this.content = content;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }

    // ==================== 第二阶段：语义验证 ====================

    /**
     * 验证JSON是否是ES DSL（查询请求，而非响应）
     * 检查是否包含ES特征字段，同时排除响应数据
     */
    private static boolean isEsDsl(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        String lower = json.toLowerCase();

        // ❌ 排除ES响应数据（包含这些字段的是响应，不是DSL）
        if (lower.contains("\"_index\"") ||
            lower.contains("\"_id\"") ||
            lower.contains("\"_score\"") ||
            lower.contains("\"hits\"") && lower.contains("\"total\"") && lower.contains("\"max_score\"") ||
            lower.contains("\"took\"") && lower.contains("\"timed_out\"") && lower.contains("\"_shards\"")) {
            return false;
        }

        // ✅ ES DSL 特征关键词（查询请求）
        return lower.contains("\"query\"") ||
               lower.contains("\"aggs\"") ||
               lower.contains("\"aggregations\"") ||
               lower.contains("\"bool\"") ||
               lower.contains("\"match\"") ||
               lower.contains("\"term\"") ||
               lower.contains("\"range\"") ||
               lower.contains("\"sort\"") ||
               lower.contains("\"_source\"") ||
               lower.contains("\"size\"") && lower.contains("\"from\"") ||
               lower.contains("\"must\"") ||
               lower.contains("\"should\"") ||
               lower.contains("\"filter\"");
    }

    // ==================== 第三阶段：上下文提取 ====================

    /**
     * 从文本中提取HTTP方法
     */
    private static String extractHttpMethod(String text, int jsonPos) {
        // 在JSON前面查找HTTP方法（向前查找200个字符）
        int searchStart = Math.max(0, jsonPos - 200);
        String context = text.substring(searchStart, jsonPos);

        Pattern methodPattern = Pattern.compile("\\b(GET|POST|PUT|DELETE|PATCH)\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = methodPattern.matcher(context);

        String lastMethod = null;
        while (matcher.find()) {
            lastMethod = matcher.group(1).toUpperCase();
        }

        return lastMethod != null ? lastMethod : "POST"; // 默认POST
    }

    /**
     * 从文本中提取URL
     */
    private static String extractUrl(String text, int jsonPos) {
        // 在JSON前面查找URL（向前查找300个字符）
        int searchStart = Math.max(0, jsonPos - 300);
        String context = text.substring(searchStart, jsonPos);

        // 匹配 http://... 或 https://...
        Pattern urlPattern = Pattern.compile("(https?://[^\\s'\"\\)\\]]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(context);

        String lastUrl = null;
        while (matcher.find()) {
            lastUrl = matcher.group(1);
        }

        return lastUrl;
    }

    // ==================== 辅助正则（仅用于特定信息提取） ====================

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

    // ✅ 匹配API接口路径（Controller层）
    private static final Pattern API_PATH_PATTERN = Pattern.compile(
        "(?:API|uri)\\s*[:：]\\s*(/[^\\s,，;；\\)）}]+)",
        Pattern.CASE_INSENSITIVE
    );

    // ✅ 匹配调用ES的Java类
    private static final Pattern CALLER_CLASS_PATTERN = Pattern.compile(
        "\\(([A-Z][a-zA-Z0-9]+\\.java:\\d+)\\)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 判断文本是否包含 ES DSL 查询
     */
    public static boolean containsEsDsl(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 快速检查：是否包含 Elasticsearch 相关关键词
        String lowerText = text.toLowerCase();
        boolean hasEsKeyword = lowerText.contains("elasticsearch") ||
                               lowerText.contains("_search") ||
                               lowerText.contains("requestlogger") ||
                               lowerText.contains("curl");

        if (!hasEsKeyword) {
            return false;
        }

        // 深度检查：提取JSON并验证
        List<JsonBlock> jsonBlocks = extractJsonBlocks(text);
        for (JsonBlock block : jsonBlocks) {
            if (isEsDsl(block.content)) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * 解析 ES DSL 查询 - 智能版
     * 采用多阶段策略：JSON提取 -> 语义验证 -> 上下文关联
     */
    public static EsDslRecord parseEsDsl(String text, String projectName) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            // 第一阶段：提取所有JSON块
            List<JsonBlock> jsonBlocks = extractJsonBlocks(text);
            if (jsonBlocks.isEmpty()) {
                return null;
            }

            // 第二阶段：找到第一个ES DSL
            JsonBlock esDslBlock = null;
            for (JsonBlock block : jsonBlocks) {
                if (isEsDsl(block.content)) {
                    esDslBlock = block;
                    break;
                }
            }

            if (esDslBlock == null) {
                return null;
            }

            // 第三阶段：构建记录
            EsDslRecord.Builder builder = EsDslRecord.builder()
                .project(projectName)
                .dslQuery(formatJson(esDslBlock.content));

            // 提取HTTP方法
            String method = extractHttpMethod(text, esDslBlock.startPos);
            builder.method(method);

            // 提取URL
            String url = extractUrl(text, esDslBlock.startPos);
            if (url != null) {
                String[] urlParts = extractUrlParts(url);
                if (urlParts != null && urlParts.length >= 2) {
                    builder.index(urlParts[0]);
                    builder.endpoint(urlParts[1]);
                }
            }

            // 判断来源
            String source = detectSource(text);
            builder.source(source);

            // 提取其他信息
            extractHttpStatus(text, builder);
            extractExecutionTime(text, builder);
            extractApiPath(text, builder);
            extractCallerClass(text, builder);

            // 如果没有状态码，默认为 200
            EsDslRecord tempRecord = builder.build();
            if (tempRecord.getHttpStatus() == null) {
                builder.httpStatus(200);
            }

            return builder.build();

        } catch (Exception e) {
            System.err.println("[ES DSL Parser] 解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检测日志来源
     */
    private static String detectSource(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("requestlogger")) {
            return "RequestLogger";
        } else if (lower.contains("spring") && lower.contains("elasticsearch")) {
            return "Spring Data Elasticsearch";
        } else if (lower.contains("restclient")) {
            return "RestClient";
        } else if (lower.contains("curl")) {
            return "cURL";
        } else {
            return "Unknown";
        }
    }

    // ==================== 辅助方法：信息提取 ====================

    /**
     * 提取执行时间
     */
    private static void extractExecutionTime(String text, EsDslRecord.Builder builder) {
        Matcher timeMatcher = EXECUTION_TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            try {
                long executionTime = Long.parseLong(timeMatcher.group(1));
                builder.executionTime(executionTime);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
    }
    
    /**
     * 提取 HTTP 状态码
     */
    private static void extractHttpStatus(String text, EsDslRecord.Builder builder) {
        try {
            // 尝试多种状态码格式
            Pattern statusPattern1 = Pattern.compile("#\\s*HTTP/\\d\\.\\d\\s+(\\d{3})");
            Pattern statusPattern2 = Pattern.compile("returned\\s+\\[HTTP/\\d\\.\\d\\s+(\\d{3})");
            Pattern statusPattern3 = Pattern.compile("status[:\\s]+(\\d{3})", Pattern.CASE_INSENSITIVE);

            Matcher matcher = statusPattern1.matcher(text);
            if (!matcher.find()) {
                matcher = statusPattern2.matcher(text);
            }
            if (!matcher.find()) {
                matcher = statusPattern3.matcher(text);
            }

            if (matcher.find()) {
                builder.httpStatus(Integer.parseInt(matcher.group(1)));
            }
        } catch (Exception e) {
            // 忽略提取失败
        }
    }

    /**
     * 提取API路径
     */
    private static void extractApiPath(String text, EsDslRecord.Builder builder) {
        Matcher matcher = API_PATH_PATTERN.matcher(text);
        if (matcher.find()) {
            builder.apiPath(matcher.group(1));
        }
    }

    /**
     * 提取调用类
     */
    private static void extractCallerClass(String text, EsDslRecord.Builder builder) {
        Matcher matcher = CALLER_CLASS_PATTERN.matcher(text);
        String lastMatch = null;
        String lastRelevantMatch = null;

        while (matcher.find()) {
            String className = matcher.group(1);
            lastMatch = className;

            // 优先选择ES相关的类
            String lowerClass = className.toLowerCase();
            if (lowerClass.contains("elastic") ||
                lowerClass.contains("vector") ||
                lowerClass.contains("retriev")) {
                lastRelevantMatch = className;
            }
        }

        String result = lastRelevantMatch != null ? lastRelevantMatch : lastMatch;
        if (result != null) {
            builder.callerClass(result);
        }
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

    // ==================== 公共方法（供测试使用） ====================

    /**
     * 提取API接口路径（公共方法）
     */
    public static String extractApiPath(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Matcher matcher = API_PATH_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 提取调用ES的Java类（公共方法）
     */
    public static String extractCallerClass(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Matcher matcher = CALLER_CLASS_PATTERN.matcher(text);
        String lastMatch = null;
        String lastRelevantMatch = null;

        while (matcher.find()) {
            String className = matcher.group(1);
            lastMatch = className;

            // 优先选择ES相关的类
            String lowerClass = className.toLowerCase();
            if (lowerClass.contains("elastic") ||
                lowerClass.contains("vector") ||
                lowerClass.contains("retriev")) {
                lastRelevantMatch = className;
            }
        }

        return lastRelevantMatch != null ? lastRelevantMatch : lastMatch;
    }
}
