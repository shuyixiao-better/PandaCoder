# PandaCoder

<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="128" alt="PandaCoder Logo">
</p>

## 插件简介

PandaCoder 是一款专为中文开发者设计的 IntelliJ IDEA 插件，集成了中文编程转换、Jenkins Pipeline支持、SpringBoot配置文件图标显示等多项强大功能，大幅提升中文开发人员的编程效率和开发体验。

## 🚀 核心功能

### 🎯 中文编程助手
- **智能中文转换**：支持中文转小驼峰、大驼峰、大写带下划线等多种命名格式
- **智能类创建**：支持中文输入快速创建Java类，自动转换为英文类名
- **类名前缀识别**：支持"Service:用户管理"格式，自动生成ServiceUserManagement等规范类名
- **多引擎翻译**：支持国内大模型（通义千问/文心一言/智谱AI）、Google Cloud Translation、百度翻译三级翻译引擎智能切换

### 🐳 Jenkins Pipeline 支持
- **智能语法高亮**：为Jenkins文件提供增强的语法高亮，11种鲜艳颜色
- **环境变量补全**：自动识别env.BUILD_NUMBER、env.WORKSPACE等环境变量
- **参数补全**：自动补全params.APP_NAME、params.DEPLOY_ENV等参数
- **文档提示**：提供完整的Jenkins Pipeline语法文档和示例
- **自定义图标**：Jenkins机器人图标，5层主题覆盖防护

### 🍃 SpringBoot 配置文件图标
- **技术栈识别**：自动识别SpringBoot配置文件中的技术栈（MySQL、Redis、Kafka等）
- **智能图标显示**：在编辑器左侧显示对应技术栈的彩色图标
- **多格式支持**：支持YAML和Properties格式的配置文件
- **优先级匹配**：特定技术栈图标优先于通用配置图标

### ⚙️ 高级功能
- **自定义翻译提示词**：支持自定义翻译prompt，适配不同技术领域
- **文件模板配置**：自定义Java文件注释模板
- **API配置验证**：实时验证各翻译引擎的API配置
- **智能错误处理**：优雅降级，确保功能可用性

## 🎨 技术特色

- **三级翻译引擎**：国内大模型 > Google翻译 > 百度翻译，确保翻译质量和可用性
- **智能优先级匹配**：避免图标冲突，显示最相关的技术栈图标
- **多语言支持**：支持中文、英文等多种编程语言环境
- **主题兼容**：自定义图标支持亮色和暗色主题

## 🎯 适用场景

- **中文开发者**：快速将中文思维转换为英文代码
- **Jenkins用户**：提升Pipeline开发效率和体验
- **SpringBoot项目**：直观识别配置文件中的技术栈
- **团队协作**：统一代码命名规范，提升代码质量

## 📦 安装指南

### 从 JetBrains 插件市场安装
1. 在 IntelliJ IDEA 中，打开 Settings/Preferences → Plugins
2. 切换到 Marketplace 标签
3. 搜索 "PandaCoder"
4. 点击 Install 按钮
5. 重启 IDE 完成安装

### 手动安装
1. 从 [GitHub Releases](https://github.com/shuyixiao-better/PandaCoder/releases) 下载最新版本
2. 在 IntelliJ IDEA 中，打开 Settings/Preferences → Plugins
3. 点击 ⚙️ 图标，选择 "Install Plugin from Disk..."
4. 选择下载的 ZIP 文件
5. 重启 IDE 完成安装

## ⚙️ 配置说明

### 翻译引擎配置
1. 打开 Settings → Tools → PandaCoder
2. 配置以下任一翻译引擎：
   - **国内大模型**：通义千问、文心一言、智谱AI
   - **Google Cloud Translation**：国际化翻译支持
   - **百度翻译**：备用翻译引擎
3. 点击"验证配置"按钮测试API连接
4. 点击"Apply"保存设置

### 获取API密钥
- **通义千问**：[阿里云DashScope](https://dashscope.aliyun.com/)
- **文心一言**：[百度智能云](https://cloud.baidu.com/product/wenxinworkshop)
- **智谱AI**：[智谱开放平台](https://open.bigmodel.cn/)
- **Google翻译**：[Google Cloud Console](https://console.cloud.google.com/)
- **百度翻译**：[百度翻译开放平台](https://fanyi-api.baidu.com/)

## 🚀 使用方法

### 中文转换功能
1. **选中中文文本**：在编辑器中选中要转换的中文
2. **右键选择转换**：
   - 中文转小驼峰 (Ctrl+Alt+C)
   - 中文转大驼峰 (Ctrl+Alt+P)
   - 中文转大写带下划线 (Ctrl+Alt+U)

### 智能类创建
1. **右键选择目录**：在项目视图中右键点击目标目录
2. **选择"智能中文类"**：从新建菜单中选择
3. **输入中文类名**：支持"Service:用户管理"格式
4. **自动生成文件**：插件自动翻译并创建Java文件

### Jenkins Pipeline支持
- **自动识别**：Jenkinsfile文件自动识别和语法高亮
- **智能补全**：pipeline、stage、step等关键字补全
- **环境变量**：env.BUILD_NUMBER等环境变量补全
- **参数补全**：params.APP_NAME等参数补全

### SpringBoot配置图标
- **自动显示**：打开SpringBoot配置文件时自动显示技术栈图标
- **鼠标悬停**：查看技术栈名称和详细信息
- **多格式支持**：支持YAML和Properties格式

## 📊 功能统计

### 支持的技术栈
- **数据库**：4种（MySQL、PostgreSQL、Oracle、SQL Server）
- **缓存**：1种（Redis）
- **消息队列**：2种（Kafka、RabbitMQ）
- **搜索引擎**：1种（Elasticsearch）
- **翻译引擎**：6种（3个国内大模型 + Google + 百度）

### 文件格式支持
- **配置文件**：6种格式（yml、yaml、properties等）
- **Java文件**：6种类型（类、接口、枚举、注解、记录、异常）
- **Jenkins文件**：多种模式（Jenkinsfile、*.jenkinsfile等）

## 🎯 使用效果

### 开发效率提升
- **命名转换**：从手动翻译到一键转换，效率提升80%
- **类创建**：从手动命名到智能生成，效率提升70%
- **配置识别**：从手动查找到图标识别，效率提升60%

### 代码质量提升
- **命名规范**：统一的英文命名规范
- **可读性**：清晰的语法高亮和图标标识
- **维护性**：规范的代码结构和注释

## 👨‍💻 作者介绍

### 舒一笑 - 「云卷云舒，学无止境，焕然一新」

**专业认证**
- 生成式AI应用工程师(高级)认证
- 阿里云博客专家
- Java应用开发职业技能等级认证

**联系方式**
- **个人博客**：[www.shuyixiao.cloud](https://www.shuyixiao.cloud)
- **公众号**：「舒一笑的架构笔记」
- **GitHub**：[github.com/shuyixiao-better](https://github.com/shuyixiao-better)

## 🤝 开源与协作

PandaCoder 是一个开源项目，欢迎社区贡献。

- **GitHub 仓库**：[https://github.com/shuyixiao-better/PandaCoder](https://github.com/shuyixiao-better/PandaCoder)
- **问题反馈**：通过 GitHub Issues 提交问题
- **贡献代码**：欢迎提交 Pull Request

## 🚀 未来规划

### 短期计划 (v1.2.0)
- [ ] 支持更多编程语言 (Python、Go、JavaScript)
- [ ] 增强SpringBoot配置验证功能
- [ ] 添加更多技术栈图标支持

### 中期计划 (v1.3.0)
- [ ] 智能代码重构功能
- [ ] 团队协作功能
- [ ] 代码质量分析

### 长期计划 (v2.0.0)
- [ ] AI驱动的代码生成
- [ ] 多IDE支持
- [ ] 云端配置同步

## 📄 许可证

请参阅项目中的LICENSE文件。

---

让编程更加高效、专业、愉悦！🚀
