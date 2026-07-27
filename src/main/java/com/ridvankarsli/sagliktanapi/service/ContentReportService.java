package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;

public interface ContentReportService {

    // Idempotent: aynı kullanıcı aynı içeriği tekrar şikayet ederse sessizce
    // geçilir (join() ile aynı desen).
    void report(ReportTargetType targetType, Long targetId, Long reporterId, String reason);
}
