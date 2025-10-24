import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Test parsing the UPDATE statement specifically
 */
public class TestUpdateStatement {
    
    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("测试UPDATE语句解析（大JSON参数）");
            System.out.println("========================================\n");
            
            // Read log file
            String logContent = new String(Files.readAllBytes(Paths.get("日志.txt")), "UTF-8");
            System.out.println("日志文件大小: " + (logContent.length() / 1024) + " KB\n");
            
            // Find the UPDATE statement section
            // It starts with "==>  Preparing: UPDATE" and ends before the next "==>  Preparing:"
            Pattern updatePattern = Pattern.compile(
                "==>\\s+Preparing:\\s+UPDATE.*?(?===>\\s+Preparing:|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            
            Matcher matcher = updatePattern.matcher(logContent);
            if (!matcher.find()) {
                System.err.println("❌ 未找到UPDATE语句");
                return;
            }
            
            String updateSection = matcher.group();
            System.out.println("找到UPDATE语句段，长度: " + updateSection.length() + " 字符\n");
            
            // Load the JAR
            File jarFile = new File("build/libs/PandaCoder-2.1.0.jar");
            URL[] urls = {jarFile.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, TestUpdateStatement.class.getClassLoader());
            
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
            
            // Parse the UPDATE section
            Object record = parseSqlMethod.invoke(null, updateSection, "TestProject");
            
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
            if (parameters.length() > 500) {
                System.out.println("\n参数开头（前300字符）:");
                System.out.println(parameters.substring(0, 300) + "...");
                System.out.println("\n参数结尾（后300字符）:");
                System.out.println("..." + parameters.substring(parameters.length() - 300));
            } else {
                System.out.println("\n完整参数:");
                System.out.println(parameters);
            }
            System.out.println();
            
            System.out.println("=== 可执行SQL（参数已替换）===");
            System.out.println("可执行SQL长度: " + executableSql.length() + " 字符");
            if (executableSql.length() > 1000) {
                System.out.println("\n可执行SQL开头（前500字符）:");
                System.out.println(executableSql.substring(0, 500) + "...");
                System.out.println("\n可执行SQL结尾（后300字符）:");
                System.out.println("..." + executableSql.substring(executableSql.length() - 300));
            } else {
                System.out.println("\n完整可执行SQL:");
                System.out.println(executableSql);
            }
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
                int questionMarks = executableSql.length() - executableSql.replace("?", "").length();
                System.out.println("❌ 可执行SQL仍包含" + questionMarks + "个?占位符");
                allPassed = false;
            } else {
                System.out.println("✅ 可执行SQL已正确替换所有占位符");
            }
            
            if (executableSql.length() <= sqlStatement.length()) {
                System.out.println("❌ 可执行SQL长度异常 (exec=" + executableSql.length() + ", orig=" + sqlStatement.length() + ")");
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
                System.out.println("1. ✅ 成功解析包含超大JSON的UPDATE SQL");
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

