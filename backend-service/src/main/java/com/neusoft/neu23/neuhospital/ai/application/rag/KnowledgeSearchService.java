package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.ai.infrastructure.jdbc.KnowledgeVectorSearchRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeSearchService {

    private final KnowledgeVectorSearchRepository vectorSearchRepository;
    private final EmbeddingModel embeddingModel;
    private final String currentEmbeddingModel;
    private final int currentEmbeddingVersion;

    public KnowledgeSearchService(KnowledgeVectorSearchRepository vectorSearchRepository,
                                  EmbeddingModel embeddingModel,
                                  @Value("${spring.ai.openai.embedding.options.model:text-embedding-v3}") String currentEmbeddingModel,
                                  @Value("${app.ai.embedding-version:1}") int currentEmbeddingVersion) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.embeddingModel = embeddingModel;
        this.currentEmbeddingModel = currentEmbeddingModel;
        this.currentEmbeddingVersion = currentEmbeddingVersion;
    }

    public List<KnowledgeSearchHit> search(KnowledgeSearchRequest request) {
        float[] queryEmbedding = embeddingModel.embed(request.query());
        int limit = request.topK() > 0 ? request.topK() : 5;
        return vectorSearchRepository.search(
                toVectorLiteral(queryEmbedding),
                request.knowledgeTypes(),
                request.departmentId(),
                request.tagKeyword(),
                currentEmbeddingModel,
                currentEmbeddingVersion,
                limit,
                request.minScore()
        );
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.6f", embedding[i]));
        }
        builder.append(']');
        return builder.toString();
    }
}
