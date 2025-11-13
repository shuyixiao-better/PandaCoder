# 版本号更新指南

## 📌 快速开始

PandaCoder 项目现在使用**统一版本管理**方案，只需修改一个文件即可更新整个项目的版本号。

## 🎯 更新步骤

### 1. 修改版本配置

编辑 `gradle.properties` 文件，修改以下配置：

```properties
pluginVersion=1.2.0                    # 版本号
versionType=正式版本                    # 版本类型
releaseDate=2025-01-01                 # 发布日期
currentFeatures=新增XXX功能             # 主要功能描述
```

### 2. 运行更新脚本

选择以下任一方式运行更新脚本：

**方式一：Python 脚本（推荐）**
```bash
python update-version.py
```

**方式二：PowerShell 脚本（Windows）**
```powershell
.\update-version.ps1
```

**方式三：Gradle 脚本**
```bash
gradlew -b update-version.gradle
```

### 3. 重新构建项目

```bash
# Windows
gradlew clean build

# Linux/Mac
./gradlew clean build
```

### 4. 同步到博客（可选）

如果您维护了博客项目，可以自动同步版本信息：

```bash
# 方式一：一键更新（推荐）
python update-all.py  # 同时更新 PandaCoder 和博客

# 方式二：单独同步
python sync-to-blog.py
```

详细说明请参考：[BLOG_SYNC_GUIDE.md](docs/BLOG_SYNC_GUIDE.md)

### 5. 提交更改

```bash
# PandaCoder 项目
git add .
git commit -m "chore: update version to 1.2.0"
git tag v1.2.0
git push && git push --tags

# 博客项目（如果已同步）
cd E:\Project\博客项目\我的博客\shuyixiao-studio
git add .
git commit -m "docs: update PandaCoder to v1.2.0"
git push
```

## 📝 自动更新的文件

运行更新脚本后，以下文件会自动更新：

1. ✅ `src/main/resources/version.properties` - 版本信息配置文件
2. ✅ `README.md` - 版本号徽章
3. ✅ `VersionInfo.java` - 运行时自动读取版本信息
4. ✅ `build.gradle` - 项目版本号

## 🔍 版本信息的使用

在代码中可以通过 `VersionInfo` 类获取版本信息：

```java
// 获取当前版本号
String version = VersionInfo.CURRENT_VERSION;  // "1.1.9"

// 获取版本类型
String type = VersionInfo.VERSION_TYPE;  // "内测版本"

// 获取发布日期
String date = VersionInfo.RELEASE_DATE;  // "2024-12-21"

// 获取主要功能
String features = VersionInfo.CURRENT_FEATURES;  // "模力方舟腾讯混元模型翻译为默认值"

// 获取完整版本信息
String info = VersionInfo.getCurrentVersionInfo();
```

## ⚠️ 注意事项

1. **唯一数据源**：只修改 `gradle.properties` 中的版本信息，不要手动修改其他文件
2. **运行脚本**：修改 `gradle.properties` 后必须运行更新脚本
3. **重新构建**：更新后需要运行 `gradlew clean build` 重新构建项目
4. **提交所有文件**：确保提交所有被更新的文件到 Git

## 🛠️ 故障排除

### 问题：运行脚本后版本号没有更新

**解决方案**：
1. 确认 `gradle.properties` 文件已正确修改
2. 重新运行更新脚本
3. 检查文件是否有写入权限

### 问题：构建后 jar 文件名还是旧版本

**解决方案**：
```bash
gradlew --stop          # 停止 Gradle 守护进程
gradlew clean build     # 重新构建
```

### 问题：VersionInfo 读取的版本号不正确

**解决方案**：
1. 确认 `src/main/resources/version.properties` 文件已更新
2. 运行 `gradlew clean build` 重新构建
3. 检查 `build/resources/main/version.properties` 文件内容

## 📚 更多信息

详细的技术文档请参考：[docs/版本号统一管理方案.md](docs/版本号统一管理方案.md)

## ✨ 示例

假设要发布 1.2.0 版本：

```bash
# 1. 修改 gradle.properties
# pluginVersion=1.2.0
# versionType=正式版本
# releaseDate=2025-01-01
# currentFeatures=新增版本统一管理功能

# 2. 运行更新脚本
python update-version.py

# 3. 重新构建
gradlew clean build

# 4. 验证
# 检查 README.md 中的版本徽章是否为 1.2.0
# 检查 build/libs/PandaCoder-1.2.0.jar 是否存在

# 5. 提交
git add .
git commit -m "chore: release version 1.2.0"
git tag v1.2.0
git push && git push --tags
```

---

**就这么简单！** 🎉

