# ES DSL Monitor 测试验证指南 - 长 DSL 支持

## 🎯 目标

验证 ES DSL Monitor 优化后能够正确捕获和显示包含长向量数据的完整 ES 查询。

---

## 📋 测试前准备

### 1. 确认已应用优化

检查以下文件是否已更新:

✅ `src/main/java/com/shuyixiao/esdsl/parser/EsDslParser.java`
- 包含 `REQUEST_LOGGER_PATTERN`
- 包含 `TRACE_CURL_PATTERN`
- `MAX_BUFFER_SIZE` 未在此文件中

✅ `src/main/java/com/shuyixiao/esdsl/listener/EsDslOutputListener.java`
- `MAX_BUFFER_SIZE = 100000`
- `CROSS_LINE_RETAIN_SIZE = 5000`
- 包含 `countNewlines` 方法

### 2. 重新构建插件

```bash
# 在项目根目录执行
./gradlew clean buildPlugin

# 或者在 IDEA 中
# Gradle -> Tasks -> intellij -> buildPlugin
```

### 3. 安装插件

1. **卸载旧版本** (如果已安装)
   - File -> Settings -> Plugins
   - 找到 PandaCoder
   - 点击卸载

2. **安装新版本**
   - File -> Settings -> Plugins -> ⚙️ -> Install Plugin from Disk...
   - 选择 `build/distributions/PandaCoder-1.x.x.zip`
   - 重启 IDEA

### 4. 配置 ais 项目日志

#### 方式 1: 修改 logback-local.xml

在 `src/main/resources/logback-local.xml` 中添加:

```xml
<configuration>
    <!-- 控制台输出 -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss,SSS} %-5level (%F:%L)- %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- Elasticsearch 日志 - TRACE 级别 -->
    <logger name="tracer" level="TRACE" additivity="false">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <logger name="org.elasticsearch.client.RestClient" level="DEBUG" additivity="false">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

#### 方式 2: 修改 application-local.yml

```yaml
logging:
  level:
    root: INFO
    com.torchv: DEBUG
    # Elasticsearch TRACE 日志
    tracer: TRACE
    org.elasticsearch.client: DEBUG
    org.elasticsearch.client.RestClient: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5level (%F:%L)- %msg%n"
```

---

## 🧪 测试步骤

### 测试 1: 基础功能验证

**目标**: 验证 ES DSL Monitor 能够正常工作

**步骤:**

1. **打开 ES DSL Monitor 窗口**
   - View -> Tool Windows -> ES DSL Monitor
   - 或点击 IDEA 底部的 "ES DSL Monitor" 标签

2. **启动 ais 应用**
   - 运行 Application 主类
   - 观察控制台输出

3. **执行任意 ES 查询**
   - 访问应用的 API 接口
   - 触发 Elasticsearch 查询

4. **检查 ES DSL Monitor**
   - 应该能看到新的查询记录
   - 点击记录查看详情

**预期结果:**
```
✅ ES DSL Monitor 窗口正常打开
✅ 能够捕获到查询记录
✅ 查询详情能够显示
✅ 状态栏显示 "监听中"
```

**如果失败:**
- 检查插件是否正确安装
- 检查 ES DSL Monitor 是否启用(工具栏开关)
- 查看 IDEA 日志: Help -> Show Log in Finder/Explorer

### 测试 2: 短 DSL 捕获测试

**目标**: 验证基础的 DSL 捕获功能

**步骤:**

1. **准备测试查询**
   - 使用简单的 match_all 查询
   - 不包含向量数据

2. **执行查询**
   ```bash
   # 示例: 通过 Kibana 或 curl 执行
   POST http://your-es:9200/users/_search
   {
     "query": {
       "match_all": {}
     },
     "size": 10
   }
   ```

3. **检查控制台输出**
   - 应该看到 DEBUG 或 TRACE 日志
   - 包含完整的查询内容

4. **检查 ES DSL Monitor**
   - 查询应该被捕获
   - DSL 内容完整

**预期结果:**
```json
✅ 捕获的 DSL:
{
  "query": {
    "match_all": {}
  },
  "size": 10
}

✅ 元数据:
- 索引: users
- 方法: POST
- 端点: users/_search
- 来源: RequestLogger (TRACE) 或 cURL
```

### 测试 3: 长 DSL 捕获测试 (核心测试)

**目标**: 验证能够捕获包含向量数据的长 DSL

**步骤:**

1. **触发包含向量的查询**
   - 在 ais 应用中执行向量搜索
   - 例如: 知识库检索、文档搜索等

2. **观察控制台输出**
   
   **期望看到类似这样的日志:**
   ```
   2025-10-18 15:19:33,988 DEBUG (RequestLogger.java:58)- request [POST http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch] returned [HTTP/1.1 200 OK]
   
   2025-10-18 15:19:33,989 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{"from":0,"query":{"bool":{"must":[...完整DSL...]}},"size":12,"sort":[{"page":{"mode":"min","order":"asc"}}],"track_scores":false,"version":true}'
   # HTTP/1.1 200 OK
   # X-elastic-product: Elasticsearch
   # content-type: application/vnd.elasticsearch+json;compatible-with=8
   # Transfer-Encoding: chunked
   #
   # {"took":2,"timed_out":false,...}
   ```

3. **检查 ES DSL Monitor**
   - 查询应该被捕获
   - DSL 内容**完整**,不会截断

4. **验证 DSL 完整性**
   
   **检查清单:**
   ```
   ✅ 包含 "from" 字段
   ✅ 包含完整的 "query" 对象
   ✅ 包含所有的 "must" 条件
   ✅ 包含 "size" 字段
   ✅ 包含 "sort" 数组
   ✅ 包含 "track_scores" 字段
   ✅ 包含 "version" 字段
   ✅ JSON 格式正确,能够解析
   ```

5. **复制 DSL 到 Kibana 验证**
   - 在 ES DSL Monitor 中点击 "复制 DSL"
   - 粘贴到 Kibana Dev Tools
   - 执行查询
   - 应该能够正常执行,不报错

**预期结果:**

**优化前(会失败):**
```json
❌ 捕获的 DSL (截断):
{
  "from": 0,
  "query": {
    "bool": {
      "must": [
        {
          "term": {
            "tenantId": {
              "value": "1943230203698479104"
```

**优化后(应该成功):**
```json
✅ 捕获的 DSL (完整):
{
  "from": 0,
  "query": {
    "bool": {
      "must": [
        {
          "term": {
            "tenantId": {
              "value": "1943230203698479104"
            }
          }
        },
        {
          "term": {
            "containerId": {
              "value": "1978435131131686912"
            }
          }
        },
        {
          "term": {
            "dataId": {
              "value": "1978435256176472064"
            }
          }
        }
      ]
    }
  },
  "size": 12,
  "sort": [
    {
      "page": {
        "mode": "min",
        "order": "asc"
      }
    }
  ],
  "track_scores": false,
  "version": true
}

✅ 元数据:
- 索引: dataset_chunk_sharding_24_1536
- 方法: POST
- 端点: dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch
- 来源: RequestLogger (TRACE)
- 状态: 200 OK
- 执行时间: 2 ms
```

### 测试 4: Kibana 验证测试

**目标**: 验证捕获的 DSL 在 Kibana 中可以正常使用

**步骤:**

1. **复制 DSL**
   - 在 ES DSL Monitor 中找到刚才的查询
   - 点击 "复制 DSL" 按钮

2. **打开 Kibana Dev Tools**
   - 访问 http://your-kibana:5601/app/dev_tools#/console

3. **构造完整请求**
   ```
   POST /dataset_chunk_sharding_24_1536/_search
   {
     ... 粘贴复制的 DSL ...
   }
   ```

4. **执行查询**
   - 点击执行按钮 ▶️
   - 观察结果

**预期结果:**
```
✅ 查询执行成功
✅ 返回正确的结果
✅ 没有语法错误
✅ 结果与应用中的查询结果一致
```

**如果失败:**
- 检查 DSL 是否完整
- 检查索引名称是否正确
- 检查 JSON 格式是否正确

### 测试 5: 压力测试

**目标**: 验证在大量查询下插件的稳定性

**步骤:**

1. **连续执行多次查询**
   - 在 ais 应用中连续执行 20-50 次查询
   - 包括短 DSL 和长 DSL

2. **观察 ES DSL Monitor**
   - 所有查询都应该被捕获
   - 不应该有重复
   - 不应该有丢失

3. **检查 IDEA 性能**
   - CPU 使用率应该正常
   - 内存使用应该稳定
   - 不应该有明显的卡顿

**预期结果:**
```
✅ 所有查询都被捕获
✅ 没有重复记录
✅ 没有遗漏记录
✅ IDEA 运行流畅
✅ 内存使用正常 (增长 < 100MB)
```

---

## 🔍 验证清单

### 功能验证

- [ ] ES DSL Monitor 窗口正常显示
- [ ] 能够捕获短 DSL 查询
- [ ] **能够捕获长 DSL 查询(包含向量)**
- [ ] **捕获的 DSL 完整,不截断**
- [ ] 元数据提取正确(索引、方法、端点)
- [ ] 状态码识别正确
- [ ] 执行时间提取正确
- [ ] JSON 格式化正常
- [ ] 复制功能正常
- [ ] 导出功能正常
- [ ] 搜索和过滤功能正常

### 性能验证

- [ ] 捕获大量查询时性能正常
- [ ] 内存使用合理 (< 100MB 增长)
- [ ] CPU 使用率正常 (< 5%)
- [ ] 不影响应用性能
- [ ] IDEA 运行流畅

### 兼容性验证

- [ ] 支持 RequestLogger DEBUG 日志
- [ ] 支持 RequestLogger TRACE 日志
- [ ] 支持标准 REST 日志
- [ ] 支持 cURL 格式
- [ ] 支持 `-iX` 参数格式
- [ ] 支持完整 URL 解析
- [ ] 支持查询参数提取

---

## 🐛 常见问题

### 问题 1: 仍然捕获不完整

**症状:**
- DSL 仍然被截断
- 只能看到部分内容

**排查步骤:**

1. **检查日志级别**
   ```bash
   # 在控制台搜索
   TRACE (RequestLogger.java:90)
   ```
   - 如果找不到,说明 TRACE 日志未启用
   - 需要修改 logback 配置

2. **检查插件版本**
   ```
   File -> Settings -> Plugins -> PandaCoder
   ```
   - 确认是最新版本
   - 确认优化已应用

3. **查看 IDEA 日志**
   ```
   Help -> Show Log in Finder/Explorer
   ```
   - 搜索 "ES DSL"
   - 查看是否有错误信息

4. **手动验证缓冲区**
   - 在 `EsDslOutputListener.java` 中添加日志:
   ```java
   LOG.info("Buffer size: " + buffer.length() + ", Text length: " + text.length());
   ```

**解决方案:**

**方案 A: 检查配置**
```xml
<!-- 确认 logback-local.xml 中有以下配置 -->
<logger name="tracer" level="TRACE"/>
```

**方案 B: 重新构建插件**
```bash
./gradlew clean buildPlugin
# 重新安装插件
```

**方案 C: 增大缓冲区** (如果 DSL 超过 100K)
```java
// 在 EsDslOutputListener.java 中修改
private static final int MAX_BUFFER_SIZE = 200000; // 增加到 200K
```

### 问题 2: 捕获到重复查询

**症状:**
- 同一个查询出现多次
- ES DSL Monitor 中有重复记录

**原因:**
- 缓冲区清理不及时
- 同一个日志被解析多次

**解决方案:**

1. **检查清空逻辑**
   ```java
   // 在 EsDslOutputListener.java 中
   if (record != null) {
       recordService.addRecord(record);
       buffer.setLength(0);  // 确保这行代码存在
   }
   ```

2. **添加去重逻辑** (临时方案)
   - 在 `EsDslRecordService.java` 中添加:
   ```java
   public void addRecord(EsDslRecord record) {
       // 检查是否已存在相同的查询
       boolean exists = records.stream()
           .anyMatch(r -> r.getDslQuery().equals(record.getDslQuery()) 
                       && Math.abs(r.getTimestamp() - record.getTimestamp()) < 1000);
       
       if (!exists) {
           records.add(0, record);
           saveToFile();
       }
   }
   ```

### 问题 3: JSON 格式化失败

**症状:**
- DSL 显示为一行
- 没有缩进和换行

**原因:**
- JSON 包含特殊字符
- 格式化逻辑失败

**解决方案:**

**方案 A: 手动格式化**
- 复制 DSL 到在线 JSON 格式化工具
- 例如: https://jsonformatter.org/

**方案 B: 在 Kibana 中格式化**
- 直接粘贴到 Kibana Dev Tools
- Kibana 会自动格式化

**方案 C: 使用 Jackson 库** (未来优化)
```java
ObjectMapper mapper = new ObjectMapper();
String formatted = mapper.writerWithDefaultPrettyPrinter()
    .writeValueAsString(mapper.readValue(json, Object.class));
```

### 问题 4: 元数据提取不正确

**症状:**
- 索引名称为 "N/A"
- 端点为 "N/A"
- 方法不正确

**原因:**
- URL 解析失败
- 正则匹配不正确

**解决方案:**

1. **查看日志**
   ```java
   LOG.info("Parsed - Index: " + record.getIndex() 
          + ", Endpoint: " + record.getEndpoint()
          + ", Method: " + record.getMethod());
   ```

2. **检查 URL 格式**
   - 确认日志中的 URL 格式
   - 手动测试 `extractUrlParts` 方法

3. **调整正则表达式**
   - 根据实际日志格式调整
   - 添加更多的匹配模式

---

## 📊 测试报告模板

### 基本信息

- **测试日期**: 2025-10-18
- **测试人员**: [你的名字]
- **插件版本**: 1.2.0
- **IDEA 版本**: [你的 IDEA 版本]
- **项目**: ais-server

### 测试结果

| 测试项 | 状态 | 备注 |
|--------|------|------|
| 基础功能验证 | ✅ / ❌ | |
| 短 DSL 捕获 | ✅ / ❌ | |
| **长 DSL 捕获** | ✅ / ❌ | **核心测试** |
| Kibana 验证 | ✅ / ❌ | |
| 压力测试 | ✅ / ❌ | |
| 性能测试 | ✅ / ❌ | |

### 详细测试数据

**长 DSL 测试:**
- DSL 长度: [例如: 65,000 字符]
- 是否完整: ✅ / ❌
- 元数据正确: ✅ / ❌
- Kibana 可用: ✅ / ❌

**性能数据:**
- 内存增长: [例如: 50MB]
- CPU 使用: [例如: 2%]
- 响应时间: [例如: < 100ms]

### 问题记录

| 问题描述 | 严重程度 | 解决方案 | 状态 |
|----------|----------|----------|------|
| [问题1] | 高/中/低 | [解决方案] | 已解决/未解决 |

### 结论

- [ ] **测试通过** - 所有功能正常,可以发布
- [ ] **测试失败** - 存在关键问题,需要修复
- [ ] **部分通过** - 基础功能正常,但有小问题

### 建议

1. [建议1]
2. [建议2]

---

## 🎉 成功标准

### 最低标准 (必须满足)

✅ 能够捕获短 DSL (< 1K 字符)  
✅ 能够捕获长 DSL (> 50K 字符)  
✅ DSL 内容完整,不截断  
✅ JSON 格式正确

### 理想标准 (建议满足)

✅ 元数据提取准确  
✅ Kibana 直接可用  
✅ 性能开销 < 5%  
✅ 内存增长 < 100MB

---

**文档创建时间**: 2025-10-18  
**文档作者**: PandaCoder Team  
**适用版本**: 1.2.0+

🎊 **祝测试顺利!** 🎊

