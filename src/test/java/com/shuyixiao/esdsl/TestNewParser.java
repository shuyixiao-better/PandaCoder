package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.model.EsDslRecord;
import com.shuyixiao.esdsl.parser.EsDslParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * 测试新的智能解析器
 */
public class TestNewParser {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("测试新的智能解析器");
        System.out.println("========================================\n");

        // 测试1：从文件读取日志
        testFromFile();

        // 测试2：测试内嵌的日志字符串
        testEmbeddedLog();
    }

    private static void testFromFile() {
        System.out.println("【测试1】从日志文件读取");
        System.out.println("----------------------------------------");

        String logFile = "日志.txt";
        StringBuilder buffer = new StringBuilder();
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                buffer.append(line).append("\n");

                // 当遇到TRACE日志时，尝试解析
                if (line.contains("TRACE") && line.contains("RequestLogger") && line.contains("curl")) {
                    System.out.println("发现TRACE日志在行 " + lineNum);

                    // 读取后续几行（响应部分）
                    for (int i = 0; i < 5; i++) {
                        String nextLine = reader.readLine();
                        if (nextLine != null) {
                            buffer.append(nextLine).append("\n");
                            lineNum++;
                        }
                    }

                    String context = buffer.toString();

                    // 测试containsEsDsl
                    boolean contains = EsDslParser.containsEsDsl(context);
                    System.out.println("  containsEsDsl: " + contains);

                    // 测试parseEsDsl
                    EsDslRecord record = EsDslParser.parseEsDsl(context, "TestProject");

                    if (record != null) {
                        System.out.println("  ✅ 解析成功！");
                        printRecord(record);
                    } else {
                        System.out.println("  ❌ 解析失败");
                    }

                    break; // 只测试第一个
                }
            }
        } catch (Exception e) {
            System.out.println("❌ 读取文件失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    private static void testEmbeddedLog() {
        System.out.println("【测试2】测试内嵌日志字符串");
        System.out.println("----------------------------------------");

        // 测试日志（来自用户提供的日志.txt）- 包含请求DSL和响应数据
        String testLog = "2025-10-29 21:01:48,008 INFO (VectorDataRetrieverElastic.java:449)- 分页获取chunk查询结果,tenantId:1943230203698479104,dims:1536,page:1,size:12\n" +
            "2025-10-29 21:01:48,008 INFO (VectorAssistant.java:50)- sharding bean time is 2024-09-04T00:00\n" +
            "2025-10-29 21:01:48,008 INFO (VectorAssistant.java:68)- sharding vector bean time is 2024-09-18T00:00\n" +
            "2025-10-29 21:01:48,008 INFO (VectorDataRetrieverElastic.java:454)- page-collectName-chunk-name:dataset_chunk_sharding_24_1536\n" +
            "2025-10-29 21:01:48,009 INFO (VectorAssistant.java:50)- sharding bean time is 2024-09-04T00:00\n" +
            "2025-10-29 21:01:48,061 DEBUG (RequestLogger.java:58)- request [POST http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch] returned [HTTP/1.1 200 OK]\n" +
            "2025-10-29 21:01:48,063 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0,\"query\":{\"bool\":{\"must\":[{\"term\":{\"tenantId\":{\"value\":\"1943230203698479104\"}}},{\"term\":{\"containerId\":{\"value\":\"1983456750321328128\"}}},{\"term\":{\"dataId\":{\"value\":\"1983456866025398272\"}}}]}},\"size\":12,\"sort\":[{\"page\":{\"mode\":\"min\",\"order\":\"asc\"}}],\"track_scores\":false,\"version\":true}'\n" +
            "# HTTP/1.1 200 OK\n" +
            "# X-elastic-product: Elasticsearch\n" +
            "# content-type: application/vnd.elasticsearch+json;compatible-with=8\n" +
            "# {\"took\":6,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1},\"hits\":{\"total\":{\"value\":16},\"max_score\":null,\"hits\":[{\"_index\":\"dataset_chunk_sharding_24_1536\",\"_id\":\"1983456948166647808\",\"_score\":null}]}}\n";

        // 测试containsEsDsl
        boolean containsDsl = EsDslParser.containsEsDsl(testLog);
        System.out.println("containsEsDsl: " + containsDsl);

        // 测试parseEsDsl
        EsDslRecord record = EsDslParser.parseEsDsl(testLog, "TestProject");

        if (record != null) {
            System.out.println("✅ 解析成功！");
            printRecord(record);

            // 验证关键字段
            verifyRecord(record);
        } else {
            System.out.println("❌ 解析失败");
        }

        System.out.println();
    }

    private static void printRecord(EsDslRecord record) {
        System.out.println("----------------------------------------");
        System.out.println("项目名称: " + record.getProject());
        System.out.println("HTTP方法: " + record.getMethod());
        System.out.println("索引名称: " + record.getIndex());
        System.out.println("端点路径: " + record.getEndpoint());
        System.out.println("HTTP状态: " + record.getHttpStatus());
        System.out.println("来源类型: " + record.getSource());
        System.out.println("调用类: " + record.getCallerClass());
        System.out.println("----------------------------------------");
        System.out.println("DSL查询 (前200字符):");
        String dsl = record.getDslQuery();
        if (dsl != null) {
            System.out.println(dsl.substring(0, Math.min(200, dsl.length())));
            if (dsl.length() > 200) {
                System.out.println("... (总长度: " + dsl.length() + " 字符)");
            }
        }
        System.out.println("----------------------------------------");
    }

    private static void verifyRecord(EsDslRecord record) {
        boolean allFieldsCorrect = true;

        if (!"POST".equals(record.getMethod())) {
            System.out.println("❌ HTTP方法错误: 期望 POST, 实际 " + record.getMethod());
            allFieldsCorrect = false;
        }

        if (!"dataset_chunk_sharding_24_1536".equals(record.getIndex())) {
            System.out.println("❌ 索引名称错误: 期望 dataset_chunk_sharding_24_1536, 实际 " + record.getIndex());
            allFieldsCorrect = false;
        }

        if (record.getHttpStatus() == null || record.getHttpStatus() != 200) {
            System.out.println("❌ HTTP状态码错误: 期望 200, 实际 " + record.getHttpStatus());
            allFieldsCorrect = false;
        }

        String dsl = record.getDslQuery();
        if (dsl == null || !dsl.contains("\"query\"") || !dsl.contains("\"bool\"")) {
            System.out.println("❌ DSL内容错误: 缺少关键字段");
            allFieldsCorrect = false;
        }

        // ✅ 关键验证：确保DSL不包含响应字段
        if (dsl != null && (dsl.contains("\"_index\"") || dsl.contains("\"_id\"") || dsl.contains("\"hits\""))) {
            System.out.println("❌ DSL包含响应字段: 提取了响应数据而不是请求DSL");
            System.out.println("   DSL内容: " + dsl.substring(0, Math.min(100, dsl.length())));
            allFieldsCorrect = false;
        }

        if (allFieldsCorrect) {
            System.out.println("🎉 所有字段验证通过！");
        } else {
            System.out.println("⚠️ 部分字段验证失败");
        }
    }
}

