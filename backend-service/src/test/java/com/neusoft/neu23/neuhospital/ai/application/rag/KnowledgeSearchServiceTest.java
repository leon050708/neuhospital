package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.ai.infrastructure.jdbc.KnowledgeVectorSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSearchServiceTest {

    @Test
    void shouldForwardModelAndVersionScopedSearchToRepository() {
        KnowledgeVectorSearchRepository repository = mock(KnowledgeVectorSearchRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        KnowledgeSearchService searchService = new KnowledgeSearchService(repository, embeddingModel, "text-embedding-v3", 1);

        KnowledgeSearchHit hit = new KnowledgeSearchHit(1L, 10L, "挂号流程说明", "REGISTRATION_PROCESS", null, "挂号", 0.91);
        when(embeddingModel.embed("怎么挂号")).thenReturn(new float[]{1.0f, 0.0f});
        when(repository.search(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(List.of(hit));

        List<KnowledgeSearchHit> hits = searchService.search(new KnowledgeSearchRequest(
                "怎么挂号",
                List.of("REGISTRATION_PROCESS"),
                null,
                "挂号",
                2,
                0.7
        ));

        assertEquals(1, hits.size());
        assertEquals(1L, hits.get(0).chunkId());
        verify(repository).search("[1.000000,0.000000]", List.of("REGISTRATION_PROCESS"), null, "挂号", "text-embedding-v3", 1, 2, 0.7);
    }

    @Test
    void shouldUseDefaultTopKWhenRequestDoesNotProvidePositiveLimit() {
        KnowledgeVectorSearchRepository repository = mock(KnowledgeVectorSearchRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        KnowledgeSearchService searchService = new KnowledgeSearchService(repository, embeddingModel, "text-embedding-v3", 1);

        when(embeddingModel.embed("挂号")).thenReturn(new float[]{1.0f, 0.0f});
        when(repository.search(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(List.of());

        searchService.search(new KnowledgeSearchRequest(
                "挂号",
                List.of("REGISTRATION_PROCESS"),
                null,
                null,
                0,
                0.2
        ));

        verify(repository).search("[1.000000,0.000000]", List.of("REGISTRATION_PROCESS"), null, null, "text-embedding-v3", 1, 5, 0.2);
    }
}
