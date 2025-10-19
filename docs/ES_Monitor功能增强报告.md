# ES DSL Monitor 功能增强报告

## 修复内容

### 1. API路径丢失问题修复

#### 问题描述

用户报告ES DSL Monitor可以解析DSL,但**API路径显示为N/A**,无法显示调用ES的接口路径。

#### 根本原因

**缓冲区管理策略错误**:

在检测到新的TRACE日志时,如果缓冲区已经超过10KB,代码会:
1. 先解析并清空缓冲区中的旧内容
2. 然后添加新的TRACE日志
3. **结果**: API路径信息(在TRACE之前的Controller/Service日志中)被解析并清空了
4. **新的TRACE日志没有API路径上下文**,导致解析时找不到API路径

#### 修复方案

**修改文件**: `src/main/java/com/shuyixiao/esdsl/listener/EsDslOutputListener.java`

**修改前** (第95-110行):
```java
if (text.contains("TRACE") && text.contains("RequestLogger") && text.contains("curl")) {
    // ❌ 如果缓冲区已有较多内容，先触发解析
    if (buffer.length() > 10000) {
        // 先解析缓冲区中的内容
        String oldBufferContent = buffer.toString();
        if (oldBufferContent.length() > 200) {
            parseAndSave(oldBufferContent);
        }
        // 清空缓冲区，准备接收新的TRACE日志
        buffer.setLength(0);
    }
    
    // 添加新TRACE日志
    buffer.append(text);
    return;
}
```

**修改后**:
```java
if (text.contains("TRACE") && text.contains("RequestLogger") && text.contains("curl")) {
    // ✅ 不要清空缓冲区!保留之前的API路径等上下文信息
    // 直接添加新TRACE日志到缓冲区
    buffer.append(text);
    
    // 调试日志
    if (DEBUG_MODE) {
        LOG.warn("[ES DSL] 📨 检测到 TRACE RequestLogger 日志！");
        LOG.warn("[ES DSL] 当前缓冲区大小: " + (buffer.length() / 1024) + "KB");
    }
    
    // ⚠️ 不要立即解析，等待后续的响应数据
    return;
}
```

#### 修复效果

**修复前**:
```
[Controller日志 - API路径]
    ↓
[解析并清空缓冲区] ❌
    ↓
[TRACE日志 - 没有API路径上下文]
    ↓
[解析] → API路径 = N/A ❌
```

**修复后**:
```
[Controller日志 - API路径]
    ↓
[保留在缓冲区] ✅
    ↓
[TRACE日志 - 有完整上下文]
    ↓
[响应数据]
    ↓
[一起解析] → API路径 = /kl/api/saas/element/detail/list ✅
```

### 2. 新增"一键Kibana"功能

#### 功能描述

将ES DSL查询转换为Kibana Dev Tools可直接使用的格式,方便用户在Kibana中调试和执行查询。

#### Kibana格式

Kibana Dev Tools使用以下格式:

```
METHOD /index/endpoint
{
  "query": {...}
}
```

例如:
```
POST /dataset_chunk_sharding_24_1536/_search
{
  "from": 0,
  "query": {
    "bool": {
      "must": [
        {"term": {"tenantId": {"value": "1943230203698479104"}}}
      ]
    }
  },
  "size": 12
}
```

#### 实现细节

**修改文件**: `src/main/java/com/shuyixiao/esdsl/ui/EsDslToolWindow.java`

**1. 添加按钮** (第227-230行):
```java
JButton kibanaButton = new JButton("一键Kibana");
kibanaButton.setToolTipText("生成可在Kibana中直接使用的查询语句");
kibanaButton.addActionListener(e -> copyKibanaFormat());
buttonPanel.add(kibanaButton);
```

**2. 实现方法** (第496-553行):
```java
private void copyKibanaFormat() {
    int selectedRow = dslTable.getSelectedRow();
    if (selectedRow < 0) {
        Messages.showWarningDialog(project, "请先选择一个查询记录", "提示");
        return;
    }
    
    EsDslRecord record = tableModel.getRecordAt(selectedRow);
    if (record == null) {
        Messages.showWarningDialog(project, "无法获取记录信息", "提示");
        return;
    }
    
    try {
        StringBuilder kibanaQuery = new StringBuilder();
        
        // 第一行: 请求方法和路径
        String method = record.getMethod() != null ? record.getMethod() : "POST";
        String index = record.getIndex() != null ? record.getIndex() : "your_index";
        String endpoint = record.getEndpoint() != null ? record.getEndpoint() : "_search";
        
        // 构建Kibana格式的第一行
        kibanaQuery.append(method).append(" /").append(index).append("/").append(endpoint);
        kibanaQuery.append("\n");
        
        // 第二行开始: DSL查询体
        String dsl = record.getDslQuery();
        if (dsl != null && !dsl.trim().isEmpty()) {
            kibanaQuery.append(dsl);
        } else {
            kibanaQuery.append("{}");
        }
        
        // 复制到剪贴板
        copyToClipboard(kibanaQuery.toString());
        
        // 显示成功消息
        Messages.showInfoMessage(project, 
            "Kibana格式已复制到剪贴板\n\n可以直接粘贴到Kibana Dev Tools中使用", 
            "操作成功");
        
    } catch (Exception e) {
        Messages.showErrorDialog(project, "生成Kibana格式失败: " + e.getMessage(), "错误");
    }
}
```

#### 使用方法

1. 在ES DSL Monitor窗口中选择一条查询记录
2. 点击"一键Kibana"按钮
3. 系统会自动生成Kibana格式并复制到剪贴板
4. 打开Kibana Dev Tools
5. 粘贴(Ctrl+V)并执行

#### 生成示例

**原始记录**:
- 方法: POST
- 索引: dataset_chunk_sharding_24_1536
- 端点: _search?typed_keys=true&search_type=query_then_fetch
- DSL: `{"from":0,"query":{"bool":{"must":[...]}},"size":12}`

**生成的Kibana格式**:
```
POST /dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch
{"from":0,"query":{"bool":{"must":[{"term":{"tenantId":{"value":"1943230203698479104"}}}]}},"size":12}
```

## 完整的日志流程

### 日志产生顺序

```
1. Controller日志 (包含API路径)
   ↓
2. Service日志 (业务逻辑)
   ↓
3. VectorDataRetriever日志 (调用类)
   ↓
4. TRACE日志 (curl命令 + DSL)
   ↓
5. HTTP响应头
   ↓
6. 响应数据 (JSON + vector数组)
```

### 缓冲区管理

```
[API路径] → [Service] → [TRACE] → [响应]
^                                    ^
|                                    |
必须保留                              可能很大
```

**关键点**:
1. **不要在检测到TRACE时清空缓冲区**
2. **保留完整的上下文信息**
3. **等待响应数据完整后一起解析**

### API路径提取

API路径通常出现在以下日志中:

```java
// Controller层
INFO (KnowledgeElementDetailController.java:79)- 分页查询知识库明细元素,API:/kl/api/saas/element/detail/list,page:1,size:12

// Service层
INFO (PlatformAuthServiceImpl.java:66)- PlatformAuthServiceImpl.check,uri:/kl/api/saas/element/detail/list
```

提取规则:
```java
Pattern API_PATH_PATTERN = Pattern.compile(
    "(?:API|uri)\\s*[:：]\\s*(/[^\\s,，;；\\)）}]+)",
    Pattern.CASE_INSENSITIVE
);
```

## 测试验证

### 1. API路径提取测试

**测试场景**:
- ✅ 完整日志(包含Controller + TRACE + 响应) → 可以提取API路径
- ❌ 只有TRACE日志(没有Controller日志) → 无法提取API路径
- ❌ 只有响应数据 → 无法提取任何信息

**结论**: 必须保留完整的日志内容,不能清空缓冲区。

### 2. Kibana格式生成测试

**输入**:
```
方法: POST
索引: dataset_chunk_sharding_24_1536
端点: _search
DSL: {"from":0,"query":{"bool":{"must":[...]}},"size":12}
```

**输出**:
```
POST /dataset_chunk_sharding_24_1536/_search
{"from":0,"query":{"bool":{"must":[...]}},"size":12}
```

**验证**: ✅ 可以直接在Kibana Dev Tools中执行

## 功能对比

### 修复前

| 功能 | 状态 | 说明 |
|------|------|------|
| DSL解析 | ✅ | 正常 |
| API路径提取 | ❌ | 显示N/A |
| 调用类提取 | ✅ | 正常 |
| 响应数据 | ✅ | 正常 |
| Kibana格式 | ❌ | 无此功能 |

### 修复后

| 功能 | 状态 | 说明 |
|------|------|------|
| DSL解析 | ✅ | 正常 |
| API路径提取 | ✅ | 正常显示 |
| 调用类提取 | ✅ | 正常 |
| 响应数据 | ✅ | 正常 |
| Kibana格式 | ✅ | 一键生成 |

## 使用指南

### 1. 查看ES查询记录

1. 打开ES DSL Monitor工具窗口
2. 启用ES监听
3. 运行包含ES查询的应用程序
4. 查询记录会自动显示在列表中

### 2. 查看详情

1. 在列表中选择一条记录
2. 下方详情面板会显示完整信息:
   - 时间
   - 服务
   - 方法
   - 索引
   - 端点
   - **API路径** (现在可以正常显示)
   - 状态码
   - 调用类
   - DSL查询

### 3. 使用Kibana格式

1. 选择一条记录
2. 点击"一键Kibana"按钮
3. 打开Kibana Dev Tools
4. 粘贴并执行

### 4. 其他功能

- **复制DSL**: 只复制DSL查询体
- **复制全部**: 复制完整的详情信息
- **格式化**: 格式化显示(开发中)

## 注意事项

### 1. 缓冲区大小

- `MAX_BUFFER_SIZE`: 2MB
- `CROSS_LINE_RETAIN_SIZE`: 200KB

确保能够容纳:
- API路径日志
- Service日志
- TRACE日志
- 大型响应数据(包含vector数组)

### 2. 日志过滤

`shouldKeepText`方法会保留:
- ✅ Controller日志(包含API路径)
- ✅ Service日志
- ✅ VectorDataRetriever日志
- ✅ RequestLogger TRACE日志
- ✅ 响应数据

过滤掉:
- ❌ SQL日志
- ❌ Spring框架日志
- ❌ Tomcat日志

### 3. 性能考虑

- 使用异步解析避免阻塞UI线程
- 解析后立即清空缓冲区
- 每10秒自动刷新UI

## 后续优化建议

### 1. 增强Kibana格式

支持更多Kibana特性:
```
POST /index/_search
{
  "query": {...},
  "_source": ["field1", "field2"],
  "highlight": {...}
}
```

### 2. 支持多种格式

- Kibana格式
- cURL格式
- Java代码格式
- Python代码格式

### 3. 批量操作

- 批量导出为Kibana格式
- 批量执行查询
- 批量对比结果

### 4. 查询优化建议

分析DSL查询,提供优化建议:
- 索引选择
- 查询条件优化
- 分页参数调整

## 总结

通过修改缓冲区管理策略和新增Kibana格式功能,成功解决了API路径丢失问题,并提供了更便捷的Kibana集成。

### 修复前

- API路径显示N/A ❌
- 无法在Kibana中快速调试 ❌

### 修复后

- API路径正常显示 ✅
- 一键生成Kibana格式 ✅
- 可以直接在Kibana中执行 ✅

### 关键改进

1. **不要清空缓冲区** - 保留完整的上下文信息
2. **一起解析** - 等待响应完整后统一解析
3. **Kibana集成** - 提供便捷的格式转换

---

**修复日期**: 2025-10-19  
**修复版本**: 1.1.9+  
**影响范围**: ES DSL Monitor功能

