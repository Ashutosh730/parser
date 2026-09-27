package com.logAnalyzer.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logAnalyzer.ai.entity.AiResult;
import com.logAnalyzer.ai.enums.AiFeatureEnum;
import com.logAnalyzer.ai.enums.LlmProviderEnum;
import com.logAnalyzer.ai.model.DiagnosisResponse;
import com.logAnalyzer.ai.model.InterpretedFilter;
import com.logAnalyzer.ai.model.LlmRequest;
import com.logAnalyzer.ai.model.NlQueryResponse;
import com.logAnalyzer.ai.model.SummaryResponse;
import com.logAnalyzer.ai.provider.LlmFactory;
import com.logAnalyzer.ai.provider.LLMProvider;
import com.logAnalyzer.ai.provider.UniversalLLMProvider;
import com.logAnalyzer.ai.repository.AiResultRepository;
import com.logAnalyzer.ai.service.impl.AIServiceImpl;
import com.logAnalyzer.core.entity.LogEntryDocument;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.exception.SessionNotFoundException;
import com.logAnalyzer.core.repository.LogEntryCustomEsRepository;
import com.logAnalyzer.core.repository.LogEntryEsRepository;
import com.logAnalyzer.core.repository.LogSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock private LlmFactory llmFactory;
    @Mock private AiResultRepository aiResultRepository;
    @Mock private LogSessionRepository sessionRepository;
    @Mock private LogEntryEsRepository logEntryEsRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private LogEntryCustomEsRepository customRepository;
    @Mock private UniversalLLMProvider universalProvider;
    @Mock private SearchHits<LogEntryDocument> searchHits;
    @Mock private SearchHit<LogEntryDocument> searchHit;

    private AIServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AIServiceImpl(
                llmFactory, aiResultRepository, sessionRepository, logEntryEsRepository,
                objectMapper, customRepository, universalProvider);
        lenient().when(sessionRepository.findByIdAndUserId("session-1", "user-1"))
                .thenReturn(Optional.of(LogSession.builder().id("session-1").build()));
    }

    @Test
    void summarizeLogs_shouldReturnCachedResponse() {
        LlmRequest request = request();
        AiResult cached = AiResult.builder()
                .result("cached summary")
                .provider(LlmProviderEnum.NVIDIA)
                .model("model-1")
                .build();
        when(aiResultRepository.findBySessionIdAndFeature("session-1", AiFeatureEnum.SUMMARY))
                .thenReturn(List.of(cached));

        SummaryResponse response = service.summarizeLogs("session-1", "user-1", request);

        assertEquals("cached summary", response.getSummary());
        assertTrue(response.isCached());
        assertEquals("NVIDIA", response.getProvider());
        verifyNoInteractions(logEntryEsRepository, universalProvider);
    }

    @Test
    void summarizeLogs_shouldReturnNoErrorsAndCacheResult() {
        LlmRequest request = request();
        when(aiResultRepository.findBySessionIdAndFeature(anyString(), eq(AiFeatureEnum.SUMMARY)))
                .thenReturn(List.of());
        when(logEntryEsRepository.findBySessionIdAndLevelIn(anyString(), anyList(), any()))
                .thenReturn(List.of());

        SummaryResponse response = service.summarizeLogs("session-1", "user-1", request);

        assertEquals("No errors found in this log file.", response.getSummary());
        assertFalse(response.isCached());
        verify(aiResultRepository).save(any(AiResult.class));
        verifyNoInteractions(universalProvider);
    }

    @Test
        void analyse_shouldReturnNoErrorsResponse() throws Throwable {
        LlmRequest request = request();
        when(aiResultRepository.findBySessionIdAndFeature(anyString(), eq(AiFeatureEnum.DIAGNOSIS)))
                .thenReturn(List.of());
        when(logEntryEsRepository.findBySessionIdAndLevelIn(anyString(), anyList(), any()))
                .thenReturn(List.of());

        DiagnosisResponse response = service.analyse("session-1", "user-1", request);

        assertEquals("No errors found in this log file.", response.getTimeline());
        assertEquals("NVIDIA", response.getProvider());
        assertFalse(response.isCached());
        verify(aiResultRepository).save(any(AiResult.class));
    }

    @Test
        void analyse_shouldDeserializeProviderResponse() throws Throwable {
        LlmRequest request = request();
        LogEntryDocument error = LogEntryDocument.builder()
                .level(LogLevel.ERROR).message("failure").build();
        DiagnosisResponse parsed = DiagnosisResponse.builder().impactSummary("impact").build();
        when(aiResultRepository.findBySessionIdAndFeature(anyString(), eq(AiFeatureEnum.DIAGNOSIS)))
                .thenReturn(List.of());
        when(logEntryEsRepository.findBySessionIdAndLevelIn(anyString(), anyList(), any()))
                .thenReturn(List.of(error));
        when(universalProvider.complete(request)).thenReturn("{json}");
        when(objectMapper.readValue("{json}", DiagnosisResponse.class)).thenReturn(parsed);

        DiagnosisResponse response = service.analyse("session-1", "user-1", request);

        assertSame(parsed, response);
        assertEquals("session-1", response.getSessionId());
        assertEquals("NVIDIA", response.getProvider());
        assertEquals("model-1", response.getModel());
        assertFalse(response.isCached());
        verify(aiResultRepository).save(any(AiResult.class));
    }

    @Test
        void queryProcessor_shouldHandleUnsupportedIntent() throws Throwable {
        LlmRequest request = request();
        when(universalProvider.complete(request)).thenReturn("unsupported-json");
        when(objectMapper.readValue("unsupported-json", InterpretedFilter.class))
                .thenReturn(InterpretedFilter.builder().intent("UNSUPPORTED").build());

        NlQueryResponse response = service.queryProcessor("session-1", "user-1", request);

        assertEquals("UNSUPPORTED", response.getIntent());
        assertEquals("query", response.getOriginalQuery());
        assertEquals("NVIDIA", response.getProvider());
        verifyNoInteractions(customRepository, llmFactory);
    }

    @Test
        void queryProcessor_shouldHandleSearchIntent() throws Throwable {
        LlmRequest request = request();
        LogEntryDocument result = LogEntryDocument.builder().id("entry-1").build();
        when(universalProvider.complete(request)).thenReturn("search-json");
        when(objectMapper.readValue("search-json", InterpretedFilter.class))
                .thenReturn(InterpretedFilter.builder().intent("SEARCH").build());
        when(customRepository.getSearchResult(eq("session-1"), any()))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(Stream.of(searchHit));
        when(searchHit.getContent()).thenReturn(result);
        when(searchHits.getTotalHits()).thenReturn(1L);

        NlQueryResponse response = service.queryProcessor("session-1", "user-1", request);

        assertEquals("SEARCH", response.getIntent());
        assertEquals(1L, response.getTotalHits());
        assertEquals(List.of(result), response.getResults());
        verify(customRepository).getSearchResult(eq("session-1"), any());
    }

    @Test
        void queryProcessor_shouldHandleCountAggregation() throws Throwable {
        LlmRequest request = request();
        when(universalProvider.complete(request)).thenReturn("aggregation-json");
        when(objectMapper.readValue("aggregation-json", InterpretedFilter.class))
                .thenReturn(InterpretedFilter.builder()
                        .intent("AGGREGATION")
                        .aggregationType("COUNT_BY_LEVEL")
                        .levels(List.of("ERROR"))
                        .build());
        when(customRepository.getLevelDistribution("session-1", List.of("ERROR")))
                .thenReturn(java.util.Map.of("ERROR", 2L));

        NlQueryResponse response = service.queryProcessor("session-1", "user-1", request);

        assertEquals("AGGREGATION", response.getIntent());
        assertEquals(java.util.Map.of("ERROR", 2L), response.getAggregationResult());
    }

    @Test
        void queryProcessor_shouldReturnFactNotFoundWhenNoRelevantLogsExist() throws Throwable {
        LlmRequest request = request();
        when(universalProvider.complete(request)).thenReturn("fact-json");
        when(objectMapper.readValue("fact-json", InterpretedFilter.class))
                .thenReturn(InterpretedFilter.builder()
                        .intent("FACT_EXTRACTION").keywords(List.of("port")).build());
        when(customRepository.findBySessionIdAndKeywords("session-1", List.of("port"),
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(List.of());

        NlQueryResponse response = service.queryProcessor("session-1", "user-1", request);

        assertEquals("FACT_EXTRACTION", response.getIntent());
        assertEquals("Could not find relevant log entries to answer this question.", response.getFactAnswer());
        verifyNoInteractions(llmFactory);
    }

    @Test
    void queryProcessor_shouldExtractFactFromRelevantLogs() throws Throwable {
        LlmRequest request = request();
        InterpretedFilter filter = InterpretedFilter.builder()
                .intent("FACT_EXTRACTION")
                .keywords(List.of("port"))
                .build();
        LogEntryDocument first = LogEntryDocument.builder()
                .level(LogLevel.INFO)
                .message("Started application on port 8080")
                .build();
        LogEntryDocument second = LogEntryDocument.builder()
                .level(LogLevel.WARN)
                .message("Health check is delayed")
                .build();
        LLMProvider provider = org.mockito.Mockito.mock(LLMProvider.class);

        when(universalProvider.complete(request)).thenReturn("fact-json");
        when(objectMapper.readValue("fact-json", InterpretedFilter.class)).thenReturn(filter);
        when(customRepository.findBySessionIdAndKeywords(
                "session-1", List.of("port"),
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(List.of(first, second));
        when(llmFactory.getProvider(LlmProviderEnum.NVIDIA)).thenReturn(provider);
        when(provider.complete(request)).thenReturn("The application runs on port 8080.");

        NlQueryResponse response = service.queryProcessor("session-1", "user-1", request);

        assertEquals("FACT_EXTRACTION", response.getIntent());
        assertEquals("query", response.getOriginalQuery());
        assertEquals("The application runs on port 8080.", response.getFactAnswer());
        assertSame(filter, response.getInterpretedFilter());
        assertEquals("NVIDIA", response.getProvider());
        assertEquals("model-1", response.getModel());

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(provider).complete(requestCaptor.capture());
        LlmRequest providerRequest = requestCaptor.getValue();
        assertTrue(providerRequest.getSystemPrompt().contains("Answer the user's question directly"));
        assertEquals(150, providerRequest.getMaxTokens());
        assertEquals(0.0f, providerRequest.getTemperature());
        assertTrue(providerRequest.getUserPrompt().contains("[INFO] Started application on port 8080"));
        assertTrue(providerRequest.getUserPrompt().contains("[WARN] Health check is delayed"));
    }

    @Test
    void summarizeLogs_shouldRejectSessionNotOwnedByUser() {
        when(sessionRepository.findByIdAndUserId("missing", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
                () -> service.summarizeLogs("missing", "user-1", request()));

        verifyNoInteractions(aiResultRepository, logEntryEsRepository, universalProvider);
    }

    private LlmRequest request() {
        return LlmRequest.builder()
                .userPrompt("query")
                .provider(LlmProviderEnum.NVIDIA)
                .model("model-1")
                .build();
    }
}
