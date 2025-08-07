package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.exception.commonexception.NotFoundException;
import com.synapse.chat_service.domain.repository.ConversationRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.session.RedisAiChatManager;
import com.synapse.chat_service.session.RedisSessionManager;
import org.springframework.data.redis.core.RedisTemplate;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MessageService 통합 테스트")
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private RedisAiChatManager redisAiChatManager;

    @MockitoBean
    private RedisSessionManager redisSessionManager;

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
    @DisplayName("메시지 생성")
    class CreateMessage {

        @Test
        @DisplayName("성공: 유효한 메시지 생성")
        void createMessage_Success() {
            // when
            MessageResponse.History response = messageService.createMessage(
                    testConversation.getUserId(),
                    SenderType.USER,
                    "새로운 메시지"
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("새로운 메시지");
            assertThat(response.senderType()).isEqualTo(SenderType.USER);
            assertThat(response.conversationId()).isEqualTo(testConversation.getId());
        }

        @Test
        @DisplayName("성공: 새로운 사용자로 대화 생성")
        void createMessage_NewUser() {
            // given
            Long newUserId = 999L;

            // when
            MessageResponse.History response = messageService.createMessage(
                    newUserId,
                    SenderType.USER,
                    "새 사용자의 첫 메시지"
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("새 사용자의 첫 메시지");
            assertThat(response.senderType()).isEqualTo(SenderType.USER);
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class GetMessage {

        @Test
        @DisplayName("성공: 메시지 조회")
        void getMessage_Success() {
            // when
            MessageResponse.History result = messageService.getMessage(testMessage.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testMessage.getId());
            assertThat(result.conversationId()).isEqualTo(testConversation.getId());
            assertThat(result.senderType()).isEqualTo(SenderType.USER);
            assertThat(result.content()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("실패: 메시지를 찾을 수 없음")
        void getMessage_MessageNotFound() {
            // given
            Long nonExistentMessageId = 999L;

            // when & then
            assertThatThrownBy(() -> messageService.getMessage(nonExistentMessageId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMessagesByConversationId 테스트")
    class GetMessagesByConversationIdTest {

        @Test
        @DisplayName("성공: 사용자 ID로 메시지 목록 조회")
        void getMessagesByUserId_Success() {
            // when
            List<MessageResponse.History> result = messageService.getMessagesByUserId(testConversation.getUserId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(testMessage.getId());
            assertThat(result.get(0).conversationId()).isEqualTo(testConversation.getId());
            assertThat(result.get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.get(0).content()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("성공: 존재하지 않는 사용자 ID로 빈 목록 조회")
        void getMessagesByUserId_EmptyResult() {
            // given
            Long nonExistentUserId = 999L;

            // when
            List<MessageResponse.History> result = messageService.getMessagesByUserId(nonExistentUserId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMessagesByUserIdWithPaging 테스트")
    class GetMessagesByUserIdWithPagingTest {

        @Test
        @DisplayName("성공: 페이징된 메시지 조회 (오름차순)")
        void getMessagesByUserIdWithPaging_Success_Ascending() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MessageResponse.History> result = messageService.getMessagesByUserIdWithPaging(testConversation.getUserId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(testMessage.getId());
            assertThat(result.getContent().get(0).conversationId()).isEqualTo(testConversation.getId());
            assertThat(result.getContent().get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.getContent().get(0).content()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("성공: 존재하지 않는 사용자 ID로 빈 페이지 조회")
        void getMessagesByUserIdWithPaging_EmptyResult() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Long nonExistentUserId = 999L;

            // when
            Page<MessageResponse.History> result = messageService.getMessagesByUserIdWithPaging(nonExistentUserId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getMessagesRecentFirst 테스트")
    class GetMessagesRecentFirstTest {

        @Test
        @DisplayName("성공: 최근 메시지 조회 (내림차순)")
        void getMessagesRecentFirst_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MessageResponse.History> result = messageService.getMessagesRecentFirst(testConversation.getUserId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(testMessage.getId());
            assertThat(result.getContent().get(0).conversationId()).isEqualTo(testConversation.getId());
            assertThat(result.getContent().get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.getContent().get(0).content()).isEqualTo("테스트 메시지");
        }
    }

    @Nested
    @DisplayName("searchMessages 테스트")
    class SearchMessagesTest {

        @Test
        @DisplayName("성공: 키워드로 메시지 검색")
        void searchMessages_Success() {
            // given
            String keyword = "테스트";

            // when
            List<MessageResponse.History> result = messageService.searchMessages(testConversation.getUserId(), keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(testMessage.getId());
            assertThat(result.get(0).conversationId()).isEqualTo(testConversation.getId());
            assertThat(result.get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.get(0).content()).isEqualTo("테스트 메시지");
            assertThat(result.get(0).content()).contains(keyword);
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("성공: 메시지 삭제")
        void deleteMessage_Success() {
            // when
            messageService.deleteMessage(testMessage.getId());

            // then
            assertThatThrownBy(() -> messageService.getMessage(testMessage.getId()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("실패: 메시지를 찾을 수 없음")
        void deleteMessage_MessageNotFound() {
            // given
            Long nonExistentMessageId = 999L;

            // when & then
            assertThatThrownBy(() -> messageService.deleteMessage(nonExistentMessageId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
