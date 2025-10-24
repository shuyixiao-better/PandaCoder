package com.shuyixiao.livingdoc.service;

import com.shuyixiao.livingdoc.ai.gitee.GiteeAiChatModel;
import com.shuyixiao.livingdoc.ai.gitee.GiteeAiEmbeddingModel;
import com.shuyixiao.livingdoc.config.LivingDocProperties;
import com.shuyixiao.livingdoc.vector.SearchResult;
import com.shuyixiao.livingdoc.vector.VectorDocument;
import com.shuyixiao.livingdoc.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) 文档检索服务
 * 
 * <p>核心功能：
 * <ul>
 *   <li>文档向量化和索引</li>
 *   <li>语义化搜索</li>
 *   <li>智能问答（RAG）</li>
 *   <li>文档分块</li>
 * </ul>
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@Service
public class LivingDocRagService {
    
    private static final Logger log = LoggerFactory.getLogger(LivingDocRagService.class);
    
    private final VectorStore vectorStore;
    private final GiteeAiEmbeddingModel embeddingModel;
    private final GiteeAiChatModel chatModel;
    private final LivingDocProperties properties;
    
    public LivingDocRagService(
            VectorStore vectorStore,
            GiteeAiEmbeddingModel embeddingModel,
            GiteeAiChatModel chatModel,
            LivingDocProperties properties) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.properties = properties;
    }
    
    /**
     * 索引文档
     * 
     * @param documents 文档列表
     */
    public void indexDocuments(List<DocumentChunk> documents) {
        log.info("Starting to index {} documents", documents.size());
        
        List<VectorDocument> vectorDocuments = new ArrayList<>();
        
        for (DocumentChunk doc : documents) {
            try {
                // 1. 生成向量
                float[] vector = embeddingModel.embed(doc.getContent());
                
                // 2. 创建向量文档
                VectorDocument vectorDoc = new VectorDocument(
                    doc.getId(),
                    vector,
                    doc.getContent(),
                    doc.getMetadata()
                );
                
                vectorDocuments.add(vectorDoc);
                
            } catch (Exception e) {
                log.error("Failed to index document: {}", doc.getId(), e);
            }
        }
        
        // 3. 批量存储
        if (!vectorDocuments.isEmpty()) {
            vectorStore.storeBatch(vectorDocuments);
            log.info("Successfully indexed {} documents", vectorDocuments.size());
        }
    }
    
    /**
     * 语义搜索
     * 
     * @param query 查询文本
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query) {
        log.debug("Searching for: {}", query);
        
        // 1. 将查询转换为向量
        float[] queryVector = embeddingModel.embed(query);
        
        // 2. 向量检索
        int topK = properties.getRag().getTopK();
        double threshold = properties.getRag().getSimilarityThreshold();
        
        List<SearchResult> results = vectorStore.searchWithThreshold(
            queryVector, 
            topK, 
            threshold
        );
        
        log.debug("Found {} results for query", results.size());
        return results;
    }
    
    /**
     * 语义搜索（带过滤条件）
     * 
     * @param query 查询文本
     * @param filter 元数据过滤条件
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query, Map<String, Object> filter) {
        float[] queryVector = embeddingModel.embed(query);
        
        List<SearchResult> results = vectorStore.search(
            queryVector,
            properties.getRag().getTopK(),
            filter
        );
        
        return results.stream()
            .filter(r -> r.getScore() >= properties.getRag().getSimilarityThreshold())
            .collect(Collectors.toList());
    }
    
    /**
     * RAG 智能问答
     * 
     * @param question 用户问题
     * @return AI答案
     */
    public String askQuestion(String question) {
        log.info("Answering question: {}", question);
        
        // 1. 检索相关文档
        List<SearchResult> relevantDocs = search(question);
        
        if (relevantDocs.isEmpty()) {
            return "抱歉，我没有找到相关的文档信息。请尝试换一种方式提问，或者确保文档已经被索引。";
        }
        
        // 2. 构建上下文
        String context = buildContext(relevantDocs);
        
        // 3. 构建提示词
        List<GiteeAiChatModel.ChatMessage> messages = buildPrompt(question, context, relevantDocs);
        
        // 4. 调用LLM生成答案
        String answer = chatModel.chat(messages);
        
        log.debug("Generated answer: {}", answer);
        return answer;
    }
    
    /**
     * RAG 流式问答
     * 
     * @param question 用户问题
     * @param streamHandler 流式响应处理器
     */
    public void askQuestionStream(String question, GiteeAiChatModel.StreamHandler streamHandler) {
        log.info("Answering question (stream): {}", question);
        
        // 1. 检索相关文档
        List<SearchResult> relevantDocs = search(question);
        
        if (relevantDocs.isEmpty()) {
            streamHandler.onChunk("抱歉，我没有找到相关的文档信息。");
            streamHandler.onComplete();
            return;
        }
        
        // 2. 构建上下文和提示词
        String context = buildContext(relevantDocs);
        List<GiteeAiChatModel.ChatMessage> messages = buildPrompt(question, context, relevantDocs);
        
        // 3. 流式调用
        chatModel.chatStream(messages, streamHandler);
    }
    
    /**
     * 文档分块
     * 
     * @param content 文档内容
     * @param chunkId 分块ID前缀
     * @return 分块列表
     */
    public List<DocumentChunk> chunkDocument(String content, String chunkId) {
        return chunkDocument(content, chunkId, null);
    }
    
    /**
     * 文档分块（带元数据）
     */
    public List<DocumentChunk> chunkDocument(String content, String chunkId, Map<String, Object> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        int chunkSize = properties.getRag().getChunkSize();
        int chunkOverlap = properties.getRag().getChunkOverlap();
        
        int start = 0;
        int chunkIndex = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            
            // 尝试在句子边界分割
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('。', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int boundary = Math.max(lastPeriod, lastNewline);
                
                if (boundary > start) {
                    end = boundary + 1;
                }
            }
            
            String chunkContent = content.substring(start, end).trim();
            
            if (!chunkContent.isEmpty()) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setId(chunkId + "_chunk_" + chunkIndex);
                chunk.setContent(chunkContent);
                chunk.setMetadata(metadata);
                chunks.add(chunk);
                chunkIndex++;
            }
            
            start = end - chunkOverlap;
            if (start < 0) start = end;
        }
        
        log.debug("Split document into {} chunks", chunks.size());
        return chunks;
    }
    
    /**
     * 构建上下文
     */
    private String buildContext(List<SearchResult> results) {
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append(String.format("[文档%d] (相似度: %.2f)\n", i + 1, result.getScore()));
            context.append(result.getContent());
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * 构建提示词
     */
    private List<GiteeAiChatModel.ChatMessage> buildPrompt(
            String question, 
            String context, 
            List<SearchResult> sources) {
        
        List<GiteeAiChatModel.ChatMessage> messages = new ArrayList<>();
        
        // System prompt
        String systemPrompt = """
            你是一个专业的项目文档助手，擅长根据项目文档回答开发者的问题。
            
            你的职责是：
            1. 仔细阅读提供的文档片段
            2. 基于文档内容准确回答问题
            3. 如果文档中没有相关信息，明确告知用户
            4. 用清晰、简洁的语言回答
            5. 在答案末尾提供文档来源引用
            
            注意：
            - 只使用提供的文档内容回答，不要编造信息
            - 保持专业、准确、有帮助的态度
            """;
        
        messages.add(GiteeAiChatModel.ChatMessage.system(systemPrompt));
        
        // User prompt with context
        String userPrompt = String.format("""
            基于以下文档内容回答问题：
            
            === 相关文档 ===
            %s
            
            === 用户问题 ===
            %s
            
            === 回答要求 ===
            1. 基于上述文档内容回答
            2. 如果文档中没有相关信息，请明确说明
            3. 在答案末尾列出引用的文档编号
            """, context, question);
        
        messages.add(GiteeAiChatModel.ChatMessage.user(userPrompt));
        
        return messages;
    }
    
    /**
     * 获取向量存储统计信息
     */
    public VectorStoreStats getStats() {
        VectorStoreStats stats = new VectorStoreStats();
        stats.setTotalDocuments(vectorStore.count());
        stats.setHealthy(vectorStore.healthCheck());
        stats.setEmbeddingModel(embeddingModel.getModel());
        stats.setEmbeddingDimensions(embeddingModel.getDimensions());
        return stats;
    }
    
    /**
     * 文档分块实体
     */
    public static class DocumentChunk {
        private String id;
        private String content;
        private Map<String, Object> metadata;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    /**
     * 向量存储统计信息
     */
    public static class VectorStoreStats {
        private long totalDocuments;
        private boolean healthy;
        private String embeddingModel;
        private int embeddingDimensions;
        
        public long getTotalDocuments() {
            return totalDocuments;
        }
        
        public void setTotalDocuments(long totalDocuments) {
            this.totalDocuments = totalDocuments;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public String getEmbeddingModel() {
            return embeddingModel;
        }
        
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }
        
        public int getEmbeddingDimensions() {
            return embeddingDimensions;
        }
        
        public void setEmbeddingDimensions(int embeddingDimensions) {
            this.embeddingDimensions = embeddingDimensions;
        }
    }
}

