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

- **用户编码**（自动生成）：根据设备ID自动生成（取前12位），用于唯一标识您的账户
- **用户名**（必填）：您的真实姓名或昵称
- **邮箱**（可选）：工作邮箱

## 🔧 配置步骤

### 步骤 1：打开配置页面

1. 打开 IntelliJ IDEA
2. 进入 **设置 (Settings)** / **偏好设置 (Preferences)**
3. 导航到 **工具 (Tools)** > **Git统计 - 用户身份配置**

### 步骤 2：填写用户信息

在配置页面中填写以下信息：

```
设备ID (自动获取): a1b2c3d4e5f6g7h8...  [自动显示，无需填写]
用户编码 (自动生成): A1B2C3D4E5F6        [根据设备ID自动生成，无需填写]

用户名 *: 张三
邮箱: zhangsan@company.com
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
  "userCode": "A1B2C3D4E5F6",                  // 用户编码（根据设备ID自动生成）
  "userEmail": "zhangsan@company.com",         // 邮箱（可选）
  "userDepartment": "",                        // 部门（已废弃，保留字段）
  
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
db.weekly_reports.find({ userCode: "A1B2C3D4E5F6" })

// 统计某个用户的周报数量
db.weekly_reports.countDocuments({ userCode: "A1B2C3D4E5F6" })
```

### 场景 2：设备识别

通过设备ID和用户编码，可以识别不同设备上的周报：

```javascript
// 查询某个设备的所有周报
db.weekly_reports.find({ deviceId: "a1b2c3d4e5f6g7h8..." })

// 查询某个用户编码的所有周报（同一设备）
db.weekly_reports.find({ userCode: "A1B2C3D4E5F6" })
```

### 场景 3：数据审计

通过设备 ID 和用户信息的组合，可以进行数据审计和追溯：

```javascript
// 查询某个设备的所有周报
db.weekly_reports.find({ deviceId: "a1b2c3d4e5f6g7h8..." })

// 查询某个时间段内某个用户的周报
db.weekly_reports.find({
  userCode: "A1B2C3D4E5F6",
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
- **稍后配置**：继续归档，但用户名字段将为空

### 2. 必填字段

用户名为必填字段，建议在首次使用前完成配置，以确保数据的完整性。

### 3. 用户编码自动生成

用户编码根据设备ID自动生成（取前12位并转为大写），无需手动填写。每台设备都有唯一的用户编码。

### 4. 设备更换

如果更换设备，设备 ID 和用户编码都会发生变化，因为它们都是基于设备硬件信息生成的。

### 5. 配置共享

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

### Q3: 用户编码是如何生成的？

A: 用户编码根据设备ID自动生成，取设备ID的前12位并转为大写。例如，设备ID为 `a1b2c3d4e5f6g7h8...`，则用户编码为 `A1B2C3D4E5F6`。

### Q4: 配置信息会被上传吗？

A: 不会。配置信息仅存储在您的本地计算机中，不会上传到任何云端服务器。只有在您主动归档周报到 MongoDB 时，这些信息才会被包含在归档数据中。

### Q5: 更换设备后怎么办？

A: 更换设备后，设备 ID 和用户编码都会发生变化。新设备会有新的设备ID和用户编码，但您可以配置相同的用户名，这样可以通过用户名识别是同一个人。

## 🎉 总结

用户身份识别功能为周报归档提供了强大的用户追溯能力，确保每条归档记录都能准确标识用户身份。通过设备唯一标识和用户自定义信息的组合，既保证了数据的唯一性，又提供了灵活的用户管理方式。

建议所有用户在首次使用周报归档功能前，先完成用户身份配置，以获得最佳的使用体验。

