-- ============================================================
-- V14__add_post_attachments.sql
-- Faz 2 adım 4: gönderiye fotoğraf ekleme. Dosyaların kendisi Cloudflare
-- R2'de tutuluyor (bkz. MediaStorageServiceImpl) - burada sadece hangi
-- storage key'in hangi post'a, hangi sırada ait olduğu tutuluyor.
--
-- Bir gönderiye birden fazla fotoğraf eklenebiliyor (galeri/carousel),
-- bu yüzden saved_posts gibi tekil bir kayıt değil, sort_order'lı bir
-- liste yapısı gerekiyor.
-- ============================================================

CREATE TABLE post_attachments (
    id           BIGSERIAL PRIMARY KEY,
    post_id      BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    storage_key  VARCHAR(512) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- Bir gönderinin fotoğraflarını sıralı şekilde çekmek için (bkz.
-- PostAttachmentRepository.findByPostIdOrderBySortOrderAsc).
CREATE INDEX idx_post_attachments_post_sort ON post_attachments(post_id, sort_order);
