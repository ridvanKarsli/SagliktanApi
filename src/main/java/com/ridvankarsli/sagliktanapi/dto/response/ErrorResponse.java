package com.ridvankarsli.sagliktanapi.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

// Rapor 4.7: "standart response formatı". Tüm hata cevapları bu şekilde döner.
// fieldErrors sadece @Valid doğrulama hatalarında dolu, diğer durumlarda null.
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), fieldErrors);
    }
}
