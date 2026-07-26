package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.SubGroup;

import java.time.LocalDateTime;

public record SubGroupResponse(
        Long id,
        Long diseaseGroupId,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static SubGroupResponse from(SubGroup subGroup) {
        return new SubGroupResponse(
                subGroup.getId(),
                subGroup.getDiseaseGroup().getId(),
                subGroup.getName(),
                subGroup.getDescription(),
                subGroup.getCreatedAt()
        );
    }
}
