# Sağlıktan – Faz 2 Özellikler Planı

Konuşulup kararlaştırıldı (2026-08-04). Kapsam: gönderi medyası, gönderi
sıralama/arama, yıldızlama, hikaye kartı, birebir mesajlaşma.

**Not:** `PLAN_buyuk_ozellikler.md`'deki "hikaye kartları" ve "Faz 0 - dosya
depolama" maddeleri bu dokümanla güncellenip somutlaştırıldı, o dosyadaki
diğer maddeler (sesli not, aile modu, mentor eşleştirme vb.) hâlâ ayrı ve
gündemde değil.

## Alınan kararlar

- **Sıra:** Küçük/bağımsız özellikler önce, mesajlaşma (en büyük/riskli
  parça) en sona.
- **Medya depolama:** Cloudflare R2 (ücretsiz kota cömert, egress ücreti
  yok, Railway ile uyumlu).
- **Video:** İlk fazda YOK, sadece fotoğraf. Video ayrı bir karar/adım.
- **Moderasyon:** Reaktif — mevcut şikayet (report) sistemine güveniyoruz,
  yüklenen medya yayınlanır yayınlanmaz görünür olur, şikayet gelirse admin
  panelinden incelenip kaldırılır. Ek otomatik tarama/admin ön-onayı yok.

## Sıralama ve kapsam

### 1. Gönderi sıralama (etkileşim / tarih)
Backend: `/api/posts` (veya ilgili liste endpoint'i) için sort parametresi
— `recent` (mevcut varsayılan) ve `popular` (reaksiyon sayısına göre).
Frontend: Posts sayfasına sıralama seçici.
Bağımlılık yok, hemen başlanabilir.

### 2. Gönderiler içinde arama
Mevcut genel arama altyapısı (post/yorum/kişi, prefix+fuzzy full-text)
zaten var — bu adım sadece "Gönderiler" sayfasına kendi arama kutusunu
eklemek, mevcut backend endpoint'ini o gruba/bağlama filtreleyerek
kullanmak. Yeni backend altyapısı gerekmiyor, sadece filtreleme + UI.

### 3. Yıldızlama (kaydetme)
Backend: yeni `saved_posts` tablosu (user_id, post_id, created_at),
kaydet/kaydı kaldır endpoint'leri, kullanıcının kaydettiklerini listeleme.
Frontend: post kartında yıldız ikonu, profilde "Kaydedilenler" sekmesi.
Bağımsız, düşük risk.

### 4. Medya altyapısı + gönderiye fotoğraf ekleme
- Cloudflare R2 hesabı/bucket kurulumu, presigned URL ile doğrudan
  client → R2 upload akışı (dosya backend üzerinden geçmiyor, sadece
  backend imzalı bir yükleme linki üretiyor).
- **Sıkıştırma:** yükleme öncesi, tarayıcıda (client-side) fotoğraf
  otomatik olarak yeniden boyutlandırılıp kalitesi hafif düşürülecek
  (örn. en uzun kenar ~1920px'e sınırlanır, JPEG/WebP kalitesi ~%75-80'e
  ayarlanır). Hem depolama maliyetini hem yükleme/gösterim süresini
  düşürür, backend'e ekstra iş bindirmez (görsel işleme sunucuda değil
  tarayıcıda yapılır).
- Backend: `Attachment`/`Media` entity, post'a bağlanması, boyut/tip
  kısıtları (sadece resim: jpg/png/webp, makul bir üst boyut sınırı).
- Frontend: gönderi oluşturma formuna fotoğraf ekleme, önizleme, post
  kartında/detayda görsel gösterimi.
- Moderasyon: yukarıdaki karar gereği reaktif (şikayet sistemi).

### 5. Hikaye kartı (paylaşılabilir görsel)
Bir gönderiyi otomatik olarak tasarlanmış bir görsele (Sağlıktan logolu)
dönüştürüp WhatsApp/Instagram story olarak paylaşılabilir hale getirme.
- Adım 4'ten sonra yapılırsa, kullanıcının eklediği fotoğrafı da karta
  dahil edebiliriz (daha zengin kart); metin-only versiyon adım 4'ü
  beklemeden de yapılabilir ama en iyi sonuç için sıradaki yerinde kalması
  öneriliyor.
- Teknik yaklaşım (netleştirilecek): frontend'de canvas ile client-side
  render + "Paylaş" (Web Share API) muhtemelen en basit yol — backend'de
  görsel üretmeye göre altyapı gerektirmiyor. Uygulama adımında detaylandırılır.

### 6. Birebir mesajlaşma (en büyük parça, ayrı bir alt-plan gerektirir)
Kapsam: mesaj isteği → kabul/red, kabul sonrası serbest mesajlaşma,
engelleme, şikayet, mesajda fotoğraf gönderme (adım 4'ün altyapısını
kullanır).
- Yeni şema: `conversations`, `messages`, `message_requests` (pending/
  accepted/rejected), `blocked_users`.
- Gerçek zamanlı iletim: şu anki WebSocket kurulumu tek yönlü (sunucu →
  istemci bildirim). Mesajlaşma için çift yönlü olması gerekiyor —
  STOMP `@MessageMapping` controller eklenmesi, ya da mesaj gönderimini
  REST ile yapıp anlık teslimatı mevcut bildirim kanalına benzer şekilde
  WS push ile yapmak (daha az değişiklik, muhtemelen tercih edilecek).
- Şikayet: mevcut content-report sistemine "MESSAGE" hedef tipi eklenerek
  genişletilir.
- Bu adım kendi içinde ayrı bir alt-plana (B1, B2, B3... gibi) bölünecek,
  uygulama başlarken detaylandırılır.

### 7. Gönderiyi kişiye gönderme
Mesajlaşma bittikten sonra anlamlı — sohbet listesinden birini seçip
gönderiyi mesaj olarak paylaşma. Adım 6'ya bağımlı, ondan önce yapılamaz.

## Çalışma şekli

Her adım `develop` branch'inde yapılıp staging'de (Railway + Vercel
preview) test edilecek, sorunsuzsa `main`'e PR ile merge edilip gerçek
canlıya alınacak. Backend + frontend birlikte, her adım kendi içinde
commit edilebilir bir birim olacak.
