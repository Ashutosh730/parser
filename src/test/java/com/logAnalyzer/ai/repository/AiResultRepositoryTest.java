package com.logAnalyzer.ai.repository;

import com.logAnalyzer.ai.enums.AiFeatureEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiResultRepositoryTest {

    @Test
    void repositoryInterface_shouldExposeExpectedContract() throws Exception {
        assertNotNull(AiResultRepository.class.getMethod(
                "findBySessionIdAndFeature", String.class, AiFeatureEnum.class));
    }
}
