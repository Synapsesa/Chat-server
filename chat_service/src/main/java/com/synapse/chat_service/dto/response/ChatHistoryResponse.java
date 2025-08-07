package com.synapse.chat_service.dto.response;

import java.util.List;

public record ChatHistoryResponse(
    List<MessageResponse.History> data,
    PaginationDto pagination
) {
    public static ChatHistoryResponse of(List<MessageResponse.History> data, PaginationDto pagination) {
        return new ChatHistoryResponse(data, pagination);
    }
}
