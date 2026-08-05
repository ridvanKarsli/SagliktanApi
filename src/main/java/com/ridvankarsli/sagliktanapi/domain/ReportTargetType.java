package com.ridvankarsli.sagliktanapi.domain;

public enum ReportTargetType {
    POST,
    COMMENT,
    // Faz 2 adım 6: mesajlaşma - rahatsız edici/uygunsuz bir mesaj da
    // artık şikayet edilebilir (bkz. V15 migration, ContentReportServiceImpl).
    MESSAGE
}
