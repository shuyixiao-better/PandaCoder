package com.shuyixiao.sql.parser;

import com.shuyixiao.sql.model.SqlRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 解析器
 * 从控制台输出中解析 SQL 查询日志
 * 支持 MyBatis、JPA 等框架的日志格式
 */
public class SqlParser {
    
    // 匹配 MyBatis 的 Preparing 日志
    // 格式: ==>  Preparing: SELECT ... FROM table_name WHERE ...
    private static final Pattern PREPARING_PATTERN = Pattern.compile(
        "==>\\s+Preparing:\\s+(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配 MyBatis 的 Parameters 日志
    // 格式: ==> Parameters: value1(Type1), value2(Type2)
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile(
        "==>\\s+Parameters:\\s*([^\\n\\r]*)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配 MyBatis 的 Total 日志
    // 格式: <==      Total: 123
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "<==\\s+Total:\\s+(\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配SQL操作类型
    private static final Pattern OPERATION_PATTERN = Pattern.compile(
        "^\\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|TRUNCATE)\\s+",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配表名
    private static final Pattern TABLE_FROM_PATTERN = Pattern.compile(
        "FROM\\s+([\\w_]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABLE_INTO_PATTERN = Pattern.compile(
        "INTO\\s+([\\w_]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABLE_UPDATE_PATTERN = Pattern.compile(
        "UPDATE\\s+([\\w_]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // API路径匹配
    private static final Pattern API_PATH_PATTERN = Pattern.compile(
        "(?:API|uri)\\s*[:：]\\s*(/[^\\s,，;；\\)）}]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 调用类匹配
    private static final Pattern CALLER_CLASS_PATTERN = Pattern.compile(
        "\\(([A-Z][a-zA-Z0-9]+\\.java:\\d+)\\)"
    );
    
    /**
     * 判断文本是否包含 SQL 日志
     */
    public static boolean containsSql(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        return lowerText.contains("preparing:") || 
               lowerText.contains("parameters:") || 
               lowerText.contains("total:") ||
               lowerText.contains("basejdbclogger");
    }
    
    /**
     * 解析 SQL 日志
     */
    public static SqlRecord parseSql(String text, String projectName) {
        if (!containsSql(text)) {
            return null;
        }
        
        try {
            SqlRecord.Builder builder = SqlRecord.builder().project(projectName);
            
            // 1. 提取 SQL 语句
            Matcher preparingMatcher = PREPARING_PATTERN.matcher(text);
            String sqlStatement = null;
            String parameters = null;
            
            if (preparingMatcher.find()) {
                sqlStatement = preparingMatcher.group(1).trim();
                builder.sqlStatement(sqlStatement);
                builder.source("MyBatis");
                
                // 提取操作类型
                String operation = extractOperation(sqlStatement);
                if (operation != null) {
                    builder.operation(operation);
                }
                
                // 提取表名
                String tableName = extractTableName(sqlStatement, operation);
                if (tableName != null) {
                    builder.tableName(tableName);
                }
            } else {
                return null; // 没有SQL语句
            }
            
            // 2. 提取参数
            Matcher parametersMatcher = PARAMETERS_PATTERN.matcher(text);
            if (parametersMatcher.find()) {
                parameters = parametersMatcher.group(1).trim();
                if (!parameters.isEmpty()) {
                    builder.parameters(parameters);
                }
            }
            
            // 3. 提取结果数量
            Matcher totalMatcher = TOTAL_PATTERN.matcher(text);
            if (totalMatcher.find()) {
                try {
                    int total = Integer.parseInt(totalMatcher.group(1));
                    builder.resultCount(total);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            
            // 4. 提取API路径
            String apiPath = extractApiPath(text);
            if (apiPath != null) {
                builder.apiPath(apiPath);
            }
            
            // 5. 提取调用类
            String callerClass = extractCallerClass(text);
            if (callerClass != null) {
                builder.callerClass(callerClass);
            }
            
            return builder.build();
            
        } catch (Exception e) {
            // 解析失败时返回 null
            return null;
        }
    }
    
    /**
     * 提取SQL操作类型
     */
    private static String extractOperation(String sql) {
        if (sql == null) return null;
        
        Matcher matcher = OPERATION_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }
    
    /**
     * 提取表名
     */
    private static String extractTableName(String sql, String operation) {
        if (sql == null) return null;
        
        Matcher matcher = null;
        
        if ("SELECT".equalsIgnoreCase(operation) || "DELETE".equalsIgnoreCase(operation)) {
            matcher = TABLE_FROM_PATTERN.matcher(sql);
        } else if ("INSERT".equalsIgnoreCase(operation)) {
            matcher = TABLE_INTO_PATTERN.matcher(sql);
        } else if ("UPDATE".equalsIgnoreCase(operation)) {
            matcher = TABLE_UPDATE_PATTERN.matcher(sql);
        }
        
        if (matcher != null && matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 提取API接口路径
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
     * 提取调用SQL的Java类
     */
    public static String extractCallerClass(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        Matcher matcher = CALLER_CLASS_PATTERN.matcher(text);
        String lastMatch = null;
        
        while (matcher.find()) {
            String className = matcher.group(1);
            lastMatch = className;
            
            // 跳过BaseJdbcLogger自身
            if (!className.toLowerCase().contains("basejdbclogger")) {
                return className;
            }
        }
        
        return lastMatch;
    }
}

