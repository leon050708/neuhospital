package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.ai.infrastructure.jdbc.KnowledgeVectorSearchRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeVectorSearchRepositoryTest {

    @Test
    void shouldBuildDatabaseSideTopKSearchWithModelScope() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        KnowledgeVectorSearchRepository repository = new KnowledgeVectorSearchRepository(jdbcTemplate);

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(
                        new KnowledgeSearchHit(1L, 10L, "挂号流程", "REGISTRATION_PROCESS", null, "挂号", 0.91),
                        new KnowledgeSearchHit(2L, 10L, "初诊材料", "VISIT_PREPARATION", null, "初诊", 0.82)
                ));

        List<KnowledgeSearchHit> hits = repository.search(
                "[1.000000,0.000000]",
                List.of("REGISTRATION_PROCESS", "VISIT_PREPARATION"),
                null,
                "挂号",
                "text-embedding-v3",
                1,
                4,
                0.6
        );

        assertEquals(2, hits.size());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("CAST(embedding AS vector)"));
        assertTrue(sql.contains("embedding_model = :embeddingModel"));
        assertTrue(sql.contains("embedding_version = :embeddingVersion"));
        assertTrue(sql.contains("LIMIT :topK"));

        MapSqlParameterSource params = paramCaptor.getValue();
        assertEquals("text-embedding-v3", params.getValue("embeddingModel"));
        assertEquals(1, params.getValue("embeddingVersion"));
        assertEquals(4, params.getValue("topK"));
        assertEquals("%挂号%", params.getValue("tagKeyword"));
    }
}
