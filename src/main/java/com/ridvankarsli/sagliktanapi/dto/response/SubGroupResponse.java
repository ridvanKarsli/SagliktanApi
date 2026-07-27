package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.SubGroup;

import java.time.LocalDateTime;

public record SubGroupResponse(
        Long id,
        Long diseaseGroupId,
        String name,
        String description,
        // Alt grupta açılmış sohbet (post) sayısı - alt grup listesinde gösterilir.
        long postCount,
        LocalDateTime createdAt
) {
    public static SubGroupResponse from(SubGroup subGroup) {
        return from(subGroup, 0);
    }

    public static SubGroupResponse from(SubGroup subGroup, long postCount) {
        return new SubGroupResponse(
                subGroup.getId(),
                subGroup.getDiseaseGroup().getId(),
                subGroup.getName(),
                subGroup.getDescription(),
                postCount,
                subGroup.getCreatedAt()
        );
    }
}
