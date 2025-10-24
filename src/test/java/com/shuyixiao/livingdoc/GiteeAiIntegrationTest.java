package com.shuyixiao.livingdoc;

import com.shuyixiao.livingdoc.ai.gitee.GiteeAiChatModel;
import com.shuyixiao.livingdoc.ai.gitee.GiteeAiEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gitee AI 集成测试
 * 
 * <p>需要设置环境变量 GITEE_AI_API_KEY才能运行
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@EnabledIfEnvironmentVariable(named = "GITEE_AI_API_KEY", matches = ".+")
public class GiteeAiIntegrationTest {
    
    private String apiKey;
    private GiteeAiChatModel chatModel;
    private GiteeAiEmbeddingModel embeddingModel;
    
    @BeforeEach
    void setUp() {
        apiKey = System.getenv("GITEE_AI_API_KEY");
        chatModel = new GiteeAiChatModel(
            apiKey,
            "https://ai.gitee.com/v1",
            "qwen-plus"
        );
        
        embeddingModel = new GiteeAiEmbeddingModel(
            apiKey,
            "https://ai.gitee.com/v1",
            "text-embedding-v3"
        );
    }
    
    @Test
    void testChatCompletion() {
        System.out.println("\n=== 测试Chat Completion ===");
        
        List<GiteeAiChatModel.ChatMessage> messages = new ArrayList<>();
        messages.add(GiteeAiChatModel.ChatMessage.user("用一句话解释什么是RAG技术"));
        
        String response = chatModel.chat(messages);
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        System.out.println("问题: 用一句话解释什么是RAG技术");
        System.out.println("回答: " + response);
    }
    
    @Test
    void testChatStream() {
        System.out.println("\n=== 测试流式Chat ===");
        
        List<GiteeAiChatModel.ChatMessage> messages = new ArrayList<>();
        messages.add(GiteeAiChatModel.ChatMessage.user("介绍一下Spring Boot"));
        
        StringBuilder fullResponse = new StringBuilder();
        
        chatModel.chatStream(messages, new GiteeAiChatModel.StreamHandler() {
            @Override
            public void onChunk(String chunk) {
                System.out.print(chunk);
                fullResponse.append(chunk);
            }
            
            @Override
            public void onComplete() {
                System.out.println("\n--- 流式输出完成 ---");
            }
            
            @Override
            public void onError(Exception e) {
                System.err.println("错误: " + e.getMessage());
            }
        });
        
        assertTrue(fullResponse.length() > 0);
    }
    
    @Test
    void testEmbedding() {
        System.out.println("\n=== 测试Embedding ===");
        
        String text = "用户登录接口";
        float[] vector = embeddingModel.embed(text);
        
        assertNotNull(vector);
        assertEquals(1024, vector.length);  // text-embedding-v3 是1024维
        
        System.out.println("文本: " + text);
        System.out.println("向量维度: " + vector.length);
        System.out.println("向量前5个值: " + Arrays.toString(Arrays.copyOf(vector, 5)));
    }
    
    @Test
    void testBatchEmbedding() {
        System.out.println("\n=== 测试批量Embedding ===");
        
        List<String> texts = Arrays.asList(
            "用户登录接口",
            "用户注册API",
            "订单查询接口",
            "商品详情页面"
        );
        
        List<float[]> vectors = embeddingModel.embedBatch(texts);
        
        assertNotNull(vectors);
        assertEquals(texts.size(), vectors.size());
        
        for (int i = 0; i < texts.size(); i++) {
            System.out.println(texts.get(i) + " -> 向量维度: " + vectors.get(i).length);
        }
    }
    
    @Test
    void testCosineSimilarity() {
        System.out.println("\n=== 测试余弦相似度 ===");
        
        List<String> texts = Arrays.asList(
            "用户登录接口",
            "用户注册接口",
            "订单查询接口"
        );
        
        List<float[]> vectors = embeddingModel.embedBatch(texts);
        
        // 计算相似度
        double sim01 = embeddingModel.cosineSimilarity(vectors.get(0), vectors.get(1));
        double sim02 = embeddingModel.cosineSimilarity(vectors.get(0), vectors.get(2));
        
        System.out.println("\"用户登录接口\" vs \"用户注册接口\" 相似度: " + String.format("%.4f", sim01));
        System.out.println("\"用户登录接口\" vs \"订单查询接口\" 相似度: " + String.format("%.4f", sim02));
        
        // 登录和注册应该比登录和订单更相似
        assertTrue(sim01 > sim02);
    }
}

