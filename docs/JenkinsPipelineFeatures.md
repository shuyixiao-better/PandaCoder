# Jenkins Pipeline 功能特性

PandaCoder 插件现已集成完整的 Jenkins Pipeline 支持功能，为 IntelliJ IDEA 用户提供专业的 Jenkins Pipeline 开发体验。

## 🚀 核心功能

### 1. 智能代码补全

#### Pipeline 结构补全
- **声明式 Pipeline**：自动补全 `pipeline`、`agent`、`stages`、`stage`、`steps` 等核心结构
- **脚本式 Pipeline**：支持 `node`、`stage` 等传统语法结构
- **上下文感知**：根据当前代码位置提供相应的补全选项

#### 构建步骤补全
- **Shell 命令**：`sh`、`bat`、`powershell` 等系统命令步骤
- **源码管理**：`checkout`、`git` 等版本控制相关步骤
- **构建工具**：`build`、`parallel` 等构建流程控制
- **工件管理**：`archiveArtifacts`、`stash`、`unstash` 等工件操作
- **测试发布**：`publishTestResults`、`junit` 等测试结果处理
- **通知通信**：`emailext`、`slackSend` 等通知功能

#### 指令补全
- **Pipeline 指令**：`environment`、`parameters`、`options`、`tools`、`triggers`、`when`、`post`
- **Post 条件**：`always`、`success`、`failure`、`unstable`、`changed` 等执行条件
- **参数类型**：`string`、`booleanParam`、`choice`、`password` 等参数定义

### 2. 环境变量智能补全

#### 内置环境变量
自动补全 Jenkins 提供的内置环境变量：
```groovy
env.BUILD_NUMBER        // 构建编号
env.JOB_NAME           // 作业名称
env.WORKSPACE          // 工作空间路径
env.BRANCH_NAME        // 分支名称
env.GIT_COMMIT         // Git 提交哈希
env.JENKINS_URL        // Jenkins 服务器地址
// ... 更多内置变量
```

#### 自定义环境变量
智能识别和补全在 `environment` 块中定义的自定义变量：
```groovy
pipeline {
    environment {
        MAVEN_OPTS = '-Xmx1024m'
        DEPLOY_ENV = 'staging'
    }
    stages {
        stage('Build') {
            steps {
                // 自动补全 env.MAVEN_OPTS 和 env.DEPLOY_ENV
                echo "Maven options: ${env.MAVEN_OPTS}"
            }
        }
    }
}
```

#### withEnv 变量识别
自动识别 `withEnv` 步骤中定义的临时环境变量：
```groovy
withEnv(['PATH+MAVEN=/usr/local/maven/bin']) {
    // 识别临时环境变量
}
```

### 3. 参数智能补全

#### 参数定义识别
自动识别 `parameters` 块中定义的参数：
```groovy
pipeline {
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false)
        choice(name: 'DEPLOY_TARGET', choices: ['dev', 'staging', 'prod'])
    }
    stages {
        stage('Deploy') {
            steps {
                // 自动补全 params.BRANCH_NAME、params.SKIP_TESTS、params.DEPLOY_TARGET
                echo "部署分支: ${params.BRANCH_NAME}"
            }
        }
    }
}
```

#### 类型推断
根据参数定义类型提供正确的类型提示：
- `string` → `java.lang.String`
- `booleanParam` → `java.lang.Boolean`
- `choice` → `java.lang.String`
- 其他参数类型 → 相应的 Java 类型

### 4. 文档支持

#### 内联文档
鼠标悬停或按 `Ctrl+Q` 查看详细的方法文档：
- **方法签名**：完整的参数列表和类型信息
- **功能描述**：方法的详细使用说明
- **参数说明**：每个参数的类型、是否必需、用途描述
- **使用示例**：常见的使用场景和代码示例

#### 外部文档链接
提供 Jenkins 官方文档的直接链接，方便查看完整的 API 文档。

### 5. 文件识别

#### 自动识别 Jenkins 文件
智能识别以下文件类型并提供 Pipeline 支持：
- `Jenkinsfile`（标准 Jenkins 文件）
- `Jenkinsfile.*`（带扩展名的 Jenkins 文件）
- 包含 `pipeline {` 或 `node {` 语法的 Groovy 文件
- 包含 `@Library` 注解的共享库文件

#### 语法类型检测
- **声明式 Pipeline**：检测 `pipeline {}` 语法结构
- **脚本式 Pipeline**：检测 `node {}` 语法结构
- **混合模式**：支持在声明式 Pipeline 中使用 `script {}` 块

## 📁 项目结构

```
src/main/java/com/shuyixiao/jenkins/
├── gdsl/                           # GDSL 智能补全系统
│   ├── JenkinsGdslService.java     # 核心服务，管理语法描述符
│   └── JenkinsGdslMembersProvider.java # 补全提供器
├── provider/                       # 内容提供器
│   └── JenkinsMapContentProvider.java  # 环境变量和参数映射
├── documentation/                  # 文档支持
│   └── JenkinsDocumentationProvider.java # 文档提供器
├── model/                         # 数据模型
│   └── Descriptor.java            # 语法描述符模型
└── util/                          # 工具类
    ├── JenkinsFileDetector.java   # 文件类型识别
    └── PsiUtils.java              # PSI 操作工具

src/main/resources/
└── descriptors/
    └── jenkinsPipeline.xml        # Jenkins Pipeline 语法定义
```

## 🛠️ 技术实现

### GDSL 系统
- 基于 IntelliJ IDEA 的 GDSL（Groovy Domain Specific Language）框架
- 动态加载 XML 格式的语法描述符文件
- 提供上下文感知的智能补全

### PSI 分析
- 利用 IntelliJ IDEA 的 PSI（Program Structure Interface）
- 实时分析代码结构和上下文
- 支持复杂的语法树遍历和模式匹配

### 缓存优化
- 使用 `ConcurrentHashMap` 提供线程安全的缓存
- 延迟加载和智能失效机制
- 优化大型项目的性能表现

## 🎯 使用场景

### DevOps 工程师
- 快速编写和维护 Jenkins Pipeline
- 减少语法错误和配置问题
- 提高 CI/CD 流程开发效率

### Java 开发者
- 无缝集成到现有的 Java 开发工作流
- 利用熟悉的 IDE 功能进行 Pipeline 开发
- 享受代码补全、文档查看等高级功能

### 团队协作
- 统一的代码风格和最佳实践
- 降低 Jenkins Pipeline 的学习门槛
- 提高团队整体的 DevOps 能力

## 🔧 配置与扩展

### 自定义语法支持
可以通过修改 `jenkinsPipeline.xml` 文件添加自定义的 Jenkins 插件语法支持。

### 性能调优
插件提供了多种缓存和优化选项，可以根据项目规模进行调整。

### 调试功能
支持重新加载语法描述符，方便插件开发和调试。

## 📝 示例文件

项目根目录提供了 `Jenkinsfile.example` 示例文件，展示了各种 Jenkins Pipeline 功能的使用方法。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来改进 Jenkins Pipeline 功能。请确保：
1. 遵循现有的代码风格
2. 添加适当的测试用例
3. 更新相关文档

## 📄 许可证

本功能遵循项目的开源许可证，详见 LICENSE 文件。 