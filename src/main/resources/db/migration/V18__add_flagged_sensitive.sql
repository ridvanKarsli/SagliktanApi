-- Basit içerik moderasyonu (bkz. ContentModerationService): kriz sinyali
-- (intihar/kendine zarar verme vb. ifadeler) içeren gönderi/yorumlar
-- ENGELLENMİYOR - sadece işaretleniyor. Frontend bu alanı görünce hem
-- yazana hem okuyana destekleyici bir kaynak bilgisi (182 ALO Yaşam Hattı)
-- gösteriyor. Küfür/spam ise servis katmanında zaten reddediliyor ve hiç
-- bu tabloya ulaşmıyor - onun için ayrı bir kolona gerek yok (bkz. V6
-- migration'daki comments.deleted ile aynı soft-flag deseni).
ALTER TABLE posts ADD COLUMN flagged_sensitive BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE comments ADD COLUMN flagged_sensitive BOOLEAN NOT NULL DEFAULT FALSE;
