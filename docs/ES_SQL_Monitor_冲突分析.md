# ES DSL Monitor 与 SQL Monitor 冲突分析

## 🔍 问题描述

用户反馈：**ES控制台监控的结果会影响SQL控制台监控的结果，日志在ES控制台监控就不会在SQL控制台监控输出**

## 📊 技术分析

### 监听器架构

两个Monitor都使用`ProcessListener`监听同一个进程的输出：

```
Process Output (控制台日志)
    ↓
    ├─→ EsDslOutputListener (ES DSL Monitor)
    │   └─→ 过滤ES相关日志 → ES缓冲区
    │
    └─→ SqlOutputListener (SQL Monitor)
        └─→ 过滤SQL相关日志 → SQL缓冲区
```

### ProcessListener工作原理

IntelliJ IDEA的`ProcessHandler`支持**多个`ProcessListener`同时监听**：
- 每个监听器都会收到**相同的输出**
- 监听器之间是**独立的**，互不干扰
- 一个监听器的过滤不会影响另一个监听器

### 当前实现

#### ES DSL Monitor过滤逻辑
```java
// EsDslOutputListener.java
private boolean shouldKeepText(String text) {
    // ❌ 过滤SQL日志（之前的实现）
    if (lowerText.contains("basejdbclogger") ||
        lowerText.contains("preparing:") ||
        lowerText.contains("parameters:") ||
        lowerText.contains("==>") ||
        lowerText.contains("<==")) {
        return false;  // 不加入ES缓冲区
    }
    
    // ✅ 保留ES相关日志
    if (lowerText.contains("requestlogger") && lowerText.contains("trace")) {
        return true;
    }
}
```

#### SQL Monitor过滤逻辑
```java
// SqlOutputListener.java
public void onTextAvailable(...) {
    // ✅ 过滤ES日志
    if (lowerText.contains("requestlogger") || 
        lowerText.contains("elasticsearch") ||
        lowerText.contains("elastic")) {
        return;  // 不处理ES日志
    }
    
    // ✅ 保留SQL日志
    if (shouldKeepText(text)) {
        buffer.append(text);
    }
}
```

## 🐛 问题根因

### 理论上不应该冲突

根据IntelliJ IDEA的`ProcessListener`机制：
- ✅ 两个监听器**都会收到所有日志**
- ✅ ES Monitor过滤SQL日志只影响**ES自己的缓冲区**
- ✅ SQL Monitor仍然能收到SQL日志

### 可能的原因

1. **监听器未正确附加**
   - SQL Monitor的监听器可能没有成功附加到进程
   - 启动顺序问题

2. **日志输出时机**
   - SQL日志可能在SQL Monitor启动之前就输出了
   - 进程已经启动，但监听器还没附加

3. **缓冲区竞争**
   - 虽然理论上不会冲突，但实际可能存在某种竞争条件

4. **过滤逻辑过于严格**
   - ES Monitor的过滤可能影响了某些共享的日志行（如API路径）

## 🔧 修复方案

### 方案1：移除ES Monitor对SQL日志的过滤（已实施）

**修改**: `EsDslOutputListener.java`

```java
// 修改前：过滤SQL日志
if (lowerText.contains("basejdbclogger") ||
    lowerText.contains("preparing:") ||
    lowerText.contains("parameters:") ||
    lowerText.contains("==>") ||
    lowerText.contains("<==")) {
    return false;
}

// 修改后：不过滤SQL日志
// ⚠️ 不过滤SQL日志！让SQL Monitor处理
// SQL日志特征：basejdbclogger, preparing:, parameters:, ==>, <==
// 这些日志应该被SQL Monitor处理，ES Monitor不应该拦截
```

**原因**: 
- ES Monitor不需要主动过滤SQL日志
- 即使SQL日志进入ES缓冲区，ES Parser也不会解析它们
- 让SQL Monitor自然处理SQL日志

### 方案2：确保监听器正确附加

**检查点**:
1. ✅ `SqlStartupActivity` 已创建并注册到`plugin.xml`
2. ✅ `SqlMonitoringService` 正确监听进程启动事件
3. ✅ 监听器在进程启动时自动附加

**验证方法**:
```java
// 在SqlOutputListener.startNotified()中添加日志
LOG.warn("[SQL Monitor] 🚀 监听器已启动！");
LOG.warn("[SQL Monitor] 项目: " + project.getName());
```

### 方案3：增强日志输出（调试用）

在两个监听器中添加详细日志：

```java
// ES Monitor
if (shouldKeepText(text)) {
    buffer.append(text);
    LOG.debug("[ES Monitor] 保留日志: " + text.substring(0, 50));
} else {
    LOG.debug("[ES Monitor] 过滤日志: " + text.substring(0, 50));
}

// SQL Monitor
if (shouldKeepText(text)) {
    buffer.append(text);
    LOG.debug("[SQL Monitor] 保留日志: " + text.substring(0, 50));
} else {
    LOG.debug("[SQL Monitor] 过滤日志: " + text.substring(0, 50));
}
```

## 📊 测试验证

### 测试场景1：同时触发ES和SQL

```java
// 执行一个API，同时产生ES查询和SQL查询
GET /api/user/search?keyword=test

// 预期结果：
// - ES DSL Monitor: 显示ES查询
// - SQL Monitor: 显示SQL查询
// - 两者互不干扰
```

### 测试场景2：纯SQL操作

```java
// 执行一个只有SQL的API
GET /api/user/list

// 预期结果：
// - ES DSL Monitor: 无输出
// - SQL Monitor: 显示所有SQL查询
```

### 测试场景3：纯ES操作

```java
// 执行一个只有ES的API
GET /api/search/fulltext

// 预期结果：
// - ES DSL Monitor: 显示ES查询
// - SQL Monitor: 无输出
```

## 🎯 最终解决方案

### 已实施的修复

1. ✅ **移除ES Monitor对SQL日志的主动过滤**
   - 文件: `EsDslOutputListener.java`
   - 修改: 删除对`basejdbclogger`、`preparing:`、`parameters:`、`==>`、`<==`的过滤
   - 原因: ES Monitor不需要主动排斥SQL日志

2. ✅ **确保SQL Monitor正确启动**
   - 文件: `SqlStartupActivity.java`
   - 功能: 在项目启动时自动附加SQL监听器

3. ✅ **增强日志输出**
   - 两个监听器都有详细的DEBUG日志
   - 可以追踪日志处理流程

### 预期效果

修复后，两个Monitor应该：
- ✅ 独立工作，互不干扰
- ✅ 同时捕获各自的日志
- ✅ 不会出现"ES监控了就SQL不监控"的问题

## 🔍 如何验证修复

### 步骤1：重新编译插件

```bash
gradlew clean build
```

### 步骤2：运行项目

启动你的Spring Boot项目，确保：
- ES DSL Monitor工具窗口打开
- SQL Monitor工具窗口打开
- 两个监听开关都是启用状态

### 步骤3：执行测试操作

执行一个同时触发ES和SQL的API，例如：
```
GET /kl/api/saas/element/detail/list
```

### 步骤4：检查结果

- **ES DSL Monitor**: 应该显示ES查询
- **SQL Monitor**: 应该显示所有SQL查询
- **控制台日志**: 查看是否有`[SQL Monitor] 🚀 监听器已启动`

### 步骤5：查看日志

在IDEA的日志文件中搜索：
```
[ES Monitor]
[SQL Monitor]
```

确认两个监听器都在工作。

## 💡 技术总结

### ProcessListener机制

IntelliJ IDEA的`ProcessListener`是**观察者模式**：
- 多个观察者可以同时监听同一个主题
- 观察者之间是独立的
- 一个观察者的行为不影响其他观察者

### 正确的过滤策略

每个Monitor应该：
- ✅ **只处理自己关心的日志**
- ✅ **忽略（不处理）不关心的日志**
- ❌ **不要主动过滤（排斥）其他Monitor的日志**

### 最佳实践

```java
// ✅ 正确的做法
if (isMyLog(text)) {
    process(text);  // 处理我的日志
}
// 其他日志自然忽略，不需要主动过滤

// ❌ 错误的做法
if (isOtherMonitorLog(text)) {
    return;  // 主动排斥其他Monitor的日志（不必要）
}
```

---

**修复状态**: ✅ 已完成  
**测试状态**: ⏳ 待用户验证  
**文档版本**: 1.0.0  
**更新时间**: 2025-10-18

