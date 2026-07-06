package com.neusoft.neu23.neuhospital.ai.application.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeChunkerTest {

    @Test
    void shouldCreateOverlappedChunksWithoutBreakingMeaningIntoTinyPieces() {
        KnowledgeChunker chunker = new KnowledgeChunker(40, 10, 20);

        String text = """
                挂号流程说明：患者先实名建档，再选择科室和医生。
                如选择预约挂号，请在预约时段前到院签到取号。
                若号源已满，请改约其他时段。
                """;

        List<String> chunks = chunker.chunk(text);

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).contains("患者先实名建档"));
        assertTrue(chunks.get(1).contains("医生。"));
        assertTrue(chunks.get(1).contains("预约挂号"));
        assertTrue(chunks.get(2).contains("到院签到取号。"));
        assertTrue(chunks.get(2).contains("若号源已满"));
    }

    @Test
    void shouldMergeShortParagraphsBeforeSplitting() {
        KnowledgeChunker chunker = new KnowledgeChunker(45, 8, 18);

        String text = """
                消化内科初诊须知

                请携带身份证。

                请携带医保卡。

                如既往有胃镜、肠镜或化验单，请一并带来供医生参考。
                """;

        List<String> chunks = chunker.chunk(text);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("请携带身份证。"));
        assertTrue(chunks.get(0).contains("请携带医保卡。"));
        assertTrue(chunks.get(1).contains("胃镜、肠镜或化验单"));
    }
}
