package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.Size;

public record ReportRequest(
        @Size(max = 500, message = "Şikayet açıklaması en fazla 500 karakter olabilir")
        String reason
) {
}
