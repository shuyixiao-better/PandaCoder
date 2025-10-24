# PandaCoder 活文档功能 - RAG智能检索设计方案（Spring AI版）

## 📋 项目概述

### 功能定位
PandaCoder活文档功能是一个基于 **Spring AI** 框架构建的智能文档管理系统，结合RAG（Retrieval-Augmented Generation）技术，能够自动从代码中提取、组织、检索和生成项目接口文档。

### 核心价值
- **自动化文档生成**：一键导出项目接口文档，告别手工维护
- **实时同步**：代码修改后快速重新生成，保持文档最新
- **智能检索**：基于RAG技术，语义化搜索文档内容
- **知识沉淀**：将项目知识转化为可检索、可问答的知识库
- **灵活配置**：支持云模型和本地模型切换，满足不同场景需求

### 技术特色
- ✅ 基于 **Spring AI** 统一API，简化AI集成开发
- ✅ 默认集成 **Gitee AI（模力方舟）**，支持国内主流大模型
- ✅ 支持本地模型（Ollama），保护企业数据隐私
- ✅ 内置向量数据库支持，开箱即用
- ✅ 完整的 RAG 工具链，快速实现智能问答

---

## 🎯 技术架构

### 1. 整体架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    PandaCoder 插件层                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 代码解析模块  │  │ 文档生成模块  │  │ UI交互模块   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│  ┌─────────────────────────▼─────────────────────────┐      │
│  │              Spring AI 核心层                      │      │
│  ├────────────────────────────────────────────────────┤      │
│  │  ChatClient  │  EmbeddingClient  │  VectorStore   │      │
│  │  Advisors    │  ChatMemory       │  DocumentReader│      │
│  └─────────────────────────────────────────────────────┘      │
│                            │                                 │
│  ┌────────────┬────────────┴────────────┬────────────┐      │
│  │            │                         │            │      │
│  ▼            ▼                         ▼            ▼      │
│ ┌──────┐  ┌─────────┐             ┌────────┐  ┌────────┐  │
│ │Gitee │  │ OpenAI  │   模型层    │ Ollama │  │ 通义   │  │
│ │ AI   │  │         │             │(本地)  │  │千问    │  │
│ └──────┘  └─────────┘             └────────┘  └────────┘  │
│     │                                   │                   │
│     │   ┌───────────────────────────────┘                   │
│     │   │                                                   │
│  ┌──▼───▼──────────────────────────────────────────┐       │
│  │              向量存储层                          │       │
│  ├──────────────────────────────────────────────────┤       │
│  │ PGVector │ Chroma │ Redis │ Simple (内存/文件) │       │
│  └──────────────────────────────────────────────────┘       │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│                      本地存储层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 文档缓存     │  │ 向量数据     │  │ 配置文件     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2. 技术栈选型

#### 核心框架
- **Spring AI 1.0.3**：AI应用开发框架，提供统一的AI模型访问API
- **Spring Boot 3.x**：应用基础框架
- **IntelliJ Platform SDK**：插件开发基础

#### AI模型提供商（可配置切换）

| 提供商 | 类型 | 优势 | 适用场景 |
|--------|------|------|----------|
| **Gitee AI** | 云端 | 国内主流模型、价格优惠、响应快 | 生产环境、需要高性能 |
| **OpenAI** | 云端 | 效果最好、功能最全 | 追求最佳效果 |
| **Ollama** | 本地 | 完全离线、数据隐私、免费 | 企业内网、敏感数据 |
| **通义千问** | 云端 | 阿里云生态、中文优化 | 阿里云用户 |

#### 向量数据库（Spring AI统一支持）

| 数据库 | 推荐场景 | 部署方式 |
|--------|----------|----------|
| **SimpleVectorStore** | 开发测试、小型项目 | 内存/文件 |
| **PGVector** | 生产环境、已有PostgreSQL | PostgreSQL扩展 |
| **Chroma** | 专业RAG应用 | Docker/本地 |
| **Redis** | 高性能要求、已有Redis | Redis Stack |

---

## 🛠️ 核心模块设计

### 模块1：配置管理模块 ⭐新增

**职责**：统一管理AI模型和向量数据库配置

#### 配置文件结构
```yaml
# livingdoc-config.yml
livingdoc:
  # AI模型配置
  ai:
    provider: gitee-ai  # gitee-ai | openai | ollama | tongyi
    
    # Gitee AI 配置（默认）
    gitee:
      api-key: ${GITEE_AI_API_KEY}
      base-url: https://ai.gitee.com/v1
      model: qwen-plus  # 默认使用通义千问Plus
      embedding-model: text-embedding-v3  # 向量化模型
      
    # OpenAI 配置
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
      embedding-model: text-embedding-3-small
      
    # Ollama 本地配置
    ollama:
      base-url: http://localhost:11434
      model: qwen2.5:7b  # 本地部署的模型
      embedding-model: nomic-embed-text
      
  # 向量数据库配置
  vector-store:
    type: simple  # simple | pgvector | chroma | redis
    
    simple:
      persist-path: .livingdoc/vectors
      
    pgvector:
      url: jdbc:postgresql://localhost:5432/livingdoc
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      
    chroma:
      base-url: http://localhost:8000
      
  # 文档生成配置
  document:
    output-dir: docs/api
    formats: [markdown, html, openapi]
    template-dir: templates/custom
    
  # RAG检索配置
  rag:
    chunk-size: 800
    chunk-overlap: 200
    top-k: 5
    similarity-threshold: 0.7
```

#### 配置类实现
```java
package com.shuyixiao.livingdoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "livingdoc")
public class LivingDocProperties {
    
    private AiConfig ai;
    private VectorStoreConfig vectorStore;
    private DocumentConfig document;
    private RagConfig rag;
    
    // Getters and Setters
    
    public static class AiConfig {
        private String provider = "gitee-ai";
        private GiteeConfig gitee;
        private OpenAiConfig openai;
        private OllamaConfig ollama;
    }
    
    public static class GiteeConfig {
        private String apiKey;
        private String baseUrl = "https://ai.gitee.com/v1";
        private String model = "qwen-plus";
        private String embeddingModel = "text-embedding-v3";
    }
    
    public static class VectorStoreConfig {
        private String type = "simple";
        private SimpleVectorStoreConfig simple;
        private PgVectorConfig pgvector;
    }
    
    public static class RagConfig {
        private int chunkSize = 800;
        private int chunkOverlap = 200;
        private int topK = 5;
        private double similarityThreshold = 0.7;
    }
}
```

### 模块2：Spring AI 集成层 ⭐核心

**职责**：封装Spring AI，提供统一的AI能力访问

#### 2.1 Gitee AI 适配器实现

由于Spring AI原生不支持Gitee AI，我们需要实现自定义适配器：

```java
package com.shuyixiao.livingdoc.ai.gitee;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gitee AI (模力方舟) ChatModel 适配器
 * 
 * API文档: https://ai.gitee.com/docs/products/apis
 */
public class GiteeAiChatModel implements ChatModel {
    
    private final WebClient webClient;
    private final String model;
    
    public GiteeAiChatModel(String apiKey, String baseUrl, String model) {
        this.model = model;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Override
    public ChatResponse call(Prompt prompt) {
        // 1. 构建请求体（兼容OpenAI格式）
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", convertMessages(prompt.getInstructions()),
            "temperature", 0.7,
            "max_tokens", 2000
        );
        
        // 2. 调用Gitee AI API
        GiteeAiResponse response = webClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(GiteeAiResponse.class)
            .block();
        
        // 3. 转换为Spring AI的ChatResponse
        return convertToChatResponse(response);
    }
    
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        // 实现流式响应
        // Gitee AI支持SSE (Server-Sent Events)
        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(buildStreamRequest(prompt))
            .retrieve()
            .bodyToFlux(String.class)
            .map(this::parseSSE)
            .map(this::convertToChatResponse);
    }
    
    private List<Map<String, String>> convertMessages(List<Message> messages) {
        return messages.stream()
            .map(msg -> Map.of(
                "role", msg.getRole().toString().toLowerCase(),
                "content", msg.getContent()
            ))
            .collect(Collectors.toList());
    }
}
```

#### 2.2 Gitee AI 向量化模型适配器

```java
package com.shuyixiao.livingdoc.ai.gitee;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Gitee AI Embedding Model 适配器
 * 
 * 支持的模型：
 * - text-embedding-v3（推荐）
 * - bge-large-zh-v1.5
 */
public class GiteeAiEmbeddingModel implements EmbeddingModel {
    
    private final WebClient webClient;
    private final String model;
    
    public GiteeAiEmbeddingModel(String apiKey, String baseUrl, String model) {
        this.model = model;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }
    
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        // 1. 调用Gitee AI Embedding API
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "input", request.getInstructions()
        );
        
        GiteeAiEmbeddingResponse response = webClient.post()
            .uri("/embeddings")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(GiteeAiEmbeddingResponse.class)
            .block();
        
        // 2. 转换为Spring AI格式
        return convertToEmbeddingResponse(response);
    }
    
    @Override
    public int dimensions() {
        // text-embedding-v3: 1024维
        // bge-large-zh-v1.5: 1024维
        return 1024;
    }
}
```

#### 2.3 配置自动装配

```java
package com.shuyixiao.livingdoc.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiAutoConfiguration {
    
    /**
     * Gitee AI ChatModel 配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "gitee-ai")
    public ChatModel giteeAiChatModel(LivingDocProperties properties) {
        GiteeConfig config = properties.getAi().getGitee();
        return new GiteeAiChatModel(
            config.getApiKey(),
            config.getBaseUrl(),
            config.getModel()
        );
    }
    
    /**
     * Gitee AI EmbeddingModel 配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "gitee-ai")
    public EmbeddingModel giteeAiEmbeddingModel(LivingDocProperties properties) {
        GiteeConfig config = properties.getAi().getGitee();
        return new GiteeAiEmbeddingModel(
            config.getApiKey(),
            config.getBaseUrl(),
            config.getEmbeddingModel()
        );
    }
    
    /**
     * Ollama ChatModel 配置（本地模型）
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "ollama")
    public ChatModel ollamaChatModel(LivingDocProperties properties) {
        // Spring AI 原生支持 Ollama
        OllamaConfig config = properties.getAi().getOllama();
        return new OllamaChatModel(
            OllamaOptions.builder()
                .baseUrl(config.getBaseUrl())
                .model(config.getModel())
                .build()
        );
    }
    
    /**
     * ChatClient Builder
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
    
    /**
     * VectorStore 配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.vector-store", name = "type", havingValue = "simple")
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel, LivingDocProperties properties) {
        SimpleVectorStore store = new SimpleVectorStore(embeddingModel);
        
        // 如果配置了持久化路径，加载已有数据
        String persistPath = properties.getVectorStore().getSimple().getPersistPath();
        File vectorFile = new File(persistPath + "/vectors.json");
        if (vectorFile.exists()) {
            store.load(vectorFile);
        }
        
        return store;
    }
    
    /**
     * PGVector 配置（生产推荐）
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.vector-store", name = "type", havingValue = "pgvector")
    public VectorStore pgVectorStore(EmbeddingModel embeddingModel, DataSource dataSource) {
        return new PgVectorStore(dataSource, embeddingModel);
    }
}
```

### 模块3：RAG 文档检索服务 ⭐核心

**职责**：基于Spring AI实现完整的RAG流程

```java
package com.shuyixiao.livingdoc.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class LivingDocRagService {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final LivingDocProperties properties;
    
    public LivingDocRagService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            LivingDocProperties properties) {
        
        // 构建带有RAG能力的ChatClient
        this.chatClient = chatClientBuilder
            .defaultAdvisors(
                // Spring AI 内置的RAG Advisor
                new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults())
            )
            .build();
        
        this.vectorStore = vectorStore;
        this.properties = properties;
    }
    
    /**
     * 索引文档到向量数据库
     */
    public void indexDocuments(List<ApiEndpoint> apis) {
        // 1. 构建文档列表
        List<Document> documents = apis.stream()
            .map(this::apiToDocument)
            .collect(Collectors.toList());
        
        // 2. 文档分块（使用Spring AI的TokenTextSplitter）
        TokenTextSplitter splitter = new TokenTextSplitter(
            properties.getRag().getChunkSize(),
            properties.getRag().getChunkOverlap(),
            5,  // 最小块大小
            10000,  // 最大块大小
            true  // 保持分隔符
        );
        
        List<Document> chunks = splitter.apply(documents);
        
        // 3. 向量化并存储（Spring AI自动处理）
        vectorStore.add(chunks);
        
        // 4. 持久化（如果使用SimpleVectorStore）
        if (vectorStore instanceof SimpleVectorStore) {
            File persistFile = new File(
                properties.getVectorStore().getSimple().getPersistPath() + "/vectors.json"
            );
            ((SimpleVectorStore) vectorStore).save(persistFile);
        }
    }
    
    /**
     * 智能搜索文档
     */
    public SearchResponse search(String query) {
        // 使用Spring AI的VectorStore进行相似度搜索
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(properties.getRag().getTopK())
                .withSimilarityThreshold(properties.getRag().getSimilarityThreshold())
        );
        
        return SearchResponse.builder()
            .query(query)
            .results(convertToSearchResults(results))
            .build();
    }
    
    /**
     * RAG智能问答
     */
    public String askQuestion(String question) {
        // Spring AI的QuestionAnswerAdvisor会自动：
        // 1. 向量检索相关文档
        // 2. 构建包含上下文的Prompt
        // 3. 调用LLM生成答案
        // 4. 返回带引用的答案
        
        String answer = chatClient.prompt()
            .user(question)
            .call()
            .content();
        
        return answer;
    }
    
    /**
     * 流式问答（实时显示生成过程）
     */
    public Flux<String> askQuestionStream(String question) {
        return chatClient.prompt()
            .user(question)
            .stream()
            .content();
    }
    
    /**
     * 将API端点转换为Document
     */
    private Document apiToDocument(ApiEndpoint api) {
        // 构建结构化文本
        String content = buildApiDocumentText(api);
        
        // 创建Document并添加元数据
        Map<String, Object> metadata = Map.of(
            "api_path", api.getPath(),
            "http_method", api.getHttpMethod(),
            "controller", api.getController(),
            "file_path", api.getFilePath(),
            "line_number", api.getLineNumber()
        );
        
        return new Document(content, metadata);
    }
    
    private String buildApiDocumentText(ApiEndpoint api) {
        StringBuilder sb = new StringBuilder();
        sb.append("# API接口文档\n\n");
        sb.append("## 基本信息\n");
        sb.append("- 路径: ").append(api.getPath()).append("\n");
        sb.append("- HTTP方法: ").append(api.getHttpMethod()).append("\n");
        sb.append("- 描述: ").append(api.getDescription()).append("\n\n");
        
        sb.append("## 请求参数\n");
        for (Parameter param : api.getParameters()) {
            sb.append("- **").append(param.getName()).append("**");
            sb.append(" (").append(param.getType()).append(")");
            if (param.isRequired()) sb.append(" [必填]");
            sb.append(": ").append(param.getDescription()).append("\n");
        }
        
        sb.append("\n## 响应格式\n");
        sb.append("```json\n");
        sb.append(api.getResponseExample()).append("\n");
        sb.append("```\n");
        
        return sb.toString();
    }
}
```

### 模块4：代码分析模块（保持不变）

```java
package com.shuyixiao.livingdoc.analyzer;

public class SpringBootAnalyzer implements CodeAnalyzer {
    
    @Override
    public ProjectDocumentation analyzeProject(Project project) {
        ProjectDocumentation doc = new ProjectDocumentation();
        
        // 1. 查找所有Controller
        Collection<PsiClass> controllers = findControllers(project);
        
        // 2. 遍历分析
        for (PsiClass controller : controllers) {
            List<ApiEndpoint> apis = extractApis(controller);
            doc.addApis(apis);
        }
        
        return doc;
    }
    
    private Collection<PsiClass> findControllers(Project project) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        
        // 查找 @RestController 和 @Controller
        return Stream.of(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller"
        )
        .flatMap(annotation -> 
            AnnotatedElementsSearch.searchPsiClasses(
                PsiJavaFacade.getInstance(project).findClass(annotation, scope),
                scope
            ).stream()
        )
        .collect(Collectors.toList());
    }
    
    private List<ApiEndpoint> extractApis(PsiClass controller) {
        List<ApiEndpoint> apis = new ArrayList<>();
        
        // 获取类级别的@RequestMapping
        String basePath = extractBasePath(controller);
        
        // 遍历方法
        for (PsiMethod method : controller.getMethods()) {
            if (isApiMethod(method)) {
                ApiEndpoint api = extractApiFromMethod(method, basePath);
                apis.add(api);
            }
        }
        
        return apis;
    }
}
```

### 模块5：UI交互界面

```java
package com.shuyixiao.livingdoc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import java.awt.*;

/**
 * 活文档工具窗口
 */
public class LivingDocToolWindow {
    
    private final Project project;
    private final LivingDocRagService ragService;
    
    private JPanel mainPanel;
    private JBTextField searchField;
    private JTextArea resultsArea;
    private JTextArea chatArea;
    
    public LivingDocToolWindow(Project project, LivingDocRagService ragService) {
        this.project = project;
        this.ragService = ragService;
        initUI();
    }
    
    private void initUI() {
        mainPanel = new JPanel(new BorderLayout());
        
        // 搜索区域
        JPanel searchPanel = createSearchPanel();
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        
        // 结果展示区域（Tab切换）
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📄 搜索结果", createSearchResultsPanel());
        tabbedPane.addTab("💬 智能问答", createChatPanel());
        tabbedPane.addTab("⚙️ 设置", createSettingsPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 操作按钮区域
        JPanel actionPanel = createActionPanel();
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel label = new JLabel("🔍 智能搜索:");
        searchField = new JBTextField();
        searchField.setToolTipText("输入问题，例如：用户登录接口的参数有哪些？");
        
        JButton searchButton = new JButton("搜索");
        searchButton.addActionListener(e -> performSearch());
        
        // 回车搜索
        searchField.addActionListener(e -> performSearch());
        
        panel.add(label, BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);
        panel.add(searchButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JBScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 输入框
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JBTextField chatInput = new JBTextField();
        JButton askButton = new JButton("提问");
        
        askButton.addActionListener(e -> {
            String question = chatInput.getText().trim();
            if (!question.isEmpty()) {
                askQuestion(question);
                chatInput.setText("");
            }
        });
        
        inputPanel.add(new JLabel("提问:"), BorderLayout.WEST);
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(askButton, BorderLayout.EAST);
        
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;
        
        // 异步搜索
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            SearchResponse response = ragService.search(query);
            
            // UI线程更新结果
            ApplicationManager.getApplication().invokeLater(() -> {
                displaySearchResults(response);
            });
        });
    }
    
    private void askQuestion(String question) {
        chatArea.append("\n🧑 您: " + question + "\n\n");
        chatArea.append("🤖 AI助手: ");
        
        // 流式显示答案
        ragService.askQuestionStream(question)
            .subscribe(
                chunk -> ApplicationManager.getApplication().invokeLater(() -> {
                    chatArea.append(chunk);
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                }),
                error -> chatArea.append("\n❌ 错误: " + error.getMessage() + "\n"),
                () -> chatArea.append("\n\n")
            );
    }
}
```

---

## 🚀 实施路线图

### Phase 1: Spring AI 基础集成（1周）⭐ 推荐起点

**目标**：搭建Spring AI环境，实现基本的AI调用

**任务清单**：
- [ ] 创建Spring Boot子项目作为后端服务
- [ ] 集成Spring AI依赖
- [ ] 实现Gitee AI适配器
- [ ] 配置文件管理
- [ ] 测试ChatModel和EmbeddingModel

**技能提升**：
- Spring AI框架原理
- RestTemplate/WebClient使用
- AI模型API调用
- Spring Boot Auto Configuration

**快速开始**：

1. **添加依赖**（build.gradle）:
```gradle
plugins {
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

dependencies {
    // Spring AI
    implementation 'org.springframework.ai:spring-ai-core:1.0.0-M3'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // JSON处理
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    
    // 配置处理
    implementation 'org.springframework.boot:spring-boot-configuration-processor'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

2. **配置Gitee AI**（application.yml）:
```yaml
livingdoc:
  ai:
    provider: gitee-ai
    gitee:
      api-key: ${GITEE_AI_API_KEY}  # 从环境变量读取
      model: qwen-plus
      embedding-model: text-embedding-v3
```

3. **测试AI调用**:
```java
@SpringBootTest
class GiteeAiTest {
    
    @Autowired
    private ChatClient.Builder chatClientBuilder;
    
    @Test
    void testChatCompletion() {
        ChatClient chatClient = chatClientBuilder.build();
        
        String response = chatClient.prompt()
            .user("解释一下什么是RESTful API")
            .call()
            .content();
        
        System.out.println(response);
        assertNotNull(response);
    }
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Test
    void testEmbedding() {
        EmbeddingResponse response = embeddingModel.embedForResponse(
            List.of("用户登录接口", "用户注册API")
        );
        
        List<Embedding> embeddings = response.getResults();
        assertEquals(2, embeddings.size());
        assertEquals(1024, embeddings.get(0).getOutput().length);
    }
}
```

### Phase 2: RAG检索实现（1-2周）⭐⭐ 核心功能

**目标**：实现完整的RAG文档检索

**任务清单**：
- [ ] 实现代码分析和文档提取
- [ ] 集成VectorStore（先用SimpleVectorStore）
- [ ] 实现文档索引流程
- [ ] 实现相似度搜索
- [ ] 集成QuestionAnswerAdvisor实现RAG问答

**技能提升**：
- 🎯 **RAG完整流程**：文档加载、分块、向量化、检索、生成
- 🎯 **Spring AI VectorStore**：统一的向量数据库抽象
- 🎯 **Advisor模式**：Spring AI的中间件机制
- 🎯 **Prompt工程**：如何构建有效的提示词

**详细实践**：

#### 2.1 实现文档索引（2-3天）
```java
@Service
public class DocumentIndexService {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private LivingDocProperties properties;
    
    public void indexProject(Project project) {
        // 1. 分析项目代码
        CodeAnalyzer analyzer = new SpringBootAnalyzer();
        ProjectDocumentation doc = analyzer.analyzeProject(project);
        
        // 2. 转换为Document列表
        List<Document> documents = convertToDocuments(doc.getApis());
        
        // 3. 文档分块
        TokenTextSplitter splitter = new TokenTextSplitter(
            properties.getRag().getChunkSize(),
            properties.getRag().getChunkOverlap()
        );
        List<Document> chunks = splitter.apply(documents);
        
        // 4. 向量化并存储
        vectorStore.add(chunks);
        
        // 5. 持久化
        persistVectorStore();
    }
    
    private List<Document> convertToDocuments(List<ApiEndpoint> apis) {
        return apis.stream()
            .map(api -> {
                String content = formatApiAsMarkdown(api);
                Map<String, Object> metadata = Map.of(
                    "type", "api",
                    "path", api.getPath(),
                    "method", api.getHttpMethod(),
                    "controller", api.getController()
                );
                return new Document(content, metadata);
            })
            .collect(Collectors.toList());
    }
}
```

#### 2.2 实现智能检索（2-3天）
```java
@Service
public class IntelligentSearchService {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private ChatClient chatClient;
    
    /**
     * 向量搜索
     */
    public List<SearchResult> vectorSearch(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
        
        return results.stream()
            .map(doc -> SearchResult.builder()
                .content(doc.getContent())
                .metadata(doc.getMetadata())
                .score(doc.getMetadata().getOrDefault("score", 0.0))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * RAG问答
     */
    public String ragAnswer(String question) {
        // Spring AI会自动执行RAG流程
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

#### 2.3 集成到IntelliJ插件（2-3天）
```java
// 在IntelliJ插件中启动Spring Boot应用
public class LivingDocService {
    
    private static SpringApplication springApp;
    private static ConfigurableApplicationContext context;
    
    public static void initialize(Project project) {
        if (context == null) {
            springApp = new SpringApplication(LivingDocApplication.class);
            springApp.setWebApplicationType(WebApplicationType.NONE);
            context = springApp.run();
        }
    }
    
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}

// Action调用
public class IndexDocumentsAction extends AnAction {
    
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        // 初始化Spring容器
        LivingDocService.initialize(project);
        
        // 获取索引服务
        DocumentIndexService indexService = 
            LivingDocService.getBean(DocumentIndexService.class);
        
        // 执行索引
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "索引文档") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indexService.indexProject(project);
                
                Notifications.Bus.notify(
                    new Notification("LivingDoc", "索引完成", 
                        "文档已成功索引到向量数据库", NotificationType.INFORMATION)
                );
            }
        });
    }
}
```

### Phase 3: Gitee AI 生产优化（3-5天）

**目标**：优化Gitee AI集成，支持高级特性

**任务清单**：
- [ ] 实现请求重试机制
- [ ] 添加速率限制
- [ ] 支持故障转移（X-Failover-Enabled）
- [ ] 实现Token计费统计
- [ ] 错误处理和日志

**Gitee AI高级特性**：

```java
@Service
public class EnhancedGiteeAiService {
    
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    
    public EnhancedGiteeAiService(String apiKey) {
        this.webClient = WebClient.builder()
            .baseUrl("https://ai.gitee.com/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            // 启用故障转移
            .defaultHeader("X-Failover-Enabled", "true")
            .filter(ExchangeFilterFunction.ofRequestProcessor(
                clientRequest -> {
                    // 记录请求日志
                    log.info("Calling Gitee AI: {}", clientRequest.url());
                    return Mono.just(clientRequest);
                }
            ))
            .build();
        
        // 速率限制：每秒最多10个请求
        this.rateLimiter = RateLimiter.create(10.0);
    }
    
    public Mono<ChatResponse> chatWithRetry(ChatRequest request) {
        rateLimiter.acquire();  // 速率限制
        
        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::is5xxServerError, response -> {
                // 服务器错误，重试
                return Mono.error(new RetryableException("Server error"));
            })
            .bodyToMono(ChatResponse.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof RetryableException))
            .doOnSuccess(response -> {
                // 统计Token使用量
                recordTokenUsage(response.getUsage());
            });
    }
    
    private void recordTokenUsage(TokenUsage usage) {
        log.info("Token usage - Prompt: {}, Completion: {}, Total: {}",
            usage.getPromptTokens(),
            usage.getCompletionTokens(),
            usage.getTotalTokens());
        
        // 存储到数据库或监控系统
        metricsService.recordTokens(usage);
    }
}
```

### Phase 4: 文档生成与导出（3-5天）

**任务清单**：
- [ ] Markdown文档生成
- [ ] HTML文档生成（带搜索功能）
- [ ] OpenAPI/Swagger规范导出
- [ ] 自定义模板支持

### Phase 5: UI完善与体验优化（1周）

**任务清单**：
- [ ] 完善Tool Window界面
- [ ] 实现流式答案显示
- [ ] 添加历史记录
- [ ] 配置面板（切换模型、调整参数）
- [ ] 快捷键支持

---

## 📚 RAG技术学习指南（Spring AI版）

### 核心概念

#### 1. Document（文档）
Spring AI的基础数据单元：
```java
// 创建文档
Document doc = new Document(
    "这是文档内容",
    Map.of("source", "api", "path", "/user/login")
);

// 文档包含：
// - content: 文本内容
// - metadata: 元数据（用于过滤和显示）
// - embedding: 向量表示（自动生成）
```

#### 2. VectorStore（向量存储）
统一的向量数据库接口：
```java
// 添加文档
vectorStore.add(List.of(doc1, doc2, doc3));

// 相似度搜索
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query("用户登录")
        .withTopK(5)
        .withSimilarityThreshold(0.7)
        .withFilterExpression("method == 'POST'")  // 元数据过滤
);
```

#### 3. Advisor（顾问）
Spring AI的中间件模式，用于扩展AI功能：

```java
// QuestionAnswerAdvisor: 自动实现RAG
ChatClient client = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()),
        new MessageChatMemoryAdvisor(chatMemory),  // 对话记忆
        new SafeGuardAdvisor()  // 安全防护
    )
    .build();

// 调用时自动执行RAG流程
String answer = client.prompt()
    .user("用户登录接口怎么用？")
    .call()
    .content();
```

#### 4. DocumentReader（文档读取器）
用于加载各种格式的文档：

```java
// 读取文本文件
TextReader textReader = new TextReader("api-docs.md");
List<Document> docs = textReader.get();

// 读取PDF
PagePdfDocumentReader pdfReader = new PagePdfDocumentReader("manual.pdf");
List<Document> pdfDocs = pdfReader.get();

// 读取JSON
JsonReader jsonReader = new JsonReader("swagger.json");
List<Document> jsonDocs = jsonReader.get();
```

### 学习路径

#### Week 1: Spring AI基础
**目标**：理解Spring AI核心概念

**学习资源**：
- [Spring AI官方文档](https://spring.io/projects/spring-ai)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
- [Spring AI Examples](https://github.com/spring-projects/spring-ai-examples)

**实践项目**：
```java
// 创建一个简单的问答系统
@RestController
public class SimpleQAController {
    
    @Autowired
    private ChatClient.Builder chatClientBuilder;
    
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatClientBuilder.build()
            .prompt()
            .user(question)
            .call()
            .content();
    }
}
```

#### Week 2: RAG实践
**目标**：实现完整的RAG系统

**实践步骤**：
1. 加载文档到VectorStore
2. 实现相似度搜索
3. 集成QuestionAnswerAdvisor
4. 测试问答效果
5. 优化检索参数

**评估指标**：
```java
@Service
public class RagEvaluator {
    
    /**
     * 评估检索质量
     */
    public double evaluateRetrieval(String query, String expectedDoc) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(10)
        );
        
        // 计算MRR (Mean Reciprocal Rank)
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getId().equals(expectedDoc)) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }
}
```

#### Week 3-4: 高级特性
- 混合检索（向量+关键词）
- 多模态RAG（文本+图片）
- 对话记忆管理
- Prompt优化

---

## 🔧 Gitee AI 使用指南

### 1. 注册和获取API Key

1. 访问 [Gitee AI](https://ai.gitee.com/)
2. 注册/登录账号
3. 进入**工作台 -> 设置 -> 访问令牌**
4. 创建新令牌，选择权限
5. 购买全模型资源包

### 2. 支持的模型

#### 文本生成模型（Chat）

| 模型ID | 提供商 | 特点 | 价格 |
|--------|--------|------|------|
| `qwen-plus` | 阿里通义 | 中文优秀、速度快 | ¥0.004/1K tokens |
| `qwen-max` | 阿里通义 | 最强性能 | ¥0.04/1K tokens |
| `deepseek-chat` | DeepSeek | 编程能力强 | ¥0.001/1K tokens |
| `glm-4` | 智谱AI | 多模态 | ¥0.01/1K tokens |
| `doubao-pro` | 字节豆包 | 性价比高 | ¥0.0008/1K tokens |

#### 向量化模型（Embedding）

| 模型ID | 维度 | 特点 |
|--------|------|------|
| `text-embedding-v3` | 1024 | 通用、高质量 |
| `bge-large-zh-v1.5` | 1024 | 中文优化 |
| `m3e-base` | 768 | 轻量、快速 |

### 3. API调用示例

#### 使用cURL测试
```bash
# Chat Completion
curl https://ai.gitee.com/v1/chat/completions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen-plus",
    "messages": [
      {"role": "user", "content": "解释一下RESTful API"}
    ]
  }'

# Embedding
curl https://ai.gitee.com/v1/embeddings \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-v3",
    "input": "用户登录接口"
  }'
```

#### 在Java中使用
```java
// 已在前面的GiteeAiChatModel和GiteeAiEmbeddingModel中实现
```

### 4. 计费说明

- **按Token计费**：输入Token和输出Token分别计费
- **全模型资源包**：一次购买，所有模型通用
- **无最低消费**：按实际使用量扣费
- **余额查询**：通过API或控制台查看

---

## 💡 最佳实践

### 1. 模型选择建议

#### 开发阶段
- **Chat模型**：`deepseek-chat`（便宜、够用）
- **Embedding模型**：`m3e-base`（快速、轻量）
- **VectorStore**：`SimpleVectorStore`（无需部署）

#### 生产环境
- **Chat模型**：`qwen-plus`（平衡性能和成本）
- **Embedding模型**：`text-embedding-v3`（高质量）
- **VectorStore**：`PGVector`（稳定、易维护）

#### 企业内网
- **所有服务**：Ollama本地部署
- **推荐模型**：`qwen2.5:7b` + `nomic-embed-text`

### 2. 性能优化

```java
@Configuration
public class PerformanceConfig {
    
    /**
     * 向量缓存
     */
    @Bean
    public CacheManager embeddingCacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("embeddings",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class, float[].class,
                    ResourcePoolsBuilder.heap(1000)
                )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(24)))
            )
            .build(true);
    }
    
    /**
     * 异步处理
     */
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("livingdoc-");
        executor.initialize();
        return executor;
    }
}
```

### 3. 错误处理

```java
@ControllerAdvice
public class LivingDocExceptionHandler {
    
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        log.error("API调用失败", e);
        
        ErrorResponse error = ErrorResponse.builder()
            .code(e.getCode())
            .message(getUserFriendlyMessage(e))
            .suggestion(getSuggestion(e))
            .build();
        
        return ResponseEntity.status(e.getStatus()).body(error);
    }
    
    private String getUserFriendlyMessage(ApiException e) {
        return switch (e.getCode()) {
            case "insufficient_quota" -> "API额度不足，请充值";
            case "rate_limit_exceeded" -> "请求过于频繁，请稍后重试";
            case "invalid_api_key" -> "API密钥无效，请检查配置";
            default -> "服务暂时不可用，请稍后重试";
        };
    }
}
```

### 4. 安全建议

```yaml
# 不要硬编码API Key
livingdoc:
  ai:
    gitee:
      api-key: ${GITEE_AI_API_KEY}  # 从环境变量读取
      
# 或使用加密配置
jasypt:
  encryptor:
    password: ${ENCRYPTION_PASSWORD}
    
livingdoc:
  ai:
    gitee:
      api-key: ENC(加密后的密钥)
```

---

## 📊 项目结构（最终）

```
PandaCoder/
├── src/main/java/com/shuyixiao/
│   ├── livingdoc/                      # 活文档模块
│   │   ├── LivingDocApplication.java   # Spring Boot启动类
│   │   │
│   │   ├── config/                     # 配置
│   │   │   ├── LivingDocProperties.java
│   │   │   ├── SpringAiAutoConfiguration.java
│   │   │   └── PerformanceConfig.java
│   │   │
│   │   ├── ai/                         # AI集成
│   │   │   ├── gitee/
│   │   │   │   ├── GiteeAiChatModel.java
│   │   │   │   ├── GiteeAiEmbeddingModel.java
│   │   │   │   └── GiteeAiProperties.java
│   │   │   ├── ollama/
│   │   │   │   └── OllamaConfiguration.java
│   │   │   └── ModelFactory.java
│   │   │
│   │   ├── analyzer/                   # 代码分析
│   │   │   ├── CodeAnalyzer.java
│   │   │   ├── SpringBootAnalyzer.java
│   │   │   └── models/
│   │   │       ├── ApiEndpoint.java
│   │   │       └── ProjectDocumentation.java
│   │   │
│   │   ├── rag/                        # RAG服务
│   │   │   ├── LivingDocRagService.java
│   │   │   ├── DocumentIndexService.java
│   │   │   ├── IntelligentSearchService.java
│   │   │   └── ChatService.java
│   │   │
│   │   ├── generator/                  # 文档生成
│   │   │   ├── MarkdownGenerator.java
│   │   │   ├── HtmlGenerator.java
│   │   │   └── OpenApiGenerator.java
│   │   │
│   │   ├── ui/                         # UI界面
│   │   │   ├── LivingDocToolWindow.java
│   │   │   ├── SearchDialog.java
│   │   │   └── SettingsPanel.java
│   │   │
│   │   └── service/                    # 业务服务
│   │       ├── LivingDocService.java
│   │       └── ProjectChangeListener.java
│   │
│   └── [其他现有模块...]
│
├── src/main/resources/
│   ├── application.yml                 # Spring配置
│   ├── livingdoc-config.yml           # 活文档配置
│   ├── META-INF/
│   │   └── plugin.xml                 # 插件配置
│   └── templates/                     # 文档模板
│       ├── api-doc.md.ftl
│       └── api-doc.html.ftl
│
├── src/test/java/
│   └── com/shuyixiao/livingdoc/
│       ├── GiteeAiIntegrationTest.java
│       ├── RagServiceTest.java
│       └── AnalyzerTest.java
│
├── docs/
│   └── PandaCoder活文档功能-RAG智能检索设计方案.md
│
└── build.gradle                       # 添加Spring AI依赖
```

---

## 🎯 里程碑

### Milestone 1: 可用版本（2周）
- ✅ Spring AI集成
- ✅ Gitee AI适配器
- ✅ 基础文档分析
- ✅ 向量检索
- ✅ 简单UI

### Milestone 2: 完整版本（4周）
- ✅ 完整RAG流程
- ✅ 智能问答
- ✅ 多格式导出
- ✅ 增量更新
- ✅ 完善UI

### Milestone 3: 企业版本（6-8周）
- ✅ 多语言支持
- ✅ 本地模型支持
- ✅ 高级检索
- ✅ 团队协作
- ✅ 性能优化

---

## 🎓 学习成果

完成本项目后，您将掌握：

### Spring AI生态
✅ Spring AI框架原理和最佳实践  
✅ ChatClient、EmbeddingClient、VectorStore等核心API  
✅ Advisor模式和中间件开发  
✅ 自定义Model适配器开发  
✅ Spring Boot Auto Configuration  

### RAG技术
✅ RAG完整流程：检索-增强-生成  
✅ 文档分块策略和向量化  
✅ 向量数据库使用和优化  
✅ 混合检索和重排序  
✅ Prompt工程和上下文管理  

### AI模型集成
✅ Gitee AI（模力方舟）完整对接  
✅ OpenAI兼容接口实现  
✅ Ollama本地模型部署  
✅ 模型切换和配置管理  
✅ Token计费和成本控制  

### 工程能力
✅ IntelliJ插件与Spring Boot集成  
✅ 异步编程和性能优化  
✅ 错误处理和用户体验  
✅ 企业级应用架构设计  

---

## 🚀 快速开始

### 第一步：环境准备

1. **安装开发工具**
```bash
# Java 17+
java -version

# Gradle
gradle -version

# IntelliJ IDEA Ultimate
```

2. **获取Gitee AI API Key**
- 访问 https://ai.gitee.com/
- 注册账号并登录
- 工作台 -> 设置 -> 访问令牌
- 创建令牌并购买资源包（建议先购买小额测试）

3. **配置环境变量**
```bash
# Windows
setx GITEE_AI_API_KEY "your_api_key_here"

# Linux/Mac
export GITEE_AI_API_KEY="your_api_key_here"
```

### 第二步：创建测试项目

```bash
# 克隆PandaCoder项目
git clone https://github.com/yourusername/PandaCoder.git
cd PandaCoder

# 创建livingdoc模块
mkdir -p src/main/java/com/shuyixiao/livingdoc
mkdir -p src/main/resources
mkdir -p src/test/java/com/shuyixiao/livingdoc
```

### 第三步：添加依赖

编辑 `build.gradle`:
```gradle
dependencies {
    // Spring AI
    implementation 'org.springframework.ai:spring-ai-core:1.0.0-M3'
    implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
    implementation 'org.springframework.boot:spring-boot-starter-webflux:3.2.0'
    
    // JSON
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    
    // 配置
    implementation 'org.springframework.boot:spring-boot-configuration-processor'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // 测试
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 第四步：运行第一个示例

创建 `QuickStartTest.java`:
```java
package com.shuyixiao.livingdoc;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class QuickStartTest {
    
    @Test
    public void testGiteeAi() {
        // 1. 创建Gitee AI ChatModel
        String apiKey = System.getenv("GITEE_AI_API_KEY");
        ChatModel chatModel = new GiteeAiChatModel(
            apiKey,
            "https://ai.gitee.com/v1",
            "qwen-plus"
        );
        
        // 2. 创建ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        // 3. 调用AI
        String response = chatClient.prompt()
            .user("用一句话解释什么是RAG技术")
            .call()
            .content();
        
        System.out.println("AI回答: " + response);
    }
}
```

运行测试：
```bash
gradle test --tests QuickStartTest
```

如果看到AI的回答，恭喜！环境搭建成功！

### 第五步：开始开发

按照实施路线图，从Phase 1开始逐步实现功能。

---

## 📞 技术支持

### Gitee AI相关
- 官方文档：https://ai.gitee.com/docs/products/apis
- API文档：https://ai.gitee.com/docs/products/apis
- 示例代码：https://gitee.com/moark/examples

### Spring AI相关
- 官方网站：https://spring.io/projects/spring-ai
- GitHub：https://github.com/spring-projects/spring-ai
- 示例项目：https://github.com/spring-projects/spring-ai-examples

### 社区讨论
- Spring AI Discord
- Gitee AI 论坛
- PandaCoder Issues

---

## 🎉 总结

本方案基于 **Spring AI** 框架，默认集成 **Gitee AI（模力方舟）**，提供了一个完整的、可配置的、企业级的活文档RAG系统设计。

### 核心优势

1. **技术先进**：基于Spring AI，跟随Spring生态最新技术
2. **灵活配置**：支持云端和本地模型切换
3. **国内优化**：默认集成Gitee AI，响应快、价格优
4. **易于扩展**：统一的API，轻松接入新模型
5. **学习友好**：完整的实施路线和学习资源

### 下一步行动

1. ✅ 获取Gitee AI API Key
2. ✅ 运行QuickStart示例
3. ✅ 按Phase 1开始实施
4. ✅ 加入社区交流
5. ✅ 持续学习和优化

**祝您开发顺利，期待PandaCoder活文档功能早日上线！** 🚀

---

*文档版本：v2.0（Spring AI版）*  
*最后更新：2025-10-24*  
*作者：PandaCoder Team*  
*技术栈：Spring AI 1.0.3 + Gitee AI + IntelliJ Platform*
