-- Faz6: sabitlenmiş gönderi (bkz. kullanıcı geri bildirimi - X'teki gibi bir
-- kullanıcının profilinde "hakkımda" niteliğindeki bir gönderiyi öne
-- çıkarabilmesi). Kullanıcı başına EN FAZLA BİR sabitlenmiş gönderi kuralı
-- - V15__add_messaging.sql'deki bekleyen mesaj isteği kısıtıyla (PARTIAL
-- unique index) AYNI desen: uygulama katmanında (PostServiceImpl.pin())
-- zaten "yeni sabitlerken eskisini otomatik kaldır" mantığı var, ama bu
-- invariant'ı sadece serviste tutmak eşzamanlı isteklerde (aynı kullanıcının
-- iki sekmeden aynı anda iki farklı postu sabitlemesi gibi) yarış durumuna
-- açık - DB seviyesinde PARTIAL unique index (sadece pinned=TRUE satırlar
-- için) bunu kesin biçimde garanti ediyor.
ALTER TABLE posts ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_posts_user_pinned ON posts(user_id) WHERE pinned = TRUE;
