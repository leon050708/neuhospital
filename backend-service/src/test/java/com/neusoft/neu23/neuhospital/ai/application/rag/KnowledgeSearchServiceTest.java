package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchServiceTest {

    @Test
    void shouldRankChunksBySimilarityAndApplyThreshold() {
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        KnowledgeSearchService searchService = new KnowledgeSearchService(chunkService, embeddingModel);

        KnowledgeChunkEntity strong = new KnowledgeChunkEntity();
        strong.setId(1L);
        strong.setContentText("挂号流程说明");
        strong.setEmbeddingText("[1.0,0.0]");

        KnowledgeChunkEntity medium = new KnowledgeChunkEntity();
        medium.setId(2L);
        medium.setContentText("初诊材料说明");
        medium.setEmbeddingText("[0.8,0.2]");

        KnowledgeChunkEntity weak = new KnowledgeChunkEntity();
        weak.setId(3L);
        weak.setContentText("药房取药提醒");
        weak.setEmbeddingText("[0.0,1.0]");

        when(chunkService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KnowledgeChunkEntity>>any())).thenReturn(List.of(strong, medium, weak));
        when(embeddingModel.embed("怎么挂号")).thenReturn(new float[]{1.0f, 0.0f});

        List<KnowledgeSearchHit> hits = searchService.search(new KnowledgeSearchRequest(
                "怎么挂号",
                List.of("REGISTRATION_PROCESS"),
                null,
                "挂号",
                2,
                0.7
        ));

        assertEquals(2, hits.size());
        assertEquals(1L, hits.get(0).chunkId());
        assertEquals(2L, hits.get(1).chunkId());
        assertTrue(hits.get(0).score() >= hits.get(1).score());
    }
}

