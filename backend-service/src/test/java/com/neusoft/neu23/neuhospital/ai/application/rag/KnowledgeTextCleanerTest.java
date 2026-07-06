package com.neusoft.neu23.neuhospital.ai.application.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeTextCleanerTest {

    @Test
    void shouldNormalizeWhitespaceAndDropRepeatedPageHeaders() {
        KnowledgeTextCleaner cleaner = new KnowledgeTextCleaner();

        String raw = """
                东软智慧医院就诊须知
                第1页

                一、挂号说明
                    初诊患者请先实名建档。


                东软智慧医院就诊须知
                第2页
                二、取号说明
                请在预约时间前 30 分钟 到院取号。
                """;

        String cleaned = cleaner.clean(raw);

        assertEquals("""
                一、挂号说明
                初诊患者请先实名建档。

                二、取号说明
                请在预约时间前 30 分钟 到院取号。
                """.trim(), cleaned);
    }

    @Test
    void shouldKeepUsefulBulletStructure() {
        KnowledgeTextCleaner cleaner = new KnowledgeTextCleaner();

        String raw = """
                检查前注意事项

                1. 检查前一天清淡饮食
                2. 检查当天保持空腹

                备注：如有特殊情况，请咨询导诊台。
                """;

        String cleaned = cleaner.clean(raw);

        assertEquals("""
                检查前注意事项

                1. 检查前一天清淡饮食
                2. 检查当天保持空腹

                备注：如有特殊情况，请咨询导诊台。
                """.trim(), cleaned);
    }
}
