package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.exception.commonexception.ValidException;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ChatRoom 도메인 엔티티 테스트")
class ChatRoomTest {

    private ChatRoom chatRoom;
    private final Long userId = 1L;
    private final String initialTitle = "초기 채팅방 제목";

    @BeforeEach
    void setUp() {
        chatRoom = TestObjectFactory.createChatRoom(userId, initialTitle);
    }

    @Nested
    @DisplayName("updateTitle 메소드 테스트")
    class UpdateTitleTest {

        @Test
        @DisplayName("성공: 유효한 새 제목으로 업데이트")
        void updateTitle_Success() {
            // given
            String newTitle = "새로운 채팅방 제목";

            // when
            chatRoom.updateTitle(newTitle);

            // then
            assertThat(chatRoom.getTitle()).isEqualTo(newTitle);
        }

        @Test
        @DisplayName("성공: 앞뒤 공백이 있는 제목으로 업데이트 시 trim() 적용")
        void updateTitle_Success_WithWhitespace() {
            // given
            String newTitleWithWhitespace = "  새로운 채팅방 제목  ";
            String expectedTitle = "새로운 채팅방 제목";

            // when
            chatRoom.updateTitle(newTitleWithWhitespace);

            // then
            assertThat(chatRoom.getTitle()).isEqualTo(expectedTitle);
        }

        @Test
        @DisplayName("성공: 최대 길이(255자) 제목으로 업데이트")
        void updateTitle_Success_MaxLength() {
            // given
            String maxLengthTitle = "a".repeat(255);

            // when
            chatRoom.updateTitle(maxLengthTitle);

            // then
            assertThat(chatRoom.getTitle()).isEqualTo(maxLengthTitle);
            assertThat(chatRoom.getTitle().length()).isEqualTo(255);
        }

        @Test
        @DisplayName("성공: 한글 제목으로 업데이트")
        void updateTitle_Success_Korean() {
            // given
            String koreanTitle = "한글 채팅방 제목입니다";

            // when
            chatRoom.updateTitle(koreanTitle);

            // then
            assertThat(chatRoom.getTitle()).isEqualTo(koreanTitle);
        }

        @Test
        @DisplayName("실패: null 제목으로 업데이트 시 ValidException 발생")
        void updateTitle_Fail_NullTitle() {
            // given
            String nullTitle = null;

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                chatRoom.updateTitle(nullTitle);
            });

            assertThat(exception.getMessage()).contains("채팅방 제목은 비어있을 수 없습니다");
            assertThat(chatRoom.getTitle()).isEqualTo(initialTitle); // 기존 제목 유지
        }

        @Test
        @DisplayName("실패: 빈 문자열 제목으로 업데이트 시 ValidException 발생")
        void updateTitle_Fail_EmptyTitle() {
            // given
            String emptyTitle = "";

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                chatRoom.updateTitle(emptyTitle);
            });

            assertThat(exception.getMessage()).contains("채팅방 제목은 비어있을 수 없습니다");
            assertThat(chatRoom.getTitle()).isEqualTo(initialTitle); // 기존 제목 유지
        }

        @Test
        @DisplayName("실패: 공백만 있는 제목으로 업데이트 시 ValidException 발생")
        void updateTitle_Fail_WhitespaceOnlyTitle() {
            // given
            String whitespaceOnlyTitle = "   ";

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                chatRoom.updateTitle(whitespaceOnlyTitle);
            });

            assertThat(exception.getMessage()).contains("채팅방 제목은 비어있을 수 없습니다");
            assertThat(chatRoom.getTitle()).isEqualTo(initialTitle); // 기존 제목 유지
        }

        @Test
        @DisplayName("실패: 255자를 초과하는 제목으로 업데이트 시 ValidException 발생")
        void updateTitle_Fail_ExceedsMaxLength() {
            // given
            String tooLongTitle = "a".repeat(256); // 256자

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                chatRoom.updateTitle(tooLongTitle);
            });

            assertThat(exception.getMessage()).contains("채팅방 제목은 255자를 초과할 수 없습니다");
            assertThat(chatRoom.getTitle()).isEqualTo(initialTitle); // 기존 제목 유지
        }

        @Test
        @DisplayName("실패: trim 후 255자를 초과하는 제목으로 업데이트 시 ValidException 발생")
        void updateTitle_Fail_ExceedsMaxLengthAfterTrim() {
            // given
            String tooLongTitleWithWhitespace = "  " + "a".repeat(256) + "  "; // trim 후 256자

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                chatRoom.updateTitle(tooLongTitleWithWhitespace);
            });

            assertThat(exception.getMessage()).contains("채팅방 제목은 255자를 초과할 수 없습니다");
            assertThat(chatRoom.getTitle()).isEqualTo(initialTitle); // 기존 제목 유지
        }

        @Test
        @DisplayName("경계값 테스트: trim 후 정확히 255자인 제목으로 업데이트")
        void updateTitle_BoundaryTest_ExactlyMaxLengthAfterTrim() {
            // given
            String exactMaxLengthWithWhitespace = "  " + "a".repeat(255) + "  "; // trim 후 정확히 255자
            String expectedTitle = "a".repeat(255);

            // when
            chatRoom.updateTitle(exactMaxLengthWithWhitespace);

            // then
            assertThat(chatRoom.getTitle()).isEqualTo(expectedTitle);
            assertThat(chatRoom.getTitle().length()).isEqualTo(255);
        }
    }

    @Nested
    @DisplayName("ChatRoom 생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("성공: 유효한 파라미터로 ChatRoom 생성")
        void constructor_Success() {
            // given
            Long testUserId = 123L;
            String testTitle = "테스트 채팅방";

            // when
            ChatRoom newChatRoom = TestObjectFactory.createChatRoom(testUserId, testTitle);

            // then
            assertThat(newChatRoom.getUserId()).isEqualTo(testUserId);
            assertThat(newChatRoom.getTitle()).isEqualTo(testTitle);
            assertThat(newChatRoom.getMessages()).isNotNull();
            assertThat(newChatRoom.getMessages()).isEmpty();
        }

        @Test
        @DisplayName("성공: 빈 메시지 리스트로 초기화")
        void constructor_Success_EmptyMessagesList() {
            // when
            ChatRoom newChatRoom = TestObjectFactory.createChatRoom(1L, "테스트");

            // then
            assertThat(newChatRoom.getMessages()).isNotNull();
            assertThat(newChatRoom.getMessages()).hasSize(0);
        }
    }
}
