package com.shuyixiao.livingdoc.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.shuyixiao.livingdoc.vector.VectorStore;
import com.shuyixiao.livingdoc.vector.impl.ElasticsearchVectorStore_8_15;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量数据库自动配置
 * 
 * <p>根据配置自动装配不同的向量存储实现
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
@Configuration
public class VectorStoreAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(VectorStoreAutoConfiguration.class);
    
    /**
     * Elasticsearch 8.15 向量存储配置（默认）
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.vector-store", name = "type", havingValue = "elasticsearch", matchIfMissing = true)
    public VectorStore elasticsearchVectorStore(LivingDocProperties properties) {
        log.info("Initializing Elasticsearch 8.15 Vector Store");
        
        LivingDocProperties.ElasticsearchConfig config = properties.getVectorStore().getElasticsearch();
        
        // 创建 RestClient
        RestClient restClient = createRestClient(config);
        
        // 创建 ElasticsearchClient (ES 8.x 新版客户端)
        RestClientTransport transport = new RestClientTransport(
            restClient,
            new JacksonJsonpMapper()
        );
        ElasticsearchClient client = new ElasticsearchClient(transport);
        
        return new ElasticsearchVectorStore_8_15(client, restClient, config);
    }
    
    /**
     * 创建 Elasticsearch RestClient (ES 8.15)
     */
    private RestClient createRestClient(LivingDocProperties.ElasticsearchConfig config) {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost(config.getHost(), config.getPort(), "http")
        );
        
        // 如果配置了用户名密码，添加认证
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(config.getUsername(), config.getPassword())
            );
            
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }
        
        // 设置超时
        builder.setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout(5000)
                .setSocketTimeout(60000)
        );
        
        return builder.build();
    }
    
    /**
     * PGVector 向量存储配置
     * 
     * TODO: 实现PGVector支持
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.vector-store", name = "type", havingValue = "pgvector")
    public VectorStore pgVectorStore(LivingDocProperties properties) {
        log.info("Initializing PGVector Vector Store");
        throw new UnsupportedOperationException("PGVector support is coming soon");
        // return new PgVectorStore(...);
    }
    
    /**
     * Chroma 向量存储配置
     * 
     * TODO: 实现Chroma支持
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.vector-store", name = "type", havingValue = "chroma")
    public VectorStore chromaVectorStore(LivingDocProperties properties) {
        log.info("Initializing Chroma Vector Store");
        throw new UnsupportedOperationException("Chroma support is coming soon");
        // return new ChromaVectorStore(...);
    }
    
    /**
     * Simple 向量存储配置（内存/文件）
     * 
     * TODO: 实现Simple Vector Store
     */
    @Bean
    @ConditionalOnProperty(prefix = "livingdoc.vector-store", name = "type", havingValue = "simple")
    public VectorStore simpleVectorStore(LivingDocProperties properties) {
        log.info("Initializing Simple Vector Store");
        throw new UnsupportedOperationException("Simple Vector Store support is coming soon");
        // return new SimpleVectorStore(...);
    }
}

