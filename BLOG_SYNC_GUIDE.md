# 博客同步使用指南

## 📖 概述

本指南介绍如何将 PandaCoder 的版本更新自动同步到您的博客项目中。

## 🎯 功能特性

- ✅ 自动更新博客文章中的版本信息
- ✅ 自动更新版本历史
- ✅ 自动生成变更日志
- ✅ 支持自定义配置
- ✅ 一键完成所有更新

## 📁 相关文件

```
PandaCoder/
├── sync-to-blog.py          # 博客同步脚本
├── update-all.py            # 一键更新脚本（包含博客同步）
├── sync-config.json         # 同步配置文件
└── BLOG_SYNC_GUIDE.md       # 本文档
```

## ⚙️ 配置

### 1. 编辑配置文件

编辑 `sync-config.json` 文件，配置您的博客项目路径：

```json
{
  "blog": {
    "projectPath": "E:\\Project\\博客项目\\我的博客\\shuyixiao-studio",
    "articlePath": "docs/articles/panda-coder-intro.md",
    "devServerUrl": "http://localhost:5173",
    "articleUrl": "/articles/panda-coder-intro.html"
  },
  "sync": {
    "enabled": true,
    "autoCommit": false,
    "createChangelog": true
  }
}
```

### 2. 配置项说明

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `blog.projectPath` | 博客项目的绝对路径 | `E:\\Project\\博客项目\\我的博客\\shuyixiao-studio` |
| `blog.articlePath` | 文章相对路径 | `docs/articles/panda-coder-intro.md` |
| `blog.devServerUrl` | 开发服务器地址 | `http://localhost:5173` |
| `blog.articleUrl` | 文章访问路径 | `/articles/panda-coder-intro.html` |
| `sync.enabled` | 是否启用同步 | `true` / `false` |
| `sync.autoCommit` | 是否自动提交 | `true` / `false` |
| `sync.createChangelog` | 是否创建变更日志 | `true` / `false` |

## 🚀 使用方法

### 方式一：一键更新（推荐）

这是最简单的方式，会自动完成版本更新和博客同步：

```bash
# 1. 修改 gradle.properties
# pluginVersion=2.0.0
# versionType=正式版本
# releaseDate=2025-01-01
# currentFeatures=新功能描述

# 2. 运行一键更新脚本
python update-all.py
```

### 方式二：分步更新

如果您想分步执行，可以这样做：

```bash
# 1. 修改 gradle.properties
# pluginVersion=2.0.0
# ...

# 2. 更新 PandaCoder 版本
python update-version.py

# 3. 同步到博客
python sync-to-blog.py
```

### 方式三：仅同步博客

如果您已经更新了版本，只想同步到博客：

```bash
python sync-to-blog.py
```

## 📝 同步内容

脚本会自动更新博客文章中的以下内容：

### 1. Frontmatter 日期

```yaml
---
date: 2025-01-01  # 自动更新为当前日期
---
```

### 2. 版本号徽章

```markdown
[![Version](https://img.shields.io/badge/Version-2.0.0-blue)]
```

### 3. 当前版本信息

```markdown
## 当前版本

**版本号**: 2.0.0  
**版本类型**: 正式版本  
**发布日期**: 2025-01-01  
**主要功能**: 新功能描述
```

### 4. 版本历史

从 `VersionInfo.java` 中提取版本历史并更新到博客文章。

### 5. 变更日志

在博客项目根目录创建/更新 `CHANGELOG.md` 文件。

## 🔍 预览更新

同步完成后，您可以在本地预览博客：

```bash
# 1. 进入博客项目目录
cd E:\Project\博客项目\我的博客\shuyixiao-studio

# 2. 启动开发服务器
npm run docs:dev

# 3. 在浏览器中访问
# http://localhost:5173/articles/panda-coder-intro.html
```

## 📤 提交更改

确认更新无误后，提交到 Git：

```bash
# 在 PandaCoder 项目中
git add .
git commit -m "chore: release v2.0.0"
git tag v2.0.0
git push && git push --tags

# 在博客项目中
cd E:\Project\博客项目\我的博客\shuyixiao-studio
git add .
git commit -m "docs: update PandaCoder to v2.0.0"
git push
```

## 🎨 自定义同步逻辑

如果您需要自定义同步逻辑，可以修改 `sync-to-blog.py` 脚本：

### 添加自定义更新规则

```python
def update_blog_article(plugin_version, version_type, release_date, current_features):
    # ... 现有代码 ...
    
    # 添加您的自定义更新逻辑
    # 例如：更新下载链接
    content = re.sub(
        r'下载链接：.*',
        f'下载链接：https://example.com/download/v{plugin_version}',
        content
    )
    
    # ... 现有代码 ...
```

### 添加额外的文件更新

```python
def update_additional_files(plugin_version):
    """更新其他相关文件"""
    # 例如：更新版本对比表
    comparison_file = BLOG_PROJECT_PATH / "docs" / "comparison.md"
    if comparison_file.exists():
        content = comparison_file.read_text(encoding='utf-8')
        # 更新逻辑...
        comparison_file.write_text(content, encoding='utf-8')
```

## ⚠️ 注意事项

1. **路径配置**：确保 `sync-config.json` 中的路径正确
2. **文件编码**：所有文件使用 UTF-8 编码
3. **备份**：首次使用前建议备份博客项目
4. **预览**：提交前务必预览确认更新正确
5. **Git 状态**：确保两个项目的 Git 状态干净

## 🔧 故障排除

### 问题 1：找不到博客项目

**错误信息**：
```
✗ 博客项目路径不存在: E:\Project\博客项目\我的博客\shuyixiao-studio
```

**解决方案**：
1. 检查 `sync-config.json` 中的路径是否正确
2. 确保使用双反斜杠 `\\` 或正斜杠 `/`
3. 确保路径存在且有访问权限

### 问题 2：文章更新失败

**错误信息**：
```
✗ 博客文章不存在: docs/articles/panda-coder-intro.md
```

**解决方案**：
1. 检查 `articlePath` 配置是否正确
2. 确保文章文件存在
3. 检查文件名拼写

### 问题 3：编码问题

**错误信息**：
```
UnicodeEncodeError: 'gbk' codec can't encode character
```

**解决方案**：
- 脚本已自动处理 Windows 编码问题
- 如果仍有问题，确保所有文件使用 UTF-8 编码保存

### 问题 4：版本历史提取失败

**解决方案**：
1. 确保 `VersionInfo.java` 文件存在
2. 检查 `getSimpleVersionHistory()` 方法格式是否正确
3. 手动更新博客文章中的版本历史部分

## 📊 工作流程图

```
┌─────────────────────────────────────────────┐
│  1. 修改 gradle.properties                   │
│     - pluginVersion=2.0.0                   │
│     - versionType=正式版本                   │
│     - releaseDate=2025-01-01                │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  2. 运行 update-all.py                       │
│     或 update-version.py + sync-to-blog.py  │
└──────────────────┬──────────────────────────┘
                   │
                   ├─────────────────────────┐
                   │                         │
                   ▼                         ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│  更新 PandaCoder         │  │  更新博客文章            │
│  - version.properties    │  │  - panda-coder-intro.md  │
│  - README.md             │  │  - CHANGELOG.md          │
└──────────────────────────┘  └──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  3. 预览验证                                  │
│     - PandaCoder: gradlew clean build       │
│     - 博客: npm run docs:dev                 │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  4. 提交到 Git                                │
│     - PandaCoder: git commit & tag          │
│     - 博客: git commit                       │
└─────────────────────────────────────────────┘
```

## 🎉 最佳实践

1. **版本发布流程**：
   ```bash
   # 完整的版本发布流程
   python update-all.py              # 更新版本和博客
   gradlew clean build               # 构建 PandaCoder
   cd E:\Project\博客项目\我的博客\shuyixiao-studio
   npm run docs:dev                  # 预览博客
   # 确认无误后提交
   ```

2. **定期备份**：
   - 在重要更新前备份两个项目
   - 使用 Git 分支进行测试

3. **版本号规范**：
   - 遵循语义化版本规范
   - 主版本号.次版本号.修订号

4. **文档同步**：
   - 保持 PandaCoder 和博客文档一致
   - 及时更新功能说明

## 📚 相关文档

- [VERSION_UPDATE_GUIDE.md](VERSION_UPDATE_GUIDE.md) - 版本更新指南
- [docs/版本号统一管理方案.md](docs/版本号统一管理方案.md) - 版本管理方案
- [docs/版本号统一管理实现总结.md](docs/版本号统一管理实现总结.md) - 实现总结

## 💡 提示

- 使用 `update-all.py` 可以一次性完成所有更新
- 配置文件支持自定义，适应不同的博客结构
- 脚本会自动处理编码问题，无需担心中文乱码
- 建议在发布前先在本地预览博客效果

---

**需要帮助？** 查看 [故障排除](#-故障排除) 部分或联系开发者。

