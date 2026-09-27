package com.logAnalyzer.core.controller;

import com.logAnalyzer.ai.model.DiagnosisResponse;
import com.logAnalyzer.ai.model.LlmRequest;
import com.logAnalyzer.ai.model.NlQueryResponse;
import com.logAnalyzer.ai.model.SummaryResponse;
import com.logAnalyzer.ai.service.AIService;
import com.logAnalyzer.auth.entity.User;
import com.logAnalyzer.auth.repository.UserRepository;
import com.logAnalyzer.core.exception.AiResponseParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAIControllerTest {

    @Mock
    private AIService aiService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @Mock
    private User user;

    private LogAIController controller;

    @BeforeEach
    void setUp() {
        controller = new LogAIController(aiService, userRepository);
    }

    @Test
    void summarizeLogs_shouldDelegateToAiService() {
        LlmRequest request = LlmRequest.builder().userPrompt("summarize").build();
        SummaryResponse expected = SummaryResponse.builder()
                .sessionId("session-1")
                .summary("Summary")
                .build();
        givenUser("user-1");
        when(aiService.summarizeLogs("session-1", "user-1", request))
                .thenReturn(expected);

        ResponseEntity<SummaryResponse> response =
                controller.summarizeLogs("session-1", request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(aiService).summarizeLogs("session-1", "user-1", request);
    }

    @Test
    void rootCause_shouldDelegateToAiService() throws AiResponseParseException {
        LlmRequest request = LlmRequest.builder().userPrompt("diagnose").build();
        DiagnosisResponse expected = DiagnosisResponse.builder()
                .sessionId("session-1")
                .timeline("timeline")
                .build();
        givenUser("user-1");
        when(aiService.analyse("session-1", "user-1", request))
                .thenReturn(expected);

        ResponseEntity<DiagnosisResponse> response =
                controller.rootCause("session-1", request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(aiService).analyse("session-1", "user-1", request);
    }

    @Test
    void search_shouldDelegateToAiService() throws AiResponseParseException {
        LlmRequest request = LlmRequest.builder().userPrompt("find errors").build();
        NlQueryResponse expected = NlQueryResponse.builder()
                .sessionId("session-1")
                .originalQuery("find errors")
                .build();
        givenUser("user-1");
        when(aiService.queryProcessor("session-1", "user-1", request))
                .thenReturn(expected);

        ResponseEntity<NlQueryResponse> response =
                controller.search("session-1", request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(aiService).queryProcessor("session-1", "user-1", request);
    }

    @Test
    void aiEndpoints_shouldAllowNullRequest() throws AiResponseParseException {
        givenUser("user-1");
        SummaryResponse expected = SummaryResponse.builder().sessionId("session-1").build();
        when(aiService.summarizeLogs("session-1", "user-1", null)).thenReturn(expected);

        ResponseEntity<SummaryResponse> response =
                controller.summarizeLogs("session-1", null, userDetails);

        assertSame(expected, response.getBody());
        verify(aiService).summarizeLogs("session-1", "user-1", null);
    }

    @Test
    void summarizeLogs_shouldRejectUnknownUser() {
        givenUnknownUser();

        assertThrows(UsernameNotFoundException.class, () ->
                controller.summarizeLogs("session-1", null, userDetails));

        verifyNoInteractions(aiService);
    }

    @Test
    void rootCause_shouldRejectUnknownUser() {
        givenUnknownUser();

        assertThrows(UsernameNotFoundException.class, () ->
                controller.rootCause("session-1", null, userDetails));

        verifyNoInteractions(aiService);
    }

    @Test
    void search_shouldRejectUnknownUser() {
        givenUnknownUser();

        assertThrows(UsernameNotFoundException.class, () ->
                controller.search("session-1", null, userDetails));

        verifyNoInteractions(aiService);
    }

    private void givenUser(String userId) {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
    }

    private void givenUnknownUser() {
        when(userDetails.getUsername()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());
    }
}
