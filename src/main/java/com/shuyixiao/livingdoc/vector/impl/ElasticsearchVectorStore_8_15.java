package com.shuyixiao.livingdoc.vector.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import com.shuyixiao.livingdoc.config.LivingDocProperties;
import com.shuyixiao.livingdoc.vector.SearchResult;
import com.shuyixiao.livingdoc.vector.VectorDocument;
import com.shuyixiao.livingdoc.vector.VectorStore;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 8.15 向量存储实现
 * 
 * <p>使用 Elasticsearch 8.15 新版 Java Client 和 kNN 搜索功能
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ElasticsearchVectorStore_8_15 implements VectorStore {
    
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchVectorStore_8_15.class);
    
    private final ElasticsearchClient client;
    private final RestClient restClient;
    private final String indexName;
    private final int dimensions;
    private final String similarityAlgorithm;
    
    public ElasticsearchVectorStore_8_15(
            ElasticsearchClient client,
            RestClient restClient,
            LivingDocProperties.ElasticsearchConfig config) {
        this.client = client;
        this.restClient = restClient;
        this.indexName = config.getIndexName();
        this.dimensions = config.getDimensions();
        this.similarityAlgorithm = config.getSimilarityAlgorithm();
        
        initialize();
    }
    
    @Override
    public void initialize() {
        try {
            // 检查索引是否存在
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
            
            if (!exists) {
                createIndex();
                log.info("Created Elasticsearch 8.15 index: {}", indexName);
            } else {
                log.info("Elasticsearch index already exists: {}", indexName);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index", e);
            throw new RuntimeException("Failed to initialize Elasticsearch vector store", e);
        }
    }
    
    /**
     * 创建索引（ES 8.15 使用新的索引映射格式）
     */
    private void createIndex() throws IOException {
        // ES 8.15 的索引映射
        String mappingJson = String.format("""
            {
              "properties": {
                "id": {
                  "type": "keyword"
                },
                "content": {
                  "type": "text",
                  "analyzer": "standard"
                },
                "vector": {
                  "type": "dense_vector",
                  "dims": %d,
                  "index": true,
                  "similarity": "%s"
                },
                "metadata": {
                  "type": "object",
                  "enabled": true
                },
                "createdAt": {
                  "type": "date"
                },
                "updatedAt": {
                  "type": "date"
                }
              }
            }
            """, dimensions, similarityAlgorithm);
        
        String settingsJson = """
            {
              "number_of_shards": 3,
              "number_of_replicas": 1,
              "max_result_window": 10000
            }
            """;
        
        client.indices().create(CreateIndexRequest.of(c -> c
            .index(indexName)
            .mappings(m -> m.withJson(new StringReader(mappingJson)))
            .settings(s -> s.withJson(new StringReader(settingsJson)))
        ));
    }
    
    @Override
    public void store(String id, float[] vector, String content, Map<String, Object> metadata) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put("id", id);
            document.put("content", content);
            document.put("vector", vector);
            document.put("metadata", metadata != null ? metadata : new HashMap<>());
            document.put("createdAt", LocalDateTime.now().toString());
            document.put("updatedAt", LocalDateTime.now().toString());
            
            client.index(IndexRequest.of(i -> i
                .index(indexName)
                .id(id)
                .document(document)
            ));
            
            log.debug("Stored document: id={}", id);
        } catch (IOException e) {
            log.error("Failed to store document: id={}", id, e);
            throw new RuntimeException("Failed to store document", e);
        }
    }
    
    @Override
    public void storeBatch(List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        
        try {
            List<BulkOperation> operations = new ArrayList<>();
            
            for (VectorDocument doc : documents) {
                Map<String, Object> source = new HashMap<>();
                source.put("id", doc.getId());
                source.put("content", doc.getContent());
                source.put("vector", doc.getVector());
                source.put("metadata", doc.getMetadata());
                source.put("createdAt", doc.getCreatedAt().toString());
                source.put("updatedAt", doc.getUpdatedAt().toString());
                
                BulkOperation operation = BulkOperation.of(b -> b
                    .index(idx -> idx
                        .index(indexName)
                        .id(doc.getId())
                        .document(source)
                    )
                );
                operations.add(operation);
            }
            
            BulkResponse response = client.bulk(BulkRequest.of(b -> b
                .index(indexName)
                .operations(operations)
            ));
            
            if (response.errors()) {
                log.warn("Bulk store has failures");
            } else {
                log.info("Successfully stored {} documents in batch", documents.size());
            }
        } catch (IOException e) {
            log.error("Failed to store documents in batch", e);
            throw new RuntimeException("Failed to store documents in batch", e);
        }
    }
    
    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, null);
    }
    
    @Override
    public List<SearchResult> search(float[] queryVector, int topK, Map<String, Object> filter) {
        try {
            // ES 8.15 使用 kNN 搜索
            SearchResponse<Map> response = client.search(s -> {
                s.index(indexName)
                 .size(topK)
                 .knn(KnnQuery.of(k -> {
                     k.field("vector")
                      .queryVector(floatArrayToList(queryVector))
                      .k(topK)
                      .numCandidates(topK * 2);
                     
                     // 如果有过滤条件
                     if (filter != null && !filter.isEmpty()) {
                         k.filter(buildFilterQuery(filter));
                     }
                     
                     return k;
                 }));
                
                return s;
            }, Map.class);
            
            return response.hits().hits().stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
                
        } catch (IOException e) {
            log.error("Failed to search vectors", e);
            throw new RuntimeException("Failed to search vectors", e);
        }
    }
    
    @Override
    public List<SearchResult> searchWithThreshold(float[] queryVector, int topK, double similarityThreshold) {
        List<SearchResult> results = search(queryVector, topK);
        return results.stream()
            .filter(r -> r.getScore() >= similarityThreshold)
            .collect(Collectors.toList());
    }
    
    @Override
    public VectorDocument getById(String id) {
        try {
            GetResponse<Map> response = client.get(GetRequest.of(g -> g
                .index(indexName)
                .id(id)
            ), Map.class);
            
            if (!response.found()) {
                return null;
            }
            
            return convertToVectorDocument(response.source());
        } catch (IOException e) {
            log.error("Failed to get document by id: {}", id, e);
            throw new RuntimeException("Failed to get document", e);
        }
    }
    
    @Override
    public void update(String id, float[] vector, String content, Map<String, Object> metadata) {
        try {
            Map<String, Object> updates = new HashMap<>();
            if (vector != null) {
                updates.put("vector", vector);
            }
            if (content != null) {
                updates.put("content", content);
            }
            if (metadata != null) {
                updates.put("metadata", metadata);
            }
            updates.put("updatedAt", LocalDateTime.now().toString());
            
            client.update(UpdateRequest.of(u -> u
                .index(indexName)
                .id(id)
                .doc(updates)
            ), Map.class);
            
            log.debug("Updated document: id={}", id);
        } catch (IOException e) {
            log.error("Failed to update document: id={}", id, e);
            throw new RuntimeException("Failed to update document", e);
        }
    }
    
    @Override
    public void delete(String id) {
        try {
            client.delete(DeleteRequest.of(d -> d
                .index(indexName)
                .id(id)
            ));
            log.debug("Deleted document: id={}", id);
        } catch (IOException e) {
            log.error("Failed to delete document: id={}", id, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
    
    @Override
    public void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        try {
            List<BulkOperation> operations = ids.stream()
                .map(id -> BulkOperation.of(b -> b.delete(d -> d.index(indexName).id(id))))
                .collect(Collectors.toList());
            
            client.bulk(BulkRequest.of(b -> b
                .index(indexName)
                .operations(operations)
            ));
            
            log.info("Deleted {} documents in batch", ids.size());
        } catch (IOException e) {
            log.error("Failed to delete documents in batch", e);
            throw new RuntimeException("Failed to delete documents in batch", e);
        }
    }
    
    @Override
    public void clear() {
        try {
            client.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
            createIndex();
            log.info("Cleared all documents from index: {}", indexName);
        } catch (IOException e) {
            log.error("Failed to clear index", e);
            throw new RuntimeException("Failed to clear index", e);
        }
    }
    
    @Override
    public long count() {
        try {
            CountResponse response = client.count(CountRequest.of(c -> c.index(indexName)));
            return response.count();
        } catch (IOException e) {
            log.error("Failed to count documents", e);
            return 0;
        }
    }
    
    @Override
    public boolean healthCheck() {
        try {
            return client.ping().value();
        } catch (IOException e) {
            log.error("Health check failed", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        try {
            restClient.close();
            log.info("Closed Elasticsearch client");
        } catch (IOException e) {
            log.error("Failed to close Elasticsearch client", e);
        }
    }
    
    /**
     * 构建元数据过滤查询
     */
    private List<Query> buildFilterQuery(Map<String, Object> filter) {
        List<Query> queries = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Query query = Query.of(q -> q
                .term(t -> t
                    .field("metadata." + entry.getKey())
                    .value(v -> v.stringValue(entry.getValue().toString()))
                )
            );
            queries.add(query);
        }
        
        return queries;
    }
    
    /**
     * 转换为SearchResult
     */
    private SearchResult convertToSearchResult(Hit<Map> hit) {
        Map<String, Object> source = hit.source();
        
        return SearchResult.builder()
            .id((String) source.get("id"))
            .content((String) source.get("content"))
            .score(hit.score() != null ? hit.score() : 0.0)
            .metadata((Map<String, Object>) source.get("metadata"))
            .build();
    }
    
    /**
     * 转换为VectorDocument
     */
    @SuppressWarnings("unchecked")
    private VectorDocument convertToVectorDocument(Map<String, Object> source) {
        VectorDocument doc = new VectorDocument();
        doc.setId((String) source.get("id"));
        doc.setContent((String) source.get("content"));
        
        // 转换向量
        List<Double> vectorList = (List<Double>) source.get("vector");
        if (vectorList != null) {
            float[] vector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i).floatValue();
            }
            doc.setVector(vector);
        }
        
        doc.setMetadata((Map<String, Object>) source.get("metadata"));
        
        String createdAt = (String) source.get("createdAt");
        if (createdAt != null) {
            doc.setCreatedAt(LocalDateTime.parse(createdAt));
        }
        
        String updatedAt = (String) source.get("updatedAt");
        if (updatedAt != null) {
            doc.setUpdatedAt(LocalDateTime.parse(updatedAt));
        }
        
        return doc;
    }
    
    /**
     * 将float数组转换为List
     */
    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }
}

