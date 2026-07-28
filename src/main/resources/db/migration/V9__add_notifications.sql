-- ============================================================
-- V9__add_notifications.sql
-- Kalıcı bildirimler (WebSocket ile anlık push edilir, bu tablo
-- geçmiş/okunmamış sayısı ve bağlantı koptuğunda senkronizasyon için).
-- ============================================================

CREATE TABLE notifications (
    id            BIGSERIAL PRIMARY KEY,
    recipient_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(30) NOT NULL CHECK (type IN ('NEW_COMMENT', 'COMMENT_REPLY')),
    post_id       BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    comment_id    BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    read          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- Bir kullanıcının bildirim listesini (en yeni önce) ve okunmamış sayısını
-- hızlı çekmek için birleşik indeks.
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_unread ON notifications(recipient_id) WHERE read = FALSE;
