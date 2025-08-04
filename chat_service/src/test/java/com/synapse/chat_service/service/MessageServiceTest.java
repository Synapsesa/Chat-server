package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.exception.commonexception.NotFoundException;
import com.synapse.chat_service.domain.repository.ChatRoomRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 단위 테스트")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private MessageService messageService;

    private ChatRoom testChatRoom;
    private Message testMessage;
    private UUID testChatRoomId;
    private Long testMessageId;

    @BeforeEach
    void setUp() {
        testChatRoomId = UUID.randomUUID();
        testMessageId = 1L;

        testChatRoom = TestObjectFactory.createChatRoomWithId(testChatRoomId, 1L, "테스트 채팅방");
        testMessage = TestObjectFactory.createUserMessageWithId(testMessageId, testChatRoom, "테스트 메시지");
    }

    @Nested
    @DisplayName("createMessage 테스트")
    class CreateMessageTest {

        @Test
        @DisplayName("성공: 메시지 생성")
        void createMessage_Success() {
            // given
            MessageRequest.Create request = new MessageRequest.Create(testChatRoomId, SenderType.USER, "새 메시지");
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

            // when
            MessageResponse.Detail result = messageService.createMessage(request);

            // then
            assertThat(result.id()).isEqualTo(testMessageId);
            assertThat(result.chatRoomId()).isEqualTo(testChatRoomId);
            assertThat(result.senderType()).isEqualTo(SenderType.USER);
            assertThat(result.content()).isEqualTo("테스트 메시지");
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, times(1)).save(any(Message.class));
        }

        @Test
        @DisplayName("실패: 채팅방을 찾을 수 없음")
        void createMessage_Fail_ChatRoomNotFound() {
            // given
            MessageRequest.Create request = new MessageRequest.Create(testChatRoomId, SenderType.USER, "새 메시지");
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> messageService.createMessage(request));
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMessage 테스트")
    class GetMessageTest {

        @Test
        @DisplayName("성공: 메시지 조회")
        void getMessage_Success() {
            // given
            when(messageRepository.findById(testMessageId)).thenReturn(Optional.of(testMessage));

            // when
            MessageResponse.Detail result = messageService.getMessage(testMessageId);

            // then
            assertThat(result.id()).isEqualTo(testMessageId);
            assertThat(result.chatRoomId()).isEqualTo(testChatRoomId);
            assertThat(result.senderType()).isEqualTo(SenderType.USER);
            assertThat(result.content()).isEqualTo("테스트 메시지");
            verify(messageRepository, times(1)).findById(testMessageId);
        }

        @Test
        @DisplayName("실패: 메시지를 찾을 수 없음")
        void getMessage_Fail_NotFound() {
            // given
            when(messageRepository.findById(testMessageId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> messageService.getMessage(testMessageId));
            verify(messageRepository, times(1)).findById(testMessageId);
        }
    }

    @Nested
    @DisplayName("getMessagesByChatRoomId 테스트")
    class GetMessagesByChatRoomIdTest {

        @Test
        @DisplayName("성공: 채팅방별 메시지 조회")
        void getMessagesByChatRoomId_Success() {
            // given
            List<Message> messages = Arrays.asList(testMessage);
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.findByChatRoomIdOrderByCreatedDateAsc(testChatRoomId)).thenReturn(messages);

            // when
            List<MessageResponse.Simple> result = messageService.getMessagesByChatRoomId(testChatRoomId);

            // then (기존 검증)
            assertThat(result).hasSize(1);
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, times(1)).findByChatRoomIdOrderByCreatedDateAsc(testChatRoomId);
            
            // then (개선된 DTO 변환 로직 검증)
            assertThat(result.get(0).id()).isEqualTo(testMessageId);
            assertThat(result.get(0).chatRoomId()).isEqualTo(testChatRoomId);
            assertThat(result.get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.get(0).content()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("실패: 채팅방을 찾을 수 없음")
        void getMessagesByChatRoomId_Fail_ChatRoomNotFound() {
            // given
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> messageService.getMessagesByChatRoomId(testChatRoomId));
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, never()).findByChatRoomIdOrderByCreatedDateAsc(any());
        }
    }

    @Nested
    @DisplayName("getMessagesByChatRoomIdWithPaging 테스트")
    class GetMessagesByChatRoomIdWithPagingTest {

        @Test
        @DisplayName("성공: 페이징된 메시지 조회 (오름차순)")
        void getMessagesByChatRoomIdWithPaging_Success_Ascending() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Message> messages = Arrays.asList(testMessage);
            Page<Message> messagePage = new PageImpl<>(messages, pageable, 1);
            
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.findByChatRoomIdOrderByCreatedDateAsc(testChatRoomId, pageable)).thenReturn(messagePage);

            // when
            Page<MessageResponse.Simple> result = messageService.getMessagesByChatRoomIdWithPaging(testChatRoomId, pageable);

            // then (기존 검증)
            assertThat(result.getContent()).hasSize(1);
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, times(1)).findByChatRoomIdOrderByCreatedDateAsc(testChatRoomId, pageable);
            
            // then (개선된 DTO 변환 로직 검증)
            assertThat(result.getContent().get(0).id()).isEqualTo(testMessageId);
            assertThat(result.getContent().get(0).chatRoomId()).isEqualTo(testChatRoomId);
            assertThat(result.getContent().get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.getContent().get(0).content()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("실패: 채팅방을 찾을 수 없음")
        void getMessagesByChatRoomIdWithPaging_Fail_ChatRoomNotFound() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> 
                messageService.getMessagesByChatRoomIdWithPaging(testChatRoomId, pageable));
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, never()).findByChatRoomIdOrderByCreatedDateAsc(any(), any());
        }
    }

    @Nested
    @DisplayName("getRecentMessagesByChatRoomId 테스트")
    class GetRecentMessagesByChatRoomIdTest {

        @Test
        @DisplayName("성공: 최근 메시지 조회 (내림차순)")
        void getRecentMessagesByChatRoomId_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Message> messages = Arrays.asList(testMessage);
            Page<Message> messagePage = new PageImpl<>(messages, pageable, 1);
            
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.findByChatRoomIdOrderByCreatedDateDesc(testChatRoomId, pageable)).thenReturn(messagePage);

            // when
            Page<MessageResponse.Simple> result = messageService.getMessagesRecentFirst(testChatRoomId, pageable);

            // then (기존 검증)
            assertThat(result.getContent()).hasSize(1);
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, times(1)).findByChatRoomIdOrderByCreatedDateDesc(testChatRoomId, pageable);
            
            // then (개선된 DTO 변환 로직 검증)
            assertThat(result.getContent().get(0).id()).isEqualTo(testMessageId);
            assertThat(result.getContent().get(0).chatRoomId()).isEqualTo(testChatRoomId);
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
            List<Message> messages = Arrays.asList(testMessage);
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.findByChatRoomIdAndContentContaining(testChatRoomId, keyword))
                    .thenReturn(messages);

            // when
            List<MessageResponse.Simple> result = messageService.searchMessages(testChatRoomId, keyword);

            // then (기존 검증)
            assertThat(result).hasSize(1);
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, times(1)).findByChatRoomIdAndContentContaining(testChatRoomId, keyword);
            
            // then (개선된 DTO 변환 로직 검증)
            assertThat(result.get(0).id()).isEqualTo(testMessageId);
            assertThat(result.get(0).chatRoomId()).isEqualTo(testChatRoomId);
            assertThat(result.get(0).senderType()).isEqualTo(SenderType.USER);
            assertThat(result.get(0).content()).isEqualTo("테스트 메시지");
            assertThat(result.get(0).content()).contains(keyword);
        }
    }

    @Nested
    @DisplayName("deleteMessage 테스트")
    class DeleteMessageTest {

        @Test
        @DisplayName("성공: 메시지 삭제")
        void deleteMessage_Success() {
            // given
            when(messageRepository.findById(testMessageId)).thenReturn(Optional.of(testMessage));

            // when
            messageService.deleteMessage(testMessageId);

            // then
            verify(messageRepository, times(1)).findById(testMessageId);
            verify(messageRepository, times(1)).delete(testMessage);
        }

        @Test
        @DisplayName("실패: 삭제할 메시지를 찾을 수 없음")
        void deleteMessage_Fail_NotFound() {
            // given
            when(messageRepository.findById(testMessageId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> messageService.deleteMessage(testMessageId));
            verify(messageRepository, times(1)).findById(testMessageId);
            verify(messageRepository, never()).delete(any());
        }
    }
}
