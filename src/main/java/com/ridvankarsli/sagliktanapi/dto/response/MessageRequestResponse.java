package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.MessageRequest;
import com.ridvankarsli.sagliktanapi.domain.MessageRequestStatus;

import java.time.LocalDateTime;

public record MessageRequestResponse(
        Long id,
        Long senderId,
        String senderName,
        Long recipientId,
        String recipientName,
        MessageRequestStatus status,
        LocalDateTime createdAt
) {
    public static MessageRequestResponse from(MessageRequest r) {
        return new MessageRequestResponse(
                r.getId(),
                r.getSender().getId(),
                r.getSender().getFullName(),
                r.getRecipient().getId(),
                r.getRecipient().getFullName(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
