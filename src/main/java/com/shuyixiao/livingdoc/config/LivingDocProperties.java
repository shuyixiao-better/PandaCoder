package com.shuyixiao.livingdoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 活文档功能配置属性
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@Configuration
@ConfigurationProperties(prefix = "livingdoc")
public class LivingDocProperties {
    
    private AiConfig ai = new AiConfig();
    private VectorStoreConfig vectorStore = new VectorStoreConfig();
    private DocumentConfig document = new DocumentConfig();
    private RagConfig rag = new RagConfig();
    
    // Getters and Setters
    
    public AiConfig getAi() {
        return ai;
    }
    
    public void setAi(AiConfig ai) {
        this.ai = ai;
    }
    
    public VectorStoreConfig getVectorStore() {
        return vectorStore;
    }
    
    public void setVectorStore(VectorStoreConfig vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    public DocumentConfig getDocument() {
        return document;
    }
    
    public void setDocument(DocumentConfig document) {
        this.document = document;
    }
    
    public RagConfig getRag() {
        return rag;
    }
    
    public void setRag(RagConfig rag) {
        this.rag = rag;
    }
    
    /**
     * AI 模型配置
     */
    public static class AiConfig {
        /**
         * AI 提供商: gitee-ai | openai | ollama | tongyi
         */
        private String provider = "gitee-ai";
        
        private GiteeConfig gitee = new GiteeConfig();
        private OpenAiConfig openai = new OpenAiConfig();
        private OllamaConfig ollama = new OllamaConfig();
        
        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
        }
        
        public GiteeConfig getGitee() {
            return gitee;
        }
        
        public void setGitee(GiteeConfig gitee) {
            this.gitee = gitee;
        }
        
        public OpenAiConfig getOpenai() {
            return openai;
        }
        
        public void setOpenai(OpenAiConfig openai) {
            this.openai = openai;
        }
        
        public OllamaConfig getOllama() {
            return ollama;
        }
        
        public void setOllama(OllamaConfig ollama) {
            this.ollama = ollama;
        }
    }
    
    /**
     * Gitee AI (模力方舟) 配置
     */
    public static class GiteeConfig {
        private String apiKey;
        private String baseUrl = "https://ai.gitee.com/v1";
        private String model = "qwen-plus";
        private String embeddingModel = "text-embedding-v3";
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getEmbeddingModel() {
            return embeddingModel;
        }
        
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }
    }
    
    /**
     * OpenAI 配置
     */
    public static class OpenAiConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private String embeddingModel = "text-embedding-3-small";
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getEmbeddingModel() {
            return embeddingModel;
        }
        
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }
    }
    
    /**
     * Ollama 本地模型配置
     */
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5:7b";
        private String embeddingModel = "nomic-embed-text";
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getEmbeddingModel() {
            return embeddingModel;
        }
        
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }
    }
    
    /**
     * 向量数据库配置
     */
    public static class VectorStoreConfig {
        /**
         * 向量存储类型: elasticsearch | pgvector | chroma | redis | simple
         */
        private String type = "elasticsearch";
        
        private ElasticsearchConfig elasticsearch = new ElasticsearchConfig();
        private PgVectorConfig pgvector = new PgVectorConfig();
        private ChromaConfig chroma = new ChromaConfig();
        private SimpleVectorStoreConfig simple = new SimpleVectorStoreConfig();
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public ElasticsearchConfig getElasticsearch() {
            return elasticsearch;
        }
        
        public void setElasticsearch(ElasticsearchConfig elasticsearch) {
            this.elasticsearch = elasticsearch;
        }
        
        public PgVectorConfig getPgvector() {
            return pgvector;
        }
        
        public void setPgvector(PgVectorConfig pgvector) {
            this.pgvector = pgvector;
        }
        
        public ChromaConfig getChroma() {
            return chroma;
        }
        
        public void setChroma(ChromaConfig chroma) {
            this.chroma = chroma;
        }
        
        public SimpleVectorStoreConfig getSimple() {
            return simple;
        }
        
        public void setSimple(SimpleVectorStoreConfig simple) {
            this.simple = simple;
        }
    }
    
    /**
     * Elasticsearch 向量存储配置
     */
    public static class ElasticsearchConfig {
        private String host = "localhost";
        private int port = 9200;
        private String username;
        private String password;
        private String indexName = "livingdoc_vectors";
        private int dimensions = 1024;  // text-embedding-v3 默认维度
        private String similarityAlgorithm = "cosine";  // cosine | dot_product | l2_norm
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getIndexName() {
            return indexName;
        }
        
        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }
        
        public int getDimensions() {
            return dimensions;
        }
        
        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
        
        public String getSimilarityAlgorithm() {
            return similarityAlgorithm;
        }
        
        public void setSimilarityAlgorithm(String similarityAlgorithm) {
            this.similarityAlgorithm = similarityAlgorithm;
        }
    }
    
    /**
     * PGVector 配置
     */
    public static class PgVectorConfig {
        private String url = "jdbc:postgresql://localhost:5432/livingdoc";
        private String username = "postgres";
        private String password;
        private String tableName = "vector_store";
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }
    
    /**
     * Chroma 配置
     */
    public static class ChromaConfig {
        private String baseUrl = "http://localhost:8000";
        private String collectionName = "livingdoc";
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getCollectionName() {
            return collectionName;
        }
        
        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
        }
    }
    
    /**
     * Simple 向量存储配置
     */
    public static class SimpleVectorStoreConfig {
        private String persistPath = ".livingdoc/vectors";
        
        public String getPersistPath() {
            return persistPath;
        }
        
        public void setPersistPath(String persistPath) {
            this.persistPath = persistPath;
        }
    }
    
    /**
     * 文档生成配置
     */
    public static class DocumentConfig {
        private String outputDir = "docs/api";
        private String[] formats = {"markdown", "html", "openapi"};
        private String templateDir = "templates/custom";
        
        public String getOutputDir() {
            return outputDir;
        }
        
        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }
        
        public String[] getFormats() {
            return formats;
        }
        
        public void setFormats(String[] formats) {
            this.formats = formats;
        }
        
        public String getTemplateDir() {
            return templateDir;
        }
        
        public void setTemplateDir(String templateDir) {
            this.templateDir = templateDir;
        }
    }
    
    /**
     * RAG 检索配置
     */
    public static class RagConfig {
        private int chunkSize = 800;
        private int chunkOverlap = 200;
        private int topK = 5;
        private double similarityThreshold = 0.7;
        
        public int getChunkSize() {
            return chunkSize;
        }
        
        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }
        
        public int getChunkOverlap() {
            return chunkOverlap;
        }
        
        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }
        
        public int getTopK() {
            return topK;
        }
        
        public void setTopK(int topK) {
            this.topK = topK;
        }
        
        public double getSimilarityThreshold() {
            return similarityThreshold;
        }
        
        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }
    }
}

