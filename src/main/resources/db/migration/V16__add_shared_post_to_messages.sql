-- ============================================================
-- V16__add_shared_post_to_messages.sql
-- Faz 2 adım 7: bir gönderiyi sohbette birine paylaşma. Gönderi silinirse
-- mesaj geçmişi bozulmasın diye ON DELETE SET NULL - mesaj satırı kalır,
-- frontend shared_post_id NULL ama content/attachment_key de boşsa
-- "gönderi silinmiş" durumunu gösterebilir.
-- ============================================================

ALTER TABLE messages ADD COLUMN shared_post_id BIGINT REFERENCES posts(id) ON DELETE SET NULL;

-- V15'teki "en az biri dolu olmalı" kuralına üçüncü seçenek eklendi -
-- CHECK constraint'i doğrudan ALTER edilemediği için DROP+ADD gerekiyor.
ALTER TABLE messages DROP CONSTRAINT chk_messages_has_content;
ALTER TABLE messages ADD CONSTRAINT chk_messages_has_content
    CHECK (content IS NOT NULL OR attachment_key IS NOT NULL OR shared_post_id IS NOT NULL);

CREATE INDEX idx_messages_shared_post ON messages(shared_post_id) WHERE shared_post_id IS NOT NULL;
