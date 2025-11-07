# Git 统计 - 周报归档功能使用指南

## 📝 功能概述

周报归档功能可以将生成的工作周报自动上传到 MongoDB 数据库进行持久化存储，方便后续查询、统计和分析。该功能通过配置文件管理 MongoDB 连接信息，确保敏感信息不会被提交到版本控制系统。

## 🚀 主要特性

- ✅ **MongoDB 持久化存储**：将周报数据安全存储到 MongoDB 数据库
- ✅ **配置文件管理**：通过配置文件管理 MongoDB 连接信息，支持开源项目
- ✅ **完整数据归档**：保存周报内容、Git 提交日志、生成时间、作者等完整信息
- ✅ **连接测试**：归档前自动测试 MongoDB 连接，确保操作成功
- ✅ **安全确认**：归档前需要用户确认，避免误操作
- ✅ **异步处理**：后台线程执行归档操作，不阻塞 UI
- ✅ **详细反馈**：提供清晰的成功/失败提示信息

## 📋 使用步骤

### 1. 配置 MongoDB 连接信息

#### 步骤 1：复制配置文件模板

在项目的 `src/main/resources/` 目录下，找到 `mongodb-config.properties.example` 文件，复制并重命名为 `mongodb-config.properties`。

```bash
# 在项目根目录执行
cp src/main/resources/mongodb-config.properties.example src/main/resources/mongodb-config.properties
```

#### 步骤 2：编辑配置文件

打开 `mongodb-config.properties` 文件，填写您的 MongoDB 连接信息：

```properties
# MongoDB连接URL
# 本地MongoDB示例（无认证）
mongodb.url=mongodb://localhost:27017

# 本地MongoDB示例（有认证）
# mongodb.url=mongodb://username:password@localhost:27017

# MongoDB Atlas云服务示例
# mongodb.url=mongodb+srv://username:password@cluster.mongodb.net/database?retryWrites=true&w=majority

# 数据库名称
mongodb.database=pandacoder

# 集合名称（存储周报的集合）
mongodb.collection=weekly_reports

# 用户名（如果需要认证）
mongodb.username=your_username

# 密码（如果需要认证）
mongodb.password=your_password

# 连接超时时间（毫秒）
mongodb.connection.timeout=10000

# Socket超时时间（毫秒）
mongodb.socket.timeout=10000

# 最大连接池大小
mongodb.connection.pool.max.size=10

# 最小连接池大小
mongodb.connection.pool.min.size=1
```

#### 步骤 3：确保配置文件不被提交

配置文件已自动添加到 `.gitignore`，不会被提交到版本控制系统。请确保不要手动提交该文件。

### 2. 安装和启动 MongoDB

#### 本地安装 MongoDB

**Windows:**
1. 下载 MongoDB Community Server：https://www.mongodb.com/try/download/community
2. 运行安装程序，选择"Complete"安装
3. 启动 MongoDB 服务：
   ```bash
   net start MongoDB
   ```

**macOS (使用 Homebrew):**
```bash
# 安装 MongoDB
brew tap mongodb/brew
brew install mongodb-community

# 启动 MongoDB 服务
brew services start mongodb-community
```

**Linux (Ubuntu/Debian):**
```bash
# 导入 MongoDB 公钥
wget -qO - https://www.mongodb.org/static/pgp/server-6.0.asc | sudo apt-key add -

# 添加 MongoDB 仓库
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list

# 更新包列表并安装
sudo apt-get update
sudo apt-get install -y mongodb-org

# 启动 MongoDB 服务
sudo systemctl start mongod
```

#### 使用 MongoDB Atlas（云服务）

如果不想在本地安装 MongoDB，可以使用 MongoDB Atlas 免费云服务：

1. 访问 https://www.mongodb.com/cloud/atlas
2. 注册账号并创建免费集群
3. 配置网络访问（添加您的 IP 地址）
4. 创建数据库用户
5. 获取连接字符串，格式类似：
   ```
   mongodb+srv://username:password@cluster.mongodb.net/database?retryWrites=true&w=majority
   ```
6. 将连接字符串填入 `mongodb-config.properties` 的 `mongodb.url` 配置项

### 3. 生成周报

在使用归档功能之前，需要先生成周报：

1. 打开 IDEA 右侧工具栏的 **Git Statistics** 工具窗口
2. 选择 **📝 工作周报** 标签页
3. 配置 AI API 信息（如果还未配置）
4. 点击 **加载提交** 按钮，加载本周的 Git 提交记录
5. 点击 **生成周报** 按钮，使用 AI 生成周报内容

### 4. 归档周报

周报生成完成后，点击 **归档周报** 按钮：

1. 系统会自动检查 MongoDB 配置是否正确
2. 测试 MongoDB 连接是否可用
3. 弹出确认对话框，确认是否归档
4. 点击"归档"按钮，系统将在后台执行归档操作
5. 归档完成后，会显示成功或失败的提示信息

## 📊 归档数据结构

归档到 MongoDB 的周报数据包含以下字段：

```javascript
{
  "_id": ObjectId("..."),              // MongoDB自动生成的唯一ID
  "reportContent": "周报内容...",       // 生成的周报内容
  "commits": "Git提交日志...",          // 原始Git提交日志
  "generatedTime": "2025-01-15T10:30:00", // 生成时间
  "weekStartDate": "2025-01-13",       // 周开始日期（周一）
  "weekEndDate": "2025-01-19",         // 周结束日期（周日）
  "authorFilter": "张三 <zhangsan@example.com>", // 作者筛选条件（null表示全部作者）
  "projectName": "PandaCoder",         // 项目名称
  "aiModel": "Qwen3-235B-A22B-Instruct-2507", // AI模型名称
  "apiUrl": "https://ai.gitee.com/v1/chat/completions", // API地址
  "totalCommits": 25,                  // 提交总数
  "totalAuthors": 3,                   // 作者总数
  "metadata": {                        // 扩展字段
    // 可以存储其他自定义信息
  }
}
```

## 🔍 查询归档的周报

您可以使用 MongoDB 客户端工具查询归档的周报：

### 使用 MongoDB Compass（图形界面）

1. 下载并安装 MongoDB Compass：https://www.mongodb.com/products/compass
2. 连接到您的 MongoDB 数据库
3. 选择 `pandacoder` 数据库
4. 选择 `weekly_reports` 集合
5. 查看和搜索归档的周报

### 使用 MongoDB Shell（命令行）

```bash
# 连接到 MongoDB
mongosh

# 切换到数据库
use pandacoder

# 查询所有周报
db.weekly_reports.find()

# 查询指定项目的周报
db.weekly_reports.find({ projectName: "PandaCoder" })

# 查询指定日期范围的周报
db.weekly_reports.find({
  weekStartDate: { $gte: "2025-01-01" },
  weekEndDate: { $lte: "2025-01-31" }
})

# 查询指定作者的周报
db.weekly_reports.find({ authorFilter: "张三 <zhangsan@example.com>" })

# 按生成时间倒序排列
db.weekly_reports.find().sort({ generatedTime: -1 })

# 统计周报总数
db.weekly_reports.countDocuments()
```

## ⚙️ 配置说明

### MongoDB URL 格式

**标准格式：**
```
mongodb://[username:password@]host[:port][/database][?options]
```

**示例：**
```
# 本地无认证
mongodb://localhost:27017

# 本地有认证
mongodb://admin:password123@localhost:27017

# 远程服务器
mongodb://user:pass@192.168.1.100:27017

# MongoDB Atlas
mongodb+srv://user:pass@cluster.mongodb.net/database?retryWrites=true&w=majority
```

### 连接池配置

- `mongodb.connection.pool.max.size`：最大连接数，默认 10
- `mongodb.connection.pool.min.size`：最小连接数，默认 1

建议根据实际使用情况调整连接池大小。

### 超时配置

- `mongodb.connection.timeout`：连接超时时间（毫秒），默认 10000（10秒）
- `mongodb.socket.timeout`：Socket 超时时间（毫秒），默认 10000（10秒）

如果网络较慢，可以适当增加超时时间。

## ❗ 常见问题

### 1. 提示"MongoDB未配置"

**原因：** 配置文件不存在或未正确命名。

**解决方法：**
1. 确认 `src/main/resources/mongodb-config.properties` 文件存在
2. 检查文件名是否正确（不是 `.example` 结尾）
3. 重启 IDEA 或重新加载项目

### 2. 提示"无法连接到MongoDB"

**原因：** MongoDB 服务未启动或连接信息错误。

**解决方法：**
1. 确认 MongoDB 服务已启动
2. 检查 `mongodb.url` 配置是否正确
3. 检查用户名和密码是否正确
4. 检查网络连接和防火墙设置
5. 如果使用 MongoDB Atlas，确认 IP 地址已添加到白名单

### 3. 归档失败

**原因：** 数据库权限不足或网络问题。

**解决方法：**
1. 检查数据库用户是否有写入权限
2. 查看 IDEA 控制台的详细错误信息
3. 确认数据库和集合名称正确
4. 检查网络连接是否稳定

### 4. 配置文件被提交到 Git

**原因：** `.gitignore` 配置不正确或文件已被跟踪。

**解决方法：**
```bash
# 从 Git 跟踪中移除（但保留本地文件）
git rm --cached src/main/resources/mongodb-config.properties

# 提交更改
git commit -m "Remove mongodb config from git tracking"
```

## 🔒 安全建议

1. **不要提交配置文件**：确保 `mongodb-config.properties` 已添加到 `.gitignore`
2. **使用强密码**：为 MongoDB 设置强密码
3. **启用认证**：生产环境务必启用 MongoDB 认证
4. **限制网络访问**：配置防火墙规则，只允许必要的 IP 访问
5. **使用 SSL/TLS**：生产环境建议启用 SSL/TLS 加密连接
6. **定期备份**：定期备份 MongoDB 数据

## 📚 相关文档

- [MongoDB 官方文档](https://docs.mongodb.com/)
- [MongoDB Atlas 使用指南](https://docs.atlas.mongodb.com/)
- [Git 统计 - 工作周报功能使用指南](./Git统计-工作周报功能使用指南.md)

## 💡 技术支持

如有问题，请通过以下方式联系：

- 公众号：舒一笑的架构笔记
- GitHub Issues：提交问题到项目仓库

