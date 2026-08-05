package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Notification;
import com.ridvankarsli.sagliktanapi.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        Long actorId,
        String actorName,
        Long postId,
        Long commentId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getActor().getId(),
                n.getActor().getFullName(),
                n.getPostId(),
                n.getCommentId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
