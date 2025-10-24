# 活文档插件 - UI 配置使用指南

## 📋 概述

PandaCoder 活文档功能提供了完整的可视化配置界面，所有设置都可以通过 UI 界面完成，无需手动编辑配置文件。

---

## 🎨 界面总览

活文档功能包含以下 UI 组件：

1. **设置面板** (Settings/Preferences) - 配置所有选项
2. **工具窗口** (Tool Window) - 搜索、问答、统计
3. **菜单操作** (Actions) - 快捷操作
4. **通知提示** (Notifications) - 状态反馈

---

## ⚙️ 设置面板使用指南

### 打开设置面板

**Windows/Linux**: `File -> Settings -> Tools -> 活文档 (Living Doc)`  
**macOS**: `IntelliJ IDEA -> Preferences -> Tools -> 活文档 (Living Doc)`

### Tab 1: AI 模型配置

#### AI 提供商选择

| 选项 | 说明 | 适用场景 |
|------|------|----------|
| **gitee-ai** | Gitee AI（模力方舟）| 国内用户（推荐） |
| **openai** | OpenAI | 追求最佳效果 |
| **ollama** | 本地模型 | 企业内网、隐私保护 |

#### Gitee AI 配置（推荐）

![Gitee AI 配置](https://via.placeholder.com/600x300?text=Gitee+AI+Config)

**配置项**:
- **API Key**: 在 [Gitee AI](https://ai.gitee.com/) 获取
  - 点击：工作台 -> 设置 -> 访问令牌
  - 创建新令牌
  - 复制到此处
  
- **Base URL**: `https://ai.gitee.com/v1` (默认，无需修改)

- **Chat 模型**: 对话生成模型
  - `qwen-plus` (推荐) - 性价比高
  - `qwen-max` - 最强性能
  - `deepseek-chat` - 编程能力强

- **Embedding 模型**: 向量化模型
  - `text-embedding-v3` (推荐) - 1024维
  - `bge-large-zh-v1.5` - 中文优化，1024维
  - `m3e-base` - 轻量快速，768维

💡 **提示**: 首次使用需要在 Gitee AI 购买资源包（建议先购买 ¥10 测试）

#### OpenAI 配置

**配置项**:
- **API Key**: OpenAI API密钥
- **Base URL**: `https://api.openai.com/v1`
- **Chat 模型**: `gpt-4o-mini` 或 `gpt-4`
- **Embedding 模型**: `text-embedding-3-small`

#### Ollama 配置（本地模型）

**配置项**:
- **Base URL**: `http://localhost:11434` (Ollama 默认端口)
- **Chat 模型**: `qwen2.5:7b`
- **Embedding 模型**: `nomic-embed-text`

💡 **提示**: 使用前需要先安装 Ollama 并拉取模型：
```bash
# 安装 Ollama
curl https://ollama.ai/install.sh | sh

# 拉取模型
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

---

### Tab 2: 向量数据库配置

#### 数据库类型选择

| 类型 | 说明 | 推荐场景 |
|------|------|----------|
| **elasticsearch** | Elasticsearch 8.15 | 生产环境（推荐）|
| **pgvector** | PostgreSQL 扩展 | 已有 PostgreSQL |
| **chroma** | 专业向量库 | 专业 RAG 应用 |
| **simple** | 内存/文件存储 | 开发测试 |

#### Elasticsearch 8.15 配置（默认推荐）

![ES 配置](https://via.placeholder.com/600x300?text=Elasticsearch+Config)

**基础配置**:
- **主机**: `localhost` (本地) 或远程 IP
- **端口**: `9200` (默认)
- **用户名**: 可选，如果启用了安全认证
- **密码**: 可选

**高级配置**:
- **索引名称**: `livingdoc_vectors` (建议不要改)
- **向量维度**: `1024` (需与 Embedding 模型匹配)
  - text-embedding-v3: 1024
  - bge-large-zh-v1.5: 1024
  - m3e-base: 768
  - OpenAI ada-002: 1536

- **相似度算法**:
  - `cosine` (推荐) - 余弦相似度
  - `dot_product` - 点积
  - `l2_norm` - 欧氏距离

💡 **快速启动 Elasticsearch**:
```bash
docker run -d --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.15.0
```

验证连接：
```bash
curl http://localhost:9200
```

---

### Tab 3: RAG 检索配置

![RAG 配置](https://via.placeholder.com/600x300?text=RAG+Config)

**文档分块配置**:
- **分块大小**: `800` 字符
  - 小值 (500-700): 精确搜索
  - 中值 (700-1000): 平衡（推荐）
  - 大值 (1000-1500): 更多上下文

- **分块重叠**: `200` 字符
  - 保证上下文连贯性
  - 建议设为分块大小的 20-30%

**检索配置**:
- **Top-K**: `5`
  - 返回最相似的文档数量
  - 建议 3-10 之间

- **相似度阈值**: `0.7`
  - 过滤不相关结果
  - 范围 0.0-1.0
  - 建议 0.6-0.8

💡 **配置建议**:
| 场景 | 分块大小 | 重叠 | Top-K | 阈值 |
|------|----------|------|-------|------|
| API 文档 | 500-700 | 100-150 | 3-5 | 0.75 |
| 技术文档 | 800-1000 | 200-300 | 5-7 | 0.70 |
| 代码注释 | 300-500 | 50-100 | 3 | 0.80 |

---

### Tab 4: 其他设置

**自动化选项**:
- ☑️ **代码保存时自动索引**: 保存 Java 文件时自动更新文档索引
- ☑️ **显示通知消息**: 显示操作成功/失败的通知
- ☑️ **启用详细日志**: 记录调试日志（排查问题时启用）

---

## 🪟 工具窗口使用指南

### 打开工具窗口

**方式 1**: `View -> Tool Windows -> 活文档`  
**方式 2**: 点击右侧边栏的 "活文档" 图标  
**方式 3**: 快捷键 `Ctrl+Alt+Shift+S`

### Tab 1: 📄 搜索

![搜索界面](https://via.placeholder.com/600x400?text=Search+Tab)

**使用步骤**:
1. 在搜索框输入问题，例如：
   - "用户登录接口的参数有哪些？"
   - "如何创建订单？"
   - "获取用户信息的 API 路径"

2. 点击"搜索"按钮或按 `Enter`

3. 查看搜索结果：
   - 显示最相关的文档
   - 包含相似度分数
   - 可以双击跳转到源代码

**搜索技巧**:
- ✅ 使用完整的问句，而不是关键词
- ✅ 描述具体的功能，如"登录"而不是"用户"
- ✅ 包含动作词，如"如何"、"怎样"
- ❌ 避免过于宽泛的查询

### Tab 2: 💬 问答

![问答界面](https://via.placeholder.com/600x400?text=Chat+Tab)

**使用步骤**:
1. 在输入框输入问题
2. 点击"发送"或按 `Enter`
3. AI 助手会基于文档回答问题

**示例对话**:
```
🧑 您: 用户登录接口需要哪些参数？

🤖 AI助手: 根据文档，用户登录接口需要以下参数：

1. username (String, 必填) - 用户名
2. password (String, 必填) - 密码

接口路径：POST /api/user/login

参考文档：UserController.java:45

还有其他问题吗？
```

**对话技巧**:
- 可以追问："那响应格式是什么？"
- 可以要求示例："给我一个请求示例"
- 可以询问位置："这个接口在哪个文件？"

### Tab 3: 📊 统计

![统计界面](https://via.placeholder.com/600x400?text=Stats+Tab)

**显示信息**:
- **系统状态**: AI 提供商、向量数据库类型
- **数据库连接**: ES 地址和端口
- **索引统计**: 
  - 总文档数
  - 健康状态
  - 最后索引时间

**操作按钮**:
- **刷新统计**: 更新统计信息
- **重新索引项目**: 完全重新索引
- **清空索引**: 删除所有文档（谨慎使用）

---

## 🎯 菜单操作使用指南

### Tools 菜单

**Tools -> 活文档**:

1. **索引项目文档** (`Ctrl+Alt+Shift+I`)
   - 分析项目代码
   - 提取 API 信息
   - 向量化并存储
   - 显示进度条

2. **搜索文档** (`Ctrl+Alt+Shift+S`)
   - 打开工具窗口
   - 切换到搜索 Tab

3. **导出文档**
   - 选择导出目录
   - 选择格式（Markdown/HTML/OpenAPI）
   - 导出文件

---

## 🔔 通知系统

### 通知类型

**信息通知** (蓝色):
- ✓ 索引完成
- ✓ 导出成功
- ✓ 连接成功

**警告通知** (黄色):
- ⚠️ 配置不完整
- ⚠️ API Key 无效
- ⚠️ 数据库未连接

**错误通知** (红色):
- ❌ 索引失败
- ❌ 网络错误
- ❌ 权限不足

### 通知位置

通知会显示在 IDE 右下角，自动消失或可手动关闭。

---

## 📝 完整使用流程

### 首次配置（5 分钟）

1. **获取 API Key**
   - 访问 https://ai.gitee.com/
   - 注册并购买资源包（¥10 起）
   - 获取 API Key

2. **启动 Elasticsearch**
   ```bash
   docker run -d --name elasticsearch \
     -p 9200:9200 \
     -e "discovery.type=single-node" \
     elasticsearch:8.15.0
   ```

3. **配置插件**
   - `Settings -> Tools -> 活文档`
   - AI 模型 Tab: 填入 Gitee AI Key
   - 向量数据库 Tab: 确认 ES 配置
   - 点击 `Apply`

4. **索引项目**
   - `Tools -> 活文档 -> 索引项目`
   - 等待完成（显示进度）

5. **开始使用**
   - 打开工具窗口
   - 搜索或提问

### 日常使用

**场景 1: 查找 API**
1. 打开工具窗口
2. 搜索："用户登录接口"
3. 查看结果，跳转到代码

**场景 2: 理解功能**
1. 切换到问答 Tab
2. 提问："订单创建接口是如何实现的？"
3. 阅读 AI 回答

**场景 3: 导出文档**
1. `Tools -> 活文档 -> 导出文档`
2. 选择目录
3. 生成 Markdown/HTML

---

## 💡 最佳实践

### 配置优化

1. **开发环境**
   ```
   AI: deepseek-chat (便宜)
   ES: 本地 Docker (1GB 内存)
   RAG: 分块 500, Top-K 3
   ```

2. **生产环境**
   ```
   AI: qwen-plus (平衡)
   ES: 集群部署 (2GB+ 内存)
   RAG: 分块 800, Top-K 5
   ```

3. **企业内网**
   ```
   AI: Ollama 本地模型
   ES: 内网部署
   安全: 启用认证
   ```

### 索引策略

- **增量索引**: 启用"代码保存时自动索引"
- **完整索引**: 大改动后手动触发
- **定期清理**: 清空旧索引，重新索引

### 搜索技巧

- 使用完整问句而非关键词
- 包含上下文信息
- 尝试多种表述方式
- 利用过滤条件

---

## 🐛 常见问题

### Q1: 设置保存后不生效？

**解决**: 
1. 点击 `Apply` 按钮
2. 重启 IDE
3. 检查日志

### Q2: 搜索结果为空？

**原因**: 未索引或索引失败

**解决**:
1. 查看统计 Tab - 确认文档数
2. 重新索引项目
3. 检查 ES 连接

### Q3: 无法连接 Elasticsearch？

**解决**:
```bash
# 检查 ES 是否运行
docker ps | grep elasticsearch

# 查看 ES 日志
docker logs elasticsearch

# 测试连接
curl http://localhost:9200
```

### Q4: AI 问答没有响应？

**检查**:
1. API Key 是否正确
2. 是否有余额
3. 网络是否正常
4. 查看错误通知

---

## 🎨 界面截图参考

### 设置面板
- AI 模型配置页
- 向量数据库配置页
- RAG 参数配置页
- 其他设置页

### 工具窗口
- 搜索界面
- 问答界面
- 统计界面

### 操作演示
- 索引项目流程
- 搜索文档流程
- AI 问答流程

---

## 📚 相关文档

- [Elasticsearch 8.15 升级指南](./Elasticsearch-8.15-升级指南.md)
- [快速开始指南](./LivingDoc活文档功能-快速开始指南.md)
- [完整设计方案](./PandaCoder活文档功能-RAG智能检索设计方案.md)

---

**享受智能文档检索的便利！有问题随时查看文档或提 Issue。** 🚀

