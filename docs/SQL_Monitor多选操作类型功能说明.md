# SQL Monitor 多选操作类型功能说明

## 功能概述

SQL Monitor 工具窗口现在支持多选操作类型筛选功能，允许用户同时选择多个操作类型（SELECT、INSERT、UPDATE、DELETE）进行筛选。

## 使用方法

### 1. 打开操作类型筛选菜单

在 SQL Monitor 工具窗口的工具栏中，找到"操作:"标签后的按钮，点击该按钮会弹出操作类型筛选菜单。

![操作类型筛选按钮](../src/main/resources/icons/mysql.svg)

### 2. 选择操作类型

在弹出的菜单中，你可以：

- **勾选/取消勾选**：点击任意操作类型（SELECT、INSERT、UPDATE、DELETE）的复选框来选择或取消选择
- **全选**：点击"全选"按钮选择所有操作类型
- **全不选**：点击"全不选"按钮取消所有选择

### 3. 查看筛选结果

- 选择操作类型后，表格会自动刷新，只显示选中的操作类型的 SQL 记录
- 按钮文本会显示当前选中的操作类型：
  - 全选时显示："全部操作"
  - 部分选择时显示："SELECT, INSERT" 等
  - 无选择时显示："无选择"

## 功能特点

### 1. 实时筛选

每次选择或取消选择操作类型后，表格会立即刷新显示筛选结果。

### 2. 组合筛选

多选操作类型筛选可以与其他筛选条件组合使用：

- **搜索框**：搜索 SQL 语句、表名、API 路径
- **时间范围**：筛选特定时间范围的 SQL
- **操作类型**：多选操作类型（新功能）

### 3. 智能显示

- 当选择所有操作类型时，按钮显示"全部操作"
- 当选择部分操作类型时，按钮显示选中的操作类型列表
- 当没有选择任何操作类型时，按钮显示"无选择"

## 使用场景

### 场景 1：查看所有查询和插入操作

1. 点击"操作:"按钮
2. 勾选 SELECT 和 INSERT
3. 取消勾选 UPDATE 和 DELETE
4. 表格只显示 SELECT 和 INSERT 操作的 SQL 记录

### 场景 2：查看所有修改操作

1. 点击"操作:"按钮
2. 勾选 INSERT、UPDATE、DELETE
3. 取消勾选 SELECT
4. 表格只显示数据修改操作的 SQL 记录

### 场景 3：快速切换到全部操作

1. 点击"操作:"按钮
2. 点击"全选"按钮
3. 表格显示所有操作类型的 SQL 记录

## 技术实现

### 核心组件

1. **MultiSelectComboBox**：自定义的多选下拉复选框组件
   - 支持多选
   - 支持全选/全不选
   - 支持变化监听

2. **SqlRecordService.getRecordsByOperations()**：新增的多操作类型筛选方法
   - 接受操作类型列表
   - 返回匹配任意操作类型的记录

3. **SqlToolWindow**：更新的 UI 组件
   - 使用 MultiSelectComboBox 替代原来的单选下拉框
   - 添加弹出菜单支持
   - 实时更新按钮文本

### 代码示例

```java
// 创建多选组件
operationFilter = new MultiSelectComboBox(new String[]{"SELECT", "INSERT", "UPDATE", "DELETE"});

// 添加变化监听器
operationFilter.addChangeListener(e -> {
    operationFilterButton.setText(operationFilter.getDisplayText());
    refreshData();
});

// 获取选中的操作类型
List<String> selectedOperations = operationFilter.getSelectedItems();

// 筛选记录
List<SqlRecord> records = recordService.getRecordsByOperations(selectedOperations);
```

## 注意事项

1. **性能**：多选筛选不会影响性能，筛选操作在内存中进行
2. **持久化**：操作类型选择不会被持久化，每次打开工具窗口时默认全选
3. **组合筛选**：多选操作类型筛选会与其他筛选条件（搜索、时间范围）组合使用

## 未来改进

1. 持久化操作类型选择状态
2. 添加快捷键支持
3. 添加操作类型统计信息
4. 支持自定义操作类型颜色

## 反馈

如有任何问题或建议，请联系开发团队。

