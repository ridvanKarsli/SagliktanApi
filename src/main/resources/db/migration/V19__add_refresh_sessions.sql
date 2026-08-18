-- Faz 3-2d: "Aktif Oturumlar" (görev #305/#306). Şimdiye kadar refresh
-- token tamamen stateless'ti (bkz. JwtService/AuthServiceImpl) - kullanıcının
-- hangi cihazlardan oturum açtığını görmesinin ya da bir cihazı uzaktan
-- çıkış yaptırmasının hiçbir yolu yoktu, /api/auth/logout bile sadece
-- istemciye "token'ı sil" diyordu. Bu tablo her refresh token'ı (ham token
-- değil, JWT içine gömülen "sid" UUID claim'i - bkz. JwtService) bir satırla
-- eşleştirip cihaz/IP bilgisiyle birlikte saklıyor.
--
-- KAPSAM NOTU: access token'lar hâlâ tamamen stateless kalıyor (her istekte
-- DB'ye gitmiyor) - bir oturum iptal edildiğinde o cihazdaki MEVCUT access
-- token'ı süresi dolana kadar (app.jwt.expiration-ms, varsayılan 1 saat)
-- çalışmaya devam eder, ama bir daha YENİLENEMEZ (refresh reddedilir). Bu,
-- performans/basitlik ile "uzaktan çıkış" ihtiyacı arasında bilinçli bir
-- denge - GitHub/Google gibi büyük servislerin de "oturumu sonlandır"
-- özelliği aynı gecikmeli modeli kullanır.
CREATE TABLE refresh_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id      VARCHAR(36) NOT NULL,
    device_label    VARCHAR(255),
    ip_address      VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMP NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_refresh_sessions_session_id UNIQUE (session_id)
);

CREATE INDEX idx_refresh_sessions_user_id ON refresh_sessions(user_id);

-- Süresi geçmiş/iptal edilmiş oturumları düzenli temizlemek için (bkz.
-- RefreshSessionCleanupJob) - kullanıcı bazlı, aktif olanları sıralı listeleme
-- de bu index'ten faydalanır.
CREATE INDEX idx_refresh_sessions_user_active ON refresh_sessions(user_id, revoked, last_used_at);
