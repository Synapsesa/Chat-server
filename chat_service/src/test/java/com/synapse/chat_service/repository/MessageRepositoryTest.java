package com.synapse.chat_service.repository;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MessageRepository 단위 테스트")
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private ChatRoom chatRoom1;
    private ChatRoom chatRoom2;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() {
        // 테스트용 ChatRoom 데이터 생성
        chatRoom1 = TestObjectFactory.createChatRoom(1L, "자바 스터디");
        chatRoom2 = TestObjectFactory.createChatRoom(2L, "스프링 부트 학습");

        entityManager.persistAndFlush(chatRoom1);
        entityManager.persistAndFlush(chatRoom2);

        // 테스트용 Message 데이터 생성
        message1 = TestObjectFactory.createMessageWithCreatedDate(chatRoom1, SenderType.USER, "안녕하세요! 자바 공부를 시작해봅시다.", LocalDateTime.now().minusHours(4));
        message2 = TestObjectFactory.createMessageWithCreatedDate(chatRoom1, SenderType.ASSISTANT, "자바의 기본 문법에 대해 알아보겠습니다.", LocalDateTime.now().minusHours(3));
        message3 = TestObjectFactory.createMessageWithCreatedDate(chatRoom1, SenderType.USER, "객체지향 프로그래밍의 핵심 개념을 설명해주세요.", LocalDateTime.now().minusHours(2));
        message4 = TestObjectFactory.createMessageWithCreatedDate(chatRoom2, SenderType.USER, "스프링 부트 프로젝트를 생성하는 방법을 알려주세요.", LocalDateTime.now().minusHours(1));
        message5 = TestObjectFactory.createMessageWithCreatedDate(chatRoom2, SenderType.ASSISTANT, "Spring Initializr를 사용하여 프로젝트를 생성할 수 있습니다.", LocalDateTime.now());

        // 데이터베이스에 저장
        entityManager.persistAndFlush(message1);
        entityManager.persistAndFlush(message2);
        entityManager.persistAndFlush(message3);
        entityManager.persistAndFlush(message4);
        entityManager.persistAndFlush(message5);
    }



    @Nested
    @DisplayName("findByChatRoomIdAndContentContaining 테스트")
    class FindByChatRoomIdAndContentContainingTest {

        @Test
        @DisplayName("성공: 특정 채팅방에서 키워드가 포함된 메시지 조회")
        void findByChatRoomIdAndContentContaining_Success() {
            // given
            String keyword = "자바";

            // when
            List<Message> result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom1.getId(), keyword);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Message::getContent)
                    .containsExactlyInAnyOrder(
                            "안녕하세요! 자바 공부를 시작해봅시다.",
                            "자바의 기본 문법에 대해 알아보겠습니다."
                    );
            assertThat(result).allMatch(message -> message.getChatRoom().getId().equals(chatRoom1.getId()));
        }

        @Test
        @DisplayName("성공: 키워드가 정확히 일치하는 경우")
        void findByChatRoomIdAndContentContaining_ExactMatch() {
            // given
            String keyword = "객체지향";

            // when
            List<Message> result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom1.getId(), keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("객체지향 프로그래밍의 핵심 개념을 설명해주세요.");
            assertThat(result.get(0).getChatRoom().getId()).isEqualTo(chatRoom1.getId());
        }

        @Test
        @DisplayName("성공: 검색 결과가 없는 경우 빈 리스트 반환")
        void findByChatRoomIdAndContentContaining_EmptyResult() {
            // given
            String keyword = "존재하지않는키워드";

            // when
            List<Message> result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom1.getId(), keyword);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 다른 채팅방의 메시지는 검색되지 않음")
        void findByChatRoomIdAndContentContaining_DifferentChatRoom() {
            // given
            String keyword = "스프링";

            // when
            List<Message> result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom1.getId(), keyword);

            // then
            assertThat(result).isEmpty(); // chatRoom1에는 스프링 관련 메시지가 없음
        }

        @Test
        @DisplayName("성공: 부분 문자열 검색")
        void findByChatRoomIdAndContentContaining_PartialMatch() {
            // given
            String keyword = "프로그래밍";

            // when
            List<Message> result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom1.getId(), keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).contains("프로그래밍");
        }

        @Test
        @DisplayName("성공: 여러 채팅방에서 같은 키워드 검색")
        void findByChatRoomIdAndContentContaining_MultipleKeywords() {
            // given
            String keyword = "프로젝트";

            // when
            List<Message> chatRoom1Result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom1.getId(), keyword);
            List<Message> chatRoom2Result = messageRepository.findByChatRoomIdAndContentContaining(chatRoom2.getId(), keyword);

            // then
            assertThat(chatRoom1Result).isEmpty(); // chatRoom1에는 "프로젝트" 키워드가 없음
            assertThat(chatRoom2Result).hasSize(2); // chatRoom2에는 "프로젝트" 키워드가 2개 메시지에 있음
            assertThat(chatRoom2Result).extracting(Message::getContent)
                    .allMatch(content -> content.contains("프로젝트"));
        }
    }

    @Nested
    @DisplayName("countByChatRoomId 테스트")
    class CountByChatRoomIdTest {

        @Test
        @DisplayName("성공: 특정 채팅방의 메시지 개수 조회")
        void countByChatRoomId_Success() {
            // when
            long chatRoom1Count = messageRepository.countByChatRoomId(chatRoom1.getId());
            long chatRoom2Count = messageRepository.countByChatRoomId(chatRoom2.getId());

            // then
            assertThat(chatRoom1Count).isEqualTo(3); // chatRoom1에 3개의 메시지
            assertThat(chatRoom2Count).isEqualTo(2); // chatRoom2에 2개의 메시지
        }

        @Test
        @DisplayName("성공: 메시지가 없는 채팅방의 경우 0 반환")
        void countByChatRoomId_EmptyResult() {
            // given
            ChatRoom emptyChatRoom = ChatRoom.builder()
                    .title("빈 채팅방")
                    .userId(3L)
                    .build();
            entityManager.persistAndFlush(emptyChatRoom);

            // when
            long count = messageRepository.countByChatRoomId(emptyChatRoom.getId());

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 존재하지 않는 채팅방 ID의 경우 0 반환")
        void countByChatRoomId_NonExistentChatRoom() {
            // given
            UUID nonExistentChatRoomId = UUID.randomUUID();

            // when
            long count = messageRepository.countByChatRoomId(nonExistentChatRoomId);

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 메시지 추가 후 개수 증가 확인")
        void countByChatRoomId_AfterAddingMessage() {
            // given
            long initialCount = messageRepository.countByChatRoomId(chatRoom1.getId());

            Message newMessage = Message.builder()
                    .content("새로운 메시지입니다.")
                    .senderType(SenderType.USER)
                    .chatRoom(chatRoom1)
                    .build();
            entityManager.persistAndFlush(newMessage);

            // when
            long updatedCount = messageRepository.countByChatRoomId(chatRoom1.getId());

            // then
            assertThat(updatedCount).isEqualTo(initialCount + 1);
            assertThat(updatedCount).isEqualTo(4); // 기존 3개 + 새로 추가된 1개
        }

        @Test
        @DisplayName("성공: 다른 채팅방의 메시지는 카운트에 포함되지 않음")
        void countByChatRoomId_IsolatedCount() {
            // given
            long chatRoom1InitialCount = messageRepository.countByChatRoomId(chatRoom1.getId());
            long chatRoom2InitialCount = messageRepository.countByChatRoomId(chatRoom2.getId());

            // chatRoom2에 새 메시지 추가
            Message newMessage = Message.builder()
                    .content("chatRoom2에 추가된 메시지")
                    .senderType(SenderType.ASSISTANT)
                    .chatRoom(chatRoom2)
                    .build();
            entityManager.persistAndFlush(newMessage);

            // when
            long chatRoom1FinalCount = messageRepository.countByChatRoomId(chatRoom1.getId());
            long chatRoom2FinalCount = messageRepository.countByChatRoomId(chatRoom2.getId());

            // then
            assertThat(chatRoom1FinalCount).isEqualTo(chatRoom1InitialCount); // chatRoom1 개수는 변화 없음
            assertThat(chatRoom2FinalCount).isEqualTo(chatRoom2InitialCount + 1); // chatRoom2 개수만 증가
        }
    }

    @Nested
    @DisplayName("findByChatRoomIdOrderByCreatedDateAsc 페이징 테스트")
    class FindByChatRoomIdOrderByCreatedDateAscTest {

        @Test
        @DisplayName("성공: 시간순(ASC) 정렬이 올바르게 동작")
        void findByChatRoomIdOrderByCreatedDateAsc_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByChatRoomIdOrderByCreatedDateAsc(chatRoom1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            
            // 시간순(ASC) 정렬 확인: 가장 오래된 것부터 (같은 시간일 수도 있으므로 isBeforeOrEqualTo 사용)
            List<Message> messages = result.getContent();
            assertThat(messages.get(0).getCreatedDate()).isBeforeOrEqualTo(messages.get(1).getCreatedDate()); // 첫 번째가 더 오래되거나 같음
            assertThat(messages.get(1).getCreatedDate()).isBeforeOrEqualTo(messages.get(2).getCreatedDate()); // 두 번째가 세 번째보다 오래되거나 같음
        }

        @Test
        @DisplayName("성공: 페이징 처리")
        void findByChatRoomIdOrderByCreatedDateAsc_Paging() {
            // given
            Pageable firstPage = PageRequest.of(0, 2);
            Pageable secondPage = PageRequest.of(1, 2);

            // when
            Page<Message> firstResult = messageRepository.findByChatRoomIdOrderByCreatedDateAsc(chatRoom1.getId(), firstPage);
            Page<Message> secondResult = messageRepository.findByChatRoomIdOrderByCreatedDateAsc(chatRoom1.getId(), secondPage);

            // then
            // 첫 번째 페이지 (가장 오래된 2개)
            assertThat(firstResult.getContent()).hasSize(2);
            assertThat(firstResult.getTotalElements()).isEqualTo(3);
            assertThat(firstResult.getTotalPages()).isEqualTo(2);
            // ASC 정렬이므로 첫 번째 페이지 내에서도 시간순 정렬 확인
            assertThat(firstResult.getContent().get(0).getCreatedDate()).isBeforeOrEqualTo(firstResult.getContent().get(1).getCreatedDate());

            // 두 번째 페이지 (가장 최신 1개)
            assertThat(secondResult.getContent()).hasSize(1);
            assertThat(secondResult.getTotalElements()).isEqualTo(3);
            assertThat(secondResult.getTotalPages()).isEqualTo(2);
            // ASC 정렬에서 두 번째 페이지의 메시지는 첫 번째 페이지의 마지막 메시지보다 더 최신이어야 함
            assertThat(secondResult.getContent().get(0).getCreatedDate()).isAfter(firstResult.getContent().get(1).getCreatedDate());
        }

        @Test
        @DisplayName("성공: 빈 결과 페이지")
        void findByChatRoomIdOrderByCreatedDateAsc_EmptyResult() {
            // given
            UUID nonExistentChatRoomId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByChatRoomIdOrderByCreatedDateAsc(nonExistentChatRoomId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findByChatRoomIdOrderByCreatedDateDesc 페이징 테스트")
    class FindByChatRoomIdOrderByCreatedDateDescTest {

        @Test
        @DisplayName("성공: 최신순(DESC) 정렬이 올바르게 동작")
        void findByChatRoomIdOrderByCreatedDateDesc_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByChatRoomIdOrderByCreatedDateDesc(chatRoom1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            
            // 최신순(DESC) 정렬 확인: 가장 최신 것부터 (같은 시간일 수도 있으므로 isAfterOrEqualTo 사용)
            List<Message> messages = result.getContent();
            assertThat(messages.get(0).getCreatedDate()).isAfterOrEqualTo(messages.get(1).getCreatedDate()); // 첫 번째가 더 최신이거나 같음
            assertThat(messages.get(1).getCreatedDate()).isAfterOrEqualTo(messages.get(2).getCreatedDate()); // 두 번째가 세 번째보다 최신이거나 같음
        }

        @Test
        @DisplayName("성공: 페이징 처리")
        void findByChatRoomIdOrderByCreatedDateDesc_Paging() {
            // given
            Pageable firstPage = PageRequest.of(0, 2);
            Pageable secondPage = PageRequest.of(1, 2);

            // when
            Page<Message> firstResult = messageRepository.findByChatRoomIdOrderByCreatedDateDesc(chatRoom1.getId(), firstPage);
            Page<Message> secondResult = messageRepository.findByChatRoomIdOrderByCreatedDateDesc(chatRoom1.getId(), secondPage);

            // then
            // 첫 번째 페이지 (최신 2개)
            assertThat(firstResult.getContent()).hasSize(2);
            assertThat(firstResult.getTotalElements()).isEqualTo(3);
            assertThat(firstResult.getTotalPages()).isEqualTo(2);
            // DESC 정렬이므로 첫 번째 페이지 내에서도 최신순 정렬 확인
            assertThat(firstResult.getContent().get(0).getCreatedDate()).isAfterOrEqualTo(firstResult.getContent().get(1).getCreatedDate());

            // 두 번째 페이지 (가장 오래된 1개)
            assertThat(secondResult.getContent()).hasSize(1);
            assertThat(secondResult.getTotalElements()).isEqualTo(3);
            assertThat(secondResult.getTotalPages()).isEqualTo(2);
            // DESC 정렬에서 두 번째 페이지의 메시지는 첫 번째 페이지의 마지막 메시지보다 더 오래되어야 함
            assertThat(secondResult.getContent().get(0).getCreatedDate()).isBefore(firstResult.getContent().get(1).getCreatedDate());
        }

        @Test
        @DisplayName("성공: 다른 채팅방과 격리된 결과")
        void findByChatRoomIdOrderByCreatedDateDesc_IsolatedResult() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> chatRoom1Result = messageRepository.findByChatRoomIdOrderByCreatedDateDesc(chatRoom1.getId(), pageable);
            Page<Message> chatRoom2Result = messageRepository.findByChatRoomIdOrderByCreatedDateDesc(chatRoom2.getId(), pageable);

            // then
            assertThat(chatRoom1Result.getContent()).hasSize(3);
            assertThat(chatRoom2Result.getContent()).hasSize(2);
            
            // 각 채팅방의 메시지만 포함되는지 확인
            assertThat(chatRoom1Result.getContent()).allMatch(message -> 
                message.getChatRoom().getId().equals(chatRoom1.getId()));
            assertThat(chatRoom2Result.getContent()).allMatch(message -> 
                message.getChatRoom().getId().equals(chatRoom2.getId()));
        }
    }
}
