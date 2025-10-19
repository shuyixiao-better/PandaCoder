# 🚀 PandaCoder 2.0.0 - ES DSL Monitor & SQL Monitor 震撼发布！

> **让数据库查询和搜索引擎调用一目了然！**  
> 告别盲目调试，拥抱可视化监控时代！

---

## 📖 目录

- [功能介绍](#功能介绍)
- [为什么需要这两个监控工具](#为什么需要这两个监控工具)
- [快速开始](#快速开始)
- [详细配置指南](#详细配置指南)
- [使用场景](#使用场景)
- [常见问题](#常见问题)
- [技术原理](#技术原理)

---

## 🎯 功能介绍

### ES DSL Monitor - Elasticsearch 查询监控

**像 MyBatis Log 一样，实时捕获和展示 Elasticsearch 查询！**

#### ✨ 核心特性

- 🔍 **实时监控**：自动捕获所有 ES 查询，无需修改代码
- 📊 **可视化展示**：独立工具窗口，清晰展示查询详情
- 🎨 **智能解析**：支持多种日志格式（REST Client、cURL、Spring Data ES）
- 🔗 **API 关联**：自动关联触发查询的 API 接口
- 💾 **持久化存储**：自动保存查询历史，方便回溯分析
- 🚀 **零性能损耗**：异步处理，不影响 IDEA 运行速度

#### 📋 监控内容

| 字段 | 说明 | 示例 |
|------|------|------|
| **方法** | HTTP 方法 | GET / POST / PUT / DELETE |
| **索引** | ES 索引名称 | `torchv_chunk_dims_1024` |
| **端点** | 请求端点 | `/_search` / `/_count` |
| **DSL 查询** | 完整的查询 JSON | `{"query": {"bool": {...}}}` |
| **API 路径** | 触发查询的 API | `/api/vector/search` |
| **调用类** | 发起查询的 Java 类 | `VectorDataRetrieverElastic.java:125` |
| **执行时间** | 查询耗时 | 23 ms |
| **响应** | ES 返回的响应 | `{"took": 5, "hits": {...}}` |

---

### SQL Monitor - SQL 查询监控

**完美复刻 MyBatis Log Plugin 的功能，在 IDEA 中原生支持！**

#### ✨ 核心特性

- 📝 **完整 SQL 记录**：捕获所有 MyBatis SQL 查询
- 🎯 **参数替换**：自动生成可执行的 SQL（参数已替换）
- 🔍 **智能分类**：按操作类型（SELECT/INSERT/UPDATE/DELETE）分类
- 📊 **统计分析**：实时统计各类 SQL 数量和表访问情况
- 🔗 **API 追踪**：自动关联触发 SQL 的 API 接口
- 🎨 **颜色标识**：不同操作类型使用不同颜色，一目了然

#### 📋 监控内容

| 字段 | 说明 | 示例 |
|------|------|------|
| **操作** | SQL 操作类型 | SELECT / INSERT / UPDATE / DELETE |
| **表名** | 操作的数据库表 | `user` / `order` |
| **SQL 语句** | 原始 SQL（带占位符） | `SELECT * FROM user WHERE id = ?` |
| **参数** | 绑定的参数值 | `123(Integer)` |
| **可执行 SQL** | 参数已替换的 SQL | `SELECT * FROM user WHERE id = 123` |
| **结果数** | 查询返回的记录数 | 10 |
| **API 路径** | 触发 SQL 的 API | `/api/user/list` |
| **调用类** | Mapper 接口 | `UserMapper.selectById` |
| **执行时间** | SQL 耗时 | 15 ms |

---

## 💡 为什么需要这两个监控工具

### 传统开发的痛点

#### 😫 Elasticsearch 调试困境

```java
// 代码中构建复杂的 ES 查询
NativeQuery query = NativeQuery.builder()
    .withQuery(boolQuery)
    .withKnnQuery(knnQuery)
    .withPageable(pageable)
    .build();

SearchHits<Document> hits = elasticsearchTemplate.search(query, Document.class);

// ❌ 问题：
// 1. 不知道实际发送给 ES 的 DSL 是什么样的
// 2. 无法直接在 Kibana 中测试
// 3. 调试时需要手动打印 JSON
// 4. 向量数据太长，难以阅读
```

#### 😫 SQL 调试困境

```java
// MyBatis Mapper 调用
List<User> users = userMapper.selectByCondition(condition);

// ❌ 问题：
// 1. 不知道实际执行的 SQL 是什么
// 2. 参数绑定是否正确？
// 3. 是否触发了 N+1 查询？
// 4. 哪个 API 调用了这个 SQL？
```

### 🎉 使用监控工具后

#### ✅ ES DSL Monitor 带来的改变

```
✨ 实时看到完整的 ES 查询：
╔═══════════════════════════════════════════════════════════════
║ ES DSL Query
╠═══════════════════════════════════════════════════════════════
║ Method: POST
║ Index: torchv_chunk_dims_1024
║ Endpoint: /_search
║ API Path: /api/vector/search
║ Caller: VectorDataRetrieverElastic.java:125
║ 
║ DSL:
║ {
║   "query": {
║     "bool": {
║       "must": [
║         {"match": {"content": {"query": "用户输入", "boost": 0.3}}}
║       ]
║     }
║   },
║   "knn": {
║     "field": "vector",
║     "k": 10,
║     "num_candidates": 50,
║     "query_vector": [0.123, 0.456, ...]
║   },
║   "size": 10
║ }
║
║ Response: 156 hits in 23ms
╚═══════════════════════════════════════════════════════════════

✅ 好处：
1. 一键复制 DSL，直接在 Kibana 测试
2. 清晰看到查询结构和参数
3. 追踪 API 到 ES 查询的完整链路
4. 发现性能瓶颈
```

#### ✅ SQL Monitor 带来的改变

```
✨ 实时看到完整的 SQL 执行：
╔═══════════════════════════════════════════════════════════════
║ SQL Query
╠═══════════════════════════════════════════════════════════════
║ Operation: SELECT
║ Table: user
║ API Path: /api/user/list
║ Caller: UserMapper.selectByCondition
║ 
║ Original SQL:
║ SELECT id, name, email, created_at
║ FROM user
║ WHERE status = ? AND created_at > ?
║ ORDER BY id DESC
║ LIMIT ?
║ 
║ Parameters:
║ 1(Integer), 2025-01-01 00:00:00(Timestamp), 10(Integer)
║ 
║ Executable SQL:
║ SELECT id, name, email, created_at
║ FROM user
║ WHERE status = 1 AND created_at > '2025-01-01 00:00:00'
║ ORDER BY id DESC
║ LIMIT 10
║
║ Result: 10 rows in 15ms
╚═══════════════════════════════════════════════════════════════

✅ 好处：
1. 一键复制可执行 SQL，直接在数据库工具运行
2. 清晰看到参数绑定
3. 追踪 API 到 SQL 的完整链路
4. 发现 N+1 查询问题
5. 优化慢查询
```

---

## 🚀 快速开始

### 第一步：安装插件

1. 打开 IntelliJ IDEA
2. `Settings` → `Plugins` → 搜索 `PandaCoder`
3. 点击 `Install` 安装
4. 重启 IDEA

### 第二步：打开工具窗口

在 IDEA 底部工具栏找到：

- 🔍 **ES DSL Monitor** - Elasticsearch 图标
- 🗄️ **SQL Monitor** - MySQL 图标

### 第三步：启用监听

在对应的工具窗口中：

- ✅ 勾选 **"启用 ES 监听"** 或 **"启用 SQL 监听"**

### 第四步：配置日志输出（重要！）

**这是最关键的一步！** 如果不配置日志，监控工具无法捕获查询。

---

## 📝 详细配置指南

### ES DSL Monitor 配置

#### 配置原理说明

**为什么需要配置日志？**

ES DSL Monitor 的工作原理是监听 IDEA 控制台的输出，解析其中的 Elasticsearch 查询日志。但是，**Elasticsearch 客户端默认不会输出详细的查询日志**，需要手动配置日志级别。

这类似于 MyBatis 需要配置 DEBUG 日志才能看到 SQL 一样。

#### 方式 1：配置 Logback（推荐）

如果你的项目使用 Logback（Spring Boot 默认），在 `src/main/resources/logback-spring.xml` 或 `logback-local.xml` 中添加：

```xml
<configuration>
    <!-- 现有配置保持不变... -->
    
    <!-- ===== Elasticsearch 日志配置（新增） ===== -->
    
    <!-- Elasticsearch Java Client 请求日志 -->
    <logger name="org.elasticsearch.client" level="DEBUG">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <!-- Spring Data Elasticsearch 查询日志 -->
    <logger name="org.springframework.data.elasticsearch" level="DEBUG">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <!-- Elasticsearch RestClient 详细日志 -->
    <logger name="org.elasticsearch.client.RestClient" level="DEBUG">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <!-- Elasticsearch 请求追踪（最详细，强烈推荐！） -->
    <logger name="tracer" level="TRACE">
        <appender-ref ref="STDOUT" />
    </logger>
    
</configuration>
```

**配置说明：**

| Logger 名称 | 级别 | 作用 | 是否必需 |
|------------|------|------|---------|
| `org.elasticsearch.client` | DEBUG | 输出 ES 客户端基础日志 | ⭐⭐⭐ 推荐 |
| `org.elasticsearch.client.RestClient` | DEBUG | 输出 HTTP 请求/响应 | ⭐⭐⭐⭐ 重要 |
| `org.springframework.data.elasticsearch` | DEBUG | Spring Data ES 查询日志 | ⭐⭐⭐ 推荐 |
| `tracer` | TRACE | 输出完整的 cURL 格式请求 | ⭐⭐⭐⭐⭐ 必需 |

**⚠️ 重点：`tracer` 日志是最重要的！** 它会输出类似以下格式的日志：

```bash
curl -iX POST "localhost:9200/torchv_chunk_dims_1024/_search?typed_keys=true" -H "Content-Type: application/json" -d '
{
  "query": {
    "bool": {
      "must": [
        {"match": {"content": {"query": "用户查询", "boost": 0.3}}}
      ]
    }
  },
  "knn": {
    "field": "vector",
    "k": 10,
    "num_candidates": 50,
    "query_vector": [0.123, 0.456, ...]
  },
  "size": 10
}'
# {
#   "took": 5,
#   "hits": {
#     "total": {"value": 156},
#     "hits": [...]
#   }
# }
```

这种格式包含了：
- ✅ 完整的 HTTP 方法和 URL
- ✅ 完整的 DSL 查询 JSON
- ✅ 完整的响应 JSON
- ✅ 可以直接复制到终端执行

#### 方式 2：配置 application.yml

在 `src/main/resources/application.yml` 或 `application-dev.yml` 中添加：

```yaml
# 日志配置
logging:
  level:
    # Elasticsearch 客户端日志
    org.elasticsearch.client: DEBUG
    org.elasticsearch.client.RestClient: DEBUG
    # Spring Data Elasticsearch
    org.springframework.data.elasticsearch: DEBUG
    org.springframework.data.elasticsearch.client.elc: DEBUG
    # HTTP 追踪（最重要！）
    tracer: TRACE
```

**⚠️ 注意：**
- 如果同时配置了 `logback.xml` 和 `application.yml`，`logback.xml` 的配置优先级更高
- 建议在 `logback.xml` 中配置，更灵活

#### 方式 3：配置 Log4j2（如果使用 Log4j2）

在 `src/main/resources/log4j2.xml` 中添加：

```xml
<Configuration>
    <!-- 现有配置... -->
    
    <!-- Elasticsearch 日志配置 -->
    <Loggers>
        <Logger name="org.elasticsearch.client" level="debug" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
        <Logger name="org.elasticsearch.client.RestClient" level="debug" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
        <Logger name="org.springframework.data.elasticsearch" level="debug" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
        <Logger name="tracer" level="trace" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
    </Loggers>
</Configuration>
```

#### 验证配置是否生效

配置完成后，重启应用程序，在 IDEA 控制台应该能看到类似以下内容：

```
2025-10-19 15:30:45.123 TRACE tracer - curl -iX POST "localhost:9200/index_name/_search" -d '{"query": {...}}'
```

**如果看到了，说明配置成功！** ✅

**如果没看到，请检查：**
1. ❌ 配置文件路径是否正确
2. ❌ 日志级别是否设置为 TRACE
3. ❌ 是否重启了应用程序
4. ❌ 是否在正确的 profile 中配置（如 dev/local）

---

### SQL Monitor 配置

#### 配置原理说明

**为什么需要配置日志？**

SQL Monitor 的工作原理是监听 MyBatis 输出的 SQL 日志。MyBatis 在 **DEBUG** 级别会输出完整的 SQL 执行信息：

```
==>  Preparing: SELECT * FROM user WHERE id = ?
==> Parameters: 123(Integer)
<==      Total: 1
```

如果日志级别设置为 INFO 或更高，这些日志不会输出，SQL Monitor 就无法捕获。

#### 方式 1：配置 Logback（推荐）

在 `logback-spring.xml` 或 `logback-local.xml` 中添加：

```xml
<configuration>
    <!-- 现有配置保持不变... -->
    
    <!-- ===== MyBatis SQL 日志配置（新增） ===== -->
    
    <!-- 方式 A：配置 Mapper 接口包路径（推荐） -->
    <logger name="com.yourpackage.mapper" level="DEBUG">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <!-- 或者 -->
    
    <!-- 方式 B：配置 MyBatis Plus（如果使用 MyBatis Plus） -->
    <logger name="com.baomidou.mybatisplus" level="DEBUG">
        <appender-ref ref="STDOUT" />
    </logger>
    
    <!-- 或者 -->
    
    <!-- 方式 C：配置所有 MyBatis 日志 -->
    <logger name="org.apache.ibatis" level="DEBUG">
        <appender-ref ref="STDOUT" />
    </logger>
    
</configuration>
```

**配置说明：**

| 配置方式 | Logger 名称 | 说明 | 推荐度 |
|---------|------------|------|--------|
| **方式 A** | `com.yourpackage.mapper` | 只输出你的 Mapper 接口的 SQL | ⭐⭐⭐⭐⭐ 最推荐 |
| **方式 B** | `com.baomidou.mybatisplus` | MyBatis Plus 的 SQL | ⭐⭐⭐⭐ 推荐 |
| **方式 C** | `org.apache.ibatis` | 所有 MyBatis 日志（可能很多） | ⭐⭐⭐ 一般 |

**⚠️ 注意：** 请将 `com.yourpackage.mapper` 替换为你项目中 Mapper 接口的实际包路径！

**示例：**
- 如果你的 Mapper 在 `com.example.demo.mapper` 包下，配置为：
  ```xml
  <logger name="com.example.demo.mapper" level="DEBUG">
  ```
- 如果你的 Mapper 在 `com.torchv.application.mapper` 包下，配置为：
  ```xml
  <logger name="com.torchv.application.mapper" level="DEBUG">
  ```

#### 方式 2：配置 application.yml

在 `application.yml` 或 `application-dev.yml` 中添加：

```yaml
# 日志配置
logging:
  level:
    # 方式 A：配置 Mapper 接口包路径（推荐）
    com.yourpackage.mapper: DEBUG
    
    # 或者
    
    # 方式 B：配置 MyBatis Plus
    com.baomidou.mybatisplus: DEBUG
    
    # 或者
    
    # 方式 C：配置所有 MyBatis 日志
    org.apache.ibatis: DEBUG
```

**⚠️ 同样需要替换为你的实际包路径！**

#### 验证配置是否生效

配置完成后，重启应用程序，在 IDEA 控制台应该能看到类似以下内容：

```
2025-10-19 15:30:45.123 DEBUG com.example.mapper.UserMapper.selectById - ==>  Preparing: SELECT * FROM user WHERE id = ?
2025-10-19 15:30:45.125 DEBUG com.example.mapper.UserMapper.selectById - ==> Parameters: 123(Integer)
2025-10-19 15:30:45.130 DEBUG com.example.mapper.UserMapper.selectById - <==      Total: 1
```

**如果看到了，说明配置成功！** ✅

**如果没看到，请检查：**
1. ❌ Mapper 包路径是否正确
2. ❌ 日志级别是否设置为 DEBUG
3. ❌ 是否重启了应用程序
4. ❌ 是否在正确的 profile 中配置

---

### 完整配置示例（基于你的 ais-server 项目）

根据你提供的配置，这是一个完整的 `logback-local.xml` 示例：

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
        <File>logs/ais-server.log</File>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/ais-server.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>50MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
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
    
    <!-- ===== MyBatis SQL 日志配置 ===== -->
    
    <!-- MyBatis Mapper 日志（请替换为你的实际包路径） -->
    <logger name="com.torchv.application.mapper" level="DEBUG">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="syslog" />
    </logger>
    
    <!-- MyBatis Plus 日志（如果使用） -->
    <logger name="com.baomidou.mybatisplus" level="DEBUG">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="syslog" />
    </logger>
    
</configuration>
```

对应的 `application.yml` 配置：

```yaml
# 日志配置
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
    # MyBatis Mapper 日志
    com.torchv.application.mapper: DEBUG
    # MyBatis Plus
    com.baomidou.mybatisplus: DEBUG
```

---

## 🎬 使用场景

### 场景 1：优化 Elasticsearch 查询性能

**问题：** 向量检索很慢，不知道是哪里的问题。

**解决方案：**

1. 打开 **ES DSL Monitor**
2. 执行向量检索操作
3. 在监控窗口查看实际的 DSL 查询
4. 检查：
   - `num_candidates` 是否设置合理？
   - `k` 值是否过大？
   - 是否有不必要的 `bool` 查询？
   - 向量维度是否正确？
5. 复制 DSL 到 Kibana 进行调优测试
6. 修改代码，再次验证

**效果：** 查询性能提升 3 倍！ 🚀

---

### 场景 2：排查 N+1 查询问题

**问题：** 列表接口很慢，怀疑有 N+1 查询。

**解决方案：**

1. 打开 **SQL Monitor**
2. 调用列表接口
3. 在监控窗口查看 SQL 执行情况
4. 发现：
   ```
   SELECT * FROM order WHERE user_id = 1    -- 1 次
   SELECT * FROM order_item WHERE order_id = 101  -- N 次
   SELECT * FROM order_item WHERE order_id = 102  -- N 次
   SELECT * FROM order_item WHERE order_id = 103  -- N 次
   ...
   ```
5. 确认是 N+1 查询问题
6. 修改为使用 `JOIN` 或批量查询
7. 再次验证，只执行 1-2 条 SQL

**效果：** 接口响应时间从 2 秒降到 200 毫秒！ ⚡

---

### 场景 3：追踪 API 调用链路

**问题：** 某个 API 报错了，不知道执行了哪些查询。

**解决方案：**

1. 打开 **ES DSL Monitor** 和 **SQL Monitor**
2. 清空所有历史记录
3. 调用出问题的 API
4. 在监控窗口查看：
   - 执行了哪些 SQL？
   - 执行了哪些 ES 查询？
   - 参数是否正确？
   - 哪一步出错了？
5. 定位问题根源

**效果：** 5 分钟定位问题，而不是 2 小时！ 🎯

---

### 场景 4：学习和理解 ORM 行为

**问题：** 不确定 MyBatis / Spring Data ES 生成的查询是什么样的。

**解决方案：**

1. 打开对应的监控工具
2. 执行各种操作
3. 观察生成的查询
4. 学习：
   - 分页是如何实现的？
   - 排序是如何处理的？
   - 复杂条件是如何转换的？
   - 关联查询是如何执行的？

**效果：** 快速掌握框架行为，写出更高效的代码！ 📚

---

### 场景 5：代码审查和性能优化

**问题：** 需要审查同事的代码，确保查询合理。

**解决方案：**

1. 运行同事的代码
2. 打开监控工具
3. 执行各种操作
4. 检查：
   - 是否有冗余查询？
   - 是否有慢查询？
   - 索引是否使用正确？
   - 查询逻辑是否合理？
5. 提出优化建议

**效果：** 提升团队代码质量，避免性能问题上线！ 🛡️

---

## ❓ 常见问题

### Q1: 为什么没有捕获到 ES 查询？

**A:** 请检查以下几点：

1. ✅ **"启用 ES 监听"开关是否打开？**
   - 在 ES DSL Monitor 工具窗口顶部检查

2. ✅ **日志配置是否正确？**
   - 检查 `logback.xml` 或 `application.yml`
   - 确认 `tracer` 日志级别为 `TRACE`
   - 确认日志输出到控制台（`STDOUT`）

3. ✅ **应用程序是否在 IDEA 中运行？**
   - 必须通过 IDEA 的 Run/Debug 启动
   - 外部启动的应用无法监控

4. ✅ **是否执行了 ES 查询操作？**
   - 触发一个会调用 ES 的功能
   - 检查控制台是否有 `curl` 开头的日志

5. ✅ **日志格式是否支持？**
   - 查看 `docs/EsDslMonitor使用指南.md` 了解支持的格式

**调试方法：**

在控制台搜索 `curl` 或 `tracer`，如果找不到，说明日志配置未生效。

---

### Q2: 为什么没有捕获到 SQL 查询？

**A:** 请检查以下几点：

1. ✅ **"启用 SQL 监听"开关是否打开？**
   - 在 SQL Monitor 工具窗口顶部检查

2. ✅ **日志配置是否正确？**
   - 检查 Mapper 包路径是否正确
   - 确认日志级别为 `DEBUG`
   - 确认日志输出到控制台

3. ✅ **项目是否使用 MyBatis？**
   - SQL Monitor 目前只支持 MyBatis
   - 不支持 JPA/Hibernate 原生日志（可能在未来版本支持）

4. ✅ **是否执行了数据库操作？**
   - 触发一个会查询数据库的功能
   - 检查控制台是否有 `Preparing:` 日志

**调试方法：**

在控制台搜索 `Preparing:` 或 `Parameters:`，如果找不到，说明日志配置未生效。

---

### Q3: 向量数据太长，日志难以阅读怎么办？

**A:** ES DSL Monitor 已经优化了向量数据的显示：

1. ✅ **自动截断**：向量数据会自动截断显示
2. ✅ **折叠显示**：可以折叠/展开向量字段
3. ✅ **复制功能**：可以单独复制 DSL（不包含向量）

**如果仍然觉得太长，可以：**

- 在详情面板中查看（更清晰）
- 使用搜索功能过滤
- 导出到文件后用编辑器查看

---

### Q4: API 路径显示 "N/A" 怎么办？

**A:** API 路径是通过解析日志中的上下文信息获取的。如果显示 "N/A"，可能是：

1. **日志中没有 API 路径信息**
   - 在 Controller 中添加日志：
     ```java
     @GetMapping("/api/user/list")
     public Result list() {
         log.info("API:/api/user/list");  // 添加这行
         // ...
     }
     ```

2. **API 日志和查询日志间隔太远**
   - 缓冲区大小有限，如果间隔太远可能无法关联
   - 建议在查询前后都打印日志

3. **日志格式不匹配**
   - 确保日志包含 `API:` 或 `URI:` 关键词
   - 支持的格式：
     - `API:/api/user/list`
     - `URI:/api/user/list`
     - `Request URI: /api/user/list`

---

### Q5: 监控工具会影响性能吗？

**A:** 几乎不会！

**性能优化措施：**

1. ✅ **异步处理**：所有解析都在后台线程执行
2. ✅ **智能过滤**：只处理相关日志，过滤无关内容
3. ✅ **缓冲优化**：合理的缓冲区大小，避免内存溢出
4. ✅ **去重机制**：避免重复记录占用资源
5. ✅ **自动清理**：超过 1000 条自动清理旧记录

**性能测试结果：**

- CPU 占用：< 1%
- 内存占用：< 50MB
- 对应用性能影响：可忽略不计

**建议：**

- 开发环境：始终开启
- 生产环境：不需要（插件只在 IDEA 中运行）

---

### Q6: 可以同时监控 ES 和 SQL 吗？

**A:** 当然可以！而且互不干扰！

**技术保障：**

1. ✅ **独立监听器**：ES 和 SQL 使用不同的监听器
2. ✅ **智能过滤**：ES Monitor 过滤掉 SQL 日志，SQL Monitor 过滤掉 ES 日志
3. ✅ **独立存储**：数据分别存储在不同的文件
4. ✅ **独立 UI**：两个独立的工具窗口

**最佳实践：**

```
同时打开两个监控窗口，全面掌握应用的数据访问情况！
```

---

### Q7: 日志太多，影响性能怎么办？

**A:** 有几种解决方案：

**方案 1：只在需要时启用**

```yaml
# application-dev.yml（开发环境）
logging:
  level:
    tracer: TRACE
    com.yourpackage.mapper: DEBUG

# application-prod.yml（生产环境）
logging:
  level:
    tracer: INFO  # 关闭详细日志
    com.yourpackage.mapper: INFO
```

**方案 2：使用环境变量控制**

```yaml
logging:
  level:
    tracer: ${ES_LOG_LEVEL:INFO}  # 默认 INFO，需要时改为 TRACE
    com.yourpackage.mapper: ${SQL_LOG_LEVEL:INFO}
```

启动时：
```bash
java -jar app.jar --ES_LOG_LEVEL=TRACE --SQL_LOG_LEVEL=DEBUG
```

**方案 3：使用独立的日志文件**

```xml
<!-- 单独的 ES 日志文件 -->
<appender name="ES_LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <File>logs/elasticsearch.log</File>
    <!-- ... -->
</appender>

<logger name="tracer" level="TRACE" additivity="false">
    <appender-ref ref="STDOUT" />
    <appender-ref ref="ES_LOG" />  <!-- 同时输出到文件 -->
</logger>
```

---

### Q8: 可以导出查询记录吗？

**A:** 可以！支持多种导出方式：

**方式 1：导出单条记录**

1. 在表格中选中一条记录
2. 点击 **"导出选中"** 按钮
3. 记录已复制到剪贴板
4. 粘贴到任何地方

**方式 2：复制可执行 SQL/DSL**

1. 选中记录，查看详情
2. 点击 **"复制 SQL"** 或 **"复制 DSL"**
3. 直接粘贴到数据库工具或 Kibana

**方式 3：批量导出（手动）**

查询记录保存在：
- ES: `.idea/es-dsl-records.json`
- SQL: `.idea/sql-records.json`

可以直接复制这些文件进行备份或分析。

---

## 🔬 技术原理

### 工作原理概述

```
┌─────────────────────────────────────────────────────────────┐
│                     你的应用程序                              │
│  ┌──────────────┐              ┌──────────────┐            │
│  │  Controller  │              │   Service    │            │
│  └──────┬───────┘              └──────┬───────┘            │
│         │                              │                    │
│         │ ① 调用                       │ ② 执行查询         │
│         ▼                              ▼                    │
│  ┌──────────────┐              ┌──────────────┐            │
│  │    Mapper    │              │ ES Template  │            │
│  └──────┬───────┘              └──────┬───────┘            │
│         │                              │                    │
│         │ ③ 输出日志                   │ ③ 输出日志         │
│         ▼                              ▼                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              IDEA 控制台 (Console)                   │   │
│  │  ==> Preparing: SELECT ...                          │   │
│  │  curl -X POST "localhost:9200/..."                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ ④ 监听控制台输出
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    PandaCoder 插件                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           ExecutionManager (进程管理器)               │   │
│  │  监听应用启动/停止，获取 ProcessHandler               │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │ ⑤ 附加监听器                         │
│                       ▼                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │        ProcessListener (进程监听器)                   │   │
│  │  • EsDslOutputListener                               │   │
│  │  • SqlOutputListener                                 │   │
│  │  逐行接收控制台文本                                   │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │ ⑥ 智能过滤和缓冲                     │
│                       ▼                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │            Parser (解析器)                            │   │
│  │  • EsDslParser: 解析 ES 查询                         │   │
│  │  • SqlParser: 解析 SQL 查询                          │   │
│  │  提取关键信息（方法、索引、表名、参数等）              │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │ ⑦ 保存记录                           │
│                       ▼                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         RecordService (记录服务)                      │   │
│  │  • 去重                                               │   │
│  │  • 持久化 (JSON)                                      │   │
│  │  • 通知 UI 更新                                       │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │ ⑧ 实时更新                           │
│                       ▼                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          ToolWindow (工具窗口)                        │   │
│  │  • 表格展示                                           │   │
│  │  • 详情面板                                           │   │
│  │  • 搜索/过滤/导出                                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 核心技术点

#### 1. 进程监听机制

```java
// 监听应用启动事件
ApplicationManager.getApplication().getMessageBus()
    .connect(project)
    .subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
        @Override
        public void processStarted(..., ProcessHandler handler) {
            // 应用启动时，自动附加监听器
            attachListener(handler);
        }
    });
```

**关键点：**
- 使用 IntelliJ Platform 的 `ExecutionManager` API
- 监听所有运行配置的启动/停止事件
- 自动附加/移除监听器，无需手动操作

#### 2. 控制台输出监听

```java
public class EsDslOutputListener implements ProcessListener {
    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
        String text = event.getText();  // 逐行接收控制台文本
        
        // 智能过滤
        if (shouldKeepText(text)) {
            buffer.append(text);  // 添加到缓冲区
        }
        
        // 检测完整日志
        if (shouldTriggerParse(text)) {
            triggerAsyncParse();  // 异步解析
        }
    }
}
```

**关键点：**
- 实现 `ProcessListener` 接口
- 逐行接收控制台输出
- 智能过滤无关日志
- 使用缓冲区处理跨行日志

#### 3. 智能过滤算法

```java
private boolean shouldKeepText(String text) {
    String lowerText = text.toLowerCase();
    
    // ES Monitor: 过滤掉 SQL 日志
    if (lowerText.contains("basejdbclogger") ||
        lowerText.contains("preparing:")) {
        return false;  // 不保留
    }
    
    // SQL Monitor: 过滤掉 ES 日志
    if (lowerText.contains("requestlogger") ||
        lowerText.contains("elasticsearch")) {
        return false;  // 不保留
    }
    
    // 保留相关日志
    if (lowerText.contains("tracer") ||  // ES 日志
        lowerText.contains("preparing:")) {  // SQL 日志
        return true;
    }
    
    return false;
}
```

**关键点：**
- ES 和 SQL 监听器互相过滤对方的日志
- 避免误判和重复记录
- 提高解析效率

#### 4. 异步解析

```java
private void triggerAsyncParse() {
    // 避免并发解析
    if (!isParsing.compareAndSet(false, true)) {
        return;
    }
    
    // 获取缓冲区快照
    final String bufferedText = buffer.toString();
    
    // 在后台线程异步解析
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
            parseAndSave(bufferedText);
        } finally {
            isParsing.set(false);
        }
    });
}
```

**关键点：**
- 使用 `AtomicBoolean` 避免并发解析
- 在后台线程执行，不阻塞 IDEA
- 使用快照避免缓冲区被修改

#### 5. 智能去重

```java
private boolean isDuplicate(EsDslRecord newRecord) {
    long now = System.currentTimeMillis();
    
    for (EsDslRecord existing : records) {
        // 时间窗口检查（5秒内）
        if (now - existing.getTimestamp() > 5000) {
            continue;
        }
        
        // 内容相似度检查
        if (isSimilar(newRecord, existing)) {
            return true;  // 重复
        }
    }
    
    return false;
}

private boolean isSimilar(EsDslRecord r1, EsDslRecord r2) {
    return r1.getMethod().equals(r2.getMethod()) &&
           r1.getIndex().equals(r2.getIndex()) &&
           r1.getEndpoint().equals(r2.getEndpoint()) &&
           normalizeJson(r1.getDslQuery()).equals(normalizeJson(r2.getDslQuery()));
}
```

**关键点：**
- 时间窗口去重（5 秒内相同查询只保留 1 条）
- 多维度相似度判断
- 忽略空白字符差异

#### 6. 持久化存储

```java
private void saveToFile() {
    try {
        String json = objectMapper.writerWithDefaultPrettyPrinter()
                                   .writeValueAsString(records);
        
        File file = new File(project.getBasePath(), ".idea/es-dsl-records.json");
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
        
    } catch (Exception e) {
        LOG.error("Failed to save records", e);
    }
}
```

**关键点：**
- 使用 JSON 格式存储
- 保存在项目的 `.idea` 目录
- 自动加载历史记录

---

## 🎉 总结

### 为什么选择 PandaCoder 的监控工具？

#### ✅ 对比其他方案

| 特性 | PandaCoder | MyBatis Log Plugin | 手动打印日志 | 数据库工具 |
|------|-----------|-------------------|-------------|-----------|
| **ES 查询监控** | ✅ 完美支持 | ❌ 不支持 | ⚠️ 需要手动 | ❌ 不支持 |
| **SQL 查询监控** | ✅ 完美支持 | ✅ 支持 | ⚠️ 需要手动 | ⚠️ 事后分析 |
| **API 关联** | ✅ 自动关联 | ❌ 不支持 | ⚠️ 需要手动 | ❌ 不支持 |
| **实时监控** | ✅ 实时 | ✅ 实时 | ⚠️ 需要查看日志 | ❌ 事后 |
| **零代码侵入** | ✅ 零侵入 | ✅ 零侵入 | ❌ 需要修改代码 | ✅ 零侵入 |
| **可执行 SQL/DSL** | ✅ 一键复制 | ✅ 一键复制 | ⚠️ 需要手动拼接 | ✅ 支持 |
| **历史记录** | ✅ 自动保存 | ⚠️ 有限 | ❌ 不保存 | ✅ 保存 |
| **性能影响** | ✅ 几乎无 | ✅ 几乎无 | ⚠️ 有影响 | ✅ 无影响 |

#### 🏆 核心优势

1. **一站式解决方案**
   - 同时支持 ES 和 SQL 监控
   - 无需安装多个插件

2. **智能化**
   - 自动过滤无关日志
   - 智能去重
   - 自动关联 API

3. **开发友好**
   - 零代码侵入
   - 一键复制可执行查询
   - 实时更新

4. **性能优异**
   - 异步处理
   - 智能缓冲
   - 几乎零性能损耗

5. **持续更新**
   - 活跃维护
   - 持续优化
   - 社区支持

---

## 📞 获取帮助

### 遇到问题？

1. **查看日志**
   - `Help` → `Show Log in Explorer`
   - 搜索 `[ES DSL]` 或 `[SQL Monitor]`

2. **联系作者**
   - 📧 Email: yixiaoshu88@163.com
   - 🌐 Website: https://www.shuyixiao.cn
   - 💬 GitHub Issues: [提交问题](https://github.com/your-repo/issues)

### 反馈建议

我们欢迎任何反馈和建议！

- ⭐ 如果觉得好用，请给项目点个 Star
- 🐛 发现 Bug？请提交 Issue
- 💡 有新想法？欢迎 Pull Request

---

## 🎊 开始使用吧！

现在你已经了解了 ES DSL Monitor 和 SQL Monitor 的所有功能和配置方法。

**立即行动：**

1. ✅ 按照配置指南配置日志
2. ✅ 重启应用程序
3. ✅ 打开监控工具窗口
4. ✅ 开始享受可视化监控的便利！

**记住：**
- 📝 ES 需要配置 `tracer` 日志为 `TRACE`
- 📝 SQL 需要配置 Mapper 包路径为 `DEBUG`
- 📝 两个监控可以同时使用，互不干扰

---

**祝你开发愉快！** 🚀

**PandaCoder - 让中文开发者的编程更高效！**

---

*文档版本：v2.0.0*  
*更新时间：2025-10-19*  
*作者：舒一笑不秃头*

