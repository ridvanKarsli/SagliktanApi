-- ============================================================
-- V5__add_comment_replies.sql
-- Yorumlara yanıt (tek seviye nested reply) desteği. Bir yoruma
-- yanıt yazılabilir; yanıta yanıt yazılırsa uygulama katmanında
-- otomatik olarak en üstteki yoruma bağlanır (parent_comment_id
-- her zaman bir üst-seviye yorumu gösterir, derinlik sınırsız
-- büyümez). Bkz. CommentServiceImpl.resolveParent.
-- ============================================================

ALTER TABLE comments ADD COLUMN parent_comment_id BIGINT REFERENCES comments(id) ON DELETE CASCADE;

CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id);
