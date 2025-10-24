import com.shuyixiao.sql.parser.SqlParser
import com.shuyixiao.sql.model.SqlRecord
import java.nio.file.Files
import java.nio.file.Paths

println "========================================"
println "开始测试：解析日志.txt中的大JSON SQL"
println "========================================"

try {
    // 读取日志文件
    def logFile = new File("日志.txt")
    if (!logFile.exists()) {
        println "错误：日志.txt文件不存在"
        return
    }
    
    def logContent = logFile.text
    println "日志文件大小: ${logContent.length() / 1024} KB"
    println ""
    
    // 解析SQL
    def record = SqlParser.parseSql(logContent, "TestProject")
    
    if (record == null) {
        println "❌ 解析失败：返回null"
        return
    }
    
    println "✅ 解析成功！"
    println ""
    
    // 输出详细信息
    println "=== SQL记录详情 ==="
    println "项目: ${record.project}"
    println "时间: ${record.formattedTimestamp}"
    println "来源: ${record.source}"
    println "操作类型: ${record.operation}"
    println "表名: ${record.tableName}"
    println "结果数: ${record.resultCount}"
    println "API路径: ${record.apiPath ?: 'N/A'}"
    println "调用类: ${record.callerClass ?: 'N/A'}"
    println ""
    
    // 输出原始SQL
    println "=== 原始SQL（带?占位符）==="
    def sqlStatement = record.sqlStatement
    println sqlStatement
    println ""
    println "SQL长度: ${sqlStatement.length()} 字符"
    println ""
    
    // 输出参数
    println "=== 参数 ==="
    def parameters = record.parameters
    println "参数长度: ${parameters.length()} 字符"
    println ""
    
    // 显示参数前500字符
    def paramPreview = parameters.length() > 500 ? 
        parameters.substring(0, 500) + "..." : parameters
    println "参数预览（前500字符）:"
    println paramPreview
    println ""
    
    // 获取可执行SQL
    println "=== 可执行SQL（参数已替换）==="
    def executableSql = record.executableSql
    println "可执行SQL长度: ${executableSql.length()} 字符"
    println ""
    
    // 显示可执行SQL的前1000字符
    def sqlPreview = executableSql.length() > 1000 ? 
        executableSql.substring(0, 1000) + "..." : executableSql
    println "可执行SQL预览（前1000字符）:"
    println sqlPreview
    println ""
    
    // 验证结果
    println "========================================"
    println "验证结果:"
    println "========================================"
    
    def allPassed = true
    
    if (record.operation != "UPDATE") {
        println "❌ 操作类型错误: ${record.operation}"
        allPassed = false
    } else {
        println "✅ 操作类型正确: UPDATE"
    }
    
    if (record.tableName != "saas_prompt_template") {
        println "❌ 表名错误: ${record.tableName}"
        allPassed = false
    } else {
        println "✅ 表名正确: saas_prompt_template"
    }
    
    if (parameters.length() < 1000) {
        println "❌ 参数太短，可能没有正确提取大JSON"
        allPassed = false
    } else {
        println "✅ 参数长度正常: ${parameters.length()} 字符"
    }
    
    if (executableSql.contains("?")) {
        println "❌ 可执行SQL仍包含?占位符"
        allPassed = false
    } else {
        println "✅ 可执行SQL已正确替换所有占位符"
    }
    
    if (executableSql.length() <= sqlStatement.length()) {
        println "❌ 可执行SQL长度异常"
        allPassed = false
    } else {
        println "✅ 可执行SQL长度正常（参数已替换）"
    }
    
    println ""
    if (allPassed) {
        println "========================================"
        println "🎉 所有测试通过！"
        println "========================================"
        
        // 保存可执行SQL到文件
        def outputFile = new File("test-executable-sql.txt")
        outputFile.text = executableSql
        println ""
        println "✅ 可执行SQL已保存到: test-executable-sql.txt"
    } else {
        println "========================================"
        println "❌ 部分测试失败"
        println "========================================"
    }
    
} catch (Exception e) {
    println "❌ 测试失败: ${e.message}"
    e.printStackTrace()
}

