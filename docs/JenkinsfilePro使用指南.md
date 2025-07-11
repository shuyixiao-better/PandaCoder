# JenkinsfilePro 功能使用指南和故障排除

## 🎯 插件功能验证

### 1. 文件识别验证
- 创建名为 `Jenkinsfile` 的文件
- 创建以 `Jenkinsfile.` 开头的文件（如 `Jenkinsfile.dev`）
- 在文件中包含 `pipeline {` 或 `node {` 语法

### 2. 智能补全验证

#### 2.1 Pipeline结构补全
在Jenkinsfile中输入以下内容，应该有智能补全：
```groovy
// 输入 "pip" 应该提示 "pipeline"
pipeline {
    // 输入 "ag" 应该提示 "agent"
    agent any
    
    // 输入 "sta" 应该提示 "stages"
    stages {
        // 输入 "sta" 应该提示 "stage"
        stage('Build') {
            // 输入 "ste" 应该提示 "steps"
            steps {
                // 这里应该有各种Jenkins方法的补全
            }
        }
    }
}
```

#### 2.2 构建步骤补全
在 `steps` 块中输入以下字符，应该有对应的补全：
- `sh` - Shell脚本执行
- `echo` - 输出消息
- `bat` - Windows批处理命令
- `checkout` - 检出代码
- `git` - Git操作
- `archiveArtifacts` - 归档工件
- `publishTestResults` - 发布测试结果

#### 2.3 环境变量补全
```groovy
pipeline {
    agent any
    environment {
        MAVEN_OPTS = '-Xmx1024m'
        NODE_VERSION = '18'
    }
    stages {
        stage('Build') {
            steps {
                // 输入 "env." 应该显示：
                // - MAVEN_OPTS, NODE_VERSION (自定义变量)
                // - BUILD_NUMBER, JOB_NAME, WORKSPACE 等 (内置变量)
                echo "${env.MAVEN_OPTS}"
            }
        }
    }
}
```

#### 2.4 参数补全
```groovy
pipeline {
    agent any
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false)
    }
    stages {
        stage('Deploy') {
            steps {
                // 输入 "params." 应该显示：
                // - BRANCH_NAME, SKIP_TESTS (自定义参数)
                echo "Branch: ${params.BRANCH_NAME}"
            }
        }
    }
}
```

### 3. 文档支持验证
- 将鼠标悬停在Jenkins方法上（如 `sh`, `echo`, `pipeline` 等）
- 按 `Ctrl+Q`（Windows/Linux）或 `Cmd+J`（macOS）查看快速文档
- 应该显示方法签名、参数说明和使用文档

## 🔧 故障排除指南

### 问题1: 没有语法高亮
**这是最常见的问题！解决方案：**

1. **手动设置文件类型（最简单）**
   - 右键点击Jenkinsfile文件
   - 选择 "Override File Type" 
   - 选择 "Groovy"
   - 点击 "OK"

2. **全局文件类型关联**
   - File → Settings → Editor → File Types
   - 选择 "Groovy" 文件类型
   - 在 "File name patterns" 中添加 `Jenkinsfile`
   - 应用设置

3. **详细配置指南**
   - 查看 `docs/Jenkinsfile语法高亮配置指南.md`

### 问题2: 没有智能补全
**可能原因和解决方案：**

1. **Groovy插件未启用**
   - 检查：File → Settings → Plugins → 搜索 "Groovy"
   - 确保Groovy插件已启用并重启IDE

2. **文件类型识别问题**
   - **重要：首先解决语法高亮问题（见问题1）**
   - 确保状态栏右下角显示 "Groovy" 而不是 "Text"

3. **缓存问题**
   - File → Invalidate Caches and Restart
   - 重启IDE

4. **GDSL脚本未生效**
   - 检查 `src/main/resources/jenkins-pipeline.gdsl` 文件是否存在
   - 在项目设置中检查GDSL脚本是否被识别

### 问题2: 环境变量和参数补全不工作
**解决方案：**

1. **检查文件内容**
   - 确保有 `environment` 块定义环境变量
   - 确保有 `parameters` 块定义参数

2. **检查语法**
   ```groovy
   // 正确的环境变量定义
   environment {
       MAVEN_OPTS = '-Xmx1024m'
   }
   
   // 正确的参数定义
   parameters {
       string(name: 'BRANCH_NAME', defaultValue: 'main')
   }
   ```

3. **重新索引项目**
   - File → Invalidate Caches and Restart

### 问题3: 文档不显示
**解决方案：**

1. **检查文档提供器**
   - 确保JenkinsDocumentationProvider已注册在plugin.xml中

2. **验证快捷键**
   - Windows/Linux: `Ctrl+Q`
   - macOS: `Cmd+J`
   - 或右键 → "Quick Documentation"

### 问题4: 插件加载失败
**解决方案：**

1. **检查插件依赖**
   - 确保已安装Groovy插件
   - 检查IntelliJ IDEA版本兼容性（需要2024.1+）

2. **查看错误日志**
   - Help → Show Log in Explorer/Finder
   - 查看idea.log文件中的错误信息

3. **重新安装插件**
   - 卸载当前插件
   - 重启IDE
   - 重新安装插件

## 🚀 最佳实践

### 1. 文件命名
推荐的Jenkins文件命名：
- `Jenkinsfile` - 主Pipeline文件
- `Jenkinsfile.dev` - 开发环境Pipeline
- `Jenkinsfile.prod` - 生产环境Pipeline

### 2. 代码组织
```groovy
pipeline {
    agent any
    
    // 环境变量定义在顶部
    environment {
        MAVEN_OPTS = '-Xmx1024m'
    }
    
    // 参数定义
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main')
    }
    
    // 主要构建阶段
    stages {
        stage('Checkout') { /* ... */ }
        stage('Build') { /* ... */ }
        stage('Test') { /* ... */ }
        stage('Deploy') { /* ... */ }
    }
    
    // 后置操作
    post {
        always { /* ... */ }
        success { /* ... */ }
        failure { /* ... */ }
    }
}
```

### 3. 充分利用智能补全
- 在不确定方法名时，输入前几个字符让IDE提示
- 使用Tab键快速接受补全建议
- 使用Ctrl+Space强制触发补全

## 📞 获取帮助

如果遇到问题：
1. 查看本故障排除指南
2. 检查IDE的事件日志（View → Tool Windows → Event Log）
3. 重启IDE并重试
4. 检查插件版本和兼容性

## 🔄 验证清单

使用以下清单验证插件功能：

### 第一步：语法高亮验证（必须）
- [ ] **Jenkinsfile文件有语法高亮**（关键字有颜色，字符串有颜色）
- [ ] **状态栏右下角显示"Groovy"**（不是"Text"或"Plain text"）
- [ ] **可以折叠代码块**（如pipeline {}、stage {}等）

### 第二步：智能补全验证
- [ ] 在空行输入"pipeline"有补全提示
- [ ] 在steps块中输入"sh"有补全提示
- [ ] 定义环境变量后，"env."有相应补全
- [ ] 定义参数后，"params."有相应补全

### 第三步：文档支持验证  
- [ ] 鼠标悬停在Jenkins方法上显示文档
- [ ] 使用Ctrl+Q可以查看详细文档

## ⚠️ 重要提示

**如果第一步语法高亮不通过，请先按照 `docs/Jenkinsfile语法高亮配置指南.md` 配置文件类型关联！**

只有正确识别为Groovy文件后，智能补全和文档功能才能正常工作。 