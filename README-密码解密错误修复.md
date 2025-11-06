# 🔧 密码解密错误 - 已修复

## 问题现象

发送邮件时出现以下错误：

```
Failed to send email
java.lang.RuntimeException: Failed to decrypt password
Caused by: javax.crypto.IllegalBlockSizeException: Input length must be multiple of 16
```

## ✅ 已修复

**修复版本：** v2.1.0  
**修复时间：** 2025-11-06  
**修复状态：** ✅ 完成

---

## 🚀 快速解决（3步）

### 1️⃣ 重新编译插件

```bash
# Windows PowerShell
.\gradlew clean build

# Linux/Mac
./gradlew clean build
```

### 2️⃣ 重新安装插件

1. IDEA → `Settings` → `Plugins`
2. 卸载旧版本 `PandaCoder`
3. 重启 IDEA
4. `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
5. 选择 `build/distributions/PandaCoder-x.x.x.zip`

### 3️⃣ 重新保存密码

1. 打开 Git 统计工具窗口
2. 切换到"📧 邮件报告"标签页
3. 重新输入"发件人密码"
4. 点击"保存配置"
5. 点击"测试连接"验证

✅ **完成！** 问题已解决。

---

## 📚 详细文档

- **[完整修复方案](docs/密码解密错误修复方案.md)** - 技术细节和多种修复方法
- **[快速修复指南](docs/密码解密错误-快速修复指南.md)** - 用户友好的操作指南
- **[修复完成报告](docs/密码解密错误修复-完成报告.md)** - 开发者技术报告

---

## 🎯 修复内容

### 核心改进

✅ **智能密码检测** - 自动识别加密/明文密码  
✅ **容错机制** - 解密失败不会崩溃  
✅ **向后兼容** - 支持旧版本配置  
✅ **详细日志** - 便于问题排查  

### 支持的密码格式

1. **新格式**：`ENC:Base64数据`（推荐）
2. **旧格式**：`Base64数据`（兼容）
3. **明文**：直接文本（兼容，会提示加密）

---

## ⚠️ 备选方案

如果上述方法不起作用，可以：

### 方案A：清除配置文件

1. 关闭 IDEA
2. 删除 `项目目录/.idea/gitStatEmailConfig.xml`
3. 重新打开 IDEA，重新配置

### 方案B：手动修复配置

1. 编辑 `项目目录/.idea/gitStatEmailConfig.xml`
2. 找到 `<senderPassword>` 标签
3. 改为 `<senderPassword></senderPassword>`
4. 保存，重启 IDEA

---

## 🔍 技术细节

### 修改的文件

1. **PasswordEncryptor.java** (v2.0.0 → v2.1.0)
   - 新增密码格式检测
   - 新增容错机制
   - 新增工具方法

2. **GitStatToolWindow.java**
   - 简化异常处理
   - 添加密码状态检查

3. **PasswordEncryptorTest.java** (新增)
   - 9个测试用例
   - 覆盖所有场景

### 代码统计

- 修改文件：2 个
- 新增文件：4 个（1个测试 + 3个文档）
- 新增代码：~260 行
- 测试用例：9 个

---

## 💡 常见问题

**Q: 为什么会出现这个错误？**  
A: 密码数据损坏、项目路径变化、或手动修改配置文件导致。

**Q: 修复后密码会丢失吗？**  
A: 不会。新版本会自动处理各种格式的密码。

**Q: 需要重新配置所有设置吗？**  
A: 不需要。只需重新输入密码即可。

**Q: 密码安全吗？**  
A: 使用 AES 加密，基于项目路径生成密钥。建议使用 SMTP 应用专用密码。

---

## 📞 技术支持

如有问题，请查看详细文档或联系：

**公众号：** 舒一笑的架构笔记  
**GitHub:** https://github.com/shuyixiao/PandaCoder

---

**修复完成 ✅**  
感谢使用 PandaCoder！

