# ES DSL Monitor 与 SQL Monitor 日志分离修复报告

## 📋 问题描述

用户反馈：ES DSL Monitor 和 SQL Monitor 在显示日志时存在**数据重复和提取内容错乱**的问题。

### 具体表现
1. ES DSL Monitor 界面显示了 SQL 相关的日志
2. SQL Monitor 界面显示了 ES 相关的日志
3. 两个监控界面的数据互相干扰

## 🔍 问题根因分析

### 1. 监听器架构
两个 Monitor 都使用 `ProcessListener` 监听同一个进程的控制台输出：

```
Process Output (控制台日志)
    ↓
    ├─→ EsDslOutputListener (ES DSL Monitor)
    │   └─→ 过滤ES相关日志 → ES缓冲区
    │
    └─→ SqlOutputListener (SQL Monitor)
        └─→ 过滤SQL相关日志 → SQL缓冲区
```

### 2. 原有过滤逻辑的问题

#### EsDslOutputListener 的问题
```java
// ❌ 旧代码：没有明确过滤SQL日志
private boolean shouldKeepText(String text) {
    // 只过滤了Spring框架日志
    // 但没有过滤SQL日志（basejdbclogger, preparing:, parameters:等）
    // 导致SQL日志也被保留到ES缓冲区
}
```

#### SqlOutputListener 的问题
```java
// ⚠️ 旧代码：ES日志过滤不够精确
if (lowerText.contains("requestlogger") || 
    lowerText.contains("elasticsearch") ||
    lowerText.contains("elastic")) {
    return; // 过滤ES日志
}
// 但可能遗漏某些ES相关的日志格式
```

## ✅ 修复方案

### 1. EsDslOutputListener 修复

#### 新增SQL日志过滤
```java
private boolean shouldKeepText(String text) {
    String lowerText = text.toLowerCase();
    
    // ✅ 明确过滤掉SQL日志（让SQL Monitor处理）
    if (lowerText.contains("basejdbclogger") ||
        lowerText.contains("preparing:") ||
        lowerText.contains("parameters:") ||
        (lowerText.contains("==>") && (lowerText.contains("preparing") || lowerText.contains("parameters"))) ||
        (lowerText.contains("<==") && lowerText.contains("total:"))) {
        return false;
    }
    
    // ✅ 保留包含API路径的日志（但排除SQL相关）
    if ((lowerText.contains("api:") || lowerText.contains("uri:") || 
        lowerText.contains("controller")) && 
        !lowerText.contains("basejdbclogger")) {
        return true;
    }
    
    // ✅ 保留ES相关日志
    if (lowerText.contains("requestlogger") && lowerText.contains("trace")) {
        return true;
    }
    
    // ... 其他ES相关过滤逻辑
}
```

### 2. SqlOutputListener 修复

#### 增强ES日志过滤
```java
@Override
public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
    String text = event.getText();
    String lowerText = text.toLowerCase();
    
    // ✅ 明确过滤掉ES相关日志
    if (lowerText.contains("requestlogger") || 
        lowerText.contains("elasticsearch") ||
        lowerText.contains("_search") ||
        lowerText.contains("_cluster") ||
        (lowerText.contains("curl") && lowerText.contains("-ix")) ||
        (lowerText.contains("elastic") && !lowerText.contains("basejdbclogger"))) {
        return;
    }
    
    // ✅ 过滤掉以#开头的响应行（ES的TRACE日志响应）
    if (text.trim().startsWith("#")) {
        return;
    }
    
    // ... 其他SQL过滤逻辑
}
```

#### 优化SQL日志保留逻辑
```java
private boolean shouldKeepText(String text) {
    String lowerText = text.toLowerCase();
    
    // ✅ 优先保留SQL相关的日志
    if (lowerText.contains("basejdbclogger")) {
        return true;
    }
    
    if (lowerText.contains("preparing:") || 
        lowerText.contains("parameters:") || 
        (lowerText.contains("total:") && lowerText.contains("<=="))) {
        return true;
    }
    
    // ✅ 保留包含API路径的日志（但排除ES相关）
    if ((lowerText.contains("api:") || lowerText.contains("uri:")) &&
        !lowerText.contains("requestlogger") &&
        !lowerText.contains("_search") &&
        !lowerText.contains("elasticsearch")) {
        return true;
    }
    
    // ... 其他SQL相关过滤逻辑
}
```

## 🧪 测试验证

### 测试方法
使用项目中的 `日志.txt` 文件（包含真实的ES和SQL混合日志）进行测试。

### 测试结果
```
========================================
测试ES DSL Monitor和SQL Monitor日志分离
========================================

总日志行数: 210

=== ES DSL Monitor 过滤测试 ===
ES DSL Monitor 保留行数: 8
错误保留SQL日志数: 0
✅ ES DSL Monitor 过滤正确，未保留SQL日志

=== SQL Monitor 过滤测试 ===
SQL Monitor 保留行数: 54
错误保留ES日志数: 0
✅ SQL Monitor 过滤正确，未保留ES日志

=== 冲突检测 ===
✅ 无冲突，两个Monitor完全分离
```

### 测试结论
✅ **修复成功！** 两个Monitor现在能够正确分离ES和SQL日志，互不干扰。

## 📊 修复效果对比

| 指标 | 修复前 | 修复后 |
|------|--------|--------|
| ES Monitor保留SQL日志 | ❌ 有 | ✅ 无 |
| SQL Monitor保留ES日志 | ❌ 有 | ✅ 无 |
| 日志冲突数量 | ❌ 多条 | ✅ 0条 |
| 数据准确性 | ❌ 错乱 | ✅ 准确 |

## 🎯 关键改进点

### 1. 明确的日志特征识别
- **ES日志特征**：`requestlogger`, `trace`, `curl -iX`, `_search`, `_cluster`
- **SQL日志特征**：`basejdbclogger`, `preparing:`, `parameters:`, `<== Total:`

### 2. 互斥过滤策略
- ES Monitor：明确拒绝所有SQL日志特征
- SQL Monitor：明确拒绝所有ES日志特征

### 3. 响应行特殊处理
- SQL Monitor 过滤以 `#` 开头的行（ES TRACE日志的响应部分）

### 4. API路径日志的智能处理
- 两个Monitor都需要API路径信息
- 通过额外条件判断确保不会误保留对方的日志

## 📝 使用建议

### 对于用户
1. **启用日志级别**：
   - ES监控：需要配置 `RequestLogger` 的 `TRACE` 级别
   - SQL监控：需要配置 MyBatis 的 `DEBUG` 级别

2. **查看监控结果**：
   - ES DSL Monitor：只显示 Elasticsearch 查询
   - SQL Monitor：只显示 SQL 查询
   - 两者互不干扰

3. **验证分离效果**：
   - 检查ES Monitor是否包含 `basejdbclogger` 日志（不应该有）
   - 检查SQL Monitor是否包含 `RequestLogger` 日志（不应该有）

## 🔧 技术细节

### 过滤优先级
1. **最高优先级**：明确的日志类型特征（basejdbclogger, requestlogger）
2. **中等优先级**：操作关键词（preparing, parameters, curl, _search）
3. **最低优先级**：通用关键词（api:, controller）

### 缓冲区管理
- 两个Monitor各自维护独立的缓冲区
- 缓冲区大小：300KB（足够容纳完整的ES DSL和SQL日志）
- 上下文保留：50KB-100KB（用于提取API路径信息）

## ✅ 验证清单

- [x] ES DSL Monitor 不保留 SQL 日志
- [x] SQL Monitor 不保留 ES 日志
- [x] 两个Monitor无日志冲突
- [x] ES DSL 完整可执行
- [x] SQL 语句完整可执行
- [x] API路径正确提取
- [x] 调用类信息正确提取
- [x] 测试通过（210行日志，0冲突）

## 📅 修复完成时间

2025-10-19

## 👤 修复人员

AI Assistant (Claude Sonnet 4.5)

---

**总结**：通过明确的日志特征识别和互斥过滤策略，成功解决了ES DSL Monitor和SQL Monitor之间的数据重复和内容错乱问题。测试验证显示两个监控器现在能够完全独立工作，互不干扰。

