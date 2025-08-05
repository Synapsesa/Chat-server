package com.synapse.chat_service.repository;

import com.synapse.chat_service.domain.entity.Conversation;
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

    private Conversation conversation1;
    private Conversation conversation2;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() {
        // 테스트용 Conversation 데이터 생성
        conversation1 = TestObjectFactory.createConversation(1L);
        conversation2 = TestObjectFactory.createConversation(2L);

        entityManager.persistAndFlush(conversation1);
        entityManager.persistAndFlush(conversation2);

        // 고정된 기준 시간 사용 (CI 환경에서의 안정성을 위해)
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        // 테스트용 Message 데이터 생성
        message1 = TestObjectFactory.createMessageWithCreatedDate(conversation1, SenderType.USER, "안녕하세요! 자바 공부를 시작해봅시다.", baseTime.minusHours(4));
        message2 = TestObjectFactory.createMessageWithCreatedDate(conversation1, SenderType.ASSISTANT, "자바의 기본 문법에 대해 알아보겠습니다.", baseTime.minusHours(3));
        message3 = TestObjectFactory.createMessageWithCreatedDate(conversation1, SenderType.USER, "객체지향 프로그래밍의 핵심 개념을 설명해주세요.", baseTime.minusHours(2));
        message4 = TestObjectFactory.createMessageWithCreatedDate(conversation2, SenderType.USER, "스프링 부트 프로젝트를 생성하는 방법을 알려주세요.", baseTime.minusHours(1));
        message5 = TestObjectFactory.createMessageWithCreatedDate(conversation2, SenderType.ASSISTANT, "Spring Initializr를 사용하여 프로젝트를 생성할 수 있습니다.", baseTime);

        // 데이터베이스에 저장
        entityManager.persistAndFlush(message1);
        entityManager.persistAndFlush(message2);
        entityManager.persistAndFlush(message3);
        entityManager.persistAndFlush(message4);
        entityManager.persistAndFlush(message5);
    }

    @Nested
    @DisplayName("findByConversationIdAndContentContaining 테스트")
    class FindByConversationIdAndContentContainingTest {

        @Test
        @DisplayName("성공: 특정 대화에서 키워드가 포함된 메시지 조회")
        void findByConversationIdAndContentContaining_Success() {
            // given
            String keyword = "자바";

            // when
            List<Message> result = messageRepository.findByConversationIdAndContentContaining(conversation1.getId(), keyword);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Message::getContent)
                    .containsExactlyInAnyOrder(
                            "안녕하세요! 자바 공부를 시작해봅시다.",
                            "자바의 기본 문법에 대해 알아보겠습니다."
                    );
            assertThat(result).allMatch(message -> message.getConversation().getId().equals(conversation1.getId()));
        }

        @Test
        @DisplayName("성공: 키워드가 정확히 일치하는 경우")
        void findByConversationIdAndContentContaining_ExactMatch() {
            // given
            String keyword = "객체지향";

            // when
            List<Message> result = messageRepository.findByConversationIdAndContentContaining(conversation1.getId(), keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("객체지향 프로그래밍의 핵심 개념을 설명해주세요.");
            assertThat(result.get(0).getConversation().getId()).isEqualTo(conversation1.getId());
        }

        @Test
        @DisplayName("성공: 검색 결과가 없는 경우 빈 리스트 반환")
        void findByConversationIdAndContentContaining_EmptyResult() {
            // given
            String keyword = "존재하지않는키워드";

            // when
            List<Message> result = messageRepository.findByConversationIdAndContentContaining(conversation1.getId(), keyword);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 다른 대화의 메시지는 검색되지 않음")
        void findByConversationIdAndContentContaining_DifferentConversation() {
            // given
            String keyword = "스프링";

            // when
            List<Message> result = messageRepository.findByConversationIdAndContentContaining(conversation1.getId(), keyword);

            // then
            assertThat(result).isEmpty(); // conversation1에는 스프링 관련 메시지가 없음
        }

        @Test
        @DisplayName("성공: 부분 문자열 검색")
        void findByConversationIdAndContentContaining_PartialMatch() {
            // given
            String keyword = "프로그래밍";

            // when
            List<Message> result = messageRepository.findByConversationIdAndContentContaining(conversation1.getId(), keyword);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).contains("프로그래밍");
        }

        @Test
        @DisplayName("성공: 여러 대화에서 같은 키워드 검색")
        void findByConversationIdAndContentContaining_MultipleKeywords() {
            // given
            String keyword = "프로젝트";

            // when
            List<Message> conversation1Result = messageRepository.findByConversationIdAndContentContaining(conversation1.getId(), keyword);
            List<Message> conversation2Result = messageRepository.findByConversationIdAndContentContaining(conversation2.getId(), keyword);

            // then
            assertThat(conversation1Result).isEmpty(); // conversation1에는 "프로젝트" 키워드가 없음
            assertThat(conversation2Result).hasSize(2); // conversation2에는 "프로젝트" 키워드가 2개 메시지에 있음
            assertThat(conversation2Result).extracting(Message::getContent)
                    .allMatch(content -> content.contains("프로젝트"));
        }
    }

    @Nested
    @DisplayName("countByConversationId 테스트")
    class CountByConversationIdTest {

        @Test
        @DisplayName("성공: 특정 대화의 메시지 개수 조회")
        void countByConversationId_Success() {
            // when
            long conversation1Count = messageRepository.countByConversationId(conversation1.getId());
            long conversation2Count = messageRepository.countByConversationId(conversation2.getId());

            // then
            assertThat(conversation1Count).isEqualTo(3); // conversation1에 3개의 메시지
            assertThat(conversation2Count).isEqualTo(2); // conversation2에 2개의 메시지
        }

        @Test
        @DisplayName("성공: 메시지가 없는 대화의 경우 0 반환")
        void countByConversationId_EmptyResult() {
            // given
            Conversation emptyConversation = Conversation.builder()
                    .userId(3L)
                    .build();
            entityManager.persistAndFlush(emptyConversation);

            // when
            long count = messageRepository.countByConversationId(emptyConversation.getId());

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 존재하지 않는 대화 ID의 경우 0 반환")
        void countByConversationId_NonExistentConversation() {
            // given
            UUID nonExistentConversationId = UUID.randomUUID();

            // when
            long count = messageRepository.countByConversationId(nonExistentConversationId);

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 메시지 추가 후 개수 증가 확인")
        void countByConversationId_AfterAddingMessage() {
            // given
            long initialCount = messageRepository.countByConversationId(conversation1.getId());

            Message newMessage = Message.builder()
                    .content("새로운 메시지입니다.")
                    .senderType(SenderType.USER)
                    .conversation(conversation1)
                    .build();
            entityManager.persistAndFlush(newMessage);

            // when
            long updatedCount = messageRepository.countByConversationId(conversation1.getId());

            // then
            assertThat(updatedCount).isEqualTo(initialCount + 1);
            assertThat(updatedCount).isEqualTo(4); // 기존 3개 + 새로 추가된 1개
        }

        @Test
        @DisplayName("성공: 다른 대화의 메시지는 카운트에 포함되지 않음")
        void countByConversationId_IsolatedCount() {
            // given
            long conversation1InitialCount = messageRepository.countByConversationId(conversation1.getId());
            long conversation2InitialCount = messageRepository.countByConversationId(conversation2.getId());

            // conversation2에 새 메시지 추가
            Message newMessage = Message.builder()
                    .content("conversation2에 추가된 메시지")
                    .senderType(SenderType.ASSISTANT)
                    .conversation(conversation2)
                    .build();
            entityManager.persistAndFlush(newMessage);

            // when
            long conversation1FinalCount = messageRepository.countByConversationId(conversation1.getId());
            long conversation2FinalCount = messageRepository.countByConversationId(conversation2.getId());

            // then
            assertThat(conversation1FinalCount).isEqualTo(conversation1InitialCount); // conversation1 개수는 변화 없음
            assertThat(conversation2FinalCount).isEqualTo(conversation2InitialCount + 1); // conversation2 개수만 증가
        }
    }

    @Nested
    @DisplayName("findByConversationIdOrderByCreatedDateAsc 페이징 테스트")
    class FindByConversationIdOrderByCreatedDateAscTest {

        @Test
        @DisplayName("성공: 시간순(ASC) 정렬이 올바르게 동작")
        void findByConversationIdOrderByCreatedDateAsc_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByConversationIdOrderByCreatedDateAsc(conversation1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("안녕하세요! 자바 공부를 시작해봅시다."); // 가장 오래된
            assertThat(result.getContent().get(1).getContent()).isEqualTo("자바의 기본 문법에 대해 알아보겠습니다.");
            assertThat(result.getContent().get(2).getContent()).isEqualTo("객체지향 프로그래밍의 핵심 개념을 설명해주세요."); // 가장 최근
        }

        @Test
        @DisplayName("성공: 페이징이 올바르게 동작")
        void findByConversationIdOrderByCreatedDateAsc_Paging() {
            // given
            Pageable pageable = PageRequest.of(0, 2); // 페이지 크기 2

            // when
            Page<Message> result = messageRepository.findByConversationIdOrderByCreatedDateAsc(conversation1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByConversationIdOrderByCreatedDateDesc 페이징 테스트")
    class FindByConversationIdOrderByCreatedDateDescTest {

        @Test
        @DisplayName("성공: 시간순(DESC) 정렬이 올바르게 동작")
        void findByConversationIdOrderByCreatedDateDesc_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByConversationIdOrderByCreatedDateDesc(conversation1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("객체지향 프로그래밍의 핵심 개념을 설명해주세요."); // 가장 최근
            assertThat(result.getContent().get(1).getContent()).isEqualTo("자바의 기본 문법에 대해 알아보겠습니다.");
            assertThat(result.getContent().get(2).getContent()).isEqualTo("안녕하세요! 자바 공부를 시작해봅시다."); // 가장 오래된
        }

        @Test
        @DisplayName("성공: 페이징이 올바르게 동작")
        void findByConversationIdOrderByCreatedDateDesc_Paging() {
            // given
            Pageable pageable = PageRequest.of(0, 2); // 페이지 크기 2

            // when
            Page<Message> result = messageRepository.findByConversationIdOrderByCreatedDateDesc(conversation1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByConversationIdOrderByCreatedDateAsc 리스트 테스트")
    class FindByConversationIdOrderByCreatedDateAscListTest {

        @Test
        @DisplayName("성공: 시간순(ASC) 정렬된 전체 메시지 조회")
        void findByConversationIdOrderByCreatedDateAsc_List_Success() {
            // when
            List<Message> result = messageRepository.findByConversationIdOrderByCreatedDateAsc(conversation1.getId());

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getContent()).isEqualTo("안녕하세요! 자바 공부를 시작해봅시다."); // 가장 오래된
            assertThat(result.get(1).getContent()).isEqualTo("자바의 기본 문법에 대해 알아보겠습니다.");
            assertThat(result.get(2).getContent()).isEqualTo("객체지향 프로그래밍의 핵심 개념을 설명해주세요."); // 가장 최근
        }

        @Test
        @DisplayName("성공: 빈 대화의 경우 빈 리스트 반환")
        void findByConversationIdOrderByCreatedDateAsc_EmptyResult() {
            // given
            Conversation emptyConversation = Conversation.builder()
                    .userId(3L)
                    .build();
            entityManager.persistAndFlush(emptyConversation);

            // when
            List<Message> result = messageRepository.findByConversationIdOrderByCreatedDateAsc(emptyConversation.getId());

            // then
            assertThat(result).isEmpty();
        }
    }
}
