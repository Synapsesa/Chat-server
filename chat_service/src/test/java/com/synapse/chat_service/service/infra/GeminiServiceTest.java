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
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;

import com.synapse.chat_service.service.ai.AIModelType;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiService 테스트")
class GeminiServiceTest {

    @Mock
    private VertexAiGeminiChatModel vertexAiGeminiChatModel;

    @InjectMocks
    private GeminiService geminiService;

    private String testPrompt;
    private String expectedResponse;

    @BeforeEach
    void setUp() {
        testPrompt = "안녕하세요, 테스트 프롬프트입니다.";
        expectedResponse = "안녕하세요! Gemini 테스트 응답입니다.";
    }

    @Test
    @DisplayName("getModelType() 호출 시 GEMINI 타입을 반환한다")
    void getModelType_ShouldReturnGeminiType() {
        // When
        AIModelType result = geminiService.getModelType();

        // Then
        assertThat(result).isEqualTo(AIModelType.GEMINI);
    }

    @Test
    @DisplayName("시나리오 1: AI 응답 생성 성공 - VertexAiGeminiChatModel의 call() 메서드가 호출되고 결과가 CompletableFuture로 래핑되어 반환된다")
    void generateResponse_Success_ShouldReturnCompletedFuture() throws ExecutionException, InterruptedException {
        // Given
        when(vertexAiGeminiChatModel.call(testPrompt)).thenReturn(expectedResponse);

        // When
        CompletableFuture<String> result = geminiService.generateResponse(testPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(expectedResponse);
        
        // VertexAiGeminiChatModel의 call() 메서드가 올바른 프롬프트로 호출되었는지 검증
        verify(vertexAiGeminiChatModel).call(testPrompt);
    }

    @Test
    @DisplayName("시나리오 2: AI 응답 생성 실패 - AI 클라이언트가 예외를 던질 때 CompletableFuture가 예외로 완료된다")
    void generateResponse_Failure_ShouldReturnFailedFuture() {
        // Given
        RuntimeException expectedException = new RuntimeException("Vertex AI Gemini API 호출 실패");
        when(vertexAiGeminiChatModel.call(anyString())).thenThrow(expectedException);

        // When
        CompletableFuture<String> result = geminiService.generateResponse(testPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isTrue();
        
        // 예외가 올바르게 래핑되어 있는지 검증
        assertThat(result)
            .failsWithin(java.time.Duration.ofSeconds(1))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(RuntimeException.class)
            .withMessageContaining("Gemini 모델 응답 생성 중 오류가 발생했습니다.");
        
        // VertexAiGeminiChatModel의 call() 메서드가 호출되었는지 검증
        verify(vertexAiGeminiChatModel).call(testPrompt);
    }

    @Test
    @DisplayName("빈 프롬프트로 응답 생성 요청 시 정상 처리된다")
    void generateResponse_WithEmptyPrompt_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        String emptyPrompt = "";
        String emptyResponse = "";
        when(vertexAiGeminiChatModel.call(emptyPrompt)).thenReturn(emptyResponse);

        // When
        CompletableFuture<String> result = geminiService.generateResponse(emptyPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(emptyResponse);
        
        verify(vertexAiGeminiChatModel).call(emptyPrompt);
    }

    @Test
    @DisplayName("긴 프롬프트로 응답 생성 요청 시 정상 처리된다")
    void generateResponse_WithLongPrompt_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        String longPrompt = "a".repeat(10000); // 10,000자 프롬프트
        String longResponse = "b".repeat(5000); // 5,000자 응답
        when(vertexAiGeminiChatModel.call(longPrompt)).thenReturn(longResponse);

        // When
        CompletableFuture<String> result = geminiService.generateResponse(longPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(longResponse);
        
        verify(vertexAiGeminiChatModel).call(longPrompt);
    }

    @Test
    @DisplayName("null 응답을 받을 때 정상 처리된다")
    void generateResponse_WithNullResponse_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        when(vertexAiGeminiChatModel.call(testPrompt)).thenReturn(null);

        // When
        CompletableFuture<String> result = geminiService.generateResponse(testPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isNull();
        
        verify(vertexAiGeminiChatModel).call(testPrompt);
    }

    @Test
    @DisplayName("특수 문자가 포함된 프롬프트로 응답 생성 요청 시 정상 처리된다")
    void generateResponse_WithSpecialCharacters_ShouldHandleGracefully() throws ExecutionException, InterruptedException {
        // Given
        String specialPrompt = "특수문자 테스트: !@#$%^&*()_+{}|:<>?[]\\;'\",./ 한글 English 123";
        String specialResponse = "특수문자가 포함된 응답입니다.";
        when(vertexAiGeminiChatModel.call(specialPrompt)).thenReturn(specialResponse);

        // When
        CompletableFuture<String> result = geminiService.generateResponse(specialPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        assertThat(result.get()).isEqualTo(specialResponse);
        
        verify(vertexAiGeminiChatModel).call(specialPrompt);
    }
}
