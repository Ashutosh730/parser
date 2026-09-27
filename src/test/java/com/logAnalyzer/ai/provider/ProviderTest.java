package com.logAnalyzer.ai.provider;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import com.logAnalyzer.ai.model.LlmRequest;
import com.logAnalyzer.ai.provider.impl.NvidiaProvider;
import com.logAnalyzer.ai.provider.impl.OpenAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class ProviderTest {

    @Test
    void nvidiaProvider_shouldExposeNameAndResolveModels() {
        NvidiaProvider provider = new NvidiaProvider(mock(org.springframework.ai.chat.client.ChatClient.class));
        ReflectionTestUtils.setField(provider, "model", "default-model");

        assertEquals(LlmProviderEnum.NVIDIA, provider.getProviderName());
        assertEquals("default-model", ReflectionTestUtils.invokeMethod(provider, "resolveEffectiveModel", (String) null));
        assertEquals("default-model", ReflectionTestUtils.invokeMethod(provider, "resolveEffectiveModel", "  "));
        assertEquals("default-model", ReflectionTestUtils.invokeMethod(provider, "resolveEffectiveModel", "some-model:free"));
        assertEquals("custom-model", ReflectionTestUtils.invokeMethod(provider, "resolveEffectiveModel", " custom-model "));
    }

    @Test
    void universalProvider_shouldResolveModels() {
        UniversalLLMProvider provider = new UniversalLLMProvider(
                mock(org.springframework.ai.chat.client.ChatClient.class));
        ReflectionTestUtils.setField(provider, "model", "default-model");

        assertEquals("default-model", provider.resolveEffectiveModel(null));
        assertEquals("default-model", provider.resolveEffectiveModel("model:FREE"));
        assertEquals("custom-model", provider.resolveEffectiveModel(" custom-model "));
    }

    @Test
    void openAiProvider_shouldExposeName() {
        OpenAiProvider provider = new OpenAiProvider(mock(org.springframework.ai.openai.OpenAiChatModel.class));

        assertEquals(LlmProviderEnum.OPENAI, provider.getProviderName());
    }
}
