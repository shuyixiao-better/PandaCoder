# SQL Monitor 快速使用指南

## 🚀 快速开始

### 1️⃣ 打开SQL Monitor工具窗口

在IntelliJ IDEA底部工具栏，找到并点击 **"SQL Monitor"** 标签页。

![MySQL图标](../src/main/resources/icons/mysql.svg)

### 2️⃣ 启用SQL监听

确保工具栏上的 **"启用SQL监听"** 复选框是**选中状态**。

### 3️⃣ 运行你的项目

启动你的Spring Boot项目或任何使用MyBatis的Java项目。

### 4️⃣ 查看SQL记录

SQL Monitor会自动捕获所有SQL查询并实时显示在表格中！

## 📊 界面功能说明

### 工具栏区域

```
[✓ 启用SQL监听] | [搜索: _____] [操作: 全部操作▼] [时间: 最近1小时▼] [刷新] [清空所有] [导出选中]
```

#### 功能说明：
- **启用SQL监听**: 开关SQL监听功能
- **搜索框**: 搜索SQL关键词、表名、API路径
- **操作过滤**: 筛选特定操作类型（SELECT/INSERT/UPDATE/DELETE）
- **时间过滤**: 筛选特定时间范围的SQL
- **刷新按钮**: 手动刷新数据（实际上会自动刷新）
- **清空所有**: 清空所有历史记录
- **导出选中**: 导出选中的SQL记录

### 表格区域

| 操作 | 表名 | API路径 | SQL摘要 | 结果数 | 时间戳 |
|-----|------|---------|---------|--------|--------|
| SELECT | user | /api/user/list | SELECT id,name FROM user... | 10 | 2025-10-18 22:21:30 |

#### 列说明：
- **操作**: SQL操作类型，带颜色标识
  - 🔵 SELECT (蓝色)
  - 🟢 INSERT (绿色)
  - 🟠 UPDATE (橙色)
  - 🔴 DELETE (红色)
- **表名**: 操作的数据库表
- **API路径**: 触发SQL的API接口
- **SQL摘要**: SQL语句前100个字符
- **结果数**: 查询返回的记录数
- **时间戳**: SQL执行时间

### 详情面板

点击表格中的任意记录，下方会显示完整的SQL详情：

```
=== SQL 查询详情 ===

时间: 2025-10-18 22:21:30
项目: MyProject
操作: SELECT
表名: user
来源: MyBatis
结果数: 10
API路径: /api/user/list
调用类: UserController.java:45

=== SQL 语句 ===
SELECT id, name, email, created_at
FROM user
WHERE status = ?
  AND created_at > ?
ORDER BY id DESC
LIMIT ?

=== 参数 ===
1(Integer), 2025-01-01 00:00:00(Timestamp), 10(Integer)
```

### 状态栏

```
监听状态: 启用 | 活动监听器: 1 | 总查询: 51 | SELECT: 48 | INSERT: 2 | UPDATE: 1 | DELETE: 0 | 表数: 8
```

实时显示：
- 监听状态
- 活动监听器数量
- 各类SQL统计
- 涉及的表数量

## 🎯 常见使用场景

### 场景1: 查找慢查询

1. 运行项目执行一些操作
2. 在SQL Monitor中查看所有SQL
3. 关注SQL语句的复杂度和表名
4. 找出可能的慢查询并优化

### 场景2: 追踪API的SQL调用

1. 调用某个API接口（如 `/api/user/list`）
2. 在搜索框输入 `/api/user/list`
3. 查看该API执行了哪些SQL
4. 分析SQL的合理性

### 场景3: 监控特定表的访问

1. 在搜索框输入表名（如 `user`）
2. 查看该表的所有访问记录
3. 分析访问频率和操作类型

### 场景4: 按操作类型筛选

1. 在"操作"下拉框选择 `UPDATE`
2. 查看所有更新操作
3. 检查更新操作的合理性

### 场景5: 导出SQL用于分析

1. 在表格中选择一条记录
2. 点击"导出选中"按钮
3. SQL详情已复制到剪贴板
4. 粘贴到SQL工具中进行分析

## 🔍 高级技巧

### 技巧1: 组合筛选

```
1. 搜索框输入: "user"
2. 操作选择: "SELECT"
3. 时间选择: "最近1小时"
→ 结果: 最近1小时内，user表的所有SELECT查询
```

### 技巧2: 快速复制SQL

```
1. 双击表格中的记录
2. 在弹出的详情对话框中查看完整SQL
3. 点击"关闭"返回
```

### 技巧3: 批量清理

```
1. 点击"清空所有"按钮
2. 确认对话框点击"是"
3. 所有历史记录被清空
```

## ⚙️ 配置说明

### 自动保存

SQL Monitor会自动将记录保存到项目的 `.idea/sql-records.json` 文件中。

### 最大记录数

默认保存最近1000条SQL记录，超过后自动删除最旧的记录。

### 实时更新

- 新SQL自动显示，无需手动刷新
- 每10秒自动刷新一次（备用机制）

## 🐛 常见问题

### Q1: 为什么没有捕获到SQL？

**A**: 检查以下几点：
1. "启用SQL监听"开关是否打开
2. 项目是否正在运行
3. 项目是否使用MyBatis或类似框架
4. 日志级别是否设置为DEBUG

**配置示例**（application.yml）：
```yaml
logging:
  level:
    com.yourpackage.mapper: DEBUG
```

### Q2: SQL记录太多怎么办？

**A**: 使用以下方法：
1. 使用时间过滤器只看最近的记录
2. 使用搜索框精确查找
3. 点击"清空所有"清理历史记录

### Q3: 如何只看特定表的SQL？

**A**: 在搜索框中输入表名，例如 `user`。

### Q4: API路径显示N/A？

**A**: 可能的原因：
1. 日志中没有记录API路径信息
2. API日志在SQL日志之前太久，超出了缓冲区

**建议**：确保Controller层有日志输出，例如：
```java
log.info("API:/api/user/list");
```

### Q5: 如何修改日志级别？

**A**: 在 `application.yml` 或 `application.properties` 中配置：

**YAML格式**：
```yaml
logging:
  level:
    root: INFO
    com.yourpackage.mapper: DEBUG  # MyBatis Mapper
```

**Properties格式**：
```properties
logging.level.root=INFO
logging.level.com.yourpackage.mapper=DEBUG
```

## 📚 支持的日志格式

### MyBatis标准格式

```
==>  Preparing: SELECT * FROM user WHERE id = ?
==> Parameters: 123(String)
<==      Total: 1
```

### JdbcLogger格式

```
DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT ...
DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: ...
DEBUG (BaseJdbcLogger.java:135)- <==      Total: ...
```

## 🎨 颜色说明

SQL Monitor使用不同颜色标识SQL操作类型：

| 操作类型 | 颜色 | 含义 |
|---------|------|------|
| SELECT | 🔵 蓝色 | 查询操作，不修改数据 |
| INSERT | 🟢 绿色 | 插入操作，新增数据 |
| UPDATE | 🟠 橙色 | 更新操作，修改数据 |
| DELETE | 🔴 红色 | 删除操作，删除数据 |

## 💡 最佳实践

### 1. 开发调试

在开发过程中，始终开启SQL Monitor：
- 实时了解每个操作触发了哪些SQL
- 及时发现N+1查询问题
- 优化SQL性能

### 2. 性能优化

使用SQL Monitor进行性能优化：
- 找出频繁执行的SQL
- 识别复杂的JOIN查询
- 发现缺少索引的表

### 3. 问题排查

使用SQL Monitor排查问题：
- 追踪特定API的SQL调用链
- 检查参数绑定是否正确
- 验证SQL执行结果

### 4. 代码审查

在代码审查时使用SQL Monitor：
- 验证新功能的SQL合理性
- 检查是否有冗余的SQL查询
- 确保事务边界正确

## 🔗 相关功能

SQL Monitor与其他PandaCoder功能配合使用：

- **ES DSL Monitor**: 监控Elasticsearch查询
- **Bug Recorder**: 记录运行时异常
- **SpringBoot配置文件图标**: 识别技术栈

## 📞 获取帮助

如果遇到问题，可以：

1. 查看插件设置：`Settings > Tools > PandaCoder`
2. 查看日志：`Help > Show Log in Explorer`
3. 联系作者：yixiaoshu88@163.com
4. 访问项目主页：https://www.shuyixiao.top

---

**版本**: 1.0.0  
**更新时间**: 2025-10-18  
**作者**: PandaCoder Team

