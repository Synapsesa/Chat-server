package com.synapse.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.repository.ConversationRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("MessageController 통합 테스트")
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Conversation testConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        // 테스트용 대화 생성
        testConversation = TestObjectFactory.createConversation(1L);
        testConversation = conversationRepository.save(testConversation);

        // 테스트용 메시지 생성
        testMessage = TestObjectFactory.createUserMessage(testConversation, "테스트 메시지");
        testMessage = messageRepository.save(testMessage);
    }

    @Nested
    @DisplayName("POST /api/v1/messages - 메시지 생성")
    class CreateMessage {

        @Test
        @DisplayName("성공: 유효한 메시지 생성 요청")
        void createMessage_Success() throws Exception {
            // given
            MessageRequest.Create request = new MessageRequest.Create(
                    testConversation.getUserId(),
                    SenderType.USER,
                    "새로운 메시지"
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.conversationId").value(testConversation.getId().toString()))
                    .andExpect(jsonPath("$.senderType").value("USER"))
                    .andExpect(jsonPath("$.content").value("새로운 메시지"))
                    .andExpect(jsonPath("$.createdDate").exists())
                    .andExpect(jsonPath("$.updatedDate").exists());
        }

        @Test
        @DisplayName("실패: 사용자 ID가 null인 경우")
        void createMessage_Fail_NullUserId() throws Exception {
            // given
            MessageRequest.Create request = new MessageRequest.Create(
                    null,
                    SenderType.USER,
                    "메시지 내용"
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 발신자 타입이 null인 경우")
        void createMessage_Fail_NullSenderType() throws Exception {
            // given
            MessageRequest.Create request = new MessageRequest.Create(
                    testConversation.getUserId(),
                    null,
                    "메시지 내용"
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 메시지 내용이 비어있는 경우")
        void createMessage_Fail_BlankContent() throws Exception {
            // given
            MessageRequest.Create request = new MessageRequest.Create(
                    testConversation.getUserId(),
                    SenderType.USER,
                    ""
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("성공: 새로운 사용자 ID로 대화 생성")
        void createMessage_Success_NewUser() throws Exception {
            // given
            Long newUserId = 999L;
            MessageRequest.Create request = new MessageRequest.Create(
                    newUserId,
                    SenderType.USER,
                    "새 사용자의 첫 메시지"
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("새 사용자의 첫 메시지"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/messages/{messageId} - 메시지 단건 조회")
    class GetMessage {

        @Test
        @DisplayName("성공: 존재하는 메시지 조회")
        void getMessage_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/{messageId}", testMessage.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testMessage.getId()))
                    .andExpect(jsonPath("$.conversationId").value(testConversation.getId().toString()))
                    .andExpect(jsonPath("$.senderType").value("USER"))
                    .andExpect(jsonPath("$.content").value("테스트 메시지"))
                    .andExpect(jsonPath("$.createdDate").exists())
                    .andExpect(jsonPath("$.updatedDate").exists());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 메시지 ID")
        void getMessage_Fail_NotFound() throws Exception {
            // given
            Long nonExistentMessageId = 99999L;

            // when & then
            mockMvc.perform(get("/api/v1/messages/{messageId}", nonExistentMessageId))
                    .andExpect(status().isNotFound());
        }
    }



    @Nested
    @DisplayName("DELETE /api/v1/messages/{messageId} - 메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("성공: 존재하는 메시지 삭제")
        void deleteMessage_Success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/messages/{messageId}", testMessage.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 메시지 ID")
        void deleteMessage_Fail_NotFound() throws Exception {
            // given
            Long nonExistentMessageId = 99999L;

            // when & then
            mockMvc.perform(delete("/api/v1/messages/{messageId}", nonExistentMessageId))
                    .andExpect(status().isNotFound());
        }
    }
}
