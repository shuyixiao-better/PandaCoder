# ES DSL Monitor 项目名称使用说明

## ❓ 为什么 `parseEsDsl()` 需要传入 `project.getName()`？

### 问题背景

你在查看代码时发现：

```java
EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, project.getName());
```

疑问：**为什么需要传入项目名称？这个参数的作用是什么？**

---

## 💡 答案解析

### 1. **`projectName` 的作用**

`projectName` 用于**标识这条 ES DSL 查询来自哪个 IDEA 项目**。

#### 代码位置:
```java:src/main/java/com/shuyixiao/esdsl/parser/EsDslParser.java
public static EsDslRecord parseEsDsl(String text, String projectName) {
    try {
        EsDslRecord.Builder builder = EsDslRecord.builder()
            .project(projectName);  // ← 这里设置项目名称
        
        // ... 解析逻辑 ...
        
        return builder.build();
    }
    // ...
}
```

#### 数据模型:
```java:src/main/java/com/shuyixiao/esdsl/model/EsDslRecord.java
public class EsDslRecord {
    private String project;      // ← 项目名称
    private String method;       // HTTP 方法
    private String index;        // ES 索引
    private String dslQuery;     // DSL 查询
    private LocalDateTime timestamp;
    // ...
}
```

### 2. **为什么需要项目名称？**

#### 场景 1: **多项目开发**

当你在 IDEA 中同时打开多个项目时:

```
IDEA
├─ ais-server (你的项目)
│  └─ ES 查询: dataset_chunk_sharding_24_1536/_search
├─ another-service (其他项目)
│  └─ ES 查询: user_index/_search  
└─ third-project (第三个项目)
   └─ ES 查询: product_index/_search
```

**ES DSL Monitor 需要知道每条查询来自哪个项目**，这样你可以:
- ✅ 按项目筛选查询
- ✅ 区分不同项目的 ES 使用情况
- ✅ 快速定位问题所属项目

#### 场景 2: **数据持久化**

ES DSL Monitor 会将捕获的查询保存到本地文件:

```
.idea/es-dsl-records/
├─ ais-server_2025-10-18.json
├─ another-service_2025-10-18.json
└─ third-project_2025-10-18.json
```

**每个项目的查询记录分开存储**，便于管理和查询。

#### 场景 3: **查询历史追踪**

在 ES DSL Monitor 工具窗口中显示:

| 时间 | 项目 | 方法 | 索引 | 状态 |
|------|------|------|------|------|
| 16:49:32 | **ais-server** | POST | dataset_chunk... | 200 |
| 16:50:15 | another-service | GET | user_index | 200 |
| 16:51:03 | **ais-server** | POST | dataset_chunk... | 200 |

**项目列显示让你一眼看出查询来源**。

### 3. **如何获取项目名称？**

#### 在监听器中获取:

```java:src/main/java/com/shuyixiao/esdsl/listener/EsDslOutputListener.java
public class EsDslOutputListener implements ProcessListener {
    private final Project project;  // ← IntelliJ IDEA 的 Project 对象
    
    public EsDslOutputListener(@NotNull Project project) {
        this.project = project;
        // ...
    }
    
    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        // ...
        
        // 使用 project.getName() 获取当前项目名称
        EsDslRecord record = EsDslParser.parseEsDsl(
            bufferedText, 
            project.getName()  // ← 传入项目名称,例如 "ais-server"
        );
        
        // ...
    }
}
```

#### `project.getName()` 返回什么？

以你的项目为例:

```
项目目录: E:\Project\hzmj\ais-server
项目名称: project.getName() = "ais-server"  ← 这就是传给解析器的值
```

**不是完整路径,而是项目在 IDEA 中的显示名称。**

---

## 🎯 实际示例

### 你的日志示例分析

```log
2025-10-18 16:49:32,248 TRACE (RequestLogger.java:90)- curl -iX POST 
'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true' 
-d '{"from":0,"query":{...}}'
# HTTP/1.1 200 OK
```

#### 解析后生成的 `EsDslRecord`:

```json
{
  "project": "ais-server",        // ← 来自 project.getName()
  "method": "POST",
  "index": "dataset_chunk_sharding_24_1536",
  "endpoint": "dataset_chunk_sharding_24_1536/_search?typed_keys=true",
  "dslQuery": "{\n  \"from\": 0,\n  \"query\": {...}\n}",
  "source": "RequestLogger (TRACE)",
  "httpStatus": 200,
  "timestamp": "2025-10-18T16:49:32"
}
```

**`project` 字段标识这条查询来自 `ais-server` 项目。**

---

## 📊 完整数据流程

```
┌─────────────────────┐
│  IDEA 项目          │
│  ais-server         │
└──────┬──────────────┘
       │ 启动运行
       ▼
┌─────────────────────┐
│  控制台输出          │
│  TRACE 日志         │
└──────┬──────────────┘
       │ 监听
       ▼
┌─────────────────────┐
│  ProcessListener    │
│  捕获控制台文本      │
└──────┬──────────────┘
       │ 传递项目名称
       ▼
┌─────────────────────┐
│  EsDslParser        │
│  parseEsDsl(text,   │
│    "ais-server")    │ ← 这里需要项目名
└──────┬──────────────┘
       │ 解析生成
       ▼
┌─────────────────────┐
│  EsDslRecord        │
│  {                  │
│    project: "ais-   │
│      server",       │ ← 项目名称被保存
│    method: "POST",  │
│    ...              │
│  }                  │
└──────┬──────────────┘
       │ 保存到
       ▼
┌─────────────────────┐
│  RecordService      │
│  持久化存储          │
│  .idea/es-dsl-      │
│    records/         │
│    ais-server_      │
│      2025-10-18.    │
│      json           │ ← 按项目名分文件
└─────────────────────┘
       │ 展示在
       ▼
┌─────────────────────┐
│  ES DSL Monitor     │
│  工具窗口            │
│  [项目: ais-server] │ ← 显示项目名
└─────────────────────┘
```

---

## 🔧 如果不传项目名称会怎样？

### 假设代码改成:

```java
EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, null);
```

### 后果:

1. ❌ **无法区分多项目查询**
   - 所有项目的查询混在一起
   - 无法按项目筛选

2. ❌ **数据存储混乱**
   - 不知道保存到哪个文件
   - 查询历史难以管理

3. ❌ **UI 显示不完整**
   - 项目列为空或显示 "Unknown"
   - 用户体验差

### 正确做法:

```java
// ✅ 总是传入项目名称
EsDslRecord record = EsDslParser.parseEsDsl(
    bufferedText, 
    project.getName()  // 从 IntelliJ Project 对象获取
);
```

---

## 📖 相关代码位置

### 1. 解析器定义:
```
src/main/java/com/shuyixiao/esdsl/parser/EsDslParser.java
第 93 行: public static EsDslRecord parseEsDsl(String text, String projectName)
```

### 2. 数据模型:
```
src/main/java/com/shuyixiao/esdsl/model/EsDslRecord.java
第 15 行: private String project;
```

### 3. 监听器调用:
```
src/main/java/com/shuyixiao/esdsl/listener/EsDslOutputListener.java
第 244 行: EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, project.getName());
```

### 4. UI 显示:
```
src/main/java/com/shuyixiao/esdsl/ui/EsDslMonitorPanel.java
(工具窗口中会显示 record.getProject())
```

---

## ✅ 总结

### 为什么需要 `project.getName()`?

1. **多项目支持**: 区分不同 IDEA 项目的 ES 查询
2. **数据组织**: 按项目分文件存储查询历史
3. **用户体验**: 在 UI 中清晰显示查询来源
4. **问题定位**: 快速识别哪个项目的 ES 查询出问题

### 项目名称的来源:

- **来自**: `com.intellij.openapi.project.Project.getName()`
- **示例**: `"ais-server"` (不是完整路径)
- **用途**: 作为 `EsDslRecord.project` 字段的值

### 最佳实践:

```java
// ✅ 正确
EsDslRecord record = EsDslParser.parseEsDsl(text, project.getName());

// ❌ 错误
EsDslRecord record = EsDslParser.parseEsDsl(text, null);
EsDslRecord record = EsDslParser.parseEsDsl(text, "");
EsDslRecord record = EsDslParser.parseEsDsl(text, "unknown");
```

---

**文档创建时间**: 2025-10-18  
**适用版本**: PandaCoder 1.2.0+  
**作者**: PandaCoder Team

现在你知道为什么需要传入项目名称了! 🎉

