-- ============================================================
-- V15__add_messaging.sql
-- Faz 2 adım 6: birebir mesajlaşma. Herkes doğrudan mesajlaşamaz - önce
-- bir message_request kabul edilmeli (istenmeyen/rahatsız edici mesaj
-- akışını en baştan engellemek için, bkz. PLAN_faz2_ozellikler.md).
-- ============================================================

CREATE TABLE message_requests (
    id            BIGSERIAL PRIMARY KEY,
    sender_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    responded_at  TIMESTAMP,

    CONSTRAINT chk_message_requests_not_self CHECK (sender_id <> recipient_id)
);

-- Aynı gönderenden aynı alıcıya birden fazla bekleyen istek gitmesin (spam
-- önleme) - kasıtlı olarak PARTIAL unique index (sadece PENDING satırlar
-- için). Bunu tablo seviyesinde UNIQUE(sender_id, recipient_id, status)
-- olarak tanımlamak YANLIŞ olurdu: bir istek reddedilip (status=REJECTED)
-- sonra tekrar gönderilip tekrar reddedilirse, aynı (sender, recipient,
-- REJECTED) üçlüsüne sahip iki satır oluşur ve constraint ihlali fırlatırdı.
-- Partial index sadece PENDING durumundaki satırlar arasında benzersizliği
-- garanti eder, geçmiş (kabul/red edilmiş) istekler serbestçe birikebilir.
CREATE UNIQUE INDEX uq_message_requests_pending ON message_requests(sender_id, recipient_id) WHERE status = 'PENDING';
CREATE INDEX idx_message_requests_recipient_pending ON message_requests(recipient_id) WHERE status = 'PENDING';
CREATE INDEX idx_message_requests_sender ON message_requests(sender_id);

-- Her kullanıcı çifti için tek konuşma. user_one_id her zaman user_two_id'den
-- küçük tutulur (uygulama katmanında garanti edilir) - böylece A-B ve B-A
-- için yanlışlıkla iki ayrı konuşma açılamaz.
CREATE TABLE conversations (
    id            BIGSERIAL PRIMARY KEY,
    user_one_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_two_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_conversations_ordered CHECK (user_one_id < user_two_id),
    CONSTRAINT uq_conversations_pair UNIQUE (user_one_id, user_two_id)
);

CREATE INDEX idx_conversations_user_one ON conversations(user_one_id);
CREATE INDEX idx_conversations_user_two ON conversations(user_two_id);

CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT,
    -- Faz 2 adım 4'ün fotoğraf altyapısı (MediaStorageService/R2) burada da
    -- kullanılıyor - mesaj başına tek fotoğraf yeterli, post galerisindeki
    -- gibi çoklu fotoğraf/sort_order'a gerek yok.
    attachment_key  VARCHAR(512),
    read_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_messages_has_content CHECK (content IS NOT NULL OR attachment_key IS NOT NULL)
);

CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
CREATE INDEX idx_messages_conversation_unread ON messages(conversation_id) WHERE read_at IS NULL;

CREATE TABLE blocked_users (
    id            BIGSERIAL PRIMARY KEY,
    blocker_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_blocked_users_not_self CHECK (blocker_id <> blocked_id),
    CONSTRAINT uq_blocked_users_pair UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_blocked_users_blocker ON blocked_users(blocker_id);
CREATE INDEX idx_blocked_users_blocked ON blocked_users(blocked_id);

-- Şikayet sistemine mesaj hedefi ekleniyor (bkz. V4/V10 content_reports) -
-- rahatsız edici bir mesaj da artık şikayet edilebilir.
ALTER TABLE content_reports DROP CONSTRAINT content_reports_target_type_check;
ALTER TABLE content_reports ADD CONSTRAINT content_reports_target_type_check
    CHECK (target_type IN ('POST', 'COMMENT', 'MESSAGE'));
