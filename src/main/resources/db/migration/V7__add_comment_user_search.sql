-- Rapor: gelişmiş arama - sadece post değil, yorum içeriği ve kullanıcı adı
-- da aranabilsin. V1'deki posts.search_vector ile aynı desen (GENERATED
-- ALWAYS AS ... STORED + GIN index), native query üzerinden kullanılıyor.

ALTER TABLE comments ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('turkish', coalesce(content, ''))) STORED;

CREATE INDEX idx_comments_search_vector ON comments USING GIN (search_vector);

ALTER TABLE users ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('turkish', coalesce(first_name, '') || ' ' || coalesce(last_name, ''))
    ) STORED;

CREATE INDEX idx_users_search_vector ON users USING GIN (search_vector);
