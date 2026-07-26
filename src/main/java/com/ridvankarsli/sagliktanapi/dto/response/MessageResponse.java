package com.ridvankarsli.sagliktanapi.dto.response;

// Body dönmeyen ama başarı/bilgi mesajı iletmek istediğimiz endpoint'ler için
// (verify-email, forgot-password, logout vb.) genel amaçlı wrapper.
public record MessageResponse(String message) {
}
