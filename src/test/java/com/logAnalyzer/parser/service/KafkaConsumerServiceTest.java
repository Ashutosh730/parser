package com.logAnalyzer.parser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logAnalyzer.parser.core.LogParserFactory;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import com.logAnalyzer.parser.model.raw.LogMessageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.mockito.Mockito.*;

class KafkaConsumerServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LogParserFactory logParserFactory;

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldParseValidMessages() throws Exception {

        // given
        String jsonMessage = "{\"logLine\":\"INFO Application started\"}";

        LogMessageModel logMessage = new LogMessageModel();
        logMessage.setLogLine("INFO Application started");

        ParsedLog parsedLog = ParsedLog.builder()
                .level("INFO")
                .message("Application started")
                .build();

        when(objectMapper.readValue(jsonMessage, LogMessageModel.class))
                .thenReturn(logMessage);

        when(logParserFactory.parse("INFO Application started"))
                .thenReturn(parsedLog);

        // when
        kafkaConsumerService.consume(List.of(jsonMessage));

        // then
        verify(objectMapper, times(1))
                .readValue(jsonMessage, LogMessageModel.class);

        verify(logParserFactory, times(1))
                .parse("INFO Application started");
    }

    @Test
    void shouldSkipEmptyMessages() {

        kafkaConsumerService.consume(List.of("", "   "));

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(logParserFactory);
    }

    @Test
    void shouldHandleInvalidJsonGracefully() throws Exception {

        String badJson = "INVALID_JSON";

        when(objectMapper.readValue(badJson, LogMessageModel.class))
                .thenThrow(new RuntimeException("Parsing error"));

        kafkaConsumerService.consume(List.of(badJson));

        verify(objectMapper, times(1))
                .readValue(badJson, LogMessageModel.class);

        // parser should never be called
        verifyNoInteractions(logParserFactory);
    }
}