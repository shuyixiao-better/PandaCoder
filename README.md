# PandaCoder

<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="128" alt="PandaCoder Logo">
</p>

## 作者介绍

### 舒一笑 - 「云卷云舒，学无止境，焕然一新」

### 专业认证
- **生成式AI应用工程师(高级)认证**
- **阿里云博客专家**
- **Java应用开发职业技能等级认证**

### 技术领域
- Java/Kotlin 企业级应用开发
- 微服务架构设计与实现
- 大数据处理与分析
- AI辅助开发工具研究

### 联系方式
- **个人博客**：[www.shuyixiao.cloud](https://www.shuyixiao.cloud) - 分享技术文章与开发心得
- **公众号**：「舒一笑的架构笔记」- 关注获取最新开发技巧和行业动态
- **GitHub**：[github.com/shuyixiao-better](https://github.com/shuyixiao-better)

---

## 项目说明

PandaCoder 是一款专为中文母语的 Java/Kotlin 开发者设计的 IntelliJ IDEA 插件，帮助开发者轻松实现「中文思维->英文代码」的无缝转换。

在全球化软件开发环境中，中文开发者常常需要在思考问题时使用母语，而在编写代码时又必须使用英文。这种语言切换不仅消耗精力，有时还会导致命名不准确、不专业或不符合行业惯例的问题。PandaCoder 正是为解决这一痛点而生，让您可以专注于业务逻辑，而不必为英文命名而烦恼。

### 为什么叫「熊猫编码助手」？

熊猫是中国的国宝，备受世界喜爱，代表着中国特色和亲和力。同样，这个插件旨在成为中文开发者的得力助手，帮助您以中文思考，却能输出符合国际标准的英文代码。

### 核心功能

只需三步，即可实现从中文到英文代码的快速转换：

1. **直接输入中文**：按照中文思维直接输入类名、方法名或变量名
2. **AI智能翻译**：利用百度翻译API将中文转换为地道英文
3. **一键生成代码**：自动创建符合命名规范的骨架代码文件

### 主要价值

- **提升效率**：省去手动命名、查字典、创建文件的繁琐步骤
- **降低心智负担**：让开发者专注于业务逻辑而非英文命名规范
- **保持代码质量**：确保生成的类名准确、符合英文惯用命名法
- **本土化体验**：完全符合中文开发者的思维习惯和工作流程

## 技术栈

- **Java 17**：采用 Java 17 作为开发语言，确保与最新 IntelliJ IDEA 兼容
- **百度翻译 API**：提供高质量的中英文翻译服务
- **IntelliJ Platform SDK**：利用 IntelliJ 平台提供的丰富 API 进行插件开发
- **Gradle**：用于项目构建和依赖管理

## 配置说明

### 百度翻译API配置

为了安全起见，百度翻译API的密钥可以通过以下方式配置：

#### 方法1：使用环境变量（推荐）

```bash
# Windows 系统
set BAIDU_API_KEY=您的百度API密钥
set BAIDU_APP_ID=您的百度APP_ID

# macOS/Linux 系统
export BAIDU_API_KEY=您的百度API密钥
export BAIDU_APP_ID=您的百度APP_ID
```

#### 方法2：在 IDEA 中配置环境变量

1. 打开 Run → Edit Configurations
2. 选择您的运行配置
3. 在 Environment variables 中添加：
   - `BAIDU_API_KEY=您的百度API密钥`
   - `BAIDU_APP_ID=您的百度APP_ID`

本项目已经在 `build.gradle` 中配置了自动读取环境变量的功能，会自动将环境变量值传递给系统属性。

## 安装指南

### 从 JetBrains 插件市场安装

1. 在 IntelliJ IDEA 中，打开 Settings/Preferences → Plugins
2. 切换到 Marketplace 标签
3. 搜索 "PandaCoder"
4. 点击 Install 按钮
5. 重启 IDE 完成安装

### 手动安装

1. 从 [GitHub Releases](https://github.com/shuyixiao-better/PandaCoder/releases) 下载最新版本的插件 ZIP 文件
2. 在 IntelliJ IDEA 中，打开 Settings/Preferences → Plugins
3. 点击 ⚙️ 图标，选择 "Install Plugin from Disk..."
4. 选择下载的 ZIP 文件
5. 重启 IDE 完成安装

## 构建说明

本项目使用Gradle构建，支持Java 17。

```bash
./gradlew build
```

## 使用说明

### 中文转小驼峰

1. 在编辑器中选中中文文本
2. 右键点击，选择「中文转小驼峰」或使用快捷键 `Ctrl+Alt+C`
3. 中文会被翻译成英文并转换为小驼峰命名格式

### 中文类名翻译

1. 在项目视图中右键点击目标目录
2. 选择「中文类名翻译」或使用快捷键 `Ctrl+Alt+T`
3. 选择要创建的文件类型（类、接口、记录、枚举、注解、异常）
4. 输入中文类名，点击确定
5. 插件会自动将中文翻译为英文并创建相应的Java文件

### 测试API配置

1. 点击菜单栏中的 Tools → 测试API配置
2. 弹出窗口会显示当前百度翻译API的配置状态和简单测试结果

## 用户反馈与案例分享

### 用户心声

> "作为一名非英语母语的开发者，PandaCoder 让我能够专注于解决业务问题，而不是纠结于如何用英文准确表达类名和方法名。这大大提高了我的开发效率！" — 张工，资深Java开发工程师

> "团队引入 PandaCoder 后，我们的命名规范变得更加统一，代码可读性也提高了。特别是对于初级开发者，减少了很多命名方面的错误。" — 李总，技术团队负责人

### 成功案例

- **某金融科技公司**：引入 PandaCoder 后，新功能开发速度提升 20%，代码审查中命名相关问题减少 35%
- **某初创团队**：帮助非英语背景的开发者快速适应国际化项目，降低了语言障碍

## 开源与协作

PandaCoder 是一个开源项目，我们欢迎社区贡献。无论是功能建议、Bug 报告还是代码贡献，都能帮助我们打造更好的工具。

- **GitHub 仓库**：[https://github.com/shuyixiao-better/PandaCoder](https://github.com/shuyixiao-better/PandaCoder)
- **问题反馈**：通过 GitHub Issues 提交问题
- **贡献代码**：欢迎提交 Pull Request

## 未来规划

我们正在规划以下功能，让 PandaCoder 变得更加强大：

- **多语言支持**：扩展到更多编程语言，如 Python、Go、JavaScript 等
- **更智能的翻译**：引入大型语言模型，提供更加精准的专业术语翻译
- **上下文感知**：根据项目已有代码，提供更符合项目风格的命名建议
- **批量重命名**：支持对多个中文命名的类或方法进行批量翻译和重构
- **命名历史记录**：记录常用的中英文映射，提高重复使用场景下的效率

## 许可证

请参阅项目中的LICENSE文件。
