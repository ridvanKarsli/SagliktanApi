-- ============================================================
-- V2__add_kvkk_consent.sql
-- KVKK (6698 sayılı Kanun) kapsamında kayıt sırasında açık rıza
-- alındığının kaydı. Var olan satırları bozmamak için nullable
-- eklendi; yeni kayıtlarda uygulama katmanında zorunlu tutulur
-- (bkz. AuthServiceImpl.register).
-- ============================================================

ALTER TABLE users ADD COLUMN kvkk_consent_at TIMESTAMP;
