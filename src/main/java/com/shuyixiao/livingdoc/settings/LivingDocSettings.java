package com.shuyixiao.livingdoc.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 活文档插件配置持久化
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@State(
    name = "com.shuyixiao.livingdoc.settings.LivingDocSettings",
    storages = @Storage("LivingDocSettings.xml")
)
public class LivingDocSettings implements PersistentStateComponent<LivingDocSettings> {
    
    // ==================== AI 模型配置 ====================
    public String aiProvider = "gitee-ai";  // gitee-ai | openai | ollama
    
    // Gitee AI
    public String giteeApiKey = "";
    public String giteeBaseUrl = "https://ai.gitee.com/v1";
    public String giteeModel = "qwen-plus";
    public String giteeEmbeddingModel = "text-embedding-v3";
    
    // OpenAI
    public String openaiApiKey = "";
    public String openaiBaseUrl = "https://api.openai.com/v1";
    public String openaiModel = "gpt-4o-mini";
    public String openaiEmbeddingModel = "text-embedding-3-small";
    
    // Ollama
    public String ollamaBaseUrl = "http://localhost:11434";
    public String ollamaModel = "qwen2.5:7b";
    public String ollamaEmbeddingModel = "nomic-embed-text";
    
    // ==================== 向量数据库配置 ====================
    public String vectorStoreType = "elasticsearch";  // elasticsearch | pgvector | chroma | simple
    
    // Elasticsearch 8.15
    public String esHost = "localhost";
    public int esPort = 9200;
    public String esUsername = "";
    public String esPassword = "";
    public String esIndexName = "livingdoc_vectors";
    public int esDimensions = 1024;
    public String esSimilarityAlgorithm = "cosine";  // cosine | dot_product | l2_norm
    
    // PGVector
    public String pgUrl = "jdbc:postgresql://localhost:5432/livingdoc";
    public String pgUsername = "postgres";
    public String pgPassword = "";
    public String pgTableName = "vector_store";
    
    // Chroma
    public String chromaBaseUrl = "http://localhost:8000";
    public String chromaCollectionName = "livingdoc";
    
    // Simple
    public String simplePersistPath = ".livingdoc/vectors";
    
    // ==================== 文档生成配置 ====================
    public String documentOutputDir = "docs/api";
    public String documentFormats = "markdown,html,openapi";  // 逗号分隔
    public String documentTemplateDir = "templates/custom";
    
    // ==================== RAG 检索配置 ====================
    public int ragChunkSize = 800;
    public int ragChunkOverlap = 200;
    public int ragTopK = 5;
    public double ragSimilarityThreshold = 0.7;
    
    // ==================== 其他配置 ====================
    public boolean autoIndexOnSave = true;  // 保存时自动索引
    public boolean showNotifications = true;  // 显示通知
    public boolean enableLogging = true;  // 启用日志
    
    /**
     * 获取项目级别的设置实例
     */
    public static LivingDocSettings getInstance(@NotNull Project project) {
        return project.getService(LivingDocSettings.class);
    }
    
    @Nullable
    @Override
    public LivingDocSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull LivingDocSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        // 检查 AI 提供商配置
        if ("gitee-ai".equals(aiProvider)) {
            if (giteeApiKey == null || giteeApiKey.trim().isEmpty()) {
                return false;
            }
        } else if ("openai".equals(aiProvider)) {
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                return false;
            }
        }
        
        // 检查向量数据库配置
        if ("elasticsearch".equals(vectorStoreType)) {
            if (esHost == null || esHost.trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取当前使用的 API Key
     */
    public String getCurrentApiKey() {
        return switch (aiProvider) {
            case "gitee-ai" -> giteeApiKey;
            case "openai" -> openaiApiKey;
            default -> "";
        };
    }
}

