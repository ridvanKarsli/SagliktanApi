package com.ridvankarsli.sagliktanapi.dto.request;

import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReportActionRequest(
        @NotNull(message = "Durum zorunludur")
        ReportStatus status
) {
}
