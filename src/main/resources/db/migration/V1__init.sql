-- ============================================================
-- V1__init.sql
-- Sağlıktan API - İlk şema: users, disease_groups, sub_groups,
-- user_disease_groups (üyelik), posts, comments
-- ============================================================

-- Ortak: updated_at kolonunu otomatik güncelleyen trigger fonksiyonu
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id                              BIGSERIAL PRIMARY KEY,
    email                           VARCHAR(255) NOT NULL UNIQUE,
    password_hash                   VARCHAR(255) NOT NULL,
    first_name                      VARCHAR(100) NOT NULL,
    last_name                       VARCHAR(100) NOT NULL,
    bio                             VARCHAR(1000),
    role                            VARCHAR(20) NOT NULL DEFAULT 'USER'
                                        CHECK (role IN ('USER', 'ADMIN')),

    email_verified                  BOOLEAN NOT NULL DEFAULT FALSE,
    verification_code               VARCHAR(10),
    verification_code_expires_at    TIMESTAMP,

    reset_code                      VARCHAR(10),
    reset_code_expires_at           TIMESTAMP,

    active                          BOOLEAN NOT NULL DEFAULT TRUE,

    created_at                      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- DISEASE_GROUPS
-- ============================================================
CREATE TABLE disease_groups (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(150) NOT NULL UNIQUE,
    description  VARCHAR(1000),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_disease_groups_updated_at
    BEFORE UPDATE ON disease_groups
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- SUB_GROUPS (bir hastalık grubuna bağlı alt kategoriler:
-- Sosyalleşme, Tavsiyeler, Ek Gıda vb.)
-- ============================================================
CREATE TABLE sub_groups (
    id                 BIGSERIAL PRIMARY KEY,
    disease_group_id   BIGINT NOT NULL REFERENCES disease_groups(id) ON DELETE CASCADE,
    name               VARCHAR(150) NOT NULL,
    description        VARCHAR(1000),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_sub_groups_name_per_disease_group UNIQUE (disease_group_id, name)
);

CREATE INDEX idx_sub_groups_disease_group_id ON sub_groups(disease_group_id);

CREATE TRIGGER trg_sub_groups_updated_at
    BEFORE UPDATE ON sub_groups
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- USER_DISEASE_GROUPS (kullanıcı - hastalık grubu üyeliği, N:N)
-- ============================================================
CREATE TABLE user_disease_groups (
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    disease_group_id  BIGINT NOT NULL REFERENCES disease_groups(id) ON DELETE CASCADE,
    joined_at         TIMESTAMP NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, disease_group_id)
);

CREATE INDEX idx_user_disease_groups_disease_group_id ON user_disease_groups(disease_group_id);

-- ============================================================
-- POSTS
-- ============================================================
CREATE TABLE posts (
    id             BIGSERIAL PRIMARY KEY,
    sub_group_id   BIGINT NOT NULL REFERENCES sub_groups(id) ON DELETE CASCADE,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(255) NOT NULL,
    content        TEXT NOT NULL,

    search_vector  tsvector GENERATED ALWAYS AS (
                       to_tsvector('turkish', coalesce(title, '') || ' ' || coalesce(content, ''))
                   ) STORED,

    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_sub_group_id ON posts(sub_group_id);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_search_vector ON posts USING GIN (search_vector);

CREATE TRIGGER trg_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- COMMENTS
-- ============================================================
CREATE TABLE comments (
    id           BIGSERIAL PRIMARY KEY,
    post_id      BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);

CREATE TRIGGER trg_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
