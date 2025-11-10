# Git 统计 - 用户身份识别功能说明

## 📋 功能概述

为了确保周报归档数据的可追溯性和用户唯一性，我们在周报归档功能中新增了**用户身份识别**功能。该功能通过设备唯一标识和用户自定义信息的组合，确保每条归档记录都能准确标识是哪个用户提交的。

## 🎯 核心特性

### 1. 设备唯一标识

- **自动获取**：系统自动获取设备的 MAC 地址
- **安全哈希**：使用 SHA-256 算法对 MAC 地址进行哈希处理，确保隐私安全
- **唯一性保证**：每台设备都有唯一的设备 ID
- **后备方案**：如果 MAC 地址获取失败，使用计算机名和用户名的组合

### 2. 用户自定义信息

用户可以在设置中配置以下信息：

- **用户名**（必填）：您的真实姓名或昵称
- **用户编码**（必填）：工号、员工编号或其他唯一标识
- **邮箱**（可选）：工作邮箱
- **部门**（可选）：所在部门或团队

## 🔧 配置步骤

### 步骤 1：打开配置页面

1. 打开 IntelliJ IDEA
2. 进入 **设置 (Settings)** / **偏好设置 (Preferences)**
3. 导航到 **工具 (Tools)** > **Git统计 - 用户身份配置**

### 步骤 2：填写用户信息

在配置页面中填写以下信息：

```
设备ID (自动获取): a1b2c3d4e5f6g7h8...  [自动显示，无需填写]

用户名 *: 张三
用户编码 *: EMP001
邮箱: zhangsan@company.com
部门: 研发部
```

**注意**：标记 `*` 的字段为必填项。

### 步骤 3：保存配置

点击 **应用 (Apply)** 或 **确定 (OK)** 保存配置。

## 📊 数据结构

归档到 MongoDB 的周报数据将包含以下用户身份字段：

```javascript
{
  "_id": ObjectId("..."),
  "reportContent": "周报内容...",
  "commits": "Git提交日志...",
  "generatedTime": "2025-01-15T10:30:00",
  "weekStartDate": "2025-01-13",
  "weekEndDate": "2025-01-19",
  "projectName": "PandaCoder",
  
  // ==================== 用户身份信息 ====================
  "deviceId": "a1b2c3d4e5f6g7h8...",           // 设备唯一标识（SHA-256哈希）
  "userName": "张三",                          // 用户名
  "userCode": "EMP001",                        // 用户编码
  "userEmail": "zhangsan@company.com",         // 邮箱（可选）
  "userDepartment": "研发部",                  // 部门（可选）
  
  "aiModel": "Qwen3-235B-A22B-Instruct-2507",
  "totalCommits": 25,
  "totalAuthors": 3,
  "metadata": {}
}
```

## 🔍 使用场景

### 场景 1：团队周报统计

通过用户编码和用户名，可以轻松统计每个团队成员的周报提交情况：

```javascript
// 查询某个用户的所有周报
db.weekly_reports.find({ userCode: "EMP001" })

// 统计某个部门的周报数量
db.weekly_reports.countDocuments({ userDepartment: "研发部" })
```

### 场景 2：跨设备识别

即使用户在不同设备上使用插件，通过用户编码也能识别是同一个用户：

```javascript
// 查询某个用户在所有设备上的周报
db.weekly_reports.find({ userCode: "EMP001" })

// 查询某个用户使用了哪些设备
db.weekly_reports.distinct("deviceId", { userCode: "EMP001" })
```

### 场景 3：数据审计

通过设备 ID 和用户信息的组合，可以进行数据审计和追溯：

```javascript
// 查询某个设备的所有周报
db.weekly_reports.find({ deviceId: "a1b2c3d4e5f6g7h8..." })

// 查询某个时间段内某个用户的周报
db.weekly_reports.find({
  userCode: "EMP001",
  generatedTime: {
    $gte: "2025-01-01T00:00:00",
    $lte: "2025-01-31T23:59:59"
  }
})
```

## 🔒 隐私和安全

### 1. 设备 ID 安全

- MAC 地址经过 SHA-256 哈希处理，无法反向推导出原始 MAC 地址
- 哈希值仅用于设备唯一性识别，不包含任何敏感信息

### 2. 数据存储

- 用户身份信息存储在本地配置文件中（应用级别）
- 配置文件位置：`~/.config/JetBrains/IntelliJIdea<version>/options/`
- 不会上传到任何云端服务器（除非您主动归档到自己的 MongoDB）

### 3. 数据控制

- 用户完全控制自己的身份信息
- 可以随时修改或清空配置
- 归档前会提示用户确认

## ⚠️ 注意事项

### 1. 首次使用

首次归档周报时，如果未配置用户身份信息，系统会提示您配置。您可以选择：

- **立即配置**：跳转到设置页面进行配置
- **稍后配置**：继续归档，但用户身份字段将为空

### 2. 必填字段

用户名和用户编码为必填字段，建议在首次使用前完成配置，以确保数据的完整性。

### 3. 设备更换

如果更换设备，设备 ID 会发生变化，但通过用户编码仍然可以识别是同一个用户。

### 4. 配置共享

用户身份配置是应用级别的，所有项目共享同一份配置。

## 🛠️ 技术实现

### 核心类

1. **DeviceIdentifierUtil**
   - 位置：`src/main/java/com/shuyixiao/gitstat/weekly/util/DeviceIdentifierUtil.java`
   - 功能：获取设备唯一标识（MAC 地址哈希）

2. **UserIdentityConfigState**
   - 位置：`src/main/java/com/shuyixiao/gitstat/weekly/config/UserIdentityConfigState.java`
   - 功能：用户身份配置的持久化存储

3. **UserIdentityConfigurable**
   - 位置：`src/main/java/com/shuyixiao/gitstat/weekly/ui/UserIdentityConfigurable.java`
   - 功能：用户身份配置界面

4. **WeeklyReportArchive**
   - 位置：`src/main/java/com/shuyixiao/gitstat/weekly/model/WeeklyReportArchive.java`
   - 功能：周报归档数据模型（已扩展用户身份字段）

### 工作流程

```
用户点击"归档周报"
  ↓
检查用户身份配置
  ↓
未配置 → 提示用户配置 → 打开设置页面
  ↓
已配置 → 继续归档流程
  ↓
获取设备ID（自动）
  ↓
获取用户配置信息
  ↓
填充到 WeeklyReportArchive 对象
  ↓
转换为 MongoDB 文档
  ↓
插入到 MongoDB
```

## 📝 常见问题

### Q1: 设备 ID 是如何生成的？

A: 设备 ID 是通过获取设备的 MAC 地址，然后使用 SHA-256 算法进行哈希生成的。如果 MAC 地址获取失败，会使用计算机名和用户名的组合作为后备方案。

### Q2: 我可以不配置用户信息吗？

A: 可以，但不建议。如果不配置，归档的周报将无法标识用户身份，影响数据的可追溯性。

### Q3: 用户编码应该填什么？

A: 用户编码可以是您的工号、员工编号、学号或任何能唯一标识您的编码。建议使用公司或组织统一的编码规范。

### Q4: 配置信息会被上传吗？

A: 不会。配置信息仅存储在您的本地计算机中，不会上传到任何云端服务器。只有在您主动归档周报到 MongoDB 时，这些信息才会被包含在归档数据中。

### Q5: 更换设备后怎么办？

A: 更换设备后，设备 ID 会发生变化，但您可以在新设备上配置相同的用户名和用户编码，这样仍然可以通过用户编码识别是同一个用户。

## 🎉 总结

用户身份识别功能为周报归档提供了强大的用户追溯能力，确保每条归档记录都能准确标识用户身份。通过设备唯一标识和用户自定义信息的组合，既保证了数据的唯一性，又提供了灵活的用户管理方式。

建议所有用户在首次使用周报归档功能前，先完成用户身份配置，以获得最佳的使用体验。

