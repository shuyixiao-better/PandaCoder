# PandaCoder

<p align="center">
  <img src="https://github.com/shuyixiao-better/PandaCoder/blob/main/src/main/resources/META-INF/pluginIcon.svg" width="128" alt="PandaCoder Logo">
</p>

## 项目说明

PandaCoder 是一款专为中文母语的 Java/Kotlin 开发者设计的 IntelliJ IDEA 插件，帮助开发者轻松实现「中文思维->英文代码」的无缝转换。

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

## 开源与协作

PandaCoder 是一个开源项目，我们欢迎社区贡献。无论是功能建议、Bug 报告还是代码贡献，都能帮助我们打造更好的工具。

- **GitHub 仓库**：[https://github.com/shuyixiao/PandaCoder](https://github.com/shuyixiao/PandaCoder)
- **问题反馈**：通过 GitHub Issues 提交问题
- **贡献代码**：欢迎提交 Pull Request

## 许可证

请参阅项目中的LICENSE文件。
