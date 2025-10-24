package com.shuyixiao.livingdoc;

import com.shuyixiao.livingdoc.ai.gitee.GiteeAiChatModel;
import com.shuyixiao.livingdoc.ai.gitee.GiteeAiEmbeddingModel;
import com.shuyixiao.livingdoc.config.LivingDocProperties;
import com.shuyixiao.livingdoc.service.LivingDocRagService;
import com.shuyixiao.livingdoc.vector.SearchResult;
import com.shuyixiao.livingdoc.vector.VectorStore;
import com.shuyixiao.livingdoc.vector.impl.ElasticsearchVectorStore;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG服务测试
 * 
 * <p>需要环境变量：
 * <ul>
 *   <li>GITEE_AI_API_KEY - Gitee AI API密钥</li>
 *   <li>ES_HOST - Elasticsearch主机（可选，默认localhost）</li>
 * </ul>
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@EnabledIfEnvironmentVariable(named = "GITEE_AI_API_KEY", matches = ".+")
public class RagServiceTest {
    
    private LivingDocRagService ragService;
    private VectorStore vectorStore;
    
    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("GITEE_AI_API_KEY");
        String esHost = System.getenv().getOrDefault("ES_HOST", "localhost");
        
        // 初始化AI模型
        GiteeAiChatModel chatModel = new GiteeAiChatModel(
            apiKey,
            "https://ai.gitee.com/v1",
            "qwen-plus"
        );
        
        GiteeAiEmbeddingModel embeddingModel = new GiteeAiEmbeddingModel(
            apiKey,
            "https://ai.gitee.com/v1",
            "text-embedding-v3"
        );
        
        // 初始化向量存储
        RestHighLevelClient esClient = new RestHighLevelClient(
            RestClient.builder(new HttpHost(esHost, 9200, "http"))
        );
        
        LivingDocProperties.ElasticsearchConfig esConfig = new LivingDocProperties.ElasticsearchConfig();
        esConfig.setIndexName("test_livingdoc_vectors");
        esConfig.setDimensions(1024);
        
        vectorStore = new ElasticsearchVectorStore(esClient, esConfig);
        
        // 初始化配置
        LivingDocProperties properties = new LivingDocProperties();
        LivingDocProperties.RagConfig ragConfig = new LivingDocProperties.RagConfig();
        ragConfig.setChunkSize(500);
        ragConfig.setChunkOverlap(100);
        ragConfig.setTopK(3);
        ragConfig.setSimilarityThreshold(0.7);
        properties.setRag(ragConfig);
        
        // 创建RAG服务
        ragService = new LivingDocRagService(vectorStore, embeddingModel, chatModel, properties);
    }
    
    @Test
    void testIndexAndSearch() {
        System.out.println("\n=== 测试文档索引和搜索 ===");
        
        // 1. 准备测试文档
        List<LivingDocRagService.DocumentChunk> docs = Arrays.asList(
            createDocument("doc1", "POST /api/user/login - 用户登录接口，参数：username, password", "api"),
            createDocument("doc2", "GET /api/user/profile - 获取用户信息，参数：userId", "api"),
            createDocument("doc3", "POST /api/order/create - 创建订单接口，参数：productId, quantity", "api")
        );
        
        // 2. 索引文档
        ragService.indexDocuments(docs);
        System.out.println("已索引 " + docs.size() + " 个文档");
        
        // 等待ES刷新
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 3. 搜索
        List<SearchResult> results = ragService.search("如何登录系统？");
        
        assertFalse(results.isEmpty());
        System.out.println("\n搜索结果：");
        for (SearchResult result : results) {
            System.out.println(result);
        }
        
        // 验证：登录接口应该排在前面
        assertTrue(results.get(0).getContent().contains("登录"));
    }
    
    @Test
    void testRagQuestion() {
        System.out.println("\n=== 测试RAG问答 ===");
        
        // 索引一些文档
        List<LivingDocRagService.DocumentChunk> docs = Arrays.asList(
            createDocument("api1", 
                "POST /api/user/login\n" +
                "用户登录接口\n" +
                "请求参数：\n" +
                "- username (String): 用户名，必填\n" +
                "- password (String): 密码，必填\n" +
                "响应：返回用户token", 
                "api")
        );
        
        ragService.indexDocuments(docs);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 提问
        String answer = ragService.askQuestion("用户登录接口需要哪些参数？");
        
        assertNotNull(answer);
        assertFalse(answer.isEmpty());
        
        System.out.println("\n问题: 用户登录接口需要哪些参数？");
        System.out.println("回答: " + answer);
        
        // 验证答案中包含相关信息
        assertTrue(answer.contains("username") || answer.contains("用户名"));
    }
    
    @Test
    void testDocumentChunking() {
        System.out.println("\n=== 测试文档分块 ===");
        
        String longContent = """
            这是一个很长的API文档。
            
            # 用户管理接口
            
            ## 用户登录
            POST /api/user/login
            用于用户登录系统。
            
            参数：
            - username: 用户名
            - password: 密码
            
            ## 用户注册
            POST /api/user/register
            用于新用户注册。
            
            参数：
            - username: 用户名
            - password: 密码
            - email: 邮箱
            """;
        
        List<LivingDocRagService.DocumentChunk> chunks = ragService.chunkDocument(
            longContent, 
            "test_doc"
        );
        
        assertFalse(chunks.isEmpty());
        System.out.println("分块数量: " + chunks.size());
        
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("\n--- 分块 " + (i + 1) + " ---");
            System.out.println(chunks.get(i).getContent());
        }
    }
    
    @Test
    void testVectorStoreStats() {
        System.out.println("\n=== 测试向量存储统计 ===");
        
        LivingDocRagService.VectorStoreStats stats = ragService.getStats();
        
        assertNotNull(stats);
        System.out.println("总文档数: " + stats.getTotalDocuments());
        System.out.println("健康状态: " + stats.isHealthy());
        System.out.println("嵌入模型: " + stats.getEmbeddingModel());
        System.out.println("向量维度: " + stats.getEmbeddingDimensions());
        
        assertTrue(stats.isHealthy());
        assertEquals(1024, stats.getEmbeddingDimensions());
    }
    
    private LivingDocRagService.DocumentChunk createDocument(String id, String content, String type) {
        LivingDocRagService.DocumentChunk doc = new LivingDocRagService.DocumentChunk();
        doc.setId(id);
        doc.setContent(content);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", type);
        doc.setMetadata(metadata);
        
        return doc;
    }
}

