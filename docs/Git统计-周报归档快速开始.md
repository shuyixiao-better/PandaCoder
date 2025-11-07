# Git 统计 - 周报归档快速开始

## 🚀 5分钟快速上手

### 步骤 1：安装 MongoDB（选择一种方式）

#### 方式 A：使用 Docker（推荐，最简单）

```bash
# 拉取 MongoDB 镜像
docker pull mongo:latest

# 启动 MongoDB 容器
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password123 \
  mongo:latest

# 验证 MongoDB 是否运行
docker ps | grep mongodb
```

#### 方式 B：使用 MongoDB Atlas（免费云服务）

1. 访问 https://www.mongodb.com/cloud/atlas/register
2. 注册并创建免费集群（M0 Sandbox）
3. 创建数据库用户
4. 添加 IP 地址到白名单（或允许所有 IP：0.0.0.0/0）
5. 获取连接字符串

### 步骤 2：配置 MongoDB 连接

```bash
# 在项目根目录执行
cd /Users/shuyixiao/IdeaProjects/PandaCoder

# 复制配置文件模板
cp src/main/resources/mongodb-config.properties.example \
   src/main/resources/mongodb-config.properties
```

编辑 `src/main/resources/mongodb-config.properties`：

**本地 Docker MongoDB（有认证）：**
```properties
mongodb.url=mongodb://admin:password123@localhost:27017
mongodb.database=pandacoder
mongodb.collection=weekly_reports
```

**本地 MongoDB（无认证）：**
```properties
mongodb.url=mongodb://localhost:27017
mongodb.database=pandacoder
mongodb.collection=weekly_reports
```

**MongoDB Atlas（云服务）：**
```properties
mongodb.url=mongodb+srv://username:password@cluster.mongodb.net/pandacoder?retryWrites=true&w=majority
mongodb.database=pandacoder
mongodb.collection=weekly_reports
```

### 步骤 3：使用归档功能

1. **打开 IDEA**，找到右侧工具栏的 **Git Statistics** 工具窗口
2. **选择** "📝 工作周报" 标签页
3. **配置 AI API**（如果还未配置）
4. **点击** "加载提交" 按钮
5. **点击** "生成周报" 按钮
6. **点击** "归档周报" 按钮
7. **确认** 归档操作
8. **完成** ✅

### 步骤 4：查看归档的周报

#### 使用 MongoDB Compass（图形界面）

```bash
# 下载 MongoDB Compass
# https://www.mongodb.com/products/compass

# 连接字符串
mongodb://localhost:27017

# 或（如果有认证）
mongodb://admin:password123@localhost:27017
```

#### 使用 MongoDB Shell（命令行）

```bash
# 连接到 MongoDB
mongosh mongodb://localhost:27017

# 或（如果有认证）
mongosh mongodb://admin:password123@localhost:27017

# 查看数据库
show dbs

# 切换到 pandacoder 数据库
use pandacoder

# 查看集合
show collections

# 查询所有周报
db.weekly_reports.find().pretty()

# 查询最新的周报
db.weekly_reports.find().sort({ generatedTime: -1 }).limit(1).pretty()

# 统计周报数量
db.weekly_reports.countDocuments()
```

## 🎯 常用 MongoDB 查询

### 查询指定项目的周报
```javascript
db.weekly_reports.find({ projectName: "PandaCoder" })
```

### 查询指定日期范围的周报
```javascript
db.weekly_reports.find({
  weekStartDate: { $gte: "2025-01-01" },
  weekEndDate: { $lte: "2025-01-31" }
})
```

### 查询指定作者的周报
```javascript
db.weekly_reports.find({ 
  authorFilter: { $regex: "张三" } 
})
```

### 按生成时间倒序排列
```javascript
db.weekly_reports.find().sort({ generatedTime: -1 })
```

### 查询本周的周报
```javascript
db.weekly_reports.find({
  weekStartDate: { $gte: "2025-01-13" },
  weekEndDate: { $lte: "2025-01-19" }
})
```

## 🔧 故障排查

### 问题 1：提示"MongoDB未配置"

**解决方法：**
```bash
# 检查配置文件是否存在
ls -la src/main/resources/mongodb-config.properties

# 如果不存在，复制模板文件
cp src/main/resources/mongodb-config.properties.example \
   src/main/resources/mongodb-config.properties
```

### 问题 2：提示"无法连接到MongoDB"

**解决方法：**
```bash
# 检查 MongoDB 是否运行（Docker）
docker ps | grep mongodb

# 如果没有运行，启动 MongoDB
docker start mongodb

# 测试连接
mongosh mongodb://localhost:27017
```

### 问题 3：Docker MongoDB 连接被拒绝

**解决方法：**
```bash
# 检查 MongoDB 容器日志
docker logs mongodb

# 重启 MongoDB 容器
docker restart mongodb

# 确保端口映射正确
docker port mongodb
```

### 问题 4：MongoDB Atlas 连接超时

**解决方法：**
1. 检查 IP 白名单设置
2. 确认网络连接正常
3. 验证连接字符串格式
4. 检查用户名和密码是否正确

## 📚 完整文档

- [周报归档功能使用指南](./Git统计-周报归档功能使用指南.md)
- [周报归档功能实现报告](./Git统计-周报归档功能实现报告.md)
- [工作周报功能使用指南](./Git统计-工作周报功能使用指南.md)

## 💡 提示

1. **首次使用建议使用 Docker**，最简单快捷
2. **生产环境建议使用 MongoDB Atlas**，免费且稳定
3. **定期备份数据**，避免数据丢失
4. **不要提交配置文件**到 Git，已自动添加到 .gitignore

## 🎉 完成！

现在您已经成功配置了周报归档功能，可以开始使用了！

如有问题，请查看完整文档或联系技术支持。

