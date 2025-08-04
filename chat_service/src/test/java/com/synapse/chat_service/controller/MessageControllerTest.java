package com.synapse.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.repository.ChatRoomRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

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
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    private ChatRoom testChatRoom;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        // 테스트용 채팅방 생성
        testChatRoom = TestObjectFactory.createChatRoom(1L, "테스트 채팅방");
        testChatRoom = chatRoomRepository.save(testChatRoom);

        // 테스트용 메시지 생성
        testMessage = TestObjectFactory.createUserMessage(testChatRoom, "테스트 메시지");
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
                    testChatRoom.getId(),
                    SenderType.USER,
                    "새로운 메시지"
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.chatRoomId").value(testChatRoom.getId().toString()))
                    .andExpect(jsonPath("$.senderType").value("USER"))
                    .andExpect(jsonPath("$.content").value("새로운 메시지"))
                    .andExpect(jsonPath("$.createdDate").exists())
                    .andExpect(jsonPath("$.updatedDate").exists());
        }

        @Test
        @DisplayName("실패: 채팅방 ID가 null인 경우")
        void createMessage_Fail_NullChatRoomId() throws Exception {
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
                    testChatRoom.getId(),
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
                    testChatRoom.getId(),
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
        @DisplayName("실패: 존재하지 않는 채팅방 ID")
        void createMessage_Fail_ChatRoomNotFound() throws Exception {
            // given
            UUID nonExistentChatRoomId = UUID.randomUUID();
            MessageRequest.Create request = new MessageRequest.Create(
                    nonExistentChatRoomId,
                    SenderType.USER,
                    "메시지 내용"
            );

            // when & then
            mockMvc.perform(post("/api/v1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
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
                    .andExpect(jsonPath("$.chatRoomId").value(testChatRoom.getId().toString()))
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
    @DisplayName("GET /api/v1/messages/chat-room/{chatRoomId} - 채팅방별 메시지 목록 조회")
    class GetMessagesByChatRoomId {

        @Test
        @DisplayName("성공: 채팅방의 메시지 목록 조회")
        void getMessagesByChatRoomId_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}", testChatRoom.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(testMessage.getId()))
                    .andExpect(jsonPath("$[0].chatRoomId").value(testChatRoom.getId().toString()))
                    .andExpect(jsonPath("$[0].senderType").value("USER"))
                    .andExpect(jsonPath("$[0].content").value("테스트 메시지"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방 ID")
        void getMessagesByChatRoomId_Fail_ChatRoomNotFound() throws Exception {
            // given
            UUID nonExistentChatRoomId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}", nonExistentChatRoomId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/messages/chat-room/{chatRoomId}/paging - 채팅방별 메시지 페이징 조회")
    class GetMessagesByChatRoomIdWithPaging {

        @Test
        @DisplayName("성공: 기본 페이징 파라미터로 조회")
        void getMessagesByChatRoomIdWithPaging_Success_DefaultParams() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/paging", testChatRoom.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(testMessage.getId()))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.size").value(50))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("성공: 커스텀 페이징 파라미터로 조회")
        void getMessagesByChatRoomIdWithPaging_Success_CustomParams() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/paging", testChatRoom.getId())
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdDate,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/messages/chat-room/{chatRoomId}/recent - 최신순 메시지 페이징 조회")
    class GetRecentMessages {

        @Test
        @DisplayName("성공: 메시지가 createdDate 기준 내림차순(DESC)으로 정렬되어 반환")
        void getRecentMessages_Success_DescendingOrder() throws Exception {
            // given
            // 추가 메시지들을 생성하여 정렬 테스트
            Message message1 = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.USER)
                    .content("첫 번째 메시지")
                    .build();
            Message message2 = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.USER)
                    .content("두 번째 메시지")
                    .build();
            Message message3 = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.USER)
                    .content("세 번째 메시지")
                    .build();

            messageRepository.save(message1);
            Thread.sleep(10); // 시간 차이를 위한 짧은 대기
            messageRepository.save(message2);
            Thread.sleep(10);
            messageRepository.save(message3);

            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", testChatRoom.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(4)) // 기존 testMessage + 3개 추가
                    .andExpect(jsonPath("$.content[0].content").value("세 번째 메시지")) // 가장 최신
                    .andExpect(jsonPath("$.content[1].content").value("두 번째 메시지"))
                    .andExpect(jsonPath("$.content[2].content").value("첫 번째 메시지"))
                    .andExpect(jsonPath("$.content[3].content").value("테스트 메시지")) // 가장 오래된
                    .andExpect(jsonPath("$.totalElements").value(4))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("성공: 페이징 정보가 정확한지 확인")
        void getRecentMessages_Success_PagingInfo() throws Exception {
            // given
            // 페이징 테스트를 위해 여러 메시지 생성
            for (int i = 1; i <= 15; i++) {
                Message message = Message.builder()
                        .chatRoom(testChatRoom)
                        .senderType(SenderType.USER)
                        .content("메시지 " + i)
                        .build();
                messageRepository.save(message);
                Thread.sleep(5); // 시간 차이를 위한 짧은 대기
            }

            // when & then - 첫 번째 페이지 (size=5)
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", testChatRoom.getId())
                            .param("page", "0")
                            .param("size", "5"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.totalElements").value(16)) // 기존 testMessage + 15개 추가
                    .andExpect(jsonPath("$.totalPages").value(4)) // 16개 / 5 = 4페이지
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false));

            // when & then - 두 번째 페이지
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", testChatRoom.getId())
                            .param("page", "1")
                            .param("size", "5"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(false));

            // when & then - 마지막 페이지
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", testChatRoom.getId())
                            .param("page", "3")
                            .param("size", "5"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1)) // 마지막 페이지는 1개만
                    .andExpect(jsonPath("$.number").value(3))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("성공: 기본 페이징 파라미터 적용")
        void getRecentMessages_Success_DefaultParams() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", testChatRoom.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.size").value(50)) // 기본 size
                    .andExpect(jsonPath("$.number").value(0)) // 기본 page
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("성공: 빈 채팅방의 경우 빈 페이지 반환")
        void getRecentMessages_Success_EmptyResult() throws Exception {
            // given
            ChatRoom emptyChatRoom = ChatRoom.builder()
                    .userId(1L)
                    .title("빈 채팅방")
                    .build();
            emptyChatRoom = chatRoomRepository.save(emptyChatRoom);

            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", emptyChatRoom.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방 ID")
        void getRecentMessages_Fail_ChatRoomNotFound() throws Exception {
            // given
            UUID nonExistentChatRoomId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", nonExistentChatRoomId))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("성공: 다양한 SenderType 메시지 혼재 시 정렬 확인")
        void getRecentMessages_Success_MixedSenderTypes() throws Exception {
            // given
            Message userMessage = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.USER)
                    .content("사용자 메시지")
                    .build();
            Message assistantMessage = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.ASSISTANT)
                    .content("어시스턴트 메시지")
                    .build();

            messageRepository.save(userMessage);
            Thread.sleep(10);
            messageRepository.save(assistantMessage);

            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/recent", testChatRoom.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.content[0].content").value("어시스턴트 메시지"))
                    .andExpect(jsonPath("$.content[0].senderType").value("ASSISTANT"))
                    .andExpect(jsonPath("$.content[1].content").value("사용자 메시지"))
                    .andExpect(jsonPath("$.content[1].senderType").value("USER"))
                    .andExpect(jsonPath("$.content[2].content").value("테스트 메시지"))
                    .andExpect(jsonPath("$.content[2].senderType").value("USER"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/messages/chat-room/{chatRoomId}/search - 메시지 내용 검색")
    class SearchMessages {

        @Test
        @DisplayName("성공: 키워드로 메시지 검색")
        void searchMessages_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/search", testChatRoom.getId())
                            .param("keyword", "테스트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].content").value("테스트 메시지"));
        }

        @Test
        @DisplayName("성공: 존재하지 않는 키워드 검색 (빈 결과)")
        void searchMessages_Success_NoResults() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/messages/chat-room/{chatRoomId}/search", testChatRoom.getId())
                            .param("keyword", "존재하지않는키워드"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
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
