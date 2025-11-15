# PandaCoder 使用统计逻辑说明

## 📊 统计系统架构

### 1. 数据存储层

**存储位置**：`~/Library/Application Support/JetBrains/<IDE版本>/options/pandacoder.xml`（应用级配置，跨项目共享）
**存储格式**：
```xml
<PandaCoderSettings>
  <option name="usageCount" value="15" />
  <option name="lastMilestoneCount" value="10" />
  <option name="firstInstall" value="false" />
  <option name="toolWindowAutoShown" value="true" />
  <option name="installTime" value="1698123456789" />
</PandaCoderSettings>
```

**重要说明**：
- 使用应用级别存储（`Service.Level.APP`），所有项目共享同一份统计数据
- 插件更新时不会删除或重置此文件，统计数据会持续累加
- 不同 IDE（如 IDEA、WebStorm）的统计数据是独立的

### 2. 服务层

**核心服务**：`PandaCoderSettings.java`
```java
@Service(Service.Level.APP)  // 应用级别，跨项目共享
@State(name = "PandaCoderSettings", storages = @Storage("pandacoder.xml"))
public final class PandaCoderSettings implements PersistentStateComponent<State> {
    
    // 统计方法
    public int getUsageCount()                    // 获取使用次数（跨所有项目）
    public void incrementUsageCount()             // 增加使用次数
    public boolean shouldShowMilestone()          // 是否达到里程碑
    public boolean shouldShowMilestoneNow()       // 是否应该显示里程碑（避免重复）
    public void updateLastMilestoneCount(int)     // 更新上次里程碑计数
}
```

### 3. 显示层

**显示位置**：
- Tool Window 仪表盘：实时显示使用次数
- 里程碑气泡：10次、50次、100次时弹出

---

## 🎯 统计触发点

### 已实现的统计点

| 功能 | 触发时机 | 统计代码位置 |
|------|----------|-------------|
| **中文转小驼峰** | 用户点击右键菜单 → PandaCoder → 中文转小驼峰 | `ConvertToCamelCaseAction.java:46-55` |
| **中文转大驼峰** | 用户点击右键菜单 → PandaCoder → 中文转大驼峰 | `ConvertToPascalCaseAction.java:46-55` |
| **中文转大写带下划线** | 用户点击右键菜单 → PandaCoder → 中文转大写带下划线 | `ConvertToUpperCaseAction.java:45-54` |
| **打开助手面板** | 用户点击右键菜单 → PandaCoder → 关于插件 或按 Alt+P | `ReportMessage.java:26-36` |
| **Tool Window 功能卡片** | 用户点击 Tool Window 中的功能卡片 | `FunctionCardsPanel.java:163-172` |

### 统计逻辑代码模板

```java
// 在每个 Action 的 actionPerformed 方法开始处添加
Project project = e.getProject();
if (project != null) {
    com.shuyixiao.service.PandaCoderSettings settings = 
        com.shuyixiao.service.PandaCoderSettings.getInstance(project);
    settings.incrementUsageCount();
    
    // 检查里程碑提示
    if (settings.shouldShowMilestoneNow()) {
        com.shuyixiao.ui.PandaCoderBalloon.showMilestone(project, settings.getUsageCount());
        settings.updateLastMilestoneCount(settings.getUsageCount());
    }
}
```

---

## 🏆 里程碑系统

### 里程碑触发点

| 使用次数 | 里程碑消息 | 显示位置 |
|----------|------------|----------|
| **10次** | 🎉 您已使用 PandaCoder 10 次！<br/>觉得有用？给个 Star 支持作者 😊 | 状态栏右侧气泡 |
| **50次** | 🚀 您已使用 PandaCoder 50 次！<br/>成为资深用户啦！关注公众号获取高级技巧 | 状态栏右侧气泡 |
| **100次** | 💎 您已使用 PandaCoder 100 次！<br/>感谢一路相伴！关注公众号第一时间获取新功能 | 状态栏右侧气泡 |

### 里程碑逻辑

```java
// 检查是否达到里程碑
public boolean shouldShowMilestone() {
    int count = state.usageCount;
    return count == 10 || count == 50 || count == 100;
}

// 检查是否应该显示里程碑（避免重复显示）
public boolean shouldShowMilestoneNow() {
    int count = state.usageCount;
    int lastCount = state.lastMilestoneCount;
    return shouldShowMilestone() && count != lastCount;
}
```

### 里程碑气泡特性

- **显示时长**：5秒自动消失
- **显示位置**：状态栏右侧
- **可点击链接**：GitHub Star、关注公众号
- **防重复**：同一里程碑只显示一次

---

## 📈 数据流转过程

### 1. 用户操作流程

```
用户使用功能
    ↓
Action.actionPerformed() 被调用
    ↓
incrementUsageCount() 增加计数
    ↓
检查 shouldShowMilestoneNow()
    ↓
如果达到里程碑 → 显示气泡提示
    ↓
更新 lastMilestoneCount 防止重复
    ↓
数据自动保存到 .idea/pandacoder.xml
```

### 2. 数据持久化

**自动保存**：每次调用 `incrementUsageCount()` 后自动保存
**存储位置**：应用配置目录 `~/Library/Application Support/JetBrains/<IDE版本>/options/pandacoder.xml`
**数据格式**：XML 格式，IDE 自动管理
**跨项目共享**：所有项目使用同一份统计数据
**插件更新**：更新插件时数据不会丢失，持续累加

### 3. 数据读取

**实时读取**：Tool Window 打开时实时读取最新数据
**显示位置**：仪表盘面板显示 "使用 X 次"

---

## 🧪 测试统计功能

### 测试步骤

```bash
# 1. 清理配置（模拟首次安装）
# macOS/Linux:
rm -rf ~/Library/Application\ Support/JetBrains/*/options/pandacoder.xml
# Windows:
# del %APPDATA%\JetBrains\*\options\pandacoder.xml

# 2. 启动测试 IDE
./gradlew runIde

# 3. 测试统计触发
# 在编辑器中选中中文文本，右键使用转换功能
# 或按 Alt+P 打开助手面板
# 或点击 Tool Window 功能卡片

# 4. 查看统计结果
# 打开 Tool Window 查看仪表盘中的使用次数
# 或查看应用配置目录中的 pandacoder.xml 文件

# 5. 测试跨项目统计
# 打开另一个项目，使用功能后查看统计是否继续累加
```

### 验证检查点

1. **统计计数**：每次使用功能后，Tool Window 中的使用次数应该增加
2. **里程碑提示**：第10、50、100次使用时应该弹出气泡
3. **数据持久化**：关闭IDE重新打开，统计数据应该保持
4. **防重复**：同一里程碑不应该重复显示

---

## 🔧 故障排查

### 问题1：统计不增加

**可能原因**：
- 配置文件权限问题
- 服务未正确注册

**解决方案**：
```bash
# 检查配置文件
cat .idea/pandacoder.xml

# 检查服务注册
grep -r "PandaCoderSettings" src/main/resources/META-INF/plugin.xml
```

### 问题2：里程碑不显示

**可能原因**：
- 气泡显示位置错误
- 里程碑逻辑判断错误

**解决方案**：
```java
// 在 PandaCoderSettings 中添加调试日志
public void incrementUsageCount() {
    state.usageCount++;
    System.out.println("Usage count: " + state.usageCount);
}
```

### 问题3：数据丢失

**可能原因**：
- IDE 配置目录被清理
- 文件权限问题
- 卸载 IDE 时删除了配置

**解决方案**：
```bash
# 备份配置（macOS）
cp ~/Library/Application\ Support/JetBrains/*/options/pandacoder.xml ~/pandacoder_backup.xml

# 检查文件是否存在
ls -la ~/Library/Application\ Support/JetBrains/*/options/pandacoder.xml

# 恢复配置
cp ~/pandacoder_backup.xml ~/Library/Application\ Support/JetBrains/<IDE版本>/options/pandacoder.xml
```

---

## 📊 统计数据分析

### 可统计的指标

1. **使用频率**：总使用次数
2. **功能偏好**：各功能使用比例
3. **用户活跃度**：使用时间分布
4. **转化效果**：里程碑后的行为变化

### 数据应用场景

1. **产品优化**：根据使用频率优化功能排序
2. **用户分层**：基于使用次数进行精准推广
3. **功能改进**：识别高频使用功能，重点优化
4. **商业转化**：在合适的时机推荐付费功能

---

## 🚀 扩展建议

### 短期优化

1. **添加更多统计点**：
   - 智能中文类创建
   - Jenkins 文件编辑
   - Spring Boot 配置文件查看

2. **细化统计维度**：
   - 按功能分类统计
   - 按时间统计（日/周/月）
   - 按项目统计（可选，当前为全局统计）

### 中期规划

1. **用户行为分析**：
   - 使用路径分析
   - 功能组合使用分析
   - 用户留存分析

2. **智能推荐**：
   - 基于使用习惯推荐功能
   - 个性化推广内容
   - 智能帮助提示

### 长期愿景

1. **数据驱动产品**：
   - A/B 测试框架
   - 用户反馈收集
   - 产品迭代优化

2. **商业化支持**：
   - 付费功能推荐
   - 用户价值评估
   - 转化漏斗分析

---

## 📝 总结

PandaCoder 的使用统计系统具有以下特点：

✅ **完整的数据流**：从触发到存储到显示  
✅ **智能里程碑**：价值驱动的用户引导  
✅ **防重复机制**：避免骚扰用户  
✅ **持久化存储**：数据不丢失  
✅ **实时更新**：即时反馈  

这套统计系统不仅能够跟踪用户使用情况，更重要的是为产品优化和商业转化提供了数据支撑，体现了纳瓦尔的"数据驱动决策"理念。

---

**创建时间**：2025-10-24  
**统计系统版本**：v2.2.0  
**设计理念**：数据驱动 + 用户友好 + 商业价值
