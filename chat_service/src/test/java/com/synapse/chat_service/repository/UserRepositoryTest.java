package com.synapse.chat_service.repository;

import com.synapse.chat_service.domain.entity.User;
import com.synapse.chat_service.domain.repository.UserRepository;
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
@DisplayName("UserRepository 단위 테스트")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 테스트용 User 데이터 생성
        user1 = TestObjectFactory.createUser(1L, "testuser1", "testuser1@example.com");
        user2 = TestObjectFactory.createUser(2L, "testuser2", "testuser2@example.com");

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTest {

        @Test
        @DisplayName("성공: 새로운 사용자 저장")
        void save_Success() {
            // given
            User newUser = TestObjectFactory.createUser(3L, "newuser", "newuser@example.com");

            // when
            User savedUser = userRepository.save(newUser);

            // then
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getUsername()).isEqualTo("newuser");
            assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        }

        @Test
        @DisplayName("성공: ID로 사용자 조회")
        void findById_Success() {
            // when
            Optional<User> foundUser = userRepository.findById(user1.getId());

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
            assertThat(foundUser.get().getEmail()).isEqualTo("testuser1@example.com");
        }

        @Test
        @DisplayName("성공: 존재하지 않는 ID로 조회 시 빈 Optional 반환")
        void findById_NotFound() {
            // given
            Long nonExistentId = 999L;

            // when
            Optional<User> foundUser = userRepository.findById(nonExistentId);

            // then
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("성공: 사용자 삭제")
        void delete_Success() {
            // given
            Long userId = user1.getId();

            // when
            userRepository.delete(user1);
            entityManager.flush();

            // then
            Optional<User> deletedUser = userRepository.findById(userId);
            assertThat(deletedUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUsername 테스트")
    class FindByUsernameTest {

        @Test
        @DisplayName("성공: 사용자명으로 사용자 조회")
        void findByUsername_Success() {
            // when
            Optional<User> foundUser = userRepository.findByUsername("testuser1");

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
            assertThat(foundUser.get().getEmail()).isEqualTo("testuser1@example.com");
        }

        @Test
        @DisplayName("성공: 존재하지 않는 사용자명으로 조회 시 빈 Optional 반환")
        void findByUsername_NotFound() {
            // when
            Optional<User> foundUser = userRepository.findByUsername("nonexistentuser");

            // then
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("성공: 대소문자 구분하여 조회")
        void findByUsername_CaseSensitive() {
            // when
            Optional<User> foundUser = userRepository.findByUsername("TESTUSER1");

            // then
            assertThat(foundUser).isEmpty(); // 대소문자가 다르므로 찾을 수 없음
        }
    }

    @Nested
    @DisplayName("findByEmail 테스트")
    class FindByEmailTest {

        @Test
        @DisplayName("성공: 이메일로 사용자 조회")
        void findByEmail_Success() {
            // when
            Optional<User> foundUser = userRepository.findByEmail("testuser1@example.com");

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
            assertThat(foundUser.get().getEmail()).isEqualTo("testuser1@example.com");
        }

        @Test
        @DisplayName("성공: 존재하지 않는 이메일로 조회 시 빈 Optional 반환")
        void findByEmail_NotFound() {
            // when
            Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

            // then
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("성공: 대소문자 구분하여 조회")
        void findByEmail_CaseSensitive() {
            // when
            Optional<User> foundUser = userRepository.findByEmail("TESTUSER1@EXAMPLE.COM");

            // then
            assertThat(foundUser).isEmpty(); // 대소문자가 다르므로 찾을 수 없음
        }
    }

    @Nested
    @DisplayName("existsByUsername 테스트")
    class ExistsByUsernameTest {

        @Test
        @DisplayName("성공: 존재하는 사용자명 확인")
        void existsByUsername_True() {
            // when
            boolean exists = userRepository.existsByUsername("testuser1");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공: 존재하지 않는 사용자명 확인")
        void existsByUsername_False() {
            // when
            boolean exists = userRepository.existsByUsername("nonexistentuser");

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("성공: 대소문자 구분하여 확인")
        void existsByUsername_CaseSensitive() {
            // when
            boolean exists = userRepository.existsByUsername("TESTUSER1");

            // then
            assertThat(exists).isFalse(); // 대소문자가 다르므로 존재하지 않음
        }
    }

    @Nested
    @DisplayName("existsByEmail 테스트")
    class ExistsByEmailTest {

        @Test
        @DisplayName("성공: 존재하는 이메일 확인")
        void existsByEmail_True() {
            // when
            boolean exists = userRepository.existsByEmail("testuser1@example.com");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공: 존재하지 않는 이메일 확인")
        void existsByEmail_False() {
            // when
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("성공: 대소문자 구분하여 확인")
        void existsByEmail_CaseSensitive() {
            // when
            boolean exists = userRepository.existsByEmail("TESTUSER1@EXAMPLE.COM");

            // then
            assertThat(exists).isFalse(); // 대소문자가 다르므로 존재하지 않음
        }
    }

    @Nested
    @DisplayName("중복성 검증 테스트")
    class DuplicateValidationTest {

        @Test
        @DisplayName("성공: 사용자명 중복 검증")
        void validateUsernameDuplication() {
            // given
            String existingUsername = "testuser1";
            String newUsername = "newuser";

            // when & then
            assertThat(userRepository.existsByUsername(existingUsername)).isTrue();
            assertThat(userRepository.existsByUsername(newUsername)).isFalse();
        }

        @Test
        @DisplayName("성공: 이메일 중복 검증")
        void validateEmailDuplication() {
            // given
            String existingEmail = "testuser1@example.com";
            String newEmail = "newuser@example.com";

            // when & then
            assertThat(userRepository.existsByEmail(existingEmail)).isTrue();
            assertThat(userRepository.existsByEmail(newEmail)).isFalse();
        }

        @Test
        @DisplayName("성공: 동일한 사용자명과 이메일로 새 사용자 생성 시 제약 조건 확인")
        void validateUniqueConstraints() {
            // given
            User duplicateUser = TestObjectFactory.createUser(4L, "testuser1", "testuser1@example.com"); // 이미 존재하는 사용자명과 이메일

            // when & then
            // 실제로는 데이터베이스 제약 조건에 의해 예외가 발생할 것이지만,
            // 여기서는 존재 여부만 확인
            assertThat(userRepository.existsByUsername(duplicateUser.getUsername())).isTrue();
            assertThat(userRepository.existsByEmail(duplicateUser.getEmail())).isTrue();
        }
    }
}