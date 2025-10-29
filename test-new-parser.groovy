import com.shuyixiao.esdsl.model.EsDslRecord
import com.shuyixiao.esdsl.parser.EsDslParser

// 测试日志（来自用户提供的日志.txt）
def testLog = """2025-10-29 21:01:48,008 INFO (VectorDataRetrieverElastic.java:449)- 分页获取chunk查询结果,tenantId:1943230203698479104,dims:1536,page:1,size:12
2025-10-29 21:01:48,008 INFO (VectorAssistant.java:50)- sharding bean time is 2024-09-04T00:00
2025-10-29 21:01:48,008 INFO (VectorAssistant.java:68)- sharding vector bean time is 2024-09-18T00:00
2025-10-29 21:01:48,008 INFO (VectorDataRetrieverElastic.java:454)- page-collectName-chunk-name:dataset_chunk_sharding_24_1536
2025-10-29 21:01:48,009 INFO (VectorAssistant.java:50)- sharding bean time is 2024-09-04T00:00
2025-10-29 21:01:48,061 DEBUG (RequestLogger.java:58)- request [POST http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch] returned [HTTP/1.1 200 OK]
2025-10-29 21:01:48,063 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{"from":0,"query":{"bool":{"must":[{"term":{"tenantId":{"value":"1943230203698479104"}}},{"term":{"containerId":{"value":"1983456750321328128"}}},{"term":{"dataId":{"value":"1983456866025398272"}}}]}},"size":12,"sort":[{"page":{"mode":"min","order":"asc"}}],"track_scores":false,"version":true}'
# HTTP/1.1 200 OK
# X-elastic-product: Elasticsearch
# content-type: application/vnd.elasticsearch+json;compatible-with=8
"""

println "========================================"
println "测试新的智能解析器"
println "========================================\n"

// 测试1：检查是否包含ES DSL
boolean containsDsl = EsDslParser.containsEsDsl(testLog)
println "✅ 测试1 - 检测ES DSL: ${containsDsl ? '成功' : '失败'}"

// 测试2：解析ES DSL
EsDslRecord record = EsDslParser.parseEsDsl(testLog, "TestProject")

if (record != null) {
    println "\n✅ 测试2 - 解析成功！"
    println "========================================"
    println "解析结果:"
    println "----------------------------------------"
    println "项目名称: ${record.project}"
    println "HTTP方法: ${record.method}"
    println "索引名称: ${record.index}"
    println "端点路径: ${record.endpoint}"
    println "HTTP状态: ${record.httpStatus}"
    println "来源类型: ${record.source}"
    println "调用类: ${record.callerClass}"
    println "----------------------------------------"
    println "DSL查询 (前200字符):"
    def dsl = record.dslQuery
    if (dsl != null) {
        println dsl.substring(0, Math.min(200, dsl.length()))
        if (dsl.length() > 200) {
            println "... (总长度: ${dsl.length()} 字符)"
        }
    }
    println "========================================"
    
    // 验证关键字段
    boolean allFieldsCorrect = true
    
    if (record.method != "POST") {
        println "❌ HTTP方法错误: 期望 POST, 实际 ${record.method}"
        allFieldsCorrect = false
    }
    
    if (record.index != "dataset_chunk_sharding_24_1536") {
        println "❌ 索引名称错误: 期望 dataset_chunk_sharding_24_1536, 实际 ${record.index}"
        allFieldsCorrect = false
    }
    
    if (record.httpStatus == null || record.httpStatus != 200) {
        println "❌ HTTP状态码错误: 期望 200, 实际 ${record.httpStatus}"
        allFieldsCorrect = false
    }
    
    if (dsl == null || !dsl.contains('"query"') || !dsl.contains('"bool"')) {
        println "❌ DSL内容错误: 缺少关键字段"
        allFieldsCorrect = false
    }
    
    if (allFieldsCorrect) {
        println "\n🎉 所有字段验证通过！"
    } else {
        println "\n⚠️ 部分字段验证失败"
    }
    
} else {
    println "\n❌ 测试2 - 解析失败！"
    println "无法从日志中提取ES DSL"
}

