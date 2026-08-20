package com.ridvankarsli.sagliktanapi.domain;

public enum ReportTargetType {
    POST,
    COMMENT,
    // Faz 2 adım 6: mesajlaşma - rahatsız edici/uygunsuz bir mesaj da
    // artık şikayet edilebilir (bkz. V15 migration, ContentReportServiceImpl).
    MESSAGE,
    // Faz7-8: tek bir içerik değil, doğrudan bir kullanıcının kendisi
    // (profili/davranışı) şikayet edilebiliyor - önceden şikayet sadece bir
    // sohbet içindeyken mesaj üzerinden mümkündü, artık herkese açık
    // profilden de erişilebilir (bkz. UserController.report, UserProfile.jsx).
    USER
}
