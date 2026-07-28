-- ============================================================
-- V10__extend_content_reports.sql
-- Admin paneli için: şikayet durumuna REJECTED ekleniyor, hangi admin'in
-- ne zaman incelediğini tutan denetim izi (audit trail) kolonları.
-- ============================================================

ALTER TABLE content_reports DROP CONSTRAINT content_reports_status_check;
ALTER TABLE content_reports ADD CONSTRAINT content_reports_status_check
    CHECK (status IN ('PENDING', 'REVIEWED', 'REJECTED'));

ALTER TABLE content_reports ADD COLUMN resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE content_reports ADD COLUMN resolved_at TIMESTAMP;
