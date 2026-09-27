package com.logAnalyzer.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AiConfigTest {

    @Test
    void chatClient_shouldBeCreatedFromChatModel() {
        ChatClient client = new AiConfig().chatClient(mock(OpenAiChatModel.class));

        assertNotNull(client);
    }
}
