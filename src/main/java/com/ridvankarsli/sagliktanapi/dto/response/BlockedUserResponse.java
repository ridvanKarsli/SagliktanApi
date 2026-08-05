package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.BlockedUser;

import java.time.LocalDateTime;

public record BlockedUserResponse(Long id, Long userId, String userName, LocalDateTime createdAt) {
    public static BlockedUserResponse from(BlockedUser b) {
        return new BlockedUserResponse(
                b.getId(),
                b.getBlocked().getId(),
                b.getBlocked().getFirstName() + " " + b.getBlocked().getLastName(),
                b.getCreatedAt()
        );
    }
}
