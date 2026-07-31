# Sağlıktan – Büyük Özellikler Teknik Planı (2. Dalga)

Kapsam: sesli not, paylaşılabilir hikaye kartları, farkındalık günü kampanyaları,
aile/bakıcı modu, akran mentor eşleştirmesi, cevapsız soru panosu, ilaç/tedavi
deneyim veritabanı.

Sıralama risk/bağımlılığa göre: önce backend'i etkilemeyen ya da minimal
etkileyen, düşük riskli olanlar; sonra yeni altyapı (dosya depolama,
mesajlaşma) gerektirenler. Her faz kendi içinde commit edilebilir birimlere
bölünecek, her adım bitince özet + git komutları verilecek — geçen sefer
(reaksiyon/bildirim/admin paneli) izlediğimiz aynı yöntem.

**Önemli kısıt:** Bu ortamda Maven/Java 21/Postgres çalıştıramıyorum — backend
değişiklikleri statik/manuel doğrulanacak, gerçek derleme + E2E doğrulaması
push sonrası GitHub Actions'ta olacak. Bu yüzden her adımı küçük tutup sık sık
doğrulamak, geçen seferki gibi büyük bir hata yığınıyla uğraşmaktan daha
güvenli.

---

## Faz 0 — Ortak ön koşul: dosya/medya depolama

Sesli not (Faz 3) için zorunlu, ileride başka medya ihtiyaçları için de temel
oluşturur. Şu an projede hiç dosya depolama altyapısı yok (görsel ekleme
daha önce bilinçli olarak kapsam dışı bırakılmıştı — moderasyon zorluğu).

- Karar: S3-uyumlu bir object storage servisi (öneri: Cloudflare R2 — ücretsiz
  kotası cömert, egress ücreti yok, Render/Vercel ile uyumlu). Alternatif:
  AWS S3, Backblaze B2.
- Backend: presigned URL ile doğrudan client → storage upload (dosya backend
  sunucusundan geçmez, Hikari/heap'e yük binmez), backend sadece
  yetkilendirme + metadata (URL, süre, boyut) kaydeder.
- **Bu faz için sizden karar gerekiyor:** hangi storage sağlayıcısını
  kullanmak istersiniz, hesap/API key sizde açılmalı (ben oluşturamam).

---

## Faz 1 — Düşük risk, hızlı kazanım (yeni backend altyapısı gerektirmiyor)

### 1.1 Paylaşılabilir hikaye kartları
- Tamamen frontend: `html-to-image` veya canvas ile post/başarı hikayesini
  görsele dönüştürme, Web Share API ile paylaşma (fallback: indir).
- Backend değişikliği yok (mevcut post verisi yeterli).
- Risk: düşük. İlk adım olarak önerim.

### 1.2 Farkındalık günü kampanyaları
- Backend: `AwarenessCampaign` entity (diseaseGroupId, başlangıç/bitiş tarihi,
  başlık, mesaj) + admin CRUD (`AdminController`'a ekleme, mevcut pattern).
- Frontend: grup sayfasında aktif kampanya varsa banner, admin panelinde
  kampanya yönetim tab'ı.
- Risk: düşük-orta. Mevcut admin paneli pattern'iyle birebir uyumlu.

---

## Faz 2 — Orta risk (yeni domain modeli, mevcut pattern'lere yakın)

### 2.1 Aile/bakıcı modu (basit versiyon)
- Kapsamı bilinçli daraltıyorum: tam "bağlı hesaplar" modeli (kim kimin
  bakıcısı, izin yönetimi) ilk sürümde YOK — bu ayrı, çok daha büyük bir
  proje. İlk sürüm: `User`'a `profileType` (PATIENT/CAREGIVER/BOTH) alanı,
  kayıt akışında seçim, buna göre onboarding metni ve isteğe bağlı
  "Bakıcı" filtresi/rozeti gruplarda.
- Risk: düşük-orta. Şema değişikliği küçük, davranışsal etkisi çoğunlukla UI.

### 2.2 Cevapsız soru panosu
- Backend: `Post`'a opsiyonel `isQuestion` boolean (paylaşırken "Bu bir
  soru" işaretleme), `GET /api/posts/unanswered` (yorum sayısı 0, N günden
  eski, isQuestion=true).
- Frontend: grup sayfasında "Cevapsız Sorular" ayrı bir bölüm/filtre.
- Risk: orta. Yeni bir sorgu + UI, şema etkisi küçük.

---

## Faz 3 — Yeni altyapı gerektiren (Faz 0 sonrası)

### 3.1 Sesli hikaye/yorum
- Backend: `Comment`/`Post`'a opsiyonel `audioUrl` + `audioDurationSeconds`,
  presigned upload akışı (Faz 0).
- **Moderasyon sorunu — çözülmeden başlanmamalı:** ses dosyası text-search
  edilemez, admin panelindeki mevcut arama/rapor akışı ses içeriğini
  kapsamaz. İki seçenek: (a) yükleme anında otomatik transkript çıkarıp
  metni de saklamak (arama + moderasyon için), (b) admin panelinde ham
  sesi dinleyip inceleyebileceği ayrı bir kuyruk. (a) daha güvenli ama
  bir Speech-to-Text servisi (ayrı maliyet/entegrasyon) gerektiriyor.
- Risk: yüksek. Hem yeni altyapı hem yeni moderasyon yüzeyi.

### 3.2 İlaç/tedavi deneyim veritabanı
- Backend: `Medication` (serbest metin + normalize edilmiş isim, başlangıçta
  kullanıcı girişiyle büyüyen bir liste — hazır ilaç veritabanı ile
  başlamıyoruz), `MedicationExperience` (user, medication, diseaseGroup,
  içerik, opsiyonel etiket/puan).
- Aynı rapor/moderasyon deseni (ContentReport target type genişletilebilir).
- Risk: orta-yüksek. Yeni domain modeli + yeni bir arama/keşif yüzeyi.

---

## Faz 4 — Bağımlılığı olan (mesajlaşma altyapısı gerekiyor)

### 4.1 Akran mentor eşleştirmesi
- **Gizli bağımlılık:** eşleştirme sonrası iki kullanıcının konuşabileceği
  bir kanal olmadan bu özelliğin değeri çok sınırlı — şu an platformda
  özel mesajlaşma (DM) YOK. Ya önce minimal bir DM altyapısı kurulmalı, ya
  da ilk sürümde "eşleştir" sadece bir bildirim + karşılıklı e-posta/iletişim
  izni paylaşımına indirgenmeli (daha zayıf ama hızlı).
- Bu yüzden bu maddeyi son sıraya aldım — DM kararı netleşmeden başlamak
  riskli.

---

## Sıra Önerisi

1. Hikaye kartları (1.1) — sıfır backend riski, hemen başlanabilir
2. Farkındalık günü kampanyaları (1.2)
3. Aile/bakıcı modu, basit versiyon (2.1)
4. Cevapsız soru panosu (2.2)
5. **Karar noktası:** Faz 0 (storage sağlayıcı) + Faz 3 sırası konuşulur
6. Sesli not (3.1) — Faz 0 bittikten sonra
7. İlaç/tedavi veritabanı (3.2)
8. Mentor eşleştirme (4.1) — DM kararı netleştikten sonra

## Çalışma şekli

Geçen seferki gibi: her adımda kodu yazarım → statik/manuel doğrularım →
özet + `git add/commit/push` komutlarını veririm → onayını beklemeden
sıradaki adıma geçerim, istediğin an durdurabilirsin. Her fazın sonunda
gerçek CI/E2E doğrulaması bekleriz.
