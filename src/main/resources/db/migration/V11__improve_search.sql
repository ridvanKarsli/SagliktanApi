-- Arama motorunu geliştirme: mevcut plainto_tsquery sadece TAM kelime
-- eşleşmesi yapıyordu (Türkçe stemming sonrası) - kullanıcı "diyab" yazınca
-- "diyabet" bulunamıyordu, yazım hatalarına da toleransı yoktu. Bu migration
-- iki katman ekliyor:
--   1) Prefix arama (harf harf yazarken bulma): safe_prefix_tsquery()
--      fonksiyonu, girilen her kelimeyi ':*' ile prefix-lexeme'e çevirip
--      mevcut search_vector/GIN index'lerini kullanır.
--   2) pg_trgm ile yazım hatası toleranslı (fuzzy) benzerlik araması -
--      word_similarity(), uzun içerik metni içindeki en yakın kelimeyi bulur.
-- İkisi de aynı sorguda OR ile birleşip GREATEST(...) ile skorlanıyor, tek
-- bir sonuç listesi + tek bir relevance sıralaması dönüyor.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Kullanıcı girdisini güvenli bir prefix tsquery'e çeviren yardımcı
-- fonksiyon. tsquery sözdizimine özel karakterleri (harf/rakam/boşluk
-- olmayan her şeyi) boşlukla değiştirip kelimelere ayırıyor, her kelimeye
-- ':*' (prefix) ekleyip '&' (VE) ile birleştiriyor. Girdi tamamen boş/özel
-- karakterse NULL tsquery döner - "@@ NULL" daima NULL (=false) olduğundan
-- hata fırlatmadan boş sonuç kümesi üretir.
CREATE OR REPLACE FUNCTION safe_prefix_tsquery(input text, cfg regconfig DEFAULT 'turkish')
RETURNS tsquery AS $$
    SELECT to_tsquery(cfg, string_agg(lexeme || ':*', ' & '))
    FROM unnest(
        regexp_split_to_array(
            trim(regexp_replace(coalesce(input, ''), '[^[:alnum:][:space:]]', ' ', 'g')),
            '\s+'
        )
    ) AS lexeme
    WHERE lexeme <> ''
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE;

-- Fuzzy (yazım hatası toleranslı) eşleştirme için trigram index'ler.
-- Not: aşağıdaki arama sorguları word_similarity() fonksiyonunu doğrudan
-- (operatörsüz) çağırıyor, bu yüzden bu index'ler şu an planner tarafından
-- otomatik kullanılmıyor - ama admin panelindeki ILIKE '%...%' sorgularını
-- (UserRepository.adminSearch) hızlandırıyorlar ve ileride %/<% operatörüne
-- geçilirse hazır oluyorlar.
CREATE INDEX idx_posts_trgm ON posts USING GIN ((title || ' ' || content) gin_trgm_ops);
CREATE INDEX idx_comments_trgm ON comments USING GIN (content gin_trgm_ops);
CREATE INDEX idx_users_name_trgm ON users USING GIN ((first_name || ' ' || last_name) gin_trgm_ops);
CREATE INDEX idx_users_email_trgm ON users USING GIN (email gin_trgm_ops);
