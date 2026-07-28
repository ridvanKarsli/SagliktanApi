package com.ridvankarsli.sagliktanapi.domain;

public enum ReportStatus {
    PENDING,
    REVIEWED,
    // Admin şikayeti inceleyip aksiyon gerektirmediğine karar verdiğinde.
    REJECTED
}
