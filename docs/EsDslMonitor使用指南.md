# ES DSL Monitor 使用指南

## 📖 功能简介

ES DSL Monitor 是 PandaCoder 插件新增的 Elasticsearch 查询监控功能，类似于 MyBatis Log 插件，能够在 IDEA 中实时捕获和展示应用程序执行的 Elasticsearch DSL 查询。

## 🎯 主要功能

### 核心特性

- **实时监控**：自动监控控制台输出，实时捕获 ES 查询
- **智能解析**：自动识别和解析多种格式的 ES DSL 查询
- **可视化展示**：在独立的工具窗口中清晰展示查询信息
- **查询管理**：支持搜索、过滤、导出等功能
- **本地存储**：自动保存查询历史，方便后续查看

### 支持的格式

1. **REST Client 格式**
   ```
   POST http://localhost:9200/users/_search {"query": {"match_all": {}}}
   ```

2. **cURL 格式**
   ```
   curl -X POST "localhost:9200/users/_search" -d '{"query": {"match_all": {}}}'
   ```

3. **Spring Data Elasticsearch 日志格式**
   ```
   Elasticsearch Query: {"query": {"match_all": {}}}
   ```

4. **纯 JSON DSL 格式**
   ```json
   {
     "query": {
       "match_all": {}
     }
   }
   ```

## 🚀 快速开始

### 1. 打开工具窗口

在 IntelliJ IDEA 底部工具栏中找到并点击 **"ES DSL Monitor"** 工具窗口。

### 2. 启用监听

在工具窗口顶部，确保 **"启用ES监听"** 复选框已勾选。

### 3. 运行应用程序

运行包含 Elasticsearch 查询的应用程序，插件会自动捕获并显示 ES DSL 查询。

## 📊 界面说明

### 工具栏功能

- **启用ES监听**：开关 ES DSL 监听功能
- **搜索框**：根据关键词搜索查询记录
- **方法过滤**：按 HTTP 方法（GET/POST/PUT/DELETE）筛选
- **时间过滤**：按时间范围筛选查询记录
- **刷新**：手动刷新查询列表
- **清空所有**：清空所有查询记录
- **导出选中**：将选中的查询导出到剪贴板

### 查询列表

显示捕获到的所有 ES DSL 查询，包含以下信息：

| 列名 | 说明 |
|------|------|
| 方法 | HTTP 方法（GET/POST/PUT/DELETE），带颜色标识 |
| 索引 | 查询的索引名称 |
| DSL摘要 | 查询语句的简短预览 |
| 执行时间 | 查询执行耗时（如果可用） |
| 时间戳 | 查询发生的时间 |
| 状态 | HTTP 状态码 |

### 详情面板

点击查询记录后，在下方详情面板中会显示：

- 完整的查询信息
- DSL 查询内容（带格式化）
- 响应信息（如果可用）

### 按钮功能

- **复制 DSL**：只复制 DSL 查询部分
- **复制全部**：复制所有详细信息
- **格式化**：格式化 JSON 内容（开发中）

### 状态栏

显示当前监听状态和统计信息：

```
监听状态: 启用 | 活动监听器: 1 | 总查询: 45 | 成功: 43 | 失败: 2 | 索引数: 5
```

## 💡 使用技巧

### 1. 快速查找特定查询

使用搜索框可以快速定位包含特定关键词的查询：

- 搜索索引名称
- 搜索查询内容
- 搜索端点路径

### 2. 按方法筛选

使用方法过滤器可以快速查看特定类型的请求：

- **GET**：查询操作（蓝色）
- **POST**：创建/搜索操作（绿色）
- **PUT**：更新操作（橙色）
- **DELETE**：删除操作（红色）

### 3. 时间范围过滤

根据需要选择合适的时间范围：

- **最近1小时**：查看最近的查询
- **最近6小时**：适合调试当前会话
- **最近12小时**：查看半天的查询记录
- **最近24小时**：查看一天的查询记录
- **全部**：查看所有历史记录

### 4. 导出查询

选中感兴趣的查询记录后，点击 **"导出选中"** 可以将查询信息导出为 Markdown 格式：

```markdown
# ES DSL 查询导出

## 基本信息
- 时间: 2024-10-17 15:30:45
- 方法: POST
- 索引: users
- 端点: users/_search

## DSL 查询
```json
{
  "query": {
    "match": {
      "name": "张三"
    }
  }
}
```
```

### 5. 双击查看详情

双击查询记录可以打开一个独立的对话框，更清晰地查看完整的查询信息。

## ⚙️ 配置说明

### 数据存储

查询记录会自动保存在项目的 `.idea/es-dsl-records.json` 文件中，最多保存 1000 条记录。

### 自动刷新

工具窗口每 10 秒自动刷新一次数据，确保显示最新的查询记录。

### 缓冲区管理

插件使用智能缓冲区管理，避免重复解析和内存溢出：

- 缓冲区最大 10000 字符
- 自动清理已解析的内容
- 跨行 DSL 支持

## 🔧 故障排除

### 问题：没有捕获到查询

**可能原因及解决方案：**

1. **监听未启用**
   - 确保工具窗口中的"启用ES监听"复选框已勾选

2. **日志级别不足**
   - 确保应用程序的日志级别足够详细
   - 建议将 Elasticsearch 客户端的日志级别设置为 DEBUG

3. **日志格式不匹配**
   - 插件支持常见的日志格式，如果使用自定义格式可能无法识别
   - 可以在日志中添加关键词：`elasticsearch`、`_search`、`es query` 等

### 问题：查询显示不完整

**解决方案：**

- 如果查询内容很长，可能被截断
- 点击查询记录查看完整的详情面板
- 使用"复制全部"功能导出完整内容

### 问题：执行时间显示 N/A

**原因：**

- 某些日志格式可能不包含执行时间信息
- 插件会尝试从日志中提取 `took`、`time`、`duration` 等字段

## 📝 日志配置示例

### Spring Boot + Elasticsearch

在 `application.yml` 中配置：

```yaml
logging:
  level:
    org.elasticsearch.client: DEBUG
    org.springframework.data.elasticsearch: DEBUG
```

### Logback 配置

在 `logback.xml` 中添加：

```xml
<logger name="org.elasticsearch.client" level="DEBUG"/>
<logger name="org.springframework.data.elasticsearch" level="DEBUG"/>
```

### Log4j2 配置

在 `log4j2.xml` 中添加：

```xml
<Logger name="org.elasticsearch.client" level="debug"/>
<Logger name="org.springframework.data.elasticsearch" level="debug"/>
```

## 🎨 UI 特性

### 颜色编码

- **HTTP 方法颜色**：
  - GET: 蓝色 (#61AFFE)
  - POST: 绿色 (#49CC90)
  - PUT: 橙色 (#FCA130)
  - DELETE: 红色 (#F93E3E)

- **状态码颜色**：
  - 2xx: 绿色（成功）
  - 4xx/5xx: 红色（失败）

### 表格布局

- 自动调整列宽
- 支持单选
- 悬停高亮
- 双击查看详情

## 🔄 版本历史

### v1.2.0 (预计)
- ✅ 实时 ES DSL 监控
- ✅ 多格式 DSL 解析
- ✅ 可视化查询展示
- ✅ 查询历史管理
- ✅ 搜索和过滤功能
- ✅ 导出功能
- 🔄 JSON 高级格式化（开发中）
- 🔄 查询性能分析（计划中）
- 🔄 查询对比功能（计划中）

## 🤝 反馈与支持

如果您在使用过程中遇到问题或有功能建议，欢迎通过以下方式联系：

- **GitHub Issues**: [PandaCoder Issues](https://github.com/shuyixiao-better/PandaCoder/issues)
- **微信公众号**: 舒一笑的架构笔记
- **邮箱**: yixiaoshu88@163.com

---

**让开发更高效，让调试更简单！** 🚀

