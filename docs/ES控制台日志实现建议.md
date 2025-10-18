# Elasticsearch 控制台日志输出实现建议

## 📋 项目背景分析

### 当前项目（ais-server）ES 技术栈分析

通过对 `E:\Project\hzmj\ais-server` 项目的深入分析，发现该项目：

1. **使用的 ES 客户端**：
   - Spring Data Elasticsearch (`ElasticsearchTemplate`)
   - Elasticsearch Java Client 8.x (`co.elastic.clients`)
   
2. **当前日志配置**：
   ```xml
   <!-- logback-local.xml -->
   <logger name="com.torchv" level="DEBUG">
       <appender-ref ref="syslog" />
   </logger>
   ```

3. **ES 调用方式**：
   - 主要通过 `ElasticsearchTemplate` 执行查询
   - 使用 `NativeQuery` 构建 DSL
   - 代表类：
     - `VectorDataRetrieverElastic.java` - 查询操作
     - `VectorDataWriterElastic.java` - 写入操作

4. **当前日志打印特点**：
   - 业务层手动使用 `log.info()` 打印关键信息
   - 示例：
     ```java
     log.info("search-index-name:{}，mode:{}", collectName, searchReq.getRetrievalModes());
     log.info("检索成功，size:{}，cosineMinScore：{}", dataSearchHits.getTotalHits(), cosineMinScore);
     ```
   - **缺点**：没有打印实际的 ES DSL 查询语句

### PandaCoder 项目实现方式

PandaCoder 采用的是 **IDEA 插件监听控制台输出** 的方式：
- 监听 IDEA Run Console 输出
- 通过正则表达式解析 ES DSL
- 在独立工具窗口展示

**局限性**：依赖于应用程序本身能够输出 ES DSL 到控制台。

---

## 🎯 问题分析

### 为什么 ais-server 项目无法像 MyBatis Logs 一样输出 ES 查询？

#### 1. MyBatis 日志输出原理

MyBatis 通过以下方式输出 SQL：
```xml
<!-- logback.xml -->
<logger name="com.torchv.application.mapper" level="DEBUG"/>
```

MyBatis 框架内置了日志拦截器：
- `org.apache.ibatis.logging` 包
- 在 SQL 执行前后自动打印
- 输出格式：`==> Preparing: SELECT * FROM users WHERE id = ?`

#### 2. Elasticsearch 客户端日志特点

**Spring Data Elasticsearch + Elasticsearch Java Client 8.x** 的日志特点：
- **不像 MyBatis 那样自动打印完整 DSL**
- 官方日志主要用于调试和错误追踪
- 需要额外配置才能看到 DSL

#### 3. 根本原因

当前 ais-server 项目：
- ❌ 未配置 Elasticsearch 客户端的 DEBUG 日志
- ❌ 未启用 RestClient 的请求/响应追踪
- ❌ 业务代码中未手动打印 DSL

---

## 💡 解决方案

### 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **方案1：配置原生 ES 日志** | 简单、无侵入 | 日志格式不够友好 | ⭐⭐⭐ |
| **方案2：自定义 ES 拦截器** | 完全控制、格式化友好 | 需要编码、有侵入性 | ⭐⭐⭐⭐⭐ |
| **方案3：手动打印 DSL** | 直接可控 | 代码侵入性大、维护成本高 | ⭐⭐ |
| **方案4：使用 AOP 切面** | 优雅、集中管理 | 需要理解切面编程 | ⭐⭐⭐⭐ |

---

## 🚀 推荐实现方案

### 方案1：配置 Elasticsearch 客户端日志（最简单）

#### 步骤 1：修改 `logback-local.xml`

在 `src/main/resources/logback-local.xml` 中添加：

```xml
<configuration>
    <!-- 现有配置... -->
    
    <!-- ===== Elasticsearch 日志配置 ===== -->
    
    <!-- Elasticsearch Java Client 请求日志 -->
    <logger name="org.elasticsearch.client" level="DEBUG">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="syslog" />
    </logger>
    
    <!-- Spring Data Elasticsearch 查询日志 -->
    <logger name="org.springframework.data.elasticsearch" level="DEBUG">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="syslog" />
    </logger>
    
    <!-- Elasticsearch RestClient 详细日志 -->
    <logger name="org.elasticsearch.client.RestClient" level="DEBUG">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="syslog" />
    </logger>
    
    <!-- Elasticsearch 请求追踪（最详细） -->
    <logger name="tracer" level="TRACE">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="syslog" />
    </logger>
    
    <!-- 现有配置... -->
</configuration>
```

#### 步骤 2：配置 Application.yml

在 `application-dev.yml` 或 `application-local.yml` 中添加：

```yaml
logging:
  level:
    # Elasticsearch 客户端日志
    org.elasticsearch.client: DEBUG
    org.elasticsearch.client.RestClient: DEBUG
    # Spring Data Elasticsearch
    org.springframework.data.elasticsearch: DEBUG
    org.springframework.data.elasticsearch.client.elc: DEBUG
    # HTTP 追踪
    tracer: TRACE
```

#### 预期效果

启用后，控制台会输出类似以下内容：

```
2024-10-18 15:30:45 DEBUG o.e.c.RestClient - request [POST http://localhost:9200/torchv_chunk_dims_1024/_search?typed_keys=true] returned [HTTP/1.1 200 OK]
2024-10-18 15:30:45 TRACE tracer - curl -X POST "localhost:9200/torchv_chunk_dims_1024/_search?typed_keys=true" -H "Content-Type: application/json" -d '{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "content": {
              "query": "用户查询内容",
              "boost": 0.3
            }
          }
        }
      ]
    }
  },
  "knn": {
    "field": "vector",
    "query_vector": [0.123, 0.456, ...],
    "k": 10,
    "num_candidates": 50
  },
  "size": 10
}'
```

---

### 方案2：自定义 Elasticsearch 请求拦截器（推荐）

这个方案类似于 MyBatis Log Plugin，能够优雅地格式化输出。

#### 步骤 1：创建 ES DSL 日志拦截器

创建文件：`src/main/java/com/torchv/system/extensions/elasticsearch/ElasticsearchQueryLogger.java`

```java
package com.torchv.system.extensions.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

/**
 * Elasticsearch 查询日志拦截器
 * 类似 MyBatis Log Plugin，在控制台美化输出 ES DSL
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 2024-10-18
 */
@Slf4j
@Aspect
@Component
public class ElasticsearchQueryLogger {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 拦截 ElasticsearchTemplate 的 search 方法
     */
    @Pointcut("execution(* org.springframework.data.elasticsearch.core.ElasticsearchOperations.search(..))")
    public void elasticsearchSearchPointcut() {
    }
    
    @Around("elasticsearchSearchPointcut()")
    public Object logElasticsearchQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取查询参数
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof NativeQuery) {
                NativeQuery nativeQuery = (NativeQuery) args[0];
                logQuery(nativeQuery, args);
            }
            
            // 执行实际查询
            Object result = joinPoint.proceed();
            
            // 记录执行时间和结果
            long executionTime = System.currentTimeMillis() - startTime;
            logResult(result, executionTime);
            
            return result;
            
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("╔═══════════════════════════════════════════════════════════════════════════════");
            log.error("║ ES Query Failed! Time: {} ms", executionTime);
            log.error("║ Error: {}", e.getMessage());
            log.error("╚═══════════════════════════════════════════════════════════════════════════════");
            throw e;
        }
    }
    
    /**
     * 记录查询语句
     */
    private void logQuery(NativeQuery nativeQuery, Object[] args) {
        try {
            log.info("╔═══════════════════════════════════════════════════════════════════════════════");
            log.info("║ Elasticsearch Query");
            log.info("╠═══════════════════════════════════════════════════════════════════════════════");
            
            // 索引名称
            if (args.length > 2) {
                log.info("║ Index: {}", args[2]);
            }
            
            // Query DSL
            if (nativeQuery.getQuery() != null) {
                String queryJson = formatJson(nativeQuery.getQuery());
                log.info("║ Query DSL:");
                printFormattedJson(queryJson);
            }
            
            // KNN Query
            if (nativeQuery.getKnnQuery() != null && !nativeQuery.getKnnQuery().isEmpty()) {
                String knnJson = formatJson(nativeQuery.getKnnQuery().get(0));
                log.info("║ KNN Query:");
                printFormattedJson(knnJson);
            }
            
            // 分页信息
            if (nativeQuery.getPageable() != null) {
                log.info("║ Page: {} | Size: {}", 
                    nativeQuery.getPageable().getPageNumber(), 
                    nativeQuery.getPageable().getPageSize());
            }
            
            log.info("╚═══════════════════════════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.warn("Failed to log ES query", e);
        }
    }
    
    /**
     * 记录查询结果
     */
    private void logResult(Object result, long executionTime) {
        try {
            if (result instanceof SearchHits) {
                SearchHits<?> searchHits = (SearchHits<?>) result;
                log.info("╔═══════════════════════════════════════════════════════════════════════════════");
                log.info("║ ES Query Result");
                log.info("╠═══════════════════════════════════════════════════════════════════════════════");
                log.info("║ Total Hits: {}", searchHits.getTotalHits());
                log.info("║ Returned: {}", searchHits.getSearchHits().size());
                log.info("║ Execution Time: {} ms", executionTime);
                log.info("╚═══════════════════════════════════════════════════════════════════════════════");
            }
        } catch (Exception e) {
            log.warn("Failed to log ES result", e);
        }
    }
    
    /**
     * 格式化 JSON
     */
    private String formatJson(Object obj) {
        try {
            StringWriter writer = new StringWriter();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, obj);
            return writer.toString();
        } catch (Exception e) {
            return obj.toString();
        }
    }
    
    /**
     * 打印格式化的 JSON（每行添加前缀）
     */
    private void printFormattedJson(String json) {
        String[] lines = json.split("\n");
        for (String line : lines) {
            log.info("║   {}", line);
        }
    }
}
```

#### 步骤 2：配置启用条件

在 `application.yml` 中添加配置：

```yaml
# 自定义 ES 日志配置
elasticsearch:
  logging:
    enabled: true  # 是否启用 ES DSL 日志
    include-vectors: false  # 是否包含向量数据（向量很长，建议关闭）
```

#### 步骤 3：修改拦截器支持配置

修改 `ElasticsearchQueryLogger.java`，添加配置支持：

```java
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "elasticsearch.logging.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchQueryLogger {
    
    @Value("${elasticsearch.logging.include-vectors:false}")
    private boolean includeVectors;
    
    // ... 其他代码
    
    /**
     * 格式化 JSON（可选择性过滤向量）
     */
    private String formatJson(Object obj) {
        try {
            if (!includeVectors && obj instanceof Query) {
                // 移除或截断 vector 字段
                // ... 实现省略
            }
            StringWriter writer = new StringWriter();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, obj);
            return writer.toString();
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
```

#### 预期效果

```
╔═══════════════════════════════════════════════════════════════════════════════
║ Elasticsearch Query
╠═══════════════════════════════════════════════════════════════════════════════
║ Index: torchv_chunk_dims_1024
║ Query DSL:
║   {
║     "bool" : {
║       "must" : [ {
║         "match" : {
║           "content" : {
║             "query" : "用户查询内容",
║             "boost" : 0.3
║           }
║         }
║       } ]
║     }
║   }
║ KNN Query:
║   {
║     "field" : "vector",
║     "k" : 10,
║     "num_candidates" : 50,
║     "query_vector" : [ 0.123, 0.456, ... ]
║   }
║ Page: 0 | Size: 10
╚═══════════════════════════════════════════════════════════════════════════════
╔═══════════════════════════════════════════════════════════════════════════════
║ ES Query Result
╠═══════════════════════════════════════════════════════════════════════════════
║ Total Hits: 156
║ Returned: 10
║ Execution Time: 23 ms
╚═══════════════════════════════════════════════════════════════════════════════
```

---

### 方案3：增强版 - 结合 RestClient 拦截器

如果想要更底层的拦截（类似于 HTTP 请求拦截），可以自定义 RestClient 拦截器。

#### 步骤 1：创建 RestClient 配置类

创建文件：`src/main/java/com/torchv/system/config/ElasticsearchClientConfig.java`

```java
package com.torchv.system.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Elasticsearch RestClient 配置
 * 添加请求/响应拦截器，输出详细日志
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 2024-10-18
 */
@Slf4j
@Configuration
public class ElasticsearchClientConfig {

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(RestClientBuilder builder) {
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    
                    // 请求拦截器
                    httpClientBuilder.addInterceptorLast(new HttpRequestInterceptor() {
                        @Override
                        public void process(HttpRequest request, HttpContext context) throws IOException {
                            log.info("╔═══════════════════════════════════════════════════════════════════════════════");
                            log.info("║ ES HTTP Request");
                            log.info("╠═══════════════════════════════════════════════════════════════════════════════");
                            log.info("║ Method: {} {}", request.getRequestLine().getMethod(), request.getRequestLine().getUri());
                            
                            // 打印请求头
                            for (var header : request.getAllHeaders()) {
                                log.info("║ Header: {}: {}", header.getName(), header.getValue());
                            }
                            
                            // 打印请求体（如果是 POST/PUT）
                            if (request instanceof org.apache.http.HttpEntityEnclosingRequest) {
                                org.apache.http.HttpEntityEnclosingRequest entityRequest = 
                                    (org.apache.http.HttpEntityEnclosingRequest) request;
                                try {
                                    String body = EntityUtils.toString(entityRequest.getEntity(), StandardCharsets.UTF_8);
                                    log.info("║ Request Body:");
                                    printFormattedJson(body);
                                } catch (Exception e) {
                                    log.warn("║ Failed to read request body");
                                }
                            }
                            
                            log.info("╚═══════════════════════════════════════════════════════════════════════════════");
                        }
                    });
                    
                    // 响应拦截器
                    httpClientBuilder.addInterceptorLast(new HttpResponseInterceptor() {
                        @Override
                        public void process(HttpResponse response, HttpContext context) throws IOException {
                            log.info("╔═══════════════════════════════════════════════════════════════════════════════");
                            log.info("║ ES HTTP Response");
                            log.info("╠═══════════════════════════════════════════════════════════════════════════════");
                            log.info("║ Status: {}", response.getStatusLine());
                            
                            // 打印响应头（可选）
                            // for (var header : response.getAllHeaders()) {
                            //     log.info("║ Header: {}: {}", header.getName(), header.getValue());
                            // }
                            
                            // 打印响应体
                            if (response.getEntity() != null) {
                                try {
                                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                                    log.info("║ Response Body:");
                                    printFormattedJson(body);
                                } catch (Exception e) {
                                    log.warn("║ Failed to read response body");
                                }
                            }
                            
                            log.info("╚═══════════════════════════════════════════════════════════════════════════════");
                        }
                    });
                    
                    return httpClientBuilder;
                });
            }
        };
    }
    
    /**
     * 打印格式化的 JSON
     */
    private void printFormattedJson(String json) {
        try {
            // 简单格式化
            String[] lines = json.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    log.info("║   {}", line);
                }
            }
        } catch (Exception e) {
            log.info("║   {}", json);
        }
    }
}
```

---

## 🔧 PandaCoder 项目适配建议

### 当前 PandaCoder 存在的问题

1. **依赖应用程序日志输出**：如果应用没有配置 ES 日志，插件无法捕获
2. **解析可能失败**：不同的日志格式导致解析不准确
3. **性能开销**：监听所有控制台输出，正则匹配消耗资源

### 改进建议

#### 1. 在 PandaCoder 中提示用户配置日志

修改 `EsDslMonitor使用指南.md`，添加更详细的配置说明：

```markdown
### 第三步：配置日志（重要！必须配置）

#### 方式1：使用 Logback（推荐）

在 `src/main/resources/logback-spring.xml` 或 `logback.xml` 中添加：

\```xml
<logger name="org.elasticsearch.client" level="DEBUG"/>
<logger name="org.elasticsearch.client.RestClient" level="DEBUG"/>
<logger name="org.springframework.data.elasticsearch" level="DEBUG"/>
<logger name="tracer" level="TRACE"/>
\```

#### 方式2：使用 application.yml

\```yaml
logging:
  level:
    org.elasticsearch.client: DEBUG
    org.elasticsearch.client.RestClient: DEBUG
    org.springframework.data.elasticsearch: DEBUG
    tracer: TRACE
\```

#### 方式3：使用 Log4j2

在 `log4j2.xml` 中添加：

\```xml
<Logger name="org.elasticsearch.client" level="debug"/>
<Logger name="org.springframework.data.elasticsearch" level="debug"/>
<Logger name="tracer" level="trace"/>
\```

### 验证配置是否生效

运行应用后，查看控制台是否输出类似以下内容：

\```
curl -X POST "localhost:9200/index_name/_search" -d '{"query": {...}}'
\```

如果没有看到，说明日志配置未生效。
```

#### 2. 增强解析器兼容性

修改 `EsDslParser.java`，添加更多日志格式支持：

```java
// 添加对 Spring Data Elasticsearch 新版本日志的支持
private static final Pattern SPRING_DATA_ES_NEW_PATTERN = Pattern.compile(
    "(?i).*?Executing\\s+(?:search|count|delete).*?index=\\[(\\w+)\\].*?query=\\[(.+?)\\]",
    Pattern.DOTALL
);

// 添加对 RestClient 日志的支持
private static final Pattern REST_CLIENT_PATTERN = Pattern.compile(
    "(?i).*?RestClient.*?request\\s+\\[(GET|POST|PUT|DELETE)\\s+.+?/(\\S+?)\\].*",
    Pattern.DOTALL
);
```

#### 3. 添加配置检测功能

在 `EsDslToolWindow.java` 中添加一个"配置检测"按钮：

```java
private JButton createConfigCheckButton() {
    JButton button = new JButton("检测日志配置");
    button.addActionListener(e -> {
        // 检查是否有 logback.xml / log4j2.xml / application.yml
        // 检查是否包含必要的 logger 配置
        // 给出配置建议
        showConfigCheckDialog();
    });
    return button;
}
```

---

## 📊 方案总结对比

### 对于 ais-server 项目

| 需求 | 方案1<br/>原生日志 | 方案2<br/>AOP拦截器 | 方案3<br/>RestClient拦截 |
|------|:------------------:|:-------------------:|:------------------------:|
| 实现难度 | ⭐ 简单 | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐ 较难 |
| 代码侵入性 | 无 | 低 | 中 |
| 日志美观度 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 性能影响 | 低 | 低 | 中 |
| 可配置性 | 低 | 高 | 高 |
| 推荐程度 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### 对于 PandaCoder 项目

| 改进项 | 优先级 | 难度 | 效果 |
|--------|:------:|:----:|:----:|
| 增强配置文档 | 高 | ⭐ | ⭐⭐⭐⭐ |
| 添加配置检测 | 中 | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| 增强解析器 | 中 | ⭐⭐ | ⭐⭐⭐ |
| 添加配置模板 | 低 | ⭐⭐ | ⭐⭐⭐ |

---

## 🎯 立即行动计划

### 对于 ais-server 项目（推荐执行顺序）

#### 阶段1：快速验证（5分钟）

1. ✅ 修改 `logback-local.xml`，添加 ES 日志配置（方案1）
2. ✅ 重启应用，观察控制台输出
3. ✅ 验证是否能看到 ES 查询

#### 阶段2：美化输出（1-2小时）

1. ✅ 创建 `ElasticsearchQueryLogger.java`（方案2）
2. ✅ 配置 AOP 切面
3. ✅ 测试验证效果

#### 阶段3：深度集成（可选，2-4小时）

1. ✅ 实现 RestClient 拦截器（方案3）
2. ✅ 添加配置开关
3. ✅ 性能测试

### 对于 PandaCoder 项目（推荐执行顺序）

#### 优先改进（1-2小时）

1. ✅ 更新 `EsDslMonitor使用指南.md`，添加详细的日志配置说明
2. ✅ 创建配置模板文件（logback.xml / application.yml 示例）
3. ✅ 在工具窗口添加"配置帮助"按钮

#### 后续优化（可选）

1. ✅ 增强 `EsDslParser.java` 的解析能力
2. ✅ 添加配置检测功能
3. ✅ 支持一键生成配置文件

---

## 📝 完整代码示例

### ais-server 项目完整配置示例

#### 1. logback-local.xml（完整版）

```xml
<configuration>
    <!-- 控制台输出 -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) %cyan(%logger{36}) - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 文件输出 -->
    <appender name="syslog" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <File>logs/torchv.log</File>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/torchv.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>50MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- Elasticsearch 专用日志 -->
    <appender name="ES_LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <File>logs/elasticsearch.log</File>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/elasticsearch.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>50MB</maxFileSize>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
    
    <!-- 业务日志 -->
    <logger name="com.torchv" level="DEBUG">
        <appender-ref ref="syslog" />
    </logger>
    
    <!-- ===== Elasticsearch 日志配置 ===== -->
    <logger name="org.elasticsearch.client" level="DEBUG" additivity="false">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="ES_LOG" />
    </logger>
    
    <logger name="org.elasticsearch.client.RestClient" level="DEBUG" additivity="false">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="ES_LOG" />
    </logger>
    
    <logger name="org.springframework.data.elasticsearch" level="DEBUG" additivity="false">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="ES_LOG" />
    </logger>
    
    <logger name="tracer" level="TRACE" additivity="false">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="ES_LOG" />
    </logger>
    
    <!-- 自定义 ES 查询日志 -->
    <logger name="com.torchv.system.extensions.elasticsearch" level="INFO" additivity="false">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="ES_LOG" />
    </logger>
</configuration>
```

#### 2. application-dev.yml（开发环境配置）

```yaml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_CLUSTER:10.10.0.210:9222}
    username: ${ELASTICSEARCH_USER:elastic}
    password: ${ELASTICSEARCH_PWD:elastic123}

# 日志配置
logging:
  level:
    root: INFO
    com.torchv: DEBUG
    # Elasticsearch 客户端日志
    org.elasticsearch.client: DEBUG
    org.elasticsearch.client.RestClient: DEBUG
    org.springframework.data.elasticsearch: DEBUG
    org.springframework.data.elasticsearch.client.elc: DEBUG
    # HTTP 追踪
    tracer: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) %cyan(%logger{36}) - %msg%n"

# 自定义 ES 日志配置
elasticsearch:
  logging:
    enabled: true  # 启用 ES DSL 日志
    include-vectors: false  # 不包含向量数据（向量太长）
    pretty-print: true  # 美化 JSON 输出
```

---

## 🎓 学习资源

### Elasticsearch 日志配置相关

1. **Elasticsearch Java Client 日志**：
   - 官方文档：https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/logging.html

2. **Spring Data Elasticsearch 日志**：
   - GitHub Issues：https://github.com/spring-projects/spring-data-elasticsearch/issues

3. **Logback 配置**：
   - 官方文档：https://logback.qos.ch/manual/configuration.html

### AOP 切面编程

1. **Spring AOP 官方文档**：
   - https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop

2. **AspectJ 表达式**：
   - https://www.baeldung.com/spring-aop-pointcut-tutorial

---

## 🔍 故障排除

### 问题1：配置后仍然看不到 ES 日志

**可能原因**：
1. 日志级别配置未生效
2. 配置文件未正确加载
3. Spring Profile 不匹配

**解决方案**：
```java
// 在启动类添加日志输出，验证配置
@PostConstruct
public void init() {
    Logger esLogger = LoggerFactory.getLogger("org.elasticsearch.client");
    log.info("Elasticsearch client logger level: {}", esLogger.getLevel());
}
```

### 问题2：日志太多，影响性能

**解决方案**：
1. 使用条件配置：
```yaml
logging:
  level:
    org.elasticsearch.client: ${ES_LOG_LEVEL:INFO}  # 默认 INFO，开发时改为 DEBUG
```

2. 使用独立的日志文件
3. 只在开发环境启用

### 问题3：向量数据太长，日志难以阅读

**解决方案**：
在 `ElasticsearchQueryLogger` 中过滤向量字段：

```java
private String formatQuery(Object query) {
    // 将查询对象转换为 JSON
    String json = objectMapper.writeValueAsString(query);
    
    // 移除或截断 query_vector 字段
    json = json.replaceAll("\"query_vector\"\\s*:\\s*\\[[^\\]]{100,}\\]", 
                           "\"query_vector\": [... vector data omitted ...]");
    
    return json;
}
```

---

## ✅ 总结

### 核心要点

1. **ais-server 项目**无法像 MyBatis Logs 一样输出 ES 查询的原因是：
   - ❌ 未配置 Elasticsearch 客户端日志
   - ❌ 未启用详细的追踪日志
   - ❌ 缺少自定义的日志拦截器

2. **推荐解决方案**：
   - ✅ 方案1（快速）：配置 ES 客户端日志（5分钟）
   - ✅ 方案2（推荐）：实现 AOP 日志拦截器（1-2小时）
   - ✅ 方案3（深度）：RestClient 拦截器（2-4小时）

3. **PandaCoder 项目**改进方向：
   - ✅ 完善配置文档
   - ✅ 添加配置检测功能
   - ✅ 提供配置模板

### 下一步行动

#### 立即执行（今天）
1. ✅ 在 ais-server 项目中配置方案1，验证效果
2. ✅ 在 PandaCoder 项目中更新文档

#### 本周执行
1. ✅ 实现方案2（AOP拦截器）
2. ✅ 测试并优化性能

#### 长期优化
1. ✅ 实现方案3（RestClient拦截器）
2. ✅ 完善 PandaCoder 的配置检测功能

---

**文档创建时间**：2024-10-18  
**作者**：AI Assistant  
**目标项目**：
- ais-server (`E:\Project\hzmj\ais-server`)  
- PandaCoder (`E:\Project\GitHub\PandaCoder`)

---

## 📧 反馈与支持

如有问题或需要进一步的帮助，欢迎反馈！

**祝开发顺利！** 🚀

