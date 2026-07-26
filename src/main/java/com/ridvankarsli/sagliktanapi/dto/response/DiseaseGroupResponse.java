package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;

import java.time.LocalDateTime;

public record DiseaseGroupResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static DiseaseGroupResponse from(DiseaseGroup group) {
        return new DiseaseGroupResponse(group.getId(), group.getName(), group.getDescription(), group.getCreatedAt());
    }
}
