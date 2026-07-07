package com.neusoft.neu23.neuhospital.ai.infrastructure.jdbc;

import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchHit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class KnowledgeVectorSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KnowledgeVectorSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KnowledgeSearchHit> search(String queryVector,
                                           List<String> knowledgeTypes,
                                           Long departmentId,
                                           String tagKeyword,
                                           String embeddingModel,
                                           int embeddingVersion,
                                           int topK,
                                           double minScore) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, document_id, content_text, knowledge_type, department_id, tags,
                       1 - (CAST(embedding AS vector) <=> CAST(:queryVector AS vector)) AS score
                FROM knowledge_chunk
                WHERE deleted = false
                  AND document_status = 'PUBLISHED'
                  AND embedding_model = :embeddingModel
                  AND embedding_version = :embeddingVersion
                """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("queryVector", queryVector)
                .addValue("embeddingModel", embeddingModel)
                .addValue("embeddingVersion", embeddingVersion)
                .addValue("topK", topK)
                .addValue("minScore", minScore);

        if (departmentId != null) {
            sql.append(" AND department_id = :departmentId");
            params.addValue("departmentId", departmentId);
        }
        if (knowledgeTypes != null && !knowledgeTypes.isEmpty()) {
            sql.append(" AND knowledge_type IN (:knowledgeTypes)");
            params.addValue("knowledgeTypes", knowledgeTypes);
        }
        if (StringUtils.hasText(tagKeyword)) {
            sql.append(" AND tags LIKE :tagKeyword");
            params.addValue("tagKeyword", "%" + tagKeyword.trim() + "%");
        }

        sql.append(" AND 1 - (CAST(embedding AS vector) <=> CAST(:queryVector AS vector)) >= :minScore");
        sql.append(" ORDER BY CAST(embedding AS vector) <=> CAST(:queryVector AS vector)");
        sql.append(" LIMIT :topK");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new KnowledgeSearchHit(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getString("content_text"),
                rs.getString("knowledge_type"),
                rs.getObject("department_id", Long.class),
                rs.getString("tags"),
                rs.getDouble("score")
        ));
    }
}
