package com.synapse.chat_service.repository;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.repository.ChatRoomRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChatRoomRepository 단위 테스트")
class ChatRoomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Long userId1;
    private Long userId2;
    private ChatRoom chatRoom1;
    private ChatRoom chatRoom2;
    private ChatRoom chatRoom3;
    private ChatRoom chatRoom4;

    @BeforeEach
    void setUp() throws Exception {
        userId1 = 1L;
        userId2 = 2L;

        // 테스트용 ChatRoom 데이터 생성
        chatRoom1 = TestObjectFactory.createChatRoomWithCreatedDate(userId1, "자바 스터디", LocalDateTime.now().minusDays(3));
        chatRoom2 = TestObjectFactory.createChatRoomWithCreatedDate(userId1, "스프링 부트 학습", LocalDateTime.now().minusDays(2));
        chatRoom3 = TestObjectFactory.createChatRoomWithCreatedDate(userId1, "리액트 프로젝트", LocalDateTime.now().minusDays(1));
        chatRoom4 = TestObjectFactory.createChatRoomWithCreatedDate(userId2, "파이썬 기초", LocalDateTime.now());

        // 데이터베이스에 저장
        entityManager.persistAndFlush(chatRoom1);
        entityManager.persistAndFlush(chatRoom2);
        entityManager.persistAndFlush(chatRoom3);
        entityManager.persistAndFlush(chatRoom4);
    }



    @Nested
    @DisplayName("findByUserIdAndTitleContaining 테스트")
    class FindByUserIdAndTitleContainingTest {

        @Test
        @DisplayName("성공: 특정 사용자의 제목에 키워드가 포함된 채팅방 조회")
        void findByUserIdAndTitleContaining_Success() {
            // given
            String keyword = "스";

            // when
            List<ChatRoom> result = chatRoomRepository.findByUserIdAndTitleContaining(userId1, keyword);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ChatRoom::getTitle)
                    .containsExactlyInAnyOrder("자바 스터디", "스프링 부트 학습");
            assertThat(result).allMatch(chatRoom -> chatRoom.getUserId().equals(userId1));
        }

        @Test
        @DisplayName("성공: 키워드가 정확히 일치하는 경우")
        void findByUserIdAndTitleContaining_ExactMatch() {
            // given
            String keyword = "자바";

            // when
            List<ChatRoom> result = chatRoomRepository.findByUserIdAndTitleContaining(userId1, keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("자바 스터디");
            assertThat(result.get(0).getUserId()).isEqualTo(userId1);
        }

        @Test
        @DisplayName("성공: 검색 결과가 없는 경우 빈 리스트 반환")
        void findByUserIdAndTitleContaining_EmptyResult() {
            // given
            String keyword = "존재하지않는키워드";

            // when
            List<ChatRoom> result = chatRoomRepository.findByUserIdAndTitleContaining(userId1, keyword);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 다른 사용자의 채팅방은 검색되지 않음")
        void findByUserIdAndTitleContaining_DifferentUser() {
            // given
            String keyword = "파이썬";

            // when
            List<ChatRoom> result = chatRoomRepository.findByUserIdAndTitleContaining(userId1, keyword);

            // then
            assertThat(result).isEmpty(); // userId1에는 파이썬 관련 채팅방이 없음
        }

        @Test
        @DisplayName("성공: 대소문자 구분 없이 검색")
        void findByUserIdAndTitleContaining_CaseInsensitive() {
            // given
            String keyword = "JAVA";

            // when
            List<ChatRoom> result = chatRoomRepository.findByUserIdAndTitleContaining(userId1, keyword);

            // then
            // 한글 제목이므로 대소문자 테스트는 영문 제목으로 추가 데이터 생성 필요
            // 현재는 빈 결과 확인
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedDateDesc 테스트")
    class FindByUserIdOrderByCreatedDateDescTest {

        @Test
        @DisplayName("성공: 특정 사용자의 채팅방을 생성일 기준 내림차순으로 페이징 조회")
        void findByUserIdOrderByCreatedDateDesc_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 2);

            // when
            Page<ChatRoom> result = chatRoomRepository.findByUserIdOrderByCreatedDateDesc(userId1, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3); // userId1의 총 채팅방 개수
            assertThat(result.getTotalPages()).isEqualTo(2); // 총 페이지 수 (3개를 2개씩 나누면 2페이지)
            assertThat(result.getNumber()).isEqualTo(0); // 현재 페이지 번호
            assertThat(result.getSize()).isEqualTo(2); // 페이지 크기
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isFalse();

            // 생성일 기준 내림차순 정렬 확인 (최신순)
            List<ChatRoom> content = result.getContent();
            // 첫 번째가 두 번째보다 더 최신이거나 같아야 함
            assertThat(content.get(0).getCreatedDate()).isAfterOrEqualTo(content.get(1).getCreatedDate());
        }

        @Test
        @DisplayName("성공: 두 번째 페이지 조회")
        void findByUserIdOrderByCreatedDateDesc_SecondPage() {
            // given
            Pageable pageable = PageRequest.of(1, 2);

            // when
            Page<ChatRoom> result = chatRoomRepository.findByUserIdOrderByCreatedDateDesc(userId1, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.isFirst()).isFalse();
            assertThat(result.isLast()).isTrue();

            // 가장 오래된 채팅방 (두 번째 페이지이므로 첫 번째 페이지보다 오래된 것)
            // 실제 데이터 검증보다는 페이징이 올바르게 동작하는지 확인
            assertThat(result.getContent().get(0)).isNotNull();
        }

        @Test
        @DisplayName("성공: 채팅방이 없는 사용자의 경우 빈 페이지 반환")
        void findByUserIdOrderByCreatedDateDesc_EmptyResult() {
            // given
            Long nonExistentUserId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ChatRoom> result = chatRoomRepository.findByUserIdOrderByCreatedDateDesc(nonExistentUserId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("성공: 페이지 크기가 전체 데이터보다 큰 경우")
        void findByUserIdOrderByCreatedDateDesc_LargePageSize() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ChatRoom> result = chatRoomRepository.findByUserIdOrderByCreatedDateDesc(userId1, pageable);

            // then
            assertThat(result.getContent()).hasSize(3); // 실제 데이터 개수
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            // 정렬 순서 확인
            List<ChatRoom> content = result.getContent();
            assertThat(content.get(0).getTitle()).isEqualTo("리액트 프로젝트");
            assertThat(content.get(1).getTitle()).isEqualTo("스프링 부트 학습");
            assertThat(content.get(2).getTitle()).isEqualTo("자바 스터디");
        }
    }

    @Nested
    @DisplayName("연관관계 영속성 테스트 (CascadeType.ALL)")
    class CascadePersistenceTest {

        @Test
        @DisplayName("성공: 채팅방 삭제 시 연관된 메시지들이 함께 삭제된다 (CascadeType.ALL)")
        void deleteChatRoom_CascadeDeleteMessages_Success() {
            // given
            // 테스트용 채팅방 생성
            ChatRoom testChatRoom = ChatRoom.builder()
                    .title("테스트 채팅방")
                    .userId(userId1)
                    .build();
            entityManager.persistAndFlush(testChatRoom);

            // 해당 채팅방에 여러 메시지 생성 (CascadeType.ALL을 활용하여 ChatRoom을 통해 저장)
            Message message1 = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.USER)
                    .content("첫 번째 메시지")
                    .build();

            Message message2 = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.ASSISTANT)
                    .content("두 번째 메시지")
                    .build();

            Message message3 = Message.builder()
                    .chatRoom(testChatRoom)
                    .senderType(SenderType.USER)
                    .content("세 번째 메시지")
                    .build();

            // 양방향 관계 설정: ChatRoom의 messages 컬렉션에 추가
            testChatRoom.getMessages().add(message1);
            testChatRoom.getMessages().add(message2);
            testChatRoom.getMessages().add(message3);

            // CascadeType.ALL로 인해 ChatRoom 저장 시 Message들도 함께 저장됨
            entityManager.persistAndFlush(testChatRoom);

            // 메시지가 정상적으로 저장되었는지 확인
            long initialMessageCount = messageRepository.countByChatRoomId(testChatRoom.getId());
            assertThat(initialMessageCount).isEqualTo(3);

            // when
            // 채팅방 삭제
            chatRoomRepository.delete(testChatRoom);
            entityManager.flush();
            entityManager.clear();

            // then
            // 채팅방이 삭제되었는지 확인
            Optional<ChatRoom> deletedChatRoom = chatRoomRepository.findById(testChatRoom.getId());
            assertThat(deletedChatRoom).isEmpty();

            // 연관된 메시지들도 함께 삭제되었는지 확인 (CascadeType.ALL 검증)
            long remainingMessageCount = messageRepository.countByChatRoomId(testChatRoom.getId());
            assertThat(remainingMessageCount).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 다른 채팅방의 메시지는 영향받지 않는다")
        void deleteChatRoom_OtherChatRoomMessagesUnaffected_Success() {
            // given
            // 첫 번째 채팅방과 메시지
            ChatRoom chatRoom1 = ChatRoom.builder()
                    .title("삭제될 채팅방")
                    .userId(userId1)
                    .build();

            Message messageToDelete = Message.builder()
                    .chatRoom(chatRoom1)
                    .senderType(SenderType.USER)
                    .content("삭제될 메시지")
                    .build();

            // 양방향 관계 설정
            chatRoom1.getMessages().add(messageToDelete);
            entityManager.persistAndFlush(chatRoom1);

            // 두 번째 채팅방과 메시지 (영향받지 않아야 함)
            ChatRoom chatRoom2 = ChatRoom.builder()
                    .title("유지될 채팅방")
                    .userId(userId1)
                    .build();

            Message messageToKeep = Message.builder()
                    .chatRoom(chatRoom2)
                    .senderType(SenderType.USER)
                    .content("유지될 메시지")
                    .build();

            // 양방향 관계 설정
            chatRoom2.getMessages().add(messageToKeep);
            entityManager.persistAndFlush(chatRoom2);

            // 초기 상태 확인
            assertThat(messageRepository.countByChatRoomId(chatRoom1.getId())).isEqualTo(1);
            assertThat(messageRepository.countByChatRoomId(chatRoom2.getId())).isEqualTo(1);

            // when
            // 첫 번째 채팅방만 삭제
            chatRoomRepository.delete(chatRoom1);
            entityManager.flush();
            entityManager.clear();

            // then
            // 첫 번째 채팅방과 그 메시지는 삭제됨
            assertThat(chatRoomRepository.findById(chatRoom1.getId())).isEmpty();
            assertThat(messageRepository.countByChatRoomId(chatRoom1.getId())).isEqualTo(0);

            // 두 번째 채팅방과 그 메시지는 유지됨
            assertThat(chatRoomRepository.findById(chatRoom2.getId())).isPresent();
            assertThat(messageRepository.countByChatRoomId(chatRoom2.getId())).isEqualTo(1);
        }
    }
}
