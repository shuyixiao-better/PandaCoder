# SQL Monitor 功能实现指南

## 📋 功能概述

SQL Monitor 是一个类似 ES DSL Monitor 的功能，用于实时监控和提取应用程序的 SQL 查询日志。

## ✅ 已完成组件

### 1. SqlRecord.java (SQL记录模型)
**路径**: `src/main/java/com/shuyixiao/sql/model/SqlRecord.java`

**字段**:
- `sqlStatement`: SQL语句
- `parameters`: 参数值
- `tableName`: 表名
- `operation`: 操作类型 (SELECT/INSERT/UPDATE/DELETE)
- `resultCount`: 结果数量
- `executionTime`: 执行时间
- `apiPath`: API接口路径
- `callerClass`: 调用SQL的类

### 2. SqlParser.java (SQL解析器)
**路径**: `src/main/java/com/shuyixiao/sql/parser/SqlParser.java`

**功能**:
- 解析 MyBatis 日志格式 (`==> Preparing:`, `==> Parameters:`, `<== Total:`)
- 提取SQL操作类型 (SELECT/INSERT/UPDATE/DELETE)
- 提取表名
- 提取API路径和调用类

**测试结果**: ✅ 51条SQL全部成功解析（100%成功率）

## 📝 待实现组件

### 3. SqlRecordService.java (SQL记录服务)
**路径**: `src/main/java/com/shuyixiao/sql/service/SqlRecordService.java`

**参考**: `EsDslRecordService.java`

**功能**:
- 管理SQL记录的存储和检索
- 支持按表名、操作类型筛选
- 支持搜索功能
- 持久化到JSON文件
- 提供统计信息

**关键方法**:
```java
public void addRecord(SqlRecord record)
public List<SqlRecord> getAllRecords()
public List<SqlRecord> getRecordsByTable(String tableName)
public List<SqlRecord> getRecordsByOperation(String operation)
public List<SqlRecord> searchRecords(String keyword)
public void clearAllRecords()
public Statistics getStatistics()
```

### 4. SqlOutputListener.java (SQL输出监听器)
**路径**: `src/main/java/com/shuyixiao/sql/listener/SqlOutputListener.java`

**参考**: `EsDslOutputListener.java`

**功能**:
- 监听控制台输出
- 缓冲SQL日志（Preparing + Parameters + Total）
- 当遇到Total行时触发解析
- 异步解析SQL
- 保留上下文用于API路径提取

**关键逻辑**:
```java
@Override
public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
    String text = event.getText();
    
    // 1. 添加到缓冲区
    buffer.append(text);
    
    // 2. 检测到Total行 -> 完整的SQL日志
    if (text.contains("<==") && text.contains("Total:")) {
        String bufferedText = buffer.toString();
        
        // 3. 异步解析
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            SqlRecord record = SqlParser.parseSql(bufferedText, project.getName());
            if (record != null) {
                recordService.addRecord(record);
            }
        });
        
        // 4. 清理缓冲区但保留上下文
        if (buffer.length() > CROSS_LINE_RETAIN_SIZE) {
            String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
            buffer.setLength(0);
            buffer.append(remaining);
        }
    }
}
```

**注意事项**:
- 缓冲区大小: `MAX_BUFFER_SIZE = 200000` (200KB)
- 保留上下文: `CROSS_LINE_RETAIN_SIZE = 50000` (50KB)
- 需要过滤掉ES相关日志（避免干扰）

### 5. SqlMonitoringService.java (SQL监控服务)
**路径**: `src/main/java/com/shuyixiao/sql/service/SqlMonitoringService.java`

**参考**: `EsDslMonitoringService.java`

**功能**:
- 管理SQL监听器的生命周期
- 监听进程启动/停止事件
- 自动附加/移除监听器

### 6. SqlToolWindow.java (SQL UI工具窗口)
**路径**: `src/main/java/com/shuyixiao/sql/ui/SqlToolWindow.java`

**参考**: `EsDslToolWindow.java`

**UI组件**:
1. **工具栏**:
   - 启用/禁用监听开关
   - 搜索框
   - 表名过滤器
   - 操作类型过滤器 (SELECT/INSERT/UPDATE/DELETE)
   - 时间范围过滤器
   - 刷新按钮
   - 清空按钮
   - 导出按钮

2. **表格** (显示SQL列表):
   - 列: 操作类型 | 表名 | API路径 | SQL摘要 | 结果数 | 时间戳
   - 支持点击查看详情
   - 不同操作类型用不同颜色标识

3. **详情面板** (下方):
   - 显示完整SQL语句
   - 显示参数
   - 显示API路径和调用类
   - 复制按钮

4. **状态栏**:
   - 监听状态
   - 活动监听器数量
   - SQL统计信息 (总数、SELECT/INSERT/UPDATE/DELETE数量)

### 7. SqlToolWindowFactory.java (工具窗口工厂)
**路径**: `src/main/java/com/shuyixiao/sql/toolwindow/SqlToolWindowFactory.java`

**参考**: `EsDslToolWindowFactory.java`

```java
public class SqlToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        SqlToolWindow sqlToolWindow = new SqlToolWindow(project);
        Content content = toolWindow.getContentManager().getFactory()
                .createContent(sqlToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
```

### 8. plugin.xml 配置
**路径**: `src/main/resources/META-INF/plugin.xml`

需要添加：

```xml
<!-- SQL Monitor 工具窗口 -->
<extensions defaultExtensionNs="com.intellij">
    <toolWindow 
        id="SQL Monitor" 
        anchor="bottom" 
        icon="/icons/mysql.svg"
        factoryClass="com.shuyixiao.sql.toolwindow.SqlToolWindowFactory"/>
</extensions>

<!-- SQL Monitor 服务 -->
<projectService 
    serviceImplementation="com.shuyixiao.sql.service.SqlRecordService"/>
<projectService 
    serviceImplementation="com.shuyixiao.sql.service.SqlMonitoringService"/>
```

### 9. services.xml 配置
**路径**: `src/main/resources/META-INF/services.xml`

```xml
<extensions defaultExtensionNs="com.intellij">
    <projectService 
        serviceImplementation="com.shuyixiao.sql.service.SqlRecordService"/>
    <projectService 
        serviceImplementation="com.shuyixiao.sql.service.SqlMonitoringService"/>
</extensions>
```

## 🔍 SQL日志格式说明

### MyBatis日志格式:
```
2025-10-18 22:21:30,509 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT id,name FROM user WHERE id = ?
2025-10-18 22:21:30,509 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: 123(String)
2025-10-18 22:21:30,511 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1
```

### 解析逻辑:
1. 检测到 `==> Preparing:` -> 开始缓冲
2. 检测到 `==> Parameters:` -> 继续缓冲
3. 检测到 `<== Total:` -> 触发解析

## 📊 测试结果

使用 `日志.txt` 进行自测:
- ✅ 日志总行数: 502
- ✅ 检测到SQL日志: 51
- ✅ 成功解析SQL: 51 (100%)
- ✅ API路径提取: 49/51 (96%)
- ✅ 调用类提取: 51/51 (100%)

## 🎨 UI设计参考

### 表格列宽设置:
```java
dslTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // 操作类型
dslTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 表名
dslTable.getColumnModel().getColumn(2).setPreferredWidth(200); // API路径
dslTable.getColumnModel().getColumn(3).setPreferredWidth(300); // SQL摘要
dslTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // 结果数
dslTable.getColumnModel().getColumn(5).setPreferredWidth(120); // 时间戳
```

### 操作类型颜色:
- SELECT: `#61AFFE` (蓝色)
- INSERT: `#49CC90` (绿色)
- UPDATE: `#FCA130` (橙色)
- DELETE: `#F93E3E` (红色)

## 🚀 实现优先级

1. ✅ **SqlRecord** - 已完成
2. ✅ **SqlParser** - 已完成并测试
3. ⏸️ **SqlRecordService** - 待实现（优先级高）
4. ⏸️ **SqlOutputListener** - 待实现（优先级高）
5. ⏸️ **SqlMonitoringService** - 待实现（优先级中）
6. ⏸️ **SqlToolWindow** - 待实现（优先级中）
7. ⏸️ **SqlToolWindowFactory** - 待实现（优先级低）
8. ⏸️ **plugin.xml配置** - 待实现（优先级低）

## 💡 实现建议

1. **复用代码**: 大部分组件可以直接参考ES DSL Monitor的实现，只需修改：
   - 类名 (EsDsl -> Sql)
   - 包名 (esdsl -> sql)
   - 日志匹配模式
   - UI文案

2. **关键差异**:
   - SQL日志是三行一组 (Preparing + Parameters + Total)
   - ES DSL日志是多行curl命令
   - SQL需要按表名和操作类型筛选
   - ES需要按索引和方法筛选

3. **性能优化**:
   - SQL日志通常比ES DSL短，可以使用较小的缓冲区
   - Total行是明确的结束标记，不需要复杂的触发逻辑
   - 可以立即清理已解析的SQL，不需要保留太多历史

4. **测试策略**:
   - 使用 `RunSqlParserTest.bat` 进行解析测试
   - 创建类似 `TestFullScenario.java` 的完整场景测试
   - 在实际项目中运行插件验证UI功能

## 📚 相关文件

- **测试程序**: `src/test/java/com/shuyixiao/sql/TestSqlParser.java`
- **测试脚本**: `RunSqlParserTest.bat`
- **测试数据**: `日志.txt`
- **参考实现**: `src/main/java/com/shuyixiao/esdsl/`

## 🎯 下一步

建议按以下顺序继续实现：
1. 创建 `SqlRecordService.java`
2. 创建 `SqlOutputListener.java`
3. 创建完整场景测试验证监听功能
4. 创建 `SqlToolWindow.java`
5. 创建 `SqlMonitoringService.java` 和 `SqlToolWindowFactory.java`
6. 配置 `plugin.xml`
7. 在实际项目中测试

**核心已完成，剩余工作主要是参考ES DSL Monitor进行复制和修改！**

