package com.ridvankarsli.sagliktanapi.dto.request;

// content, attachmentKey, sharedPostId üçlüsünden en az biri dolu olmalı -
// bu kural DTO validasyonu yerine servis katmanında uygulanıyor (bkz.
// MessageServiceImpl), çünkü "en az biri" ilişkisel bir kural, tek alan
// @NotBlank ile ifade edilemiyor.
public record SendMessageRequest(String content, String attachmentKey, Long sharedPostId) {
}
