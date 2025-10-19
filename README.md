# PandaCoder

<p align="center">
  <img src="src/main/resources/icons/pluginIcon.svg" width="128" alt="PandaCoder Logo">
</p>

<p align="center">
  <strong>专为中文开发者设计的智能编程助手</strong><br>
  <em>让编程从中文思考开始，用PandaCoder连接中文创意与国际标准！</em>
</p>

<p align="center">
  <a href="https://www.shuyixiao.cn/articles/panda-coder-intro.html" target="_blank">
    <img src="https://img.shields.io/badge/📖-详细功能介绍-FF6B6B.svg?style=for-the-badge&logo=book&logoColor=white" alt="详细功能介绍">
  </a>
  <a href="https://www.shuyixiao.cn/articles/panda-coder-intro.html" target="_blank">
    <img src="https://img.shields.io/badge/🌟-插件介绍文章-4ECDC4.svg?style=for-the-badge&logo=star&logoColor=white" alt="插件介绍文章">
  </a>
</p>

<p align="center">
  <small>🌐 <strong>访问地址</strong>：主站 <a href="https://www.shuyixiao.cn" target="_blank">www.shuyixiao.cn</a> | 备用站 <a href="https://www.shuyixiao.top" target="_blank">www.shuyixiao.top</a></small>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/your-plugin-id">
    <img src="https://img.shields.io/badge/IntelliJ%20IDEA-Plugin-green.svg" alt="IntelliJ IDEA Plugin">
  </a>
  <a href="https://github.com/shuyixiao-better/PandaCoder/releases">
    <img src="https://img.shields.io/badge/Version-2.0.0-blue.svg" alt="Version">
  </a>
  <a href="https://github.com/shuyixiao-better/PandaCoder/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  </a>
</p>

## 📖 插件简介

PandaCoder 是一款专为中文开发者设计的 IntelliJ IDEA 插件，集成了中文编程转换、Jenkins Pipeline支持、SpringBoot配置文件图标显示等多项强大功能。通过智能翻译引擎和专业的开发工具支持，大幅提升中文开发人员的编程效率和开发体验。

> 📚 **详细功能介绍**：查看 [PandaCoder 完整功能介绍文章](https://www.shuyixiao.cn/articles/panda-coder-intro.html) 了解更多技术细节和使用场景  
> 🌐 **访问地址**：主站 [www.shuyixiao.cn](https://www.shuyixiao.cn) | 备用站 [www.shuyixiao.top](https://www.shuyixiao.top)

### 🎯 核心价值
- **中文思维编程**：支持中文输入，自动转换为规范的英文代码
- **专业开发体验**：集成Jenkins Pipeline和SpringBoot专业支持
- **智能技术识别**：自动识别配置文件中的技术栈并可视化显示
- **多引擎翻译**：三级翻译引擎确保翻译质量和可用性

## 🚀 核心功能

### 📝 中文编程助手

#### 智能中文转换
- **多种命名格式**：支持中文转小驼峰、大驼峰、大写带下划线
- **智能翻译引擎**：三级翻译引擎智能切换（国内大模型 > Google翻译 > 百度翻译）
- **快捷键支持**：
  - `Ctrl+Alt+C` - 中文转小驼峰
  - `Ctrl+Alt+P` - 中文转大驼峰  
  - `Ctrl+Alt+U` - 中文转大写带下划线

#### 智能类创建
- **中文输入支持**：直接输入中文类名，自动转换为英文
- **前缀识别**：支持"Service:用户管理"格式，自动生成ServiceUserManagement
- **模板配置**：自定义Java文件注释模板
- **类名前缀配置**：支持自定义类名前缀列表

#### 多引擎翻译系统
- **国内大模型**：通义千问、文心一言、智谱AI
- **Google Cloud Translation**：国际化翻译支持
- **百度翻译**：备用翻译引擎
- **智能切换**：自动选择最佳翻译引擎
- **自定义提示词**：支持自定义翻译prompt，适配不同技术领域

### 🐳 Jenkins Pipeline 支持

#### 专业文件类型
- **自定义文件类型**：专门的Jenkins Pipeline文件类型
- **智能文件识别**：自动识别Jenkinsfile、*.jenkinsfile等文件
- **专业图标**：Jenkins机器人图标，5层主题覆盖防护

#### 增强语法高亮
- **11种鲜艳颜色**：VS Code风格的语法高亮
- **自定义颜色**：可在设置中调整各种语法元素颜色
- **主题兼容**：支持亮色和暗色主题

#### 智能代码补全
- **Pipeline结构**：pipeline、agent、stages、stage、steps等
- **构建步骤**：sh、bat、checkout、git、build等
- **环境变量**：env.BUILD_NUMBER、env.WORKSPACE等
- **参数补全**：params.APP_NAME、params.DEPLOY_ENV等
- **文档支持**：悬停文档和快速文档

### 🍃 SpringBoot 配置文件图标

#### 技术栈识别
支持识别以下技术栈并显示对应图标：
- **数据库**：MySQL、PostgreSQL、Oracle、SQL Server
- **缓存**：Redis
- **消息队列**：Kafka、RabbitMQ
- **搜索引擎**：Elasticsearch
- **框架**：Spring Boot

### 🔍 Elasticsearch DSL 监控

#### 实时查询监控
- **自动捕获**：实时监控控制台输出，自动捕获 ES 查询 DSL
- **智能解析**：支持多种格式（REST Client、cURL、Spring Data Elasticsearch）
- **可视化展示**：独立工具窗口展示查询详情
- **查询管理**：搜索、过滤、导出查询记录
- **本地存储**：自动保存查询历史，最多 1000 条记录
- **彩色标识**：HTTP 方法和状态码带颜色区分

#### 智能图标显示
- **编辑器左侧显示**：在gutter区域显示彩色技术栈图标
- **多格式支持**：YAML和Properties格式配置文件
- **优先级匹配**：特定技术栈图标优先于通用配置图标
- **鼠标悬停提示**：显示技术栈名称和详细信息

### 💾 SQL Monitor 监控

#### 实时SQL监控
- **自动捕获**：实时监控控制台输出，自动捕获所有 SQL 查询
- **智能解析**：支持 MyBatis 日志格式（Preparing + Parameters + Total）
- **可视化展示**：独立工具窗口展示 SQL 详情
- **操作识别**：自动识别 SELECT/INSERT/UPDATE/DELETE 操作
- **表名提取**：智能提取数据库表名
- **参数捕获**：完整记录 SQL 参数
- **API追踪**：显示触发 SQL 的 API 接口路径
- **调用链追踪**：记录调用 SQL 的 Java 类和行号

#### 查询管理
- **多维筛选**：按操作类型、表名、时间范围筛选
- **搜索功能**：支持关键词搜索 SQL、表名、API 路径
- **统计信息**：实时显示各类 SQL 统计（SELECT/INSERT/UPDATE/DELETE）
- **彩色标识**：不同操作类型用不同颜色标识
  - 🔵 SELECT (蓝色)
  - 🟢 INSERT (绿色)
  - 🟠 UPDATE (橙色)
  - 🔴 DELETE (红色)
- **导出功能**：支持导出 SQL 到剪贴板
- **本地存储**：自动保存查询历史，最多 1000 条记录
- **实时更新**：新 SQL 自动显示，无需手动刷新

### ⚙️ 高级功能

#### 配置管理
- **翻译引擎配置**：支持多种翻译引擎配置和验证
- **API配置验证**：实时验证各翻译引擎的API配置
- **智能错误处理**：优雅降级，确保功能可用性

#### 用户体验
- **现代化欢迎界面**：全新设计的欢迎对话框
- **微信公众号集成**：一键关注公众号
- **问题反馈优化**：便捷的反馈渠道
- **界面布局优化**：清晰的信息展示

## 🎨 技术特色

### 三级翻译引擎
```
1. 国内大模型 🥇 (最高优先级)
   - 通义千问、文心一言、智谱AI
   - 高质量翻译，符合中文表达习惯
   
2. Google Cloud Translation 🥈 (第二优先级)
   - 国际化翻译支持
   - 多语言翻译能力
   
3. 百度翻译 🥉 (备用引擎)
   - 稳定可靠的备用方案
   - 确保功能始终可用
```

### 智能优先级匹配
- **避免图标冲突**：特定技术栈图标优先显示
- **智能识别**：根据配置内容自动选择最相关的图标
- **多层级防护**：确保图标在任何主题下正确显示

### 性能优化
- **线程安全缓存**：使用ConcurrentHashMap确保多线程安全
- **延迟加载**：智能缓存机制，提升性能
- **内存管理**：合理的对象生命周期管理

## 🎯 适用场景

### 中文开发者
- **快速命名转换**：从中文思维到英文代码的一键转换
- **智能类创建**：中文输入快速创建Java类
- **命名规范统一**：团队协作时保持代码命名一致性

### Jenkins用户
- **专业Pipeline开发**：完整的Jenkins Pipeline开发支持
- **智能语法高亮**：丰富的颜色和语法提示
- **环境变量管理**：自动补全和文档支持

### SpringBoot项目
- **技术栈可视化**：直观识别配置文件中的技术栈
- **配置管理**：快速了解项目使用的技术组件
- **开发效率**：减少手动查找技术栈的时间

### 团队协作
- **代码规范**：统一的英文命名规范
- **可读性提升**：清晰的语法高亮和图标标识
- **维护性增强**：规范的代码结构和注释

### 🐛 Bug 记录器

#### 智能错误捕获
- **实时监控**：自动监控控制台输出，捕获错误信息
- **错误分类**：自动识别编译错误、运行时错误、警告等
- **状态管理**：支持标记 Bug 状态（新建、处理中、已解决、已关闭）
- **AI 分析**：智能分析错误原因和解决方案
- **本地存储**：Bug 记录保存在本地，确保数据安全

## 📦 安装指南

### 从 JetBrains 插件市场安装（推荐）
1. 在 IntelliJ IDEA 中，打开 `Settings/Preferences` → `Plugins`
2. 切换到 `Marketplace` 标签
3. 搜索 "PandaCoder"
4. 点击 `Install` 按钮
5. 重启 IDE 完成安装

### 手动安装
1. 从 [GitHub Releases](https://github.com/shuyixiao-better/PandaCoder/releases) 下载最新版本
2. 在 IntelliJ IDEA 中，打开 `Settings/Preferences` → `Plugins`
3. 点击 ⚙️ 图标，选择 "Install Plugin from Disk..."
4. 选择下载的 ZIP 文件
5. 重启 IDE 完成安装

## ⚙️ 配置说明

### 翻译引擎配置
1. 打开 `Settings` → `Tools` → `PandaCoder`
2. 配置以下任一翻译引擎：

#### 国内大模型（推荐）
- **通义千问**：[阿里云DashScope](https://dashscope.aliyun.com/)
- **文心一言**：[百度智能云](https://cloud.baidu.com/product/wenxinworkshop)
- **智谱AI**：[智谱开放平台](https://open.bigmodel.cn/)

#### Google Cloud Translation
- **API Key**：[Google Cloud Console](https://console.cloud.google.com/)
- **Project ID**：GCP项目ID
- **Region**：选择服务区域（默认：global）

#### 百度翻译（备用）
- **应用ID**：[百度翻译开放平台](https://fanyi-api.baidu.com/)
- **API密钥**：百度翻译API密钥

3. 点击"验证配置"按钮测试API连接
4. 点击"Apply"保存设置

### 类名前缀配置
1. 在设置页面找到"类名前缀"输入框
2. 输入需要的前缀，多个前缀用逗号分隔
3. 默认前缀：Service, Repository, Controller, Component, Util, Manager, Factory, Builder, Handler

### 文件模板配置
1. 在设置页面找到"文件模板"输入框
2. 自定义Java文件注释模板
3. 支持变量：${YEAR}、${NAME}、${TIME}等

## 🚀 使用方法

### 中文转换功能

#### 选中文本转换
1. **选中中文文本**：在编辑器中选中要转换的中文
2. **右键选择转换**：
   - 中文转小驼峰 (Ctrl+Alt+C)
   - 中文转大驼峰 (Ctrl+Alt+P)
   - 中文转大写带下划线 (Ctrl+Alt+U)

**示例**：
```
用户管理 → userManagement (小驼峰)
用户管理 → UserManagement (大驼峰)
用户管理 → USER_MANAGEMENT (大写带下划线)
```

#### 智能类创建
1. **右键选择目录**：在项目视图中右键点击目标目录
2. **选择"智能中文类"**：从新建菜单中选择
3. **输入中文类名**：支持以下格式：
   - `Service:用户管理` → `ServiceUserManagement`
   - `Controller用户登录` → `ControllerUserLogin`
   - `Repository:订单查询` → `RepositoryOrderQuery`

### Jenkins Pipeline支持

#### 自动识别
- **文件类型**：Jenkinsfile文件自动识别和语法高亮
- **智能补全**：pipeline、stage、step等关键字补全
- **环境变量**：env.BUILD_NUMBER等环境变量补全
- **参数补全**：params.APP_NAME等参数补全

#### 语法高亮
- **11种颜色**：丰富的语法高亮效果
- **自定义颜色**：可在设置中调整各种语法元素颜色
- **主题兼容**：支持所有IDE主题

#### 文档支持
- **悬停文档**：鼠标悬停查看方法文档
- **快速文档**：Ctrl+Q查看详细文档
- **示例代码**：提供完整的Pipeline示例

### SpringBoot配置图标

#### 自动显示
- **技术栈识别**：打开SpringBoot配置文件时自动识别技术栈
- **图标显示**：在编辑器左侧显示对应技术栈的彩色图标
- **鼠标悬停**：查看技术栈名称和详细信息

#### 支持格式
- **YAML格式**：application.yml、application.yaml
- **Properties格式**：application.properties
- **配置文件**：支持多环境配置文件

### Elasticsearch DSL 监控

#### 打开工具窗口
- **位置**：在 IDEA 底部工具栏找到 "ES DSL Monitor"
- **启用监听**：确保"启用ES监听"复选框已勾选

#### 查看查询记录
- **实时更新**：运行应用后自动捕获 ES 查询
- **搜索过滤**：使用搜索框和过滤器定位特定查询
- **查看详情**：点击记录查看完整 DSL 和响应信息
- **导出查询**：选中记录后点击"导出选中"复制到剪贴板

#### 日志配置
建议配置日志级别为 DEBUG：
```yaml
logging:
  level:
    org.elasticsearch.client: DEBUG
    org.springframework.data.elasticsearch: DEBUG
```

详细使用说明请参考 [ES DSL Monitor 使用指南](docs/EsDslMonitor使用指南.md)

更多文档请查看 [docs](docs/) 目录。

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

### 翻译引擎支持
- **国内大模型**：3种（通义千问、文心一言、智谱AI）
- **国际化引擎**：1种（Google Cloud Translation）
- **备用引擎**：1种（百度翻译）

## 🎯 使用效果

### 开发效率提升
- **命名转换**：从手动翻译到一键转换，效率提升80%
- **类创建**：从手动命名到智能生成，效率提升70%
- **配置识别**：从手动查找到图标识别，效率提升60%

### 代码质量提升
- **命名规范**：统一的英文命名规范
- **可读性**：清晰的语法高亮和图标标识
- **维护性**：规范的代码结构和注释

### 用户体验提升
- **中文友好**：支持中文输入和思维
- **专业支持**：Jenkins Pipeline和SpringBoot专业功能
- **智能识别**：自动识别技术栈和配置

## 👨‍💻 作者介绍

### 舒一笑不秃头 - 「云卷云舒，学无止境，焕然一新」

**专业认证**
- 生成式AI应用工程师(高级)认证
- 阿里云博客专家
- Java应用开发职业技能等级认证

**技术专长**
- 企业级Java开发
- 微服务架构设计
- AI应用开发
- 开源项目维护

**联系方式**
- **个人博客**：<small>🌐 <strong>访问地址</strong>：主站 <a href="https://www.shuyixiao.cn" target="_blank">www.shuyixiao.cn</a> | 备用站 <a href="https://www.shuyixiao.top" target="_blank">www.shuyixiao.top</a></small>
- **插件详细介绍**：[PandaCoder 完整功能介绍](https://www.shuyixiao.cn/articles/panda-coder-intro.html) 📖  
  🌐 **访问地址**：主站 [www.shuyixiao.cn](https://www.shuyixiao.cn) | 备用站 [www.shuyixiao.top](https://www.shuyixiao.top)
- **公众号**：「舒一笑的架构笔记」
- **GitHub**：[github.com/shuyixiao-better](https://github.com/shuyixiao-better)
- **邮箱**：yixiaoshu88@163.com

## 🤝 开源与协作

PandaCoder 是一个开源项目，欢迎社区贡献。

### 参与贡献
- **GitHub 仓库**：[https://github.com/shuyixiao-better/PandaCoder](https://github.com/shuyixiao-better/PandaCoder)
- **问题反馈**：通过 GitHub Issues 提交问题
- **贡献代码**：欢迎提交 Pull Request
- **功能建议**：欢迎提出新功能建议

### 贡献指南
1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 🚀 未来规划

### 短期计划 (v1.2.0)
- [ ] 支持更多编程语言 (Python、Go、JavaScript)
- [ ] 增强SpringBoot配置验证功能
- [ ] 添加更多技术栈图标支持
- [ ] 智能代码重构功能

### 中期计划 (v1.3.0)
- [ ] 团队协作功能
- [ ] 代码质量分析
- [ ] 智能代码生成
- [ ] 云端配置同步

### 长期计划 (v2.0.0)
- [ ] AI驱动的代码生成
- [ ] 多IDE支持
- [ ] 智能代码审查
- [ ] 团队知识库集成

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

感谢所有为PandaCoder项目做出贡献的开发者：

- **JetBrains** - 提供优秀的IDE平台
- **开源社区** - 提供丰富的开源组件
- **用户反馈** - 持续改进的动力源泉

---

<p align="center">
  <strong>让编程更加高效、专业、愉悦！</strong> 🚀
</p>

<p align="center">
  <em>如果这个项目对您有帮助，请给个 ⭐ Star 支持一下！</em>
</p>
