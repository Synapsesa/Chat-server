package com.synapse.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.chat_service.dto.request.ChatRoomRequest;
import com.synapse.chat_service.dto.response.ChatRoomResponse;
import com.synapse.chat_service.service.ChatRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ChatRoomController 통합 테스트")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatRoomService chatRoomService;

    @Nested
    @DisplayName("POST /api/v1/chat-rooms - 채팅방 생성")
    class CreateChatRoom {

        @Test
        @DisplayName("성공: 유효한 요청으로 채팅방 생성")
        void createChatRoom_Success() throws Exception {
            // given
            ChatRoomRequest.Create request = new ChatRoomRequest.Create(1L, "테스트 채팅방");

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.title").value("테스트 채팅방"))
                    .andExpect(jsonPath("$.createdDate").exists())
                    .andExpect(jsonPath("$.updatedDate").exists())
                    .andExpect(jsonPath("$.messageCount").value(0));
        }

        @Test
        @DisplayName("실패: userId가 null인 경우")
        void createChatRoom_Fail_UserIdNull() throws Exception {
            // given
            ChatRoomRequest.Create request = new ChatRoomRequest.Create(null, "테스트 채팅방");

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: title이 비어있는 경우")
        void createChatRoom_Fail_TitleBlank() throws Exception {
            // given
            ChatRoomRequest.Create request = new ChatRoomRequest.Create(1L, "");

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: title이 255자를 초과하는 경우")
        void createChatRoom_Fail_TitleTooLong() throws Exception {
            // given
            String longTitle = "a".repeat(256);
            ChatRoomRequest.Create request = new ChatRoomRequest.Create(1L, longTitle);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms/{chatRoomId} - 채팅방 단건 조회")
    class GetChatRoom {

        @Test
        @DisplayName("성공: 존재하는 채팅방 조회")
        void getChatRoom_Success() throws Exception {
            // given
            ChatRoomRequest.Create createRequest = new ChatRoomRequest.Create(1L, "테스트 채팅방");
            ChatRoomResponse.Detail createdChatRoom = chatRoomService.createChatRoom(createRequest);

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", createdChatRoom.id()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdChatRoom.id().toString()))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.title").value("테스트 채팅방"))
                    .andExpect(jsonPath("$.messageCount").value(0));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방 조회")
        void getChatRoom_Fail_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", nonExistentId))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms/paging - 채팅방 페이징 조회")
    class GetChatRoomsByUserIdPaging {

        @Test
        @DisplayName("성공: 페이징 파라미터로 채팅방 조회")
        void getChatRoomsByUserId_Success() throws Exception {
            // given
            Long userId = 1L;
            for (int i = 1; i <= 25; i++) {
                chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "채팅방 " + i));
            }

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms")
                            .param("userId", userId.toString())
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdDate,desc"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(10))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("성공: 기본 페이징 설정으로 조회")
        void getChatRoomsByUserId_Success_DefaultPaging() throws Exception {
            // given
            Long userId = 1L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "채팅방 1"));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms")
                            .param("userId", userId.toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.size").value(20)); // 기본 size
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms/search - 채팅방 제목 검색")
    class SearchChatRooms {

        @Test
        @DisplayName("성공: 특정 userId와 keyword로 검색 시 조건에 맞는 채팅방 목록 반환")
        void searchChatRooms_Success() throws Exception {
            // given
            Long userId = 1L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Java 스터디"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Spring Boot 프로젝트"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "React 개발"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(2L, "Java 마스터")); // 다른 사용자

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/search")
                            .param("userId", userId.toString())
                            .param("keyword", "Java"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Java 스터디"))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].createdDate").exists())
                    .andExpect(jsonPath("$[0].messageCount").exists());
        }

        @Test
        @DisplayName("성공: 부분 문자열 검색")
        void searchChatRooms_Success_PartialMatch() throws Exception {
            // given
            Long userId = 1L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Spring Boot 프로젝트"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Spring Security 학습"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "React 개발"));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/search")
                            .param("userId", userId.toString())
                            .param("keyword", "Spring"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].title").value("Spring Boot 프로젝트"))
                    .andExpect(jsonPath("$[1].title").value("Spring Security 학습"));
        }

        @Test
        @DisplayName("성공: 검색 결과가 없는 경우 빈 배열 반환")
        void searchChatRooms_Success_EmptyResult() throws Exception {
            // given
            Long userId = 1L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Java 스터디"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Spring Boot 프로젝트"));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/search")
                            .param("userId", userId.toString())
                            .param("keyword", "Python"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("성공: 사용자 격리 - 다른 사용자의 채팅방은 검색되지 않음")
        void searchChatRooms_Success_UserIsolation() throws Exception {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId1, "Java 스터디"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId2, "Java 마스터"));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/search")
                            .param("userId", userId1.toString())
                            .param("keyword", "Java"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Java 스터디"));
        }

        @Test
        @DisplayName("성공: 키워드 포함 검색")
        void searchChatRooms_Success_KeywordContaining() throws Exception {
            // given
            Long userId = 1L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "Java 스터디"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "JavaScript 프로젝트"));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/search")
                            .param("userId", userId.toString())
                            .param("keyword", "Java"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("성공: 빈 키워드로 검색 시 모든 채팅방 반환")
        void searchChatRooms_Success_EmptyKeyword() throws Exception {
            // given
            Long userId = 1L;
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "채팅방 1"));
            chatRoomService.createChatRoom(new ChatRoomRequest.Create(userId, "채팅방 2"));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/search")
                            .param("userId", userId.toString())
                            .param("keyword", ""))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/chat-rooms/{chatRoomId} - 채팅방 수정")
    class UpdateChatRoom {

        @Test
        @DisplayName("성공: 유효한 요청으로 채팅방 수정")
        void updateChatRoom_Success() throws Exception {
            // given
            ChatRoomRequest.Create createRequest = new ChatRoomRequest.Create(1L, "원본 제목");
            ChatRoomResponse.Detail createdChatRoom = chatRoomService.createChatRoom(createRequest);
            
            ChatRoomRequest.Update updateRequest = new ChatRoomRequest.Update("수정된 제목");

            // when & then
            mockMvc.perform(put("/api/v1/chat-rooms/{chatRoomId}", createdChatRoom.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdChatRoom.id().toString()))
                    .andExpect(jsonPath("$.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.userId").value(1L));
        }

        @Test
        @DisplayName("실패: title이 비어있는 경우")
        void updateChatRoom_Fail_TitleBlank() throws Exception {
            // given
            ChatRoomRequest.Create createRequest = new ChatRoomRequest.Create(1L, "원본 제목");
            ChatRoomResponse.Detail createdChatRoom = chatRoomService.createChatRoom(createRequest);
            
            ChatRoomRequest.Update updateRequest = new ChatRoomRequest.Update("");

            // when & then
            mockMvc.perform(put("/api/v1/chat-rooms/{chatRoomId}", createdChatRoom.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방 수정")
        void updateChatRoom_Fail_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();
            ChatRoomRequest.Update updateRequest = new ChatRoomRequest.Update("수정된 제목");

            // when & then
            mockMvc.perform(put("/api/v1/chat-rooms/{chatRoomId}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/chat-rooms/{chatRoomId} - 채팅방 삭제")
    class DeleteChatRoom {

        @Test
        @DisplayName("성공: 존재하는 채팅방 삭제")
        void deleteChatRoom_Success() throws Exception {
            // given
            ChatRoomRequest.Create createRequest = new ChatRoomRequest.Create(1L, "삭제할 채팅방");
            ChatRoomResponse.Detail createdChatRoom = chatRoomService.createChatRoom(createRequest);

            // when & then
            mockMvc.perform(delete("/api/v1/chat-rooms/{chatRoomId}", createdChatRoom.id()))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 확인
            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", createdChatRoom.id()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방 삭제")
        void deleteChatRoom_Fail_NotFound() throws Exception {
            // given
            UUID nonExistentId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/chat-rooms/{chatRoomId}", nonExistentId))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}
