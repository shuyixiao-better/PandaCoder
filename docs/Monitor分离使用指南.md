# ES DSL Monitor 与 SQL Monitor 分离使用指南

## 🎯 概述

ES DSL Monitor 和 SQL Monitor 现在已经完全分离，各自独立工作，互不干扰。

## ✅ 修复完成

### 问题
- ❌ ES DSL Monitor 显示了 SQL 日志
- ❌ SQL Monitor 显示了 ES 日志  
- ❌ 数据重复和内容错乱

### 解决方案
- ✅ ES DSL Monitor **只提取** ES 交互相关的接口和完整可执行的 DSL
- ✅ SQL Monitor **只提取** SQL 相关的日志内容
- ✅ 两者互不影响，完全独立

## 📊 测试结果

使用 `日志.txt` 文件进行测试验证：

```
总日志行数: 210

ES DSL Monitor 保留行数: 8
错误保留SQL日志数: 0
✅ ES DSL Monitor 过滤正确

SQL Monitor 保留行数: 54
错误保留ES日志数: 0
✅ SQL Monitor 过滤正确

冲突检测: 0条冲突
✅ 两个Monitor完全分离
```

## 🔍 日志特征识别

### ES 日志特征
ES DSL Monitor 会捕获包含以下特征的日志：
- `RequestLogger` + `TRACE`
- `curl -iX POST/GET/PUT/DELETE`
- `_search`, `_cluster`
- `elasticsearch`
- ES 响应（以 `#` 开头的行）

### SQL 日志特征
SQL Monitor 会捕获包含以下特征的日志：
- `BaseJdbcLogger`
- `==> Preparing:`
- `==> Parameters:`
- `<== Total:`

## 🚀 使用方法

### 1. 启用 ES DSL Monitor

1. 打开 IDEA 底部的 **"ES DSL Monitor"** 工具窗口
2. 勾选 **"启用ES监听"** 复选框
3. 运行应用程序
4. ES 查询会自动显示在 ES DSL Monitor 中

**显示内容**：
- ✅ Elasticsearch 查询 DSL
- ✅ 索引名称、HTTP方法、端点
- ✅ API 路径、调用类
- ❌ 不会显示 SQL 日志

### 2. 启用 SQL Monitor

1. 打开 IDEA 底部的 **"SQL Monitor"** 工具窗口
2. 勾选 **"启用SQL监听"** 复选框
3. 运行应用程序
4. SQL 查询会自动显示在 SQL Monitor 中

**显示内容**：
- ✅ SQL 语句（SELECT/INSERT/UPDATE/DELETE）
- ✅ 参数值、表名、结果数量
- ✅ API 路径、调用类
- ❌ 不会显示 ES 日志

## 🔧 配置要求

### ES DSL Monitor 日志配置

在 `logback-local.xml` 中添加：

```xml
<!-- Elasticsearch RequestLogger TRACE 日志 -->
<logger name="tracer" level="TRACE">
    <appender-ref ref="STDOUT" />
</logger>
```

### SQL Monitor 日志配置

MyBatis 日志配置（通常已默认开启）：

```xml
<!-- MyBatis SQL 日志 -->
<logger name="com.baomidou.mybatisplus" level="DEBUG">
    <appender-ref ref="STDOUT" />
</logger>
```

## 📋 验证分离效果

### 检查 ES DSL Monitor

在 ES DSL Monitor 中查看日志，确保：
- ✅ 只显示 `RequestLogger` 相关的日志
- ✅ 包含 `curl` 命令和 JSON DSL
- ❌ **不应该**包含 `BaseJdbcLogger`
- ❌ **不应该**包含 `Preparing:` 或 `Parameters:`

### 检查 SQL Monitor

在 SQL Monitor 中查看日志，确保：
- ✅ 只显示 `BaseJdbcLogger` 相关的日志
- ✅ 包含 SQL 语句和参数
- ❌ **不应该**包含 `RequestLogger`
- ❌ **不应该**包含 `curl` 命令或 `_search`

## 🎨 界面示例

### ES DSL Monitor 界面
```
┌─────────────────────────────────────────────┐
│ ☑ 启用ES监听  🔍 [搜索]  [方法▼] [时间▼]    │
├─────────────────────────────────────────────┤
│ 方法   索引                    端点         │
├─────────────────────────────────────────────┤
│ GET   _cluster              _cluster/health │
│ POST  dataset_chunk_...     .../_search     │
│ GET   _cluster              _cluster/health │
└─────────────────────────────────────────────┘
```

### SQL Monitor 界面
```
┌─────────────────────────────────────────────┐
│ ☑ 启用SQL监听  🔍 [搜索]  [操作▼] [时间▼]   │
├─────────────────────────────────────────────┤
│ 操作    表名                   结果数        │
├─────────────────────────────────────────────┤
│ SELECT  app_channel_dingtalk    1           │
│ SELECT  saas_knowledge_element  6           │
│ SELECT  saas_sys_user          1           │
└─────────────────────────────────────────────┘
```

## 🐛 故障排查

### 问题：ES DSL Monitor 显示了 SQL 日志

**原因**：可能是旧版本的代码  
**解决**：确保使用最新修复后的代码（2025-10-19 之后）

### 问题：SQL Monitor 显示了 ES 日志

**原因**：可能是旧版本的代码  
**解决**：确保使用最新修复后的代码（2025-10-19 之后）

### 问题：两个 Monitor 都没有显示日志

**原因**：日志级别未正确配置  
**解决**：
1. 检查 `logback-local.xml` 配置
2. 确保 `tracer` 日志级别为 `TRACE`（ES）
3. 确保 MyBatis 日志级别为 `DEBUG`（SQL）
4. 重启应用程序

## 📝 技术说明

### 过滤机制

两个 Monitor 使用**互斥过滤策略**：

```java
// ES DSL Monitor
if (lowerText.contains("basejdbclogger") ||
    lowerText.contains("preparing:") ||
    lowerText.contains("parameters:")) {
    return false; // 拒绝SQL日志
}

// SQL Monitor
if (lowerText.contains("requestlogger") || 
    lowerText.contains("_search") ||
    lowerText.contains("elasticsearch")) {
    return false; // 拒绝ES日志
}
```

### 缓冲区独立

- ES DSL Monitor：独立缓冲区（300KB）
- SQL Monitor：独立缓冲区（300KB）
- 两者互不干扰

## ✅ 总结

经过修复后，ES DSL Monitor 和 SQL Monitor 现在能够：

1. ✅ **完全独立**：各自处理自己的日志类型
2. ✅ **互不干扰**：不会出现数据重复或错乱
3. ✅ **准确提取**：ES 提取完整 DSL，SQL 提取完整语句
4. ✅ **测试验证**：210行日志测试，0冲突

享受清晰、准确的日志监控体验！🎉

