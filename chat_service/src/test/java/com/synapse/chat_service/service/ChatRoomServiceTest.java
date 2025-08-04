package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.dto.request.ChatRoomRequest;
import com.synapse.chat_service.dto.response.ChatRoomResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private ChatRoom testChatRoom;
    private UUID testChatRoomId;

    @BeforeEach
    void setUp() {
        testChatRoomId = UUID.randomUUID();
        testChatRoom = TestObjectFactory.createChatRoomWithId(testChatRoomId, 1L, "테스트 채팅방");
    }

    @Nested
    @DisplayName("createChatRoom 테스트")
    class CreateChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 생성")
        void createChatRoom_Success() {
            // given
            ChatRoomRequest.Create request = new ChatRoomRequest.Create(1L, "새 채팅방");
            ChatRoom savedChatRoom = TestObjectFactory.createChatRoomWithId(testChatRoomId, 1L, "새 채팅방");
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedChatRoom);

            // when
            ChatRoomResponse.Detail result = chatRoomService.createChatRoom(request);

            // then
            assertThat(result.id()).isEqualTo(testChatRoomId);
            assertThat(result.title()).isEqualTo("새 채팅방");
            assertThat(result.userId()).isEqualTo(1L);
            verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        }
    }

    @Nested
    @DisplayName("getChatRoom 테스트")
    class GetChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 조회")
        void getChatRoom_Success() {
            // given
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.countByChatRoomId(testChatRoomId)).thenReturn(5L);

            // when
            ChatRoomResponse.Detail result = chatRoomService.getChatRoom(testChatRoomId);

            // then
            assertThat(result.id()).isEqualTo(testChatRoomId);
            assertThat(result.title()).isEqualTo("테스트 채팅방");
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.messageCount()).isEqualTo(5L);
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, times(1)).countByChatRoomId(testChatRoomId);
        }

        @Test
        @DisplayName("실패: 채팅방을 찾을 수 없음")
        void getChatRoom_Fail_NotFound() {
            // given
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> chatRoomService.getChatRoom(testChatRoomId));
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(messageRepository, never()).countByChatRoomId(any());
        }
    }

    @Nested
    @DisplayName("updateChatRoom 테스트")
    class UpdateChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 제목 수정")
        void updateChatRoom_Success() {
            // given
            ChatRoomRequest.Update request = new ChatRoomRequest.Update("수정된 제목");
            ChatRoom updatedChatRoom = TestObjectFactory.createChatRoomWithId(testChatRoomId, 1L, "수정된 제목");

            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(updatedChatRoom);

            // when
            ChatRoomResponse.Detail result = chatRoomService.updateChatRoom(testChatRoomId, request);

            // then
            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.userId()).isEqualTo(1L);
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("실패: 수정할 채팅방을 찾을 수 없음")
        void updateChatRoom_Fail_NotFound() {
            // given
            ChatRoomRequest.Update request = new ChatRoomRequest.Update("수정된 제목");
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> chatRoomService.updateChatRoom(testChatRoomId, request));
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(chatRoomRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteChatRoom 테스트")
    class DeleteChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 삭제")
        void deleteChatRoom_Success() {
            // given
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.of(testChatRoom));

            // when
            chatRoomService.deleteChatRoom(testChatRoomId);

            // then
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(chatRoomRepository, times(1)).delete(testChatRoom);
        }

        @Test
        @DisplayName("실패: 삭제할 채팅방을 찾을 수 없음")
        void deleteChatRoom_Fail_NotFound() {
            // given
            when(chatRoomRepository.findById(testChatRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundException.class, () -> chatRoomService.deleteChatRoom(testChatRoomId));
            verify(chatRoomRepository, times(1)).findById(testChatRoomId);
            verify(chatRoomRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getChatRoomsByUserId 테스트")
    class GetChatRoomsByUserIdTest {

        @Test
        @DisplayName("성공: 페이징을 통한 채팅방 조회")
        void getChatRoomsByUserId_Success() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 2);
            ChatRoom chatRoom1 = TestObjectFactory.createChatRoom(userId, "채팅방 1");
            ChatRoom chatRoom2 = TestObjectFactory.createChatRoom(userId, "채팅방 2");
            List<ChatRoom> chatRooms = Arrays.asList(chatRoom1, chatRoom2);
            Page<ChatRoom> chatRoomPage = new PageImpl<>(chatRooms, pageable, 2);

            when(chatRoomRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable)).thenReturn(chatRoomPage);
            when(messageRepository.countByChatRoomId(any())).thenReturn(3L);

            // when
            Page<ChatRoomResponse.Simple> result = chatRoomService.getChatRoomsByUserId(userId, pageable);

            // then (기존 검증)
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(chatRoomRepository, times(1)).findByUserIdOrderByCreatedDateDesc(userId, pageable);
            verify(messageRepository, times(2)).countByChatRoomId(any());
            
            // then (개선된 DTO 변환 로직 검증)
            assertThat(result.getContent().get(0).title()).isEqualTo("채팅방 1");
            assertThat(result.getContent().get(0).messageCount()).isEqualTo(3L);
            assertThat(result.getContent().get(1).title()).isEqualTo("채팅방 2");
            assertThat(result.getContent().get(1).messageCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("성공: 빈 페이지 반환")
        void getChatRoomsByUserId_EmptyPage() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<ChatRoom> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(chatRoomRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable)).thenReturn(emptyPage);

            // when
            Page<ChatRoomResponse.Simple> result = chatRoomService.getChatRoomsByUserId(userId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            verify(chatRoomRepository, times(1)).findByUserIdOrderByCreatedDateDesc(userId, pageable);
            verify(messageRepository, never()).countByChatRoomId(any());
        }
    }

    @Nested
    @DisplayName("searchChatRooms 테스트")
    class SearchChatRoomsTest {

        @Test
        @DisplayName("성공: 키워드로 채팅방 검색")
        void searchChatRooms_Success() {
            // given
            Long userId = 1L;
            String keyword = "Java";
            ChatRoom chatRoom1 = TestObjectFactory.createChatRoom(userId, "Java 스터디");
            ChatRoom chatRoom2 = TestObjectFactory.createChatRoom(userId, "JavaScript 프로젝트");
            List<ChatRoom> searchResults = Arrays.asList(chatRoom1, chatRoom2);

            when(chatRoomRepository.findByUserIdAndTitleContaining(userId, keyword)).thenReturn(searchResults);
            when(messageRepository.countByChatRoomId(any())).thenReturn(5L);

            // when
            List<ChatRoomResponse.Simple> result = chatRoomService.searchChatRooms(userId, keyword);

            // then (기존 검증)
            assertThat(result).hasSize(2);
            verify(chatRoomRepository, times(1)).findByUserIdAndTitleContaining(userId, keyword);
            verify(messageRepository, times(2)).countByChatRoomId(any());
            
            // then (개선된 DTO 변환 로직 검증)
            assertThat(result.get(0).title()).isEqualTo("Java 스터디");
            assertThat(result.get(0).messageCount()).isEqualTo(5L);
            
            assertThat(result.get(1).title()).isEqualTo("JavaScript 프로젝트");
            assertThat(result.get(1).messageCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("성공: 검색 결과가 없는 경우 빈 리스트 반환")
        void searchChatRooms_EmptyResult() {
            // given
            Long userId = 1L;
            String keyword = "Python";
            when(chatRoomRepository.findByUserIdAndTitleContaining(userId, keyword)).thenReturn(Collections.emptyList());

            // when
            List<ChatRoomResponse.Simple> result = chatRoomService.searchChatRooms(userId, keyword);

            // then
            assertThat(result).isEmpty();
            verify(chatRoomRepository, times(1)).findByUserIdAndTitleContaining(userId, keyword);
            verify(messageRepository, never()).countByChatRoomId(any());
        }

        @Test
        @DisplayName("성공: 빈 키워드로 검색")
        void searchChatRooms_EmptyKeyword() {
            // given
            Long userId = 1L;
            String keyword = "";
            ChatRoom chatRoom = TestObjectFactory.createChatRoom(userId, "테스트 채팅방");
            List<ChatRoom> allChatRooms = Arrays.asList(chatRoom);

            when(chatRoomRepository.findByUserIdAndTitleContaining(userId, keyword)).thenReturn(allChatRooms);
            when(messageRepository.countByChatRoomId(any())).thenReturn(2L);

            // when
            List<ChatRoomResponse.Simple> result = chatRoomService.searchChatRooms(userId, keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("테스트 채팅방");
            verify(chatRoomRepository, times(1)).findByUserIdAndTitleContaining(userId, keyword);
            verify(messageRepository, times(1)).countByChatRoomId(any());
        }
    }
}
