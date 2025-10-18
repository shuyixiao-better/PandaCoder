# ES DSL Monitor 调试指南 - 解决捕获不到 DSL 的问题

## 🔍 问题现象

**症状**: ES DSL Monitor 工具窗口中很久都没有显示捕获到的 DSL

**可能的原因:**
1. ✅ 触发条件太严格（已优化）
2. ⚠️ 日志级别不正确
3. ⚠️ 监听器未启动
4. ⚠️ 日志格式不匹配

---

## 🚀 最新优化（2025-10-18）

### 已完成的改进

#### 1. **降低触发门槛**
```java
// 旧值: 500 字符
MIN_PARSE_TRIGGER_SIZE = 200;  // ✅ 降低到 200

// 旧值: 100ms
MIN_PARSE_INTERVAL_MS = 50;    // ✅ 降低到 50ms
```

#### 2. **放宽触发条件**

**新增 5 种触发条件:**
1. ✅ TRACE 日志结束标记 (`]}}}` 或 `# {"took":`)
2. ✅ RequestLogger 日志（包括缓冲区检查）
3. ✅ curl 命令完整（检查 `-d` 和 JSON 结束）
4. ✅ ES 查询关键词 + JSON 对象
5. ✅ 缓冲区较大时自动触发（>5K）

#### 3. **增加详细调试日志**

**启用调试模式:**
```java
private static final boolean DEBUG_MODE = true;  // ✅ 已启用
```

**日志输出示例:**
```
[ES DSL] ✅ 触发解析：检测到 RequestLogger 日志，缓冲区大小: 15K
[ES DSL] 🔍 开始解析，文本长度: 15K
[ES DSL] 📝 包含 ES DSL 关键词，开始详细解析...
✅ 成功捕获 ES DSL 查询:
  ├─ 索引: dataset_chunk_sharding_24_1536
  ├─ 方法: POST
  ├─ 端点: dataset_chunk_sharding_24_1536/_search
  ├─ 来源: RequestLogger (TRACE)
  └─ DSL 长度: 12K
[ES DSL] 🧹 已清空缓冲区
```

---

## 📝 诊断步骤

### 第 1 步: 检查 IDEA 日志

#### 1.1 查看日志文件

**Windows:**
```
Help -> Show Log in Finder/Explorer
```

**位置:**
```
C:\Users\YourName\AppData\Local\JetBrains\IntelliJIdea202x.x\log\idea.log
```

#### 1.2 搜索关键词

在日志文件中搜索:
```
[ES DSL]
```

#### 1.3 分析日志输出

**情况 A: 有触发日志**
```
[ES DSL] ✅ 触发解析：检测到 RequestLogger 日志，缓冲区大小: 15K
[ES DSL] 🔍 开始解析，文本长度: 15K
```
→ **说明触发正常,继续看解析结果**

**情况 B: 没有触发日志**
```
(没有任何 [ES DSL] 相关日志)
```
→ **说明触发条件未满足,需要检查日志格式**

**情况 C: 解析失败**
```
[ES DSL] ✅ 触发解析：...
[ES DSL] 🔍 开始解析，文本长度: 15K
[ES DSL] ⚠️ 不包含 ES DSL 关键词，跳过
```
→ **说明日志不包含 ES 关键词**

**情况 D: 解析返回 null**
```
[ES DSL] ✅ 触发解析：...
[ES DSL] 🔍 开始解析，文本长度: 15K
[ES DSL] 📝 包含 ES DSL 关键词，开始详细解析...
[ES DSL] ❌ 解析失败，返回 null
[ES DSL] 文本预览:
DEBUG (RequestLogger.java:58)- request [POST ...
```
→ **说明正则匹配失败,需要看预览内容**

### 第 2 步: 检查日志配置

#### 2.1 确认 TRACE 日志已启用

**检查 logback-local.xml:**
```xml
<logger name="tracer" level="TRACE"/>
<logger name="org.elasticsearch.client.RestClient" level="DEBUG"/>
```

**或检查 application-local.yml:**
```yaml
logging:
  level:
    tracer: TRACE  # ⚠️ 必须是 TRACE
    org.elasticsearch.client.RestClient: DEBUG
```

#### 2.2 验证日志输出

**在控制台中应该能看到:**
```
DEBUG (RequestLogger.java:58)- request [POST http://.../_search] returned [HTTP/1.1 200 OK]
TRACE (RequestLogger.java:90)- curl -iX POST '...' -d '{"query":{...}}'
# HTTP/1.1 200 OK
# ...
```

**如果看不到:**
- ❌ 日志配置未生效
- ❌ 需要重启应用

### 第 3 步: 检查监听器状态

#### 3.1 查看 ES DSL Monitor 工具窗口

**检查状态栏:**
```
✅ 正常: "监听中 | 已捕获 X 条查询"
❌ 异常: "未启动" 或 "已停止"
```

#### 3.2 检查监听开关

工具栏左上角应该有一个开关按钮:
- ✅ 绿色/高亮 = 已启用
- ❌ 灰色/暗淡 = 已禁用

**如果是禁用状态:**
- 点击开关按钮启用

#### 3.3 查看 IDEA 日志

搜索监听器启动日志:
```
Process started, ES DSL monitoring active
Attached ES DSL listener to process
```

### 第 4 步: 手动测试触发条件

#### 4.1 复制实际日志

从控制台复制包含 ES 查询的完整日志,例如:
```
2025-10-18 15:19:33,988 DEBUG (RequestLogger.java:58)- request [POST http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true] returned [HTTP/1.1 200 OK]
2025-10-18 15:19:33,989 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true' -d '{"from":0,"query":{"bool":{"must":[...]}}}'
# HTTP/1.1 200 OK
```

#### 4.2 检查是否包含关键标记

**检查清单:**
- [ ] 是否包含 `RequestLogger.java`?
- [ ] 是否包含 `curl -`?
- [ ] 是否包含 `-d`?
- [ ] 是否包含 JSON 对象 `{"`?
- [ ] 是否包含 `_search`?
- [ ] 是否包含 JSON 结束 `}`?

**如果都包含:**
→ 应该能触发解析

**如果缺少关键标记:**
→ 需要调整触发条件

---

## 🔧 解决方案

### 方案 1: 确认日志配置（最常见）

#### 问题: 没有看到 TRACE 日志

**检查:**
1. 打开 `src/main/resources/logback-local.xml`
2. 确认有以下配置:
```xml
<logger name="tracer" level="TRACE" additivity="false">
    <appender-ref ref="STDOUT" />
</logger>
```

3. 如果没有,添加配置
4. **重启 ais 应用**（重要！）

#### 验证:
执行一次 ES 查询,控制台应该输出:
```
TRACE (RequestLogger.java:90)- curl -iX POST '...' -d '...'
```

### 方案 2: 手动触发监听（偶尔问题）

#### 步骤:
1. 在 ES DSL Monitor 工具窗口中
2. 点击工具栏的 **刷新按钮** 🔄
3. 或点击 **开关按钮** 关闭再打开

### 方案 3: 调整触发条件（日志格式特殊）

如果你的日志格式与标准格式不同,可能需要调整。

#### 查看实际日志格式:

在 IDEA 日志中找到 `[ES DSL] 文本预览:` 后面的内容,将其发送给开发者。

#### 临时解决方案:

如果触发条件太严格,可以临时降低门槛:

```java
// 在 EsDslOutputListener.java 中修改
private static final int MIN_PARSE_TRIGGER_SIZE = 100;  // 降低到 100
private static final long MIN_PARSE_INTERVAL_MS = 0;    // 取消时间限制
```

然后重新构建插件。

### 方案 4: 增加缓冲区保留时间（极少情况）

如果 DSL 非常长,且跨越多行,可能需要增加保留时间:

```java
// 在 shouldCleanBuffer() 中调整
if (buffer.length() > CROSS_LINE_RETAIN_SIZE * 2) {  // 增加到 2 倍
    // 清理...
}
```

---

## 📊 常见问题排查表

| 症状 | 可能原因 | 解决方案 |
|------|----------|----------|
| 工具窗口空白 | 监听器未启动 | 重启 IDEA,确保插件已安装 |
| 长时间无数据 | 日志级别不是 TRACE | 修改配置为 TRACE,重启应用 |
| 偶尔能捕获 | 触发条件太严格 | 已优化,重新构建插件 |
| 解析返回 null | 日志格式不匹配 | 查看调试日志,调整正则 |
| 性能卡顿 | 缓冲区太大 | 已优化,使用异步处理 |

---

## 🎯 快速验证流程

### 1 分钟快速检查:

```bash
# 1. 检查日志配置
grep -r "tracer.*TRACE" src/main/resources/

# 2. 重启应用
# (在 IDEA 中点击停止然后重新运行)

# 3. 执行 ES 查询
# (访问任何触发 ES 查询的接口)

# 4. 检查控制台
# 应该看到: TRACE (RequestLogger.java:90)- curl...

# 5. 检查 IDEA 日志
# Help -> Show Log
# 搜索: [ES DSL]

# 6. 查看 ES DSL Monitor
# 应该能看到新的查询记录
```

### 预期结果:

✅ **成功的标志:**
```
控制台: TRACE 日志输出完整
IDEA 日志: [ES DSL] ✅ 触发解析...
工具窗口: 显示新的查询记录
```

❌ **失败的标志:**
```
控制台: 没有 TRACE 日志
IDEA 日志: 没有 [ES DSL] 相关日志
工具窗口: 长时间空白
```

---

## 📞 获取支持

### 如果以上方案都不行:

#### 1. 收集调试信息

**需要提供:**
1. IDEA 日志文件 (最近 1000 行)
2. 控制台输出 (包含 ES 查询的部分)
3. `logback-local.xml` 配置内容
4. `application-local.yml` 日志配置

#### 2. 查找关键信息

**在 IDEA 日志中搜索:**
```
[ES DSL]
Process started, ES DSL
Attached ES DSL listener
```

**在控制台搜索:**
```
RequestLogger
curl -
_search
```

#### 3. 描述问题

**清晰描述:**
- 是否看到 TRACE 日志?
- 是否看到 [ES DSL] 触发日志?
- 是否看到解析失败的日志?
- 工具窗口是什么状态?

---

## ✅ 优化后的预期效果

### 触发更灵敏

- ✅ 200 字符就可以触发（原来 500）
- ✅ 50ms 间隔即可重试（原来 100ms）
- ✅ 5 种触发条件（原来 4 种）
- ✅ 缓冲区检查（不只是当前文本）
- ✅ 缓冲区较大时自动尝试

### 调试更方便

- ✅ 详细的触发日志
- ✅ 完整的解析过程日志
- ✅ 失败时输出预览
- ✅ 清楚的状态标记（✅ ⚠️ ❌）

### 性能更好

- ✅ 异步处理,不阻塞 IDEA
- ✅ 智能触发,减少无效解析
- ✅ 合理缓冲区,低内存占用

---

**文档创建时间**: 2025-10-18  
**适用版本**: 1.2.0 (调试优化版)  
**作者**: PandaCoder Team

🎊 **现在应该能够正常捕获 DSL 了!** 🎊

如果还有问题,请查看 IDEA 日志中的 `[ES DSL]` 相关日志,并反馈具体的日志内容。

