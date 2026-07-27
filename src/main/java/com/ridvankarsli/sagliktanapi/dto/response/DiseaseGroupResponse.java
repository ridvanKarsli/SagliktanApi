package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;

import java.time.LocalDateTime;

public record DiseaseGroupResponse(
        Long id,
        String name,
        String description,
        // Gruba kayıtlı üye sayısı - grup listesinde ve detayında gösterilir.
        long memberCount,
        LocalDateTime createdAt
) {
    public static DiseaseGroupResponse from(DiseaseGroup group) {
        return from(group, 0);
    }

    public static DiseaseGroupResponse from(DiseaseGroup group, long memberCount) {
        return new DiseaseGroupResponse(
                group.getId(), group.getName(), group.getDescription(), memberCount, group.getCreatedAt()
        );
    }
}
