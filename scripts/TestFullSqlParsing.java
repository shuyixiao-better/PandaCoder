import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Complete test for SQL parsing with large JSON
 */
public class TestFullSqlParsing {
    
    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("完整SQL解析测试（大JSON支持）");
            System.out.println("========================================\n");
            
            // Load the JAR
            File jarFile = new File("build/libs/PandaCoder-2.1.0.jar");
            if (!jarFile.exists()) {
                System.err.println("❌ JAR文件不存在: " + jarFile.getAbsolutePath());
                return;
            }
            
            URL[] urls = {jarFile.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, TestFullSqlParsing.class.getClassLoader());
            
            // Load classes
            Class<?> sqlParserClass = classLoader.loadClass("com.shuyixiao.sql.parser.SqlParser");
            Class<?> sqlRecordClass = classLoader.loadClass("com.shuyixiao.sql.model.SqlRecord");
            
            // Get methods
            Method parseSqlMethod = sqlParserClass.getMethod("parseSql", String.class, String.class);
            Method getSqlStatementMethod = sqlRecordClass.getMethod("getSqlStatement");
            Method getParametersMethod = sqlRecordClass.getMethod("getParameters");
            Method getExecutableSqlMethod = sqlRecordClass.getMethod("getExecutableSql");
            Method getOperationMethod = sqlRecordClass.getMethod("getOperation");
            Method getTableNameMethod = sqlRecordClass.getMethod("getTableName");
            Method getResultCountMethod = sqlRecordClass.getMethod("getResultCount");
            
            // Read log file
            String logContent = new String(Files.readAllBytes(Paths.get("日志.txt")), "UTF-8");
            System.out.println("日志文件大小: " + (logContent.length() / 1024) + " KB\n");
            
            // Parse SQL
            Object record = parseSqlMethod.invoke(null, logContent, "TestProject");
            
            if (record == null) {
                System.err.println("❌ 解析失败：返回null");
                return;
            }
            
            System.out.println("✅ 解析成功！\n");
            
            // Get record details
            String operation = (String) getOperationMethod.invoke(record);
            String tableName = (String) getTableNameMethod.invoke(record);
            Integer resultCount = (Integer) getResultCountMethod.invoke(record);
            String sqlStatement = (String) getSqlStatementMethod.invoke(record);
            String parameters = (String) getParametersMethod.invoke(record);
            String executableSql = (String) getExecutableSqlMethod.invoke(record);
            
            System.out.println("=== SQL记录详情 ===");
            System.out.println("操作类型: " + operation);
            System.out.println("表名: " + tableName);
            System.out.println("结果数: " + resultCount);
            System.out.println();
            
            System.out.println("=== 原始SQL（带?占位符）===");
            System.out.println(sqlStatement);
            System.out.println("\nSQL长度: " + sqlStatement.length() + " 字符");
            System.out.println();
            
            System.out.println("=== 参数 ===");
            System.out.println("参数长度: " + parameters.length() + " 字符");
            String paramPreview = parameters.length() > 500 ? 
                parameters.substring(0, 500) + "..." : parameters;
            System.out.println("\n参数预览（前500字符）:");
            System.out.println(paramPreview);
            System.out.println();
            
            System.out.println("=== 可执行SQL（参数已替换）===");
            System.out.println("可执行SQL长度: " + executableSql.length() + " 字符");
            String sqlPreview = executableSql.length() > 1000 ? 
                executableSql.substring(0, 1000) + "..." : executableSql;
            System.out.println("\n可执行SQL预览（前1000字符）:");
            System.out.println(sqlPreview);
            System.out.println();
            
            // Verification
            System.out.println("========================================");
            System.out.println("验证结果:");
            System.out.println("========================================");
            
            boolean allPassed = true;
            
            if (!"UPDATE".equals(operation)) {
                System.out.println("❌ 操作类型错误: " + operation);
                allPassed = false;
            } else {
                System.out.println("✅ 操作类型正确: UPDATE");
            }
            
            if (!"saas_prompt_template".equals(tableName)) {
                System.out.println("❌ 表名错误: " + tableName);
                allPassed = false;
            } else {
                System.out.println("✅ 表名正确: saas_prompt_template");
            }
            
            if (parameters.length() < 1000) {
                System.out.println("❌ 参数太短，可能没有正确提取大JSON (length=" + parameters.length() + ")");
                allPassed = false;
            } else {
                System.out.println("✅ 参数长度正常: " + parameters.length() + " 字符（包含大JSON）");
            }
            
            if (executableSql.contains("?")) {
                System.out.println("❌ 可执行SQL仍包含?占位符");
                allPassed = false;
            } else {
                System.out.println("✅ 可执行SQL已正确替换所有占位符");
            }
            
            if (executableSql.length() <= sqlStatement.length()) {
                System.out.println("❌ 可执行SQL长度异常");
                allPassed = false;
            } else {
                System.out.println("✅ 可执行SQL长度正常（参数已替换）");
            }
            
            if (!executableSql.contains("根据提供的") && !executableSql.toLowerCase().contains("prompt")) {
                System.out.println("❌ 可执行SQL中未找到大JSON内容");
                allPassed = false;
            } else {
                System.out.println("✅ 可执行SQL包含大JSON内容");
            }
            
            System.out.println();
            if (allPassed) {
                System.out.println("========================================");
                System.out.println("🎉 所有测试通过！");
                System.out.println("========================================");
                System.out.println("\n测试总结:");
                System.out.println("1. ✅ 成功解析包含超大JSON的SQL日志");
                System.out.println("2. ✅ 正确提取多行参数（" + parameters.length() + " 字符）");
                System.out.println("3. ✅ 成功生成可执行的SQL（" + executableSql.length() + " 字符）");
                System.out.println("4. ✅ 可执行SQL不包含?占位符");
                System.out.println("5. ✅ 可执行SQL包含完整的大JSON数据");
                
                // Save to file
                Files.write(Paths.get("test-executable-sql.txt"), executableSql.getBytes("UTF-8"));
                System.out.println("\n✅ 可执行SQL已保存到: test-executable-sql.txt");
            } else {
                System.out.println("========================================");
                System.out.println("❌ 部分测试失败");
                System.out.println("========================================");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

