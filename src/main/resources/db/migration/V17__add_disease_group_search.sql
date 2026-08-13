-- Gruplar sayfası arama kutusu: PostRepository.search() (bkz. V1 + V11
-- migration'ları) ile birebir aynı prefix + pg_trgm fuzzy (yazım hatası
-- toleranslı) tam metin arama altyapısı, disease_groups tablosuna
-- uygulanıyor - istemci tarafı basit bir substring filtresi yerine
-- backend'deki gönderi aramasıyla tutarlı bir arama deneyimi olsun diye.
-- pg_trgm extension'ı V11'de zaten eklendi, burada tekrar oluşturmuyoruz.

ALTER TABLE disease_groups ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('turkish', coalesce(name, '') || ' ' || coalesce(description, ''))
    ) STORED;

CREATE INDEX idx_disease_groups_search_vector ON disease_groups USING GIN (search_vector);

-- idx_posts_trgm (V11) ile aynı gerekçe: word_similarity() şu an operatörsüz
-- çağrıldığı için planner bunu otomatik kullanmıyor, ama admin panelindeki
-- ILIKE sorgularını hızlandırıyor ve ileride %/<% operatörüne geçilirse
-- hazır oluyor.
CREATE INDEX idx_disease_groups_trgm ON disease_groups
    USING GIN ((coalesce(name, '') || ' ' || coalesce(description, '')) gin_trgm_ops);
