-- ============================================================
-- V8__add_reactions.sql
-- Gönderi/yorum reaksiyonları (beğeni yerine "Faydalı" / "Faydalı Değil").
-- content_reports ile aynı polimorfik desen (target_type + target_id).
-- ============================================================

CREATE TABLE reactions (
    id            BIGSERIAL PRIMARY KEY,
    target_type   VARCHAR(20) NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id     BIGINT NOT NULL,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value         VARCHAR(20) NOT NULL CHECK (value IN ('HELPFUL', 'NOT_HELPFUL')),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),

    -- Kullanıcı başına hedef başına tek reaksiyon - tekrar reaksiyon
    -- vermek güncelleme (UPSERT benzeri) olarak ele alınır.
    CONSTRAINT uq_reactions_target_user UNIQUE (target_type, target_id, user_id)
);

CREATE INDEX idx_reactions_target ON reactions(target_type, target_id);
CREATE INDEX idx_reactions_user_id ON reactions(user_id);

CREATE TRIGGER trg_reactions_updated_at
    BEFORE UPDATE ON reactions
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
