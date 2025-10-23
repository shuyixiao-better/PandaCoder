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
    // 支持多行参数（包含大JSON）
    // 匹配从 "==> Parameters:" 开始，到下一个日志行（以时间戳开头）或 "<==" 或文件末尾
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile(
        "==>\\s+Parameters:\\s*([\\s\\S]*?)(?=\\n\\d{4}-\\d{2}-\\d{2}.*?<==|$)",
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
    
    /**
     * 替换SQL中的参数，生成可执行的SQL
     * 将 MyBatis 的占位符(?)替换为实际参数值
     * 
     * @param sqlStatement 原始SQL语句（带?占位符）
     * @param parametersStr 参数字符串，格式: value1(Type1), value2(Type2), ...
     * @return 可执行的SQL语句
     */
    public static String replaceParameters(String sqlStatement, String parametersStr) {
        if (sqlStatement == null || parametersStr == null || parametersStr.trim().isEmpty()) {
            return sqlStatement;
        }
        
        try {
            // 解析参数字符串
            // 格式: value1(Type1), value2(Type2), ...
            // 例如: 2025-10-21T10:36:34(LocalDateTime), DEFAULT(String), {...}(String), ...
            // 需要处理包含逗号和括号的复杂值（如JSON）
            
            java.util.List<String> paramValues = new java.util.ArrayList<>();
            StringBuilder currentValue = new StringBuilder();
            StringBuilder currentType = new StringBuilder();
            int bracketDepth = 0;
            int curlyBraceDepth = 0;
            boolean inQuotes = false;
            boolean inType = false;  // 是否在类型括号内
            
            // 逐字符解析，正确处理嵌套的括号、大括号和逗号
            for (int i = 0; i < parametersStr.length(); i++) {
                char c = parametersStr.charAt(i);
                
                if (c == '"' || c == '\'') {
                    // 处理引号
                    inQuotes = !inQuotes;
                    if (!inType) {
                        currentValue.append(c);
                    }
                } else if (!inQuotes) {
                    if (c == '{') {
                        curlyBraceDepth++;
                        if (!inType) {
                            currentValue.append(c);
                        }
                    } else if (c == '}') {
                        curlyBraceDepth--;
                        if (!inType) {
                            currentValue.append(c);
                        }
                    } else if (c == '(' && curlyBraceDepth == 0) {
                        // 类型括号开始
                        bracketDepth++;
                        inType = true;
                    } else if (c == ')' && curlyBraceDepth == 0 && inType) {
                        // 类型括号结束
                        bracketDepth--;
                        if (bracketDepth == 0) {
                            inType = false;
                            // 提取参数值（忽略类型）
                            String paramValue = currentValue.toString().trim();
                            if (!paramValue.isEmpty()) {
                                paramValues.add(paramValue);
                            }
                            currentValue.setLength(0);
                            currentType.setLength(0);
                        }
                    } else if (c == ',' && bracketDepth == 0 && curlyBraceDepth == 0) {
                        // 参数之间的分隔符，跳过空白
                        // 下一个参数即将开始
                        continue;
                    } else if (inType) {
                        // 在类型括号内，记录类型（但我们不使用它）
                        currentType.append(c);
                    } else {
                        // 记录参数值
                        currentValue.append(c);
                    }
                } else {
                    // 在引号内
                    if (!inType) {
                        currentValue.append(c);
                    }
                }
            }
            
            // 如果解析失败或没有参数，返回原始SQL
            if (paramValues.isEmpty()) {
                return sqlStatement;
            }
            
            // 替换SQL中的占位符
            StringBuilder result = new StringBuilder();
            int paramIndex = 0;
            boolean inSqlQuotes = false;
            
            for (int i = 0; i < sqlStatement.length(); i++) {
                char c = sqlStatement.charAt(i);
                
                // 跟踪SQL中的引号，避免替换字符串内的?
                if (c == '\'') {
                    inSqlQuotes = !inSqlQuotes;
                    result.append(c);
                } else if (c == '?' && !inSqlQuotes && paramIndex < paramValues.size()) {
                    // 找到占位符，替换为参数值
                    String paramValue = paramValues.get(paramIndex++);
                    
                    // 根据参数值的类型决定是否添加引号
                    if (needsQuotes(paramValue)) {
                        result.append('\'');
                        // 转义单引号
                        result.append(paramValue.replace("'", "''"));
                        result.append('\'');
                    } else {
                        result.append(paramValue);
                    }
                } else {
                    result.append(c);
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            // 解析失败，返回原始SQL
            return sqlStatement;
        }
    }
    
    /**
     * 判断参数值是否需要添加引号
     * 数字和布尔值不需要，字符串需要
     */
    private static boolean needsQuotes(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        // 检查是否是数字（整数或小数）
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return false;
        }
        
        // 检查是否是布尔值
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return false;
        }
        
        // 检查是否是null
        if ("null".equalsIgnoreCase(value)) {
            return false;
        }
        
        // 其他情况都需要引号
        return true;
    }
}

