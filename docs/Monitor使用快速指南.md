# ES DSL Monitor 与 SQL Monitor 使用快速指南

## 🎯 修复完成

✅ **日志分离问题**已修复：ES和SQL日志完全独立，互不干扰  
✅ **日志重复问题**已修复：相同查询不再重复显示

## 🚀 快速开始

### 1. 启动监控

#### ES DSL Monitor
```
1. 打开IDEA底部工具栏
2. 点击"ES DSL Monitor"
3. 勾选"启用ES监听"
4. 运行应用程序
```

#### SQL Monitor
```
1. 打开IDEA底部工具栏
2. 点击"SQL Monitor"
3. 勾选"启用SQL监听"
4. 运行应用程序
```

### 2. 查看日志

**ES DSL Monitor 显示**：
- ✅ Elasticsearch 查询（GET/POST/PUT/DELETE）
- ✅ 索引名称、端点、DSL内容
- ✅ API路径、调用类、执行时间
- ❌ 不会显示SQL日志

**SQL Monitor 显示**：
- ✅ SQL 查询（SELECT/INSERT/UPDATE/DELETE）
- ✅ 表名、参数、结果数量
- ✅ API路径、调用类
- ❌ 不会显示ES日志

## ✅ 验证修复效果

### 检查日志分离

**ES DSL Monitor**：
```
检查项：
- 不应该包含"BaseJdbcLogger"
- 不应该包含"Preparing:"或"Parameters:"
- 不应该包含"<== Total:"

如果包含以上内容，说明有问题
```

**SQL Monitor**：
```
检查项：
- 不应该包含"RequestLogger"
- 不应该包含"curl"命令
- 不应该包含"_search"或"_cluster"

如果包含以上内容，说明有问题
```

### 检查去重效果

**测试方法**：
```
1. 快速刷新页面3次（触发相同查询）
2. 检查对应的Monitor窗口
3. 预期：只显示1条记录，而不是3条

如果显示3条，说明去重未生效
```

## 🔧 配置要求

### ES DSL Monitor 配置

在 `logback-local.xml` 中添加：

```xml
<!-- Elasticsearch TRACE 日志 -->
<logger name="tracer" level="TRACE">
    <appender-ref ref="STDOUT" />
</logger>
```

### SQL Monitor 配置

MyBatis 日志（通常已默认开启）：

```xml
<!-- MyBatis DEBUG 日志 -->
<logger name="com.baomidou.mybatisplus" level="DEBUG">
    <appender-ref ref="STDOUT" />
</logger>
```

## 📊 功能特性

### 日志分离
- ✅ ES和SQL日志完全独立
- ✅ 互不干扰，数据准确
- ✅ 智能识别日志类型

### 智能去重
- ✅ ES DSL：5秒内相同查询只保留1条
- ✅ SQL：3秒内相同查询只保留1条
- ✅ 自动识别重复，无需手动处理

### 界面功能
- ✅ 搜索过滤
- ✅ 时间范围筛选
- ✅ 操作类型筛选
- ✅ 导出功能
- ✅ 详情查看

## ⚠️ 注意事项

### 1. 时间窗口

**ES DSL Monitor**：
- 5秒内的相同查询只显示1次
- 超过5秒的相同查询会被视为新查询

**SQL Monitor**：
- 3秒内的相同SQL只显示1次
- 超过3秒的相同SQL会被视为新查询

### 2. 相似度判断

**ES DSL**：
```
相同 = (方法相同) AND (索引相同) AND (端点相同) AND (DSL内容相同)
```

**SQL**：
```
相同 = (操作类型相同) AND (表名相同) AND (SQL语句相同) AND (参数相同)
```

### 3. 空白字符

- 去重时会忽略空白字符差异
- 例如：`SELECT * FROM user` 和 `SELECT  *  FROM  user` 被视为相同

## 🐛 故障排查

### 问题1：ES Monitor显示了SQL日志

**原因**：可能使用了旧版本代码  
**解决**：
```
1. 确认使用2025-10-19之后的代码
2. 重新编译插件
3. 重启IDEA
```

### 问题2：SQL Monitor显示了ES日志

**原因**：可能使用了旧版本代码  
**解决**：
```
1. 确认使用2025-10-19之后的代码
2. 重新编译插件
3. 重启IDEA
```

### 问题3：仍然有重复记录

**原因**：可能是不同的查询  
**检查**：
```
1. 查看详情，比较DSL/SQL内容
2. 检查参数是否不同
3. 检查时间间隔是否超过窗口期
```

### 问题4：没有捕获到日志

**原因**：日志级别未配置  
**解决**：
```
1. 检查logback-local.xml配置
2. 确认tracer日志级别为TRACE（ES）
3. 确认MyBatis日志级别为DEBUG（SQL）
4. 重启应用程序
```

## 📚 相关文档

- **ES_SQL_Monitor_分离修复报告.md** - 日志分离技术细节
- **日志重复问题修复报告.md** - 去重算法实现
- **Monitor分离使用指南.md** - 详细使用说明
- **Monitor完整修复总结.md** - 完整修复过程

## 💡 使用技巧

### 1. 快速定位问题

```
1. 使用搜索框快速查找关键词
2. 使用时间过滤器缩小范围
3. 点击记录查看完整详情
```

### 2. 导出查询

```
1. 选中需要导出的记录
2. 点击"导出选中"按钮
3. 粘贴到Kibana或数据库工具
```

### 3. 清理历史

```
1. 点击"清空所有"清除所有记录
2. 或等待自动清理（保留最近1000条）
```

## ✅ 快速检查清单

使用前检查：
- [ ] 已启用对应的Monitor
- [ ] 日志级别已正确配置
- [ ] 应用程序正在运行

使用中检查：
- [ ] ES Monitor只显示ES日志
- [ ] SQL Monitor只显示SQL日志
- [ ] 没有重复记录
- [ ] 数据实时更新

遇到问题：
- [ ] 查看IDEA日志（Help -> Show Log）
- [ ] 检查配置文件
- [ ] 重启IDEA
- [ ] 查阅故障排查文档

---

**享受清晰、准确的日志监控体验！** 🎉

如有问题，请查阅详细文档或联系支持。

