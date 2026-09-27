package com.logAnalyzer.ai.provider;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmFactoryTest {

    @Test
    void getProvider_shouldReturnProviderWithRequestedName() {
        LLMProvider nvidia = mock(LLMProvider.class);
        LLMProvider openAi = mock(LLMProvider.class);
        when(nvidia.getProviderName()).thenReturn(LlmProviderEnum.NVIDIA);
        when(openAi.getProviderName()).thenReturn(LlmProviderEnum.OPENAI);

        LLMProvider result = new LlmFactory(List.of(nvidia, openAi))
                .getProvider(LlmProviderEnum.OPENAI);

        assertSame(openAi, result);
    }

    @Test
    void getProvider_shouldThrowWhenProviderIsUnavailable() {
        LLMProvider provider = mock(LLMProvider.class);
        when(provider.getProviderName()).thenReturn(LlmProviderEnum.NVIDIA);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new LlmFactory(List.of(provider)).getProvider(LlmProviderEnum.OPENAI));

        assertEquals("No suitable LLM provider found with name: OPENAI", exception.getMessage());
    }
}
