package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.exception.commonexception.ValidException;
import com.synapse.chat_service.testutil.TestObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Message 도메인 엔티티 테스트")
class MessageTest {

    private ChatRoom chatRoom;
    private Message message;
    private final String initialContent = "초기 메시지 내용";

    @BeforeEach
    void setUp() {
        chatRoom = TestObjectFactory.createChatRoom(1L, "테스트 채팅방");

        message = TestObjectFactory.createUserMessage(chatRoom, initialContent);
    }

    @Nested
    @DisplayName("updateContent 메소드 테스트")
    class UpdateContentTest {

        @Test
        @DisplayName("성공: 유효한 새 내용으로 업데이트")
        void updateContent_Success() {
            // given
            String newContent = "새로운 메시지 내용입니다.";

            // when
            message.updateContent(newContent);

            // then
            assertThat(message.getContent()).isEqualTo(newContent);
        }

        @Test
        @DisplayName("성공: 최대 길이(1000자) 내용으로 업데이트")
        void updateContent_Success_MaxLength() {
            // given
            String maxLengthContent = "a".repeat(1000);

            // when
            message.updateContent(maxLengthContent);

            // then
            assertThat(message.getContent()).isEqualTo(maxLengthContent);
            assertThat(message.getContent().length()).isEqualTo(1000);
        }

        @Test
        @DisplayName("성공: 한글 내용으로 업데이트")
        void updateContent_Success_Korean() {
            // given
            String koreanContent = "안녕하세요! 한글 메시지 내용입니다.";

            // when
            message.updateContent(koreanContent);

            // then
            assertThat(message.getContent()).isEqualTo(koreanContent);
        }

        @Test
        @DisplayName("성공: 특수문자가 포함된 내용으로 업데이트")
        void updateContent_Success_SpecialCharacters() {
            // given
            String contentWithSpecialChars = "메시지 내용! @#$%^&*()_+-=[]{}|;':\",./<>?";

            // when
            message.updateContent(contentWithSpecialChars);

            // then
            assertThat(message.getContent()).isEqualTo(contentWithSpecialChars);
        }

        @Test
        @DisplayName("성공: 줄바꿈이 포함된 내용으로 업데이트")
        void updateContent_Success_WithNewlines() {
            // given
            String contentWithNewlines = "첫 번째 줄\n두 번째 줄\n세 번째 줄";

            // when
            message.updateContent(contentWithNewlines);

            // then
            assertThat(message.getContent()).isEqualTo(contentWithNewlines);
        }

        @Test
        @DisplayName("실패: null 내용으로 업데이트 시 ValidException 발생")
        void updateContent_Fail_NullContent() {
            // given
            String nullContent = null;

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                message.updateContent(nullContent);
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 비어있을 수 없습니다");
            assertThat(message.getContent()).isEqualTo(initialContent); // 기존 내용 유지
        }

        @Test
        @DisplayName("실패: 빈 문자열 내용으로 업데이트 시 ValidException 발생")
        void updateContent_Fail_EmptyContent() {
            // given
            String emptyContent = "";

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                message.updateContent(emptyContent);
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 비어있을 수 없습니다");
            assertThat(message.getContent()).isEqualTo(initialContent); // 기존 내용 유지
        }

        @Test
        @DisplayName("실패: 공백만 있는 내용으로 업데이트 시 ValidException 발생")
        void updateContent_Fail_WhitespaceOnlyContent() {
            // given
            String whitespaceOnlyContent = "   ";

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                message.updateContent(whitespaceOnlyContent);
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 비어있을 수 없습니다");
            assertThat(message.getContent()).isEqualTo(initialContent); // 기존 내용 유지
        }

        @Test
        @DisplayName("실패: 1000자를 초과하는 내용으로 업데이트 시 ValidException 발생")
        void updateContent_Fail_ExceedsMaxLength() {
            // given
            String tooLongContent = "a".repeat(1001); // 1001자

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                message.updateContent(tooLongContent);
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 1000자를 초과할 수 없습니다");
            assertThat(message.getContent()).isEqualTo(initialContent); // 기존 내용 유지
        }

        @Test
        @DisplayName("경계값 테스트: 정확히 1000자인 내용으로 업데이트")
        void updateContent_BoundaryTest_ExactlyMaxLength() {
            // given
            String exactMaxLengthContent = "a".repeat(1000);

            // when
            message.updateContent(exactMaxLengthContent);

            // then
            assertThat(message.getContent()).isEqualTo(exactMaxLengthContent);
            assertThat(message.getContent().length()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Message 생성자(Builder) 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("성공: 유효한 파라미터로 Message 생성")
        void constructor_Success() {
            // given
            String testContent = "테스트 메시지 내용";

            // when
            Message newMessage = TestObjectFactory.createAssistantMessage(chatRoom, testContent);

            // then
            assertThat(newMessage.getChatRoom()).isEqualTo(chatRoom);
            assertThat(newMessage.getSenderType()).isEqualTo(SenderType.ASSISTANT);
            assertThat(newMessage.getContent()).isEqualTo(testContent);
        }

        @Test
        @DisplayName("성공: USER 타입으로 Message 생성")
        void constructor_Success_UserType() {
            // given
            String testContent = "사용자 메시지";

            // when
            Message userMessage = TestObjectFactory.createUserMessage(chatRoom, testContent);

            // then
            assertThat(userMessage.getSenderType()).isEqualTo(SenderType.USER);
            assertThat(userMessage.getContent()).isEqualTo(testContent);
        }

        @Test
        @DisplayName("성공: AI 타입으로 Message 생성")
        void constructor_Success_AIType() {
            // given
            String testContent = "AI 응답 메시지";

            // when
            Message aiMessage = TestObjectFactory.createAssistantMessage(chatRoom, testContent);

            // then
            assertThat(aiMessage.getSenderType()).isEqualTo(SenderType.ASSISTANT);
            assertThat(aiMessage.getContent()).isEqualTo(testContent);
        }

        @Test
        @DisplayName("실패: null 내용으로 Message 생성 시 ValidException 발생")
        void constructor_Fail_NullContent() {
            // given
            String nullContent = null;

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                Message.builder()
                        .chatRoom(chatRoom)
                        .senderType(SenderType.USER)
                        .content(nullContent)
                        .build();
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 비어있을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 빈 문자열 내용으로 Message 생성 시 ValidException 발생")
        void constructor_Fail_EmptyContent() {
            // given
            String emptyContent = "";

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                Message.builder()
                        .chatRoom(chatRoom)
                        .senderType(SenderType.USER)
                        .content(emptyContent)
                        .build();
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 비어있을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 공백만 있는 내용으로 Message 생성 시 ValidException 발생")
        void constructor_Fail_WhitespaceOnlyContent() {
            // given
            String whitespaceOnlyContent = "   ";

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                Message.builder()
                        .chatRoom(chatRoom)
                        .senderType(SenderType.USER)
                        .content(whitespaceOnlyContent)
                        .build();
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 비어있을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 1000자를 초과하는 내용으로 Message 생성 시 ValidException 발생")
        void constructor_Fail_ExceedsMaxLength() {
            // given
            String tooLongContent = "a".repeat(1001); // 1001자

            // when & then
            ValidException exception = assertThrows(ValidException.class, () -> {
                Message.builder()
                        .chatRoom(chatRoom)
                        .senderType(SenderType.USER)
                        .content(tooLongContent)
                        .build();
            });

            assertThat(exception.getMessage()).contains("메시지 내용은 1000자를 초과할 수 없습니다");
        }

        @Test
        @DisplayName("성공: 최대 길이(1000자) 내용으로 Message 생성")
        void constructor_Success_MaxLength() {
            // given
            String maxLengthContent = "a".repeat(1000);

            // when
            Message newMessage = TestObjectFactory.createUserMessage(chatRoom, maxLengthContent);

            // then
            assertThat(newMessage.getContent()).isEqualTo(maxLengthContent);
            assertThat(newMessage.getContent().length()).isEqualTo(1000);
        }

        @Test
        @DisplayName("경계값 테스트: 정확히 1000자인 내용으로 Message 생성")
        void constructor_BoundaryTest_ExactlyMaxLength() {
            // given
            String exactMaxLengthContent = "b".repeat(1000);

            // when
            Message newMessage = TestObjectFactory.createAssistantMessage(chatRoom, exactMaxLengthContent);

            // then
            assertThat(newMessage.getContent()).isEqualTo(exactMaxLengthContent);
            assertThat(newMessage.getContent().length()).isEqualTo(1000);
            assertThat(newMessage.getSenderType()).isEqualTo(SenderType.ASSISTANT);
        }
    }
}
