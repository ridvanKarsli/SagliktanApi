package com.ridvankarsli.sagliktanapi.dto.request;

import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReportActionRequest(
        @NotNull(message = "Durum zorunludur")
        ReportStatus status,

        // true ise, durum güncellemesiyle AYNI işlemde raporlanan içerik
        // (post/yorum) de silinir - bkz. AdminServiceImpl.resolveReport.
        // İstenmezse (varsayılan/JSON'da yoksa) false kabul edilir.
        boolean deleteContent
) {
}
