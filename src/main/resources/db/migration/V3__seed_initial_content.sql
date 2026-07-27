-- ============================================================
-- V3__seed_initial_content.sql
-- İlk lansman için tek bir hastalık grubuna odaklanma stratejisi
-- (önce Retinitis Pigmentosa topluluğunu sağlamlaştır, sonra
-- diğer hastalıklara genişle). Boş bir grup ekranıyla karşılaşan
-- ilk kullanıcılar için temel alt gruplar burada tanımlanıyor.
-- Gönderi (post) seed edilmiyor, çünkü her post gerçek bir
-- kullanıcıya (user_id) bağlı olmak zorunda - ilk içerik gerçek
-- hesaptan elle paylaşılmalı.
-- ============================================================

INSERT INTO disease_groups (name, description)
VALUES (
    'Retinitis Pigmentosa',
    'Retinitis pigmentosa hastaları ve yakınları için deneyim paylaşımı, dayanışma ve bilgi alışverişi topluluğu.'
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO sub_groups (disease_group_id, name, description)
SELECT dg.id, v.name, v.description
FROM disease_groups dg
CROSS JOIN (VALUES
    ('Sohbet & Sosyalleşme', 'Günlük sohbet, tanışma ve moral desteği için genel paylaşım alanı.'),
    ('Deneyim Paylaşımları', 'Tanı süreci, günlük hayata uyum ve kişisel deneyimlerin paylaşıldığı alan.'),
    ('Tedavi & Araştırmalar', 'Güncel tedavi seçenekleri, klinik çalışmalar ve gen tedavisi ile ilgili paylaşımlar. Bu alandaki içerik tıbbi tavsiye niteliği taşımaz.'),
    ('Soru-Cevap', 'Topluluğa yöneltilen soruların ve deneyime dayalı yanıtların paylaşıldığı alan.')
) AS v(name, description)
WHERE dg.name = 'Retinitis Pigmentosa'
ON CONFLICT (disease_group_id, name) DO NOTHING;
