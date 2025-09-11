package com.synapse.chat_service.service.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;

import com.synapse.chat_service.service.ai.AIModelType;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClaudeService 테스트")
class ClaudeServiceTest {

    @Mock
    private AnthropicChatModel anthropicChatModel;

    @InjectMocks
    private ClaudeService claudeService;

    private String testPrompt;
    private String expectedResponse;

    @BeforeEach
    void setUp() {
        testPrompt = "안녕하세요, 테스트 프롬프트입니다.";
        expectedResponse = "안녕하세요! Claude 테스트 응답입니다.";
    }

    @Test
    @DisplayName("getModelType() 호출 시 CLAUDE 타입을 반환한다")
    void getModelType_ShouldReturnClaudeType() {
        // When
        AIModelType result = claudeService.getModelType();

        // Then
        assertThat(result).isEqualTo(AIModelType.CLAUDE);
    }

    @Test
    @DisplayName("시나리오 1: AI 응답 생성 성공 - AnthropicChatModel의 call() 메서드가 호출되고 결과가 CompletableFuture로 래핑되어 반환된다")
    void generateResponse_Success_ShouldReturnCompletedFuture() throws ExecutionException, InterruptedException {
        // Given
        when(anthropicChatModel.call(testPrompt)).thenReturn(expectedResponse);

        // When
        CompletableFuture<String> result = claudeService.generateResponse(testPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(expectedResponse);

        // AnthropicChatModel의 call() 메서드가 올바른 프롬프트로 호출되었는지 검증
        verify(anthropicChatModel).call(testPrompt);
    }

    @Test
    @DisplayName("시나리오 2: AI 응답 생성 실패 - AI 클라이언트가 예외를 던질 때 CompletableFuture가 예외로 완료된다")
    void generateResponse_Failure_ShouldReturnFailedFuture() {
        // Given
        RuntimeException expectedException = new RuntimeException("Anthropic API 호출 실패");
        when(anthropicChatModel.call(anyString())).thenThrow(expectedException);

        // When
        CompletableFuture<String> result = claudeService.generateResponse(testPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isTrue();

        // 예외가 올바르게 래핑되어 있는지 검증
        assertThat(result)
                .failsWithin(java.time.Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(RuntimeException.class)
                .withMessageContaining("Claude 모델 응답 생성 중 오류가 발생했습니다.");

        // AnthropicChatModel의 call() 메서드가 호출되었는지 검증
        verify(anthropicChatModel).call(testPrompt);
    }

    @Test
    @DisplayName("빈 프롬프트로 응답 생성 요청 시 정상 처리된다")
    void generateResponse_WithEmptyPrompt_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        String emptyPrompt = "";
        String emptyResponse = "";
        when(anthropicChatModel.call(emptyPrompt)).thenReturn(emptyResponse);

        // When
        CompletableFuture<String> result = claudeService.generateResponse(emptyPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(emptyResponse);

        verify(anthropicChatModel).call(emptyPrompt);
    }

    @Test
    @DisplayName("긴 프롬프트로 응답 생성 요청 시 정상 처리된다")
    void generateResponse_WithLongPrompt_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        String longPrompt = "a".repeat(10000); // 10,000자 프롬프트
        String longResponse = "b".repeat(5000); // 5,000자 응답
        when(anthropicChatModel.call(longPrompt)).thenReturn(longResponse);

        // When
        CompletableFuture<String> result = claudeService.generateResponse(longPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(longResponse);

        verify(anthropicChatModel).call(longPrompt);
    }

    @Test
    @DisplayName("null 응답을 받을 때 정상 처리된다")
    void generateResponse_WithNullResponse_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        when(anthropicChatModel.call(testPrompt)).thenReturn(null);

        // When
        CompletableFuture<String> result = claudeService.generateResponse(testPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isNull();

        verify(anthropicChatModel).call(testPrompt);
    }
}
