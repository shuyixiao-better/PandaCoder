# PandaCoder 活文档功能 - 快速开始指南

## 🚀 快速开始

### 1. 环境准备

#### 必需环境
- **Java 17+**
- **Elasticsearch 7.x+**（默认向量数据库）
- **Gitee AI API Key**

#### 可选环境
- PostgreSQL + PGVector扩展（如果使用PGVector）
- Chroma向量数据库（如果使用Chroma）
- Ollama（如果使用本地模型）

### 2. 获取 Gitee AI API Key

1. 访问 [Gitee AI](https://ai.gitee.com/)
2. 注册/登录账号
3. 进入 **工作台 -> 设置 -> 访问令牌**
4. 创建新令牌
5. 购买全模型资源包（建议先购买小额测试，如¥10）

### 3. 安装 Elasticsearch

#### 使用 Docker（推荐）
```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:7.17.9
```

#### 验证安装
```bash
curl http://localhost:9200
```

应该返回Elasticsearch版本信息。

### 4. 配置环境变量

#### Windows
```powershell
setx GITEE_AI_API_KEY "your_api_key_here"
```

#### Linux/Mac
```bash
export GITEE_AI_API_KEY="your_api_key_here"
```

### 5. 配置文件

编辑 `src/main/resources/livingdoc-config.yml`：

```yaml
livingdoc:
  ai:
    provider: gitee-ai
    gitee:
      api-key: ${GITEE_AI_API_KEY}
      model: qwen-plus
      embedding-model: text-embedding-v3
  
  vector-store:
    type: elasticsearch
    elasticsearch:
      host: localhost
      port: 9200
      index-name: livingdoc_vectors
      dimensions: 1024
```

### 6. 运行测试

#### 测试 Gitee AI 连接
```bash
./gradlew test --tests GiteeAiIntegrationTest
```

#### 测试 RAG 功能
```bash
./gradlew test --tests RagServiceTest
```

### 7. 使用示例

#### 7.1 索引项目文档

```java
// 1. 分析项目代码
SpringBootAnalyzer analyzer = new SpringBootAnalyzer();
ProjectDocumentation doc = analyzer.analyzeProject(project);

// 2. 转换为文档块
List<LivingDocRagService.DocumentChunk> chunks = new ArrayList<>();
for (ApiEndpoint api : doc.getApis()) {
    String content = formatApiAsDocument(api);
    LivingDocRagService.DocumentChunk chunk = new LivingDocRagService.DocumentChunk();
    chunk.setId(api.getId());
    chunk.setContent(content);
    chunks.add(chunk);
}

// 3. 索引到向量数据库
LivingDocRagService ragService = ...; // 从Spring容器获取
ragService.indexDocuments(chunks);
```

#### 7.2 语义搜索

```java
// 搜索相关文档
List<SearchResult> results = ragService.search("用户登录接口怎么用？");

for (SearchResult result : results) {
    System.out.println("相似度: " + result.getScore());
    System.out.println("内容: " + result.getContent());
}
```

#### 7.3 智能问答

```java
// RAG 问答
String answer = ragService.askQuestion("用户登录接口需要哪些参数？");
System.out.println("AI回答: " + answer);

// 流式问答
ragService.askQuestionStream("如何实现用户注册功能？", new StreamHandler() {
    @Override
    public void onChunk(String chunk) {
        System.out.print(chunk);
    }
    
    @Override
    public void onComplete() {
        System.out.println("\n完成");
    }
    
    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
});
```

### 8. 集成到 IntelliJ 插件

#### 8.1 添加 Action

```java
public class IndexProjectAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "索引项目文档") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // 获取服务
                LivingDocRagService ragService = project.getService(LivingDocRagService.class);
                
                // 分析项目
                SpringBootAnalyzer analyzer = new SpringBootAnalyzer();
                ProjectDocumentation doc = analyzer.analyzeProject(project);
                
                // 索引
                List<DocumentChunk> chunks = convertToChunks(doc);
                ragService.indexDocuments(chunks);
                
                // 通知
                Notifications.Bus.notify(
                    new Notification("LivingDoc", "索引完成", 
                        "已索引 " + chunks.size() + " 个文档",
                        NotificationType.INFORMATION)
                );
            }
        });
    }
}
```

#### 8.2 注册 Action

在 `plugin.xml` 中：

```xml
<actions>
    <action id="LivingDoc.IndexProject" 
            class="com.shuyixiao.livingdoc.action.IndexProjectAction"
            text="索引项目文档"
            description="分析项目代码并索引到向量数据库">
        <add-to-group group-id="ToolsMenu" anchor="first"/>
    </action>
</actions>
```

### 9. 常见问题

#### Q1: Elasticsearch 连接失败
**A:** 检查Elasticsearch是否正常运行：
```bash
curl http://localhost:9200
```

如果无法连接，尝试重启Elasticsearch。

#### Q2: API Key 无效
**A:** 
1. 检查环境变量是否正确设置
2. 确认已购买资源包
3. 访问 Gitee AI 控制台查看令牌状态

#### Q3: 向量维度不匹配
**A:** 确保配置文件中的维度与嵌入模型匹配：
- `text-embedding-v3`: 1024维
- `bge-large-zh-v1.5`: 1024维
- `m3e-base`: 768维

#### Q4: 内存不足
**A:** 增加 Elasticsearch 内存：
```bash
docker run ... -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" ...
```

### 10. 性能优化建议

#### 10.1 批量索引
```java
// 不要一个一个索引，使用批量操作
ragService.indexDocuments(allChunks);  // ✅ 好
// 不要：for (chunk : chunks) store(chunk);  // ❌ 慢
```

#### 10.2 调整分块大小
```yaml
livingdoc:
  rag:
    chunk-size: 800  # 根据文档长度调整
    chunk-overlap: 200  # 保证上下文连贯
```

#### 10.3 设置合适的 topK
```yaml
livingdoc:
  rag:
    top-k: 5  # 返回前5个最相似结果
    similarity-threshold: 0.7  # 只返回相似度>0.7的结果
```

### 11. 下一步

- 阅读 [完整设计方案](./PandaCoder活文档功能-RAG智能检索设计方案.md)
- 查看 [API文档](./LivingDoc-API文档.md)
- 加入社区讨论

### 12. 技术支持

- **Gitee AI文档**: https://ai.gitee.com/docs/products/apis
- **Elasticsearch文档**: https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html
- **项目Issues**: https://github.com/your-repo/issues

---

**祝使用愉快！如有问题欢迎提Issue！** 🎉

