package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.RefreshSession;
import com.ridvankarsli.sagliktanapi.domain.User;

import java.util.List;

// Rapor 4.1 Auth fonksiyonlarının servis katmanı sözleşmesi.
// Bu adımda DTO katmanı henüz kurulmadığı için metodlar ilkel tiplerle
// çalışıyor; DTO/Mapping adımında (rapor adım 7) Controller bunun üzerine
// ince bir DTO <-> domain dönüşüm katmanı olarak eklenecek.
public interface AuthService {

    User register(String email, String rawPassword, String firstName, String lastName, boolean kvkkConsent);

    void verifyEmail(String email, String code);

    // deviceLabel/ipAddress: "Aktif Oturumlar" (görev #305) için - login
    // anında bir RefreshSession satırı oluşturulur, bu bilgiler orada saklanır.
    AuthTokens login(String email, String rawPassword, String deviceLabel, String ipAddress);

    AuthTokens refresh(String refreshToken);

    List<RefreshSession> listActiveSessions(Long userId);

    // sessionRowId: RefreshSession.id (DB PK) - JWT'deki "sid" (sessionId,
    // UUID) DEĞİL. Sahiplik kontrolü (userId eşleşmesi) burada yapılır.
    void revokeSession(Long userId, Long sessionRowId);

    // /api/auth/logout tarafından çağrılır - sessionId burada JWT'deki "sid"
    // claim'i (bkz. JwtAuthenticationFilter.CURRENT_SESSION_ID_ATTRIBUTE),
    // RefreshSession.id DEĞİL. sessionId null olabilir (eski, sid'siz bir
    // access token'la geldiyse) - bu durumda sessizce hiçbir şey yapılmaz.
    void revokeCurrentSession(Long userId, String sessionId);

    void changePassword(Long userId, String currentPassword, String newPassword);

    void requestPasswordReset(String email);

    void resetPassword(String email, String code, String newPassword);
}
