-- ============================================================
-- V13__seed_bulk_test_data.sql
-- GEÇİCİ - sadece tasarımı gerçekçi miktarda veriyle görebilmek için.
-- Sahte kullanıcılar, gönderiler, yorumlar (bazılarında 4-6 seviye
-- derinliğinde yanıt zinciri) ve reaksiyonlar ekler. İşin bitince bu
-- migration'ı geri alan bir sonraki migration (V14) ile kaldıracağız -
-- Flyway migration'ları elle silmek geçmiş kaydını bozar, bu yüzden
-- veriyi temizlemek için YENİ bir migration eklemek doğru yöntem.
--
-- SADECE YEREL/GELİŞTİRME VERİTABANINDA ÇALIŞTIR. Prod'a deploy etme.
--
-- Sahte kullanıcıların şifre hash'i bilinçli olarak bilinmeyen bir
-- placeholder (login denemesi hata verir ama uygulama çökmez) - bu
-- hesaplara giriş yapmak gerekmiyor, sadece içerik yazarı olarak var
-- olmaları yeterli.
-- ============================================================

DO $$
DECLARE
    placeholder_hash CONSTANT VARCHAR := '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

    first_names TEXT[] := ARRAY['Ahmet','Ayşe','Mehmet','Elif','Mustafa','Zeynep','Emre','Merve',
                                 'Can','Selin','Burak','Ece','Kerem','Gizem','Onur','İrem',
                                 'Yusuf','Deniz','Cem','Nazlı','Serkan','Buse','Tolga','Aslı'];
    last_names  TEXT[] := ARRAY['Yılmaz','Demir','Şahin','Çelik','Yıldız','Aydın','Arslan','Doğan',
                                 'Kaya','Öztürk','Koç','Kurt','Özdemir','Aksoy','Polat','Erdoğan',
                                 'Güneş','Şimşek','Uçar','Bulut'];

    titles_sohbet TEXT[] := ARRAY[
        'Bugün herkese merhaba demek istedim','Yeni katıldım, kendimi tanıtayım',
        'Hafta sonu nasıl geçti sizlere?','Küçük bir iyi haberim var, paylaşmak istedim',
        'Bu gruptan çok şey öğreniyorum, teşekkürler','Bugün moralim biraz düşüktü, buraya yazınca iyi geldi'];
    bodies_sohbet TEXT[] := ARRAY[
        'Bu gruba yeni katıldım, burada birçok kişinin benzer şeyler yaşadığını görmek gerçekten güven verici.',
        'Bugün güzel bir gün geçirdim, biraz da buradan bahsetmek istedim.',
        'Sizinle sohbet etmek, aynı şeyleri yaşayan insanlarla bir arada olmak çok değerli.',
        'Uzun zamandır buradayım ama ilk defa yazıyorum, herkese merhaba.'];

    titles_deneyim TEXT[] := ARRAY[
        'Tanı sürecim nasıl başladı','Gece görüşüm zorlaşınca yaptığım küçük değişiklikler',
        'Beyaz baston kullanmaya başladığım gün','Görme alanımdaki daralmayı fark ettiğim an',
        'İş hayatında karşılaştığım zorluklar ve çözümlerim','Ailemle bu süreci nasıl konuştuk'];
    bodies_deneyim TEXT[] := ARRAY[
        'Bu deneyimimi paylaşmak istedim, belki benzer bir şey yaşayan biri için faydalı olur.',
        'Bu süreçte öğrendiğim en önemli şey, kendime zaman tanımak oldu.',
        'İlk başta zor geldi ama zamanla kendi rutinimi oluşturdum.',
        'Aynı şeyleri yaşayanların deneyimlerini okumak bana çok yardımcı oldu, ben de kendi hikayemi bırakmak istedim.'];

    titles_tedavi TEXT[] := ARRAY[
        'Gen tedavisiyle ilgili okuduğum bir haber','Yeni bir klinik çalışma duyurusu gördüm',
        'Vitamin A takviyesi hakkında ne düşünüyorsunuz?','Düzenli göz kontrolü deneyimlerinizi merak ediyorum',
        'Araştırmalardaki güncel gelişmeler hakkında bir yazı okudum'];
    bodies_tedavi TEXT[] := ARRAY[
        'Bu konuda okuduklarımı paylaşmak istedim, tabii ki herkes kendi doktoruyla değerlendirmeli.',
        'Bilgi tıbbi tavsiye niteliği taşımıyor, sadece okuduğum bir kaynağı paylaşmak istedim.',
        'Doktoruma da sormayı düşünüyorum, sizin bu konudaki deneyimlerinizi merak ediyorum.',
        'Bu alandaki gelişmeleri takip etmek umut verici, sizlerle de paylaşmak istedim.'];

    titles_soru TEXT[] := ARRAY[
        'Görme alanı daralması ile ilgili deneyiminiz var mı?','Ekran okuyucu için önerisi olan var mı?',
        'Sürüş ehliyeti konusunda bilgisi olan var mı?','İş yerinde hangi düzenlemeleri talep ettiniz?',
        'Gece görüşü için kullandığınız bir yöntem var mı?','Çocuğuma nasıl anlatabilirim, önerisi olan var mı?'];
    bodies_soru TEXT[] := ARRAY[
        'Bu konuda deneyimi olan varsa çok memnun olurum, kendi durumumu değerlendirmeme yardımcı olur.',
        'Benzer bir soruyla karşılaşan oldu mu bilmiyorum ama sormak istedim.',
        'Sizce hangi kaynaklara bakmalıyım, önerisi olan var mı?',
        'Bu konuda ne yapacağımı bilemedim, sizlerin deneyimlerini merak ediyorum.'];

    comment_templates TEXT[] := ARRAY[
        'Bu paylaşımın için teşekkürler, benim için de faydalı oldu.',
        'Aynı süreçten geçiyorum, yalnız olmadığını bilmeni isterim.',
        'Bu konuyu doktoruma da sormuştum, benzer şeyler söylemişti.',
        'Paylaşımını okumak iyi geldi, teşekkürler.',
        'Ben de benzer bir şey yaşadım, zamanla kolaylaşıyor.',
        'Çok değerli bir paylaşım olmuş, elinize sağlık.',
        'Bu konuda bende de sorular var, takipteyim.',
        'Anlattıkların bana çok tanıdık geldi.'];
    reply_templates TEXT[] := ARRAY[
        'Katılıyorum, ben de böyle düşünüyorum.',
        'Bunu bilmiyordum, bilgi için teşekkürler.',
        'Aynen öyle, benim için de böyle oldu.',
        'Çok haklısın, ben de aynısını yaşadım.',
        'Bu konuda daha fazla konuşabilir miyiz?'];

    v_user_ids BIGINT[] := ARRAY[]::BIGINT[];
    v_new_id BIGINT;
    v_group RECORD;
    v_group_id BIGINT;
    v_sub RECORD;
    v_author BIGINT;
    v_reactor BIGINT;
    v_post_id BIGINT;
    v_comment_id BIGINT;
    v_parent_id BIGINT;
    v_title TEXT;
    v_body TEXT;
    i INT;
    j INT;
    k INT;
    depth INT;
    posts_per_subgroup CONSTANT INT := 14;
    num_users CONSTANT INT := 20;
BEGIN
    -- ---------- 1) Sahte kullanıcılar ----------
    FOR i IN 1..num_users LOOP
        INSERT INTO users (email, password_hash, first_name, last_name, role, email_verified, active, kvkk_consent_at)
        VALUES (
            'seed.test.' || i || '@example.com',
            placeholder_hash,
            first_names[1 + floor(random() * array_length(first_names, 1))::int],
            last_names[1 + floor(random() * array_length(last_names, 1))::int],
            'USER', TRUE, TRUE, now()
        )
        RETURNING id INTO v_new_id;
        v_user_ids := array_append(v_user_ids, v_new_id);
    END LOOP;

    -- ---------- 2) Ek hastalık grupları (grup listesi ızgarasında çeşitlilik için) ----------
    INSERT INTO disease_groups (name, description) VALUES
        ('Kistik Fibrozis', 'Kistik fibrozis hastaları ve yakınları için deneyim paylaşımı ve dayanışma topluluğu.'),
        ('Tip 1 Diyabet', 'Tip 1 diyabetle yaşayanlar için bilgi alışverişi ve destek topluluğu.'),
        ('Fibromiyalji', 'Fibromiyalji ile yaşayanlar için deneyim paylaşımı topluluğu.')
    ON CONFLICT (name) DO NOTHING;

    INSERT INTO sub_groups (disease_group_id, name, description)
    SELECT dg.id, v.name, v.description
    FROM disease_groups dg
    CROSS JOIN (VALUES
        ('Sohbet', 'Genel sohbet alanı.'),
        ('Deneyim Paylaşımları', 'Deneyim paylaşımlarının yapıldığı alan.'),
        ('Soru-Cevap', 'Soruların yanıtlandığı alan.')
    ) AS v(name, description)
    WHERE dg.name IN ('Kistik Fibrozis', 'Tip 1 Diyabet', 'Fibromiyalji')
    ON CONFLICT (disease_group_id, name) DO NOTHING;

    -- ---------- 3) Her hastalık grubu için: tüm sahte kullanıcıları üye yap,
    --              her alt gruba gönderi/yorum/reaksiyon ekle ----------
    FOR v_group IN SELECT id, name FROM disease_groups LOOP
        v_group_id := v_group.id;

        FOREACH v_author IN ARRAY v_user_ids LOOP
            INSERT INTO user_disease_groups (user_id, disease_group_id)
            VALUES (v_author, v_group_id)
            ON CONFLICT DO NOTHING;
        END LOOP;

        FOR v_sub IN SELECT id, name FROM sub_groups WHERE disease_group_id = v_group_id LOOP
            FOR j IN 1..posts_per_subgroup LOOP
                v_author := v_user_ids[1 + floor(random() * array_length(v_user_ids, 1))::int];

                CASE v_sub.name
                    WHEN 'Deneyim Paylaşımları' THEN
                        v_title := titles_deneyim[1 + floor(random() * array_length(titles_deneyim, 1))::int];
                        v_body := bodies_deneyim[1 + floor(random() * array_length(bodies_deneyim, 1))::int];
                    WHEN 'Tedavi & Araştırmalar' THEN
                        v_title := titles_tedavi[1 + floor(random() * array_length(titles_tedavi, 1))::int];
                        v_body := bodies_tedavi[1 + floor(random() * array_length(bodies_tedavi, 1))::int];
                    WHEN 'Soru-Cevap' THEN
                        v_title := titles_soru[1 + floor(random() * array_length(titles_soru, 1))::int];
                        v_body := bodies_soru[1 + floor(random() * array_length(bodies_soru, 1))::int];
                    ELSE
                        v_title := titles_sohbet[1 + floor(random() * array_length(titles_sohbet, 1))::int];
                        v_body := bodies_sohbet[1 + floor(random() * array_length(bodies_sohbet, 1))::int];
                END CASE;

                INSERT INTO posts (sub_group_id, user_id, title, content)
                VALUES (v_sub.id, v_author, v_title, v_body)
                RETURNING id INTO v_post_id;

                -- Rastgele reaksiyonlar (0-6 farklı kullanıcıdan).
                FOR k IN 1..floor(random() * 6)::int LOOP
                    v_reactor := v_user_ids[1 + floor(random() * array_length(v_user_ids, 1))::int];
                    IF v_reactor <> v_author THEN
                        INSERT INTO reactions (target_type, target_id, user_id, value)
                        VALUES ('POST', v_post_id, v_reactor,
                                CASE WHEN random() < 0.8 THEN 'HELPFUL' ELSE 'NOT_HELPFUL' END)
                        ON CONFLICT (target_type, target_id, user_id) DO NOTHING;
                    END IF;
                END LOOP;

                -- Üst seviye yorumlar (0-5 adet).
                v_parent_id := NULL;
                FOR k IN 1..floor(random() * 5)::int LOOP
                    v_author := v_user_ids[1 + floor(random() * array_length(v_user_ids, 1))::int];
                    INSERT INTO comments (post_id, user_id, content, parent_comment_id)
                    VALUES (v_post_id, v_author,
                            comment_templates[1 + floor(random() * array_length(comment_templates, 1))::int],
                            NULL)
                    RETURNING id INTO v_comment_id;
                    v_parent_id := v_comment_id;
                END LOOP;

                -- Gönderilerin ~%40'ında, son yoruma 4-6 seviye derinliğinde
                -- yanıt zinciri ekle (X tarzı "düz yanıt" tasarımının 2.
                -- seviyeden itibaren nasıl göründüğünü gerçek veriyle görmek için).
                IF v_parent_id IS NOT NULL AND random() < 0.4 THEN
                    depth := 4 + floor(random() * 3)::int;
                    FOR k IN 1..depth LOOP
                        v_author := v_user_ids[1 + floor(random() * array_length(v_user_ids, 1))::int];
                        INSERT INTO comments (post_id, user_id, content, parent_comment_id)
                        VALUES (v_post_id, v_author,
                                reply_templates[1 + floor(random() * array_length(reply_templates, 1))::int],
                                v_parent_id)
                        RETURNING id INTO v_comment_id;
                        v_parent_id := v_comment_id;
                    END LOOP;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
