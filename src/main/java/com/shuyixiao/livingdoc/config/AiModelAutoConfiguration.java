package com.shuyixiao.livingdoc.config;

import com.shuyixiao.livingdoc.ai.gitee.GiteeAiChatModel;
import com.shuyixiao.livingdoc.ai.gitee.GiteeAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型自动配置
 * 
 * <p>根据配置自动装配不同的AI模型提供商
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@Configuration
public class AiModelAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(AiModelAutoConfiguration.class);
    
    /**
     * Gitee AI ChatModel 配置（默认）
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "gitee-ai", matchIfMissing = true)
    public GiteeAiChatModel giteeAiChatModel(LivingDocProperties properties) {
        log.info("Initializing Gitee AI Chat Model");
        
        LivingDocProperties.GiteeConfig config = properties.getAi().getGitee();
        
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IllegalStateException(
                "Gitee AI API Key is not configured. " +
                "Please set GITEE_AI_API_KEY environment variable or configure livingdoc.ai.gitee.api-key"
            );
        }
        
        return new GiteeAiChatModel(
            config.getApiKey(),
            config.getBaseUrl(),
            config.getModel()
        );
    }
    
    /**
     * Gitee AI EmbeddingModel 配置（默认）
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "gitee-ai", matchIfMissing = true)
    public GiteeAiEmbeddingModel giteeAiEmbeddingModel(LivingDocProperties properties) {
        log.info("Initializing Gitee AI Embedding Model");
        
        LivingDocProperties.GiteeConfig config = properties.getAi().getGitee();
        
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IllegalStateException(
                "Gitee AI API Key is not configured. " +
                "Please set GITEE_AI_API_KEY environment variable"
            );
        }
        
        return new GiteeAiEmbeddingModel(
            config.getApiKey(),
            config.getBaseUrl(),
            config.getEmbeddingModel()
        );
    }
    
    /**
     * OpenAI ChatModel 配置
     * 
     * TODO: 实现OpenAI支持
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "openai")
    public Object openAiChatModel(LivingDocProperties properties) {
        log.info("Initializing OpenAI Chat Model");
        throw new UnsupportedOperationException("OpenAI support is coming soon");
        // return new OpenAiChatModel(...);
    }
    
    /**
     * Ollama ChatModel 配置
     * 
     * TODO: 实现Ollama支持
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.ai", name = "provider", havingValue = "ollama")
    public Object ollamaChatModel(LivingDocProperties properties) {
        log.info("Initializing Ollama Chat Model");
        throw new UnsupportedOperationException("Ollama support is coming soon");
        // return new OllamaChatModel(...);
    }
}

