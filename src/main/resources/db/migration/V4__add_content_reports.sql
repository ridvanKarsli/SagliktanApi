-- ============================================================
-- V4__add_content_reports.sql
-- Kullanıcıların gönderi/yorum şikayet edebilmesi (moderasyonun
-- ilk adımı). Şu an için sadece kayıt tutuluyor; admin panelden
-- inceleme akışı ayrı bir iş olarak ele alınacak.
-- ============================================================

CREATE TABLE content_reports (
    id            BIGSERIAL PRIMARY KEY,
    target_type   VARCHAR(20) NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id     BIGINT NOT NULL,
    reporter_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason        VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED')),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),

    -- Aynı kullanıcı aynı içeriği birden fazla kez şikayet ederek
    -- listeyi şişiremesin diye idempotent tutuluyor (join() ile aynı mantık).
    CONSTRAINT uq_content_reports_target_reporter UNIQUE (target_type, target_id, reporter_id)
);

CREATE INDEX idx_content_reports_target ON content_reports(target_type, target_id);
CREATE INDEX idx_content_reports_status ON content_reports(status);
