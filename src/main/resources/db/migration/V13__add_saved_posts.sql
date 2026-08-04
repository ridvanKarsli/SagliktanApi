-- ============================================================
-- V13__add_saved_posts.sql
-- Faz 2 adım 3: gönderi yıldızlama (kaydetme). content_reports/reactions'taki
-- polimorfik target_type/target_id deseni yerine doğrudan post_id FK
-- kullanılıyor - kaydetme şu an sadece gönderiler için var (yorum/kişi
-- kaydetme kapsamda değil), gereksiz genellemeye gerek yok.
--
-- Yapı reactions tablosuyla aynı mantığı izliyor (surrogate id + unique
-- constraint) - user_disease_groups'taki gibi composite PK yerine bunu
-- tercih etme sebebi: SavedPost entity'sinin sade bir Long id + iki
-- ManyToOne alanla kalması, @EmbeddedId/@IdClass karmaşıklığına gerek
-- kalmaması.
-- ============================================================

CREATE TABLE saved_posts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),

    -- Bir kullanıcı aynı gönderiyi birden fazla kez kaydedemez - tekrar
    -- kaydetme isteği idempotent olarak ele alınır (bkz. SavedPostServiceImpl).
    CONSTRAINT uq_saved_posts_user_post UNIQUE (user_id, post_id)
);

-- "Kaydedilenler" sekmesi: kullanıcının kaydettiklerini en son kaydedilen
-- en üstte olacak şekilde listelemek için.
CREATE INDEX idx_saved_posts_user_created ON saved_posts(user_id, created_at DESC);
