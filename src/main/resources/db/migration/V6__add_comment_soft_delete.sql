-- Yorumlar artık gerçekten silinmiyor (soft delete). Bir yorum silindiğinde
-- alt yanıtların bağlı kaldığı tartışma zinciri bozulmasın diye içerik
-- "[Bu yorum silindi]" yer tutucusuyla gösterilir, satır veritabanında kalır.
ALTER TABLE comments ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
