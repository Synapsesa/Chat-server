package com.synapse.chat_service.repository;

import com.synapse.chat_service.domain.entity.ChatUsage;
import com.synapse.chat_service.domain.entity.enums.SubscriptionType;
import com.synapse.chat_service.domain.repository.ChatUsageRepository;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChatUsageRepository 단위 테스트")
class ChatUsageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatUsageRepository chatUsageRepository;

    private ChatUsage chatUsage1;
    private ChatUsage chatUsage2;

    @BeforeEach
    void setUp() {
        // 테스트용 ChatUsage 데이터 생성
        chatUsage1 = TestObjectFactory.createChatUsage(1L, SubscriptionType.FREE, 100);
        chatUsage2 = TestObjectFactory.createChatUsage(2L, SubscriptionType.PRO, 1000);
    }

    @Nested
    @DisplayName("save 테스트")
    class SaveTest {

        @Test
        @DisplayName("성공: ChatUsage 저장")
        void save_Success() {
            // when
            ChatUsage savedChatUsage = chatUsageRepository.save(chatUsage1);

            // then
            assertThat(savedChatUsage).isNotNull();
            assertThat(savedChatUsage.getId()).isNotNull();
            assertThat(savedChatUsage.getUserId()).isEqualTo(1L);
            assertThat(savedChatUsage.getSubscriptionType()).isEqualTo(SubscriptionType.FREE);
            assertThat(savedChatUsage.getMessageLimit()).isEqualTo(100);
            assertThat(savedChatUsage.getMessageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: PRO 구독 타입으로 ChatUsage 저장")
        void save_Success_ProSubscription() {
            // when
            ChatUsage savedChatUsage = chatUsageRepository.save(chatUsage2);

            // then
            assertThat(savedChatUsage).isNotNull();
            assertThat(savedChatUsage.getId()).isNotNull();
            assertThat(savedChatUsage.getUserId()).isEqualTo(2L);
            assertThat(savedChatUsage.getSubscriptionType()).isEqualTo(SubscriptionType.PRO);
            assertThat(savedChatUsage.getMessageLimit()).isEqualTo(1000);
            assertThat(savedChatUsage.getMessageCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("성공: ID로 ChatUsage 조회")
        void findById_Success() {
            // given
            ChatUsage savedChatUsage = entityManager.persistAndFlush(chatUsage1);

            // when
            Optional<ChatUsage> foundChatUsage = chatUsageRepository.findById(savedChatUsage.getId());

            // then
            assertThat(foundChatUsage).isPresent();
            assertThat(foundChatUsage.get().getUserId()).isEqualTo(1L);
            assertThat(foundChatUsage.get().getSubscriptionType()).isEqualTo(SubscriptionType.FREE);
            assertThat(foundChatUsage.get().getMessageLimit()).isEqualTo(100);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID로 조회")
        void findById_NotFound() {
            // when
            Optional<ChatUsage> foundChatUsage = chatUsageRepository.findById(999L);

            // then
            assertThat(foundChatUsage).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll 테스트")
    class FindAllTest {

        @Test
        @DisplayName("성공: 모든 ChatUsage 조회")
        void findAll_Success() {
            // given
            entityManager.persistAndFlush(chatUsage1);
            entityManager.persistAndFlush(chatUsage2);

            // when
            var allChatUsages = chatUsageRepository.findAll();

            // then
            assertThat(allChatUsages).hasSize(2);
            assertThat(allChatUsages)
                    .extracting(ChatUsage::getUserId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("성공: 빈 결과 반환")
        void findAll_EmptyResult() {
            // when
            var allChatUsages = chatUsageRepository.findAll();

            // then
            assertThat(allChatUsages).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete 테스트")
    class DeleteTest {

        @Test
        @DisplayName("성공: ChatUsage 삭제")
        void delete_Success() {
            // given
            ChatUsage savedChatUsage = entityManager.persistAndFlush(chatUsage1);
            Long chatUsageId = savedChatUsage.getId();

            // when
            chatUsageRepository.delete(savedChatUsage);
            entityManager.flush();

            // then
            Optional<ChatUsage> deletedChatUsage = chatUsageRepository.findById(chatUsageId);
            assertThat(deletedChatUsage).isEmpty();
        }

        @Test
        @DisplayName("성공: deleteById로 ChatUsage 삭제")
        void deleteById_Success() {
            // given
            ChatUsage savedChatUsage = entityManager.persistAndFlush(chatUsage1);
            Long chatUsageId = savedChatUsage.getId();

            // when
            chatUsageRepository.deleteById(chatUsageId);
            entityManager.flush();

            // then
            Optional<ChatUsage> deletedChatUsage = chatUsageRepository.findById(chatUsageId);
            assertThat(deletedChatUsage).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsById 테스트")
    class ExistsByIdTest {

        @Test
        @DisplayName("성공: 존재하는 ChatUsage 확인")
        void existsById_Success() {
            // given
            ChatUsage savedChatUsage = entityManager.persistAndFlush(chatUsage1);

            // when
            boolean exists = chatUsageRepository.existsById(savedChatUsage.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ChatUsage 확인")
        void existsById_NotFound() {
            // when
            boolean exists = chatUsageRepository.existsById(999L);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("count 테스트")
    class CountTest {

        @Test
        @DisplayName("성공: ChatUsage 개수 조회")
        void count_Success() {
            // given
            entityManager.persistAndFlush(chatUsage1);
            entityManager.persistAndFlush(chatUsage2);

            // when
            long count = chatUsageRepository.count();

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("성공: 빈 테이블의 개수 조회")
        void count_EmptyTable() {
            // when
            long count = chatUsageRepository.count();

            // then
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("JPA 매핑 검증 테스트")
    class JpaMappingTest {

        @Test
        @DisplayName("성공: userId unique 제약 조건 검증")
        void uniqueUserId_Validation() {
            // given
            entityManager.persistAndFlush(chatUsage1);

            ChatUsage duplicateUserIdChatUsage = TestObjectFactory.createChatUsage(1L, SubscriptionType.PRO, 500);

            // when & then
            try {
                entityManager.persistAndFlush(duplicateUserIdChatUsage);
                entityManager.flush();
                // 예외가 발생하지 않으면 테스트 실패
                assertThat(false).as("Unique constraint violation should occur").isTrue();
            } catch (Exception e) {
                // unique 제약 조건 위반으로 예외 발생 예상
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("성공: 기본값 검증")
        void defaultValues_Validation() {
            // when
            ChatUsage savedChatUsage = entityManager.persistAndFlush(chatUsage1);

            // then
            assertThat(savedChatUsage.getMessageCount()).isEqualTo(0);
        }
    }
}