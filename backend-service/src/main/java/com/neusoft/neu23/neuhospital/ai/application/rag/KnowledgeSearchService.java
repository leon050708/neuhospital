package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class KnowledgeSearchService {

    private final KnowledgeChunkService chunkService;
    private final EmbeddingModel embeddingModel;

    public KnowledgeSearchService(KnowledgeChunkService chunkService,
                                  EmbeddingModel embeddingModel) {
        this.chunkService = chunkService;
        this.embeddingModel = embeddingModel;
    }

    public List<KnowledgeSearchHit> search(KnowledgeSearchRequest request) {
        QueryWrapper<KnowledgeChunkEntity> wrapper = new QueryWrapper<KnowledgeChunkEntity>()
                .eq("deleted", false)
                .eq("document_status", "PUBLISHED");
        if (request.departmentId() != null) {
            wrapper.eq("department_id", request.departmentId());
        }
        if (request.knowledgeTypes() != null && !request.knowledgeTypes().isEmpty()) {
            wrapper.in("knowledge_type", request.knowledgeTypes());
        }
        if (StringUtils.hasText(request.tagKeyword())) {
            wrapper.like("tags", request.tagKeyword().trim());
        }

        float[] queryEmbedding = embeddingModel.embed(request.query());
        List<KnowledgeSearchHit> hits = new ArrayList<>();
        for (KnowledgeChunkEntity entity : chunkService.list(wrapper)) {
            double score = cosineSimilarity(queryEmbedding, parseEmbedding(entity.getEmbeddingText()));
            if (score >= request.minScore()) {
                hits.add(new KnowledgeSearchHit(
                        entity.getId(),
                        entity.getDocumentId(),
                        entity.getContentText(),
                        entity.getKnowledgeType(),
                        entity.getDepartmentId(),
                        entity.getTags(),
                        score
                ));
            }
        }

        hits.sort(Comparator.comparingDouble(KnowledgeSearchHit::score).reversed());
        int limit = request.topK() > 0 ? request.topK() : 5;
        return hits.size() > limit ? hits.subList(0, limit) : hits;
    }

    private float[] parseEmbedding(String vectorText) {
        String normalized = vectorText.replace("[", "").replace("]", "").trim();
        if (normalized.isEmpty()) {
            return new float[0];
        }
        String[] parts = normalized.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0.0d;
        }
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
