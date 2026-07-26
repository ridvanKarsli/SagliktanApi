package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.User;

// Rapor 4.1 Auth fonksiyonlarının servis katmanı sözleşmesi.
// Bu adımda DTO katmanı henüz kurulmadığı için metodlar ilkel tiplerle
// çalışıyor; DTO/Mapping adımında (rapor adım 7) Controller bunun üzerine
// ince bir DTO <-> domain dönüşüm katmanı olarak eklenecek.
public interface AuthService {

    User register(String email, String rawPassword, String firstName, String lastName);

    void verifyEmail(String email, String code);

    AuthTokens login(String email, String rawPassword);

    AuthTokens refresh(String refreshToken);

    void changePassword(Long userId, String currentPassword, String newPassword);

    void requestPasswordReset(String email);

    void resetPassword(String email, String code, String newPassword);
}
