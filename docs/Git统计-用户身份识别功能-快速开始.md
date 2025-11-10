# Git 统计 - 用户身份识别功能 - 快速开始

## 🚀 5分钟快速配置

### 第一步：打开配置页面

1. 打开 IntelliJ IDEA
2. 按 `Ctrl + Alt + S` (Windows/Linux) 或 `Cmd + ,` (macOS) 打开设置
3. 在左侧菜单中找到 **工具 (Tools)** > **Git统计 - 用户身份配置**

### 第二步：填写信息

填写以下信息（标记 * 的为必填）：

| 字段 | 说明 | 示例 |
|------|------|------|
| 设备ID | 自动获取，无需填写 | `a1b2c3d4...` |
| 用户编码 | 自动生成，无需填写 | `A1B2C3D4E5F6` |
| 用户名 * | 您的姓名或昵称 | `张三` |
| 邮箱 | 工作邮箱（可选） | `zhangsan@company.com` |

### 第三步：保存配置

点击 **应用 (Apply)** 或 **确定 (OK)** 保存。

### 第四步：归档周报

1. 打开 **Git Statistics** 工具窗口
2. 切换到 **📝 工作周报** 标签页
3. 生成周报
4. 点击 **归档周报** 按钮

✅ 完成！您的周报已成功归档，并包含了用户身份信息。

## 📊 查看归档数据

在 MongoDB 中查看归档的周报：

```javascript
// 连接到 MongoDB
use pandacoder

// 查看最新的周报
db.weekly_reports.find().sort({generatedTime: -1}).limit(1).pretty()

// 输出示例：
{
  "_id": ObjectId("..."),
  "reportContent": "本周工作总结...",
  "deviceId": "a1b2c3d4e5f6g7h8...",
  "userName": "张三",
  "userCode": "A1B2C3D4E5F6",
  "userEmail": "zhangsan@company.com",
  "userDepartment": "",
  ...
}
```

## 🔍 常用查询

### 查询我的所有周报

```javascript
db.weekly_reports.find({ userCode: "A1B2C3D4E5F6" })
```

### 统计我的周报数量

```javascript
db.weekly_reports.countDocuments({ userCode: "A1B2C3D4E5F6" })
```

### 查询本月的周报

```javascript
db.weekly_reports.find({
  userCode: "A1B2C3D4E5F6",
  generatedTime: {
    $gte: "2025-01-01T00:00:00",
    $lte: "2025-01-31T23:59:59"
  }
})
```

### 查询某个用户名的周报

```javascript
db.weekly_reports.find({ userName: "张三" })
```

## ❓ 常见问题

### Q: 我忘记配置用户信息了怎么办？

A: 没关系！在归档周报时，系统会自动检测并提示您配置。您可以选择立即配置或稍后配置。

### Q: 我可以修改已配置的信息吗？

A: 可以！随时可以在设置中修改用户信息。修改后，新归档的周报将使用新的信息。

### Q: 设备ID是什么？

A: 设备ID是基于您的MAC地址生成的唯一标识，用于区分不同的设备。它经过SHA-256哈希处理，确保隐私安全。

### Q: 用户编码是如何生成的？

A: 用户编码根据设备ID自动生成，取设备ID的前12位并转为大写。每台设备都有唯一的用户编码。

### Q: 我在多台电脑上使用，怎么办？

A: 每台电脑的设备ID和用户编码都不同，但您可以在所有电脑上配置相同的用户名，这样可以通过用户名识别是同一个人。

## 🎯 最佳实践

1. **首次使用前配置**：建议在首次使用周报归档功能前完成用户身份配置
2. **保持信息准确**：确保用户名的准确性，便于后续数据统计和分析
3. **定期检查**：定期检查配置信息是否正确
4. **备份设备ID**：如果需要在多台设备间保持一致性，可以记录下设备ID和用户编码

## 📞 需要帮助？

如有问题或建议，请联系：
- 微信：Tobeabetterman1001（备注：PandaCoder问题交流）
- 公众号：舒一笑的架构笔记

---

**祝您使用愉快！** 🎉

