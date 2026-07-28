package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.ContentReport;

// Bir şikayeti, raporlanan içeriğin (post/yorum) kısa önizlemesi ve sahibi
// bilgisiyle birlikte taşıyan admin-paneli-özel görünüm. Controller'ın
// repository'lere erişmeden AdminReportResponse üretebilmesi için
// (bkz. AdminServiceImpl.listReports / AdminReportResponse.from).
public record AdminReportItem(
        ContentReport report,
        String targetPreview,
        Long targetOwnerId,
        String targetOwnerName
) {
}
