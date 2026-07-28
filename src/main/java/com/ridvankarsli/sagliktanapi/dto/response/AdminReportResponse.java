package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.ContentReport;
import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.service.AdminReportItem;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        // Raporlanan içeriğin kısa metin önizlemesi (içerik silinmişse bunu belirten bir metin).
        String targetPreview,
        Long targetOwnerId,
        String targetOwnerName,
        Long reporterId,
        String reporterName,
        String reason,
        ReportStatus status,
        Long resolvedById,
        String resolvedByName,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static AdminReportResponse from(AdminReportItem item) {
        ContentReport r = item.report();
        return new AdminReportResponse(
                r.getId(),
                r.getTargetType(),
                r.getTargetId(),
                item.targetPreview(),
                item.targetOwnerId(),
                item.targetOwnerName(),
                r.getReporter().getId(),
                r.getReporter().getFirstName() + " " + r.getReporter().getLastName(),
                r.getReason(),
                r.getStatus(),
                r.getResolvedBy() != null ? r.getResolvedBy().getId() : null,
                r.getResolvedBy() != null ? r.getResolvedBy().getFirstName() + " " + r.getResolvedBy().getLastName() : null,
                r.getResolvedAt(),
                r.getCreatedAt()
        );
    }
}
