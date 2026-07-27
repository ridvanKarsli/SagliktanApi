# Sağlıktan – Production Yol Haritası

Railway Pro'ya geçiş ve e-posta/SMTP konusu ayrı yürütülüyor, bu listeye dahil değil.

## Backend (SagliktanApi)

- [ ] JWT secret ve Gmail şifresini rotate et (bu konuşmalarda açık yazıldı, gerçek kullanıcı verisi girmeden önce şart)
- [ ] Grup üyeliği kontrolünü sıkılaştır — kullanıcı üye olmadığı gruba post/yorum atamamalı
- [ ] Rate limiting ekle — register/login/post uçlarına karşı spam/abuse koruması
- [ ] CORS politikasını sıkılaştır — sadece sagliktan.com ve api.sagliktan.com origin'lerine izin ver
- [ ] Kayıt akışına KVKK açık rıza onayı (checkbox + backend'de zorunlu alan) ekle
- [ ] İlk içerik seed mekanizması — Flyway data migration veya basit admin endpoint ile örnek hastalık grubu/alt grup/post
- [ ] Rapor etme (şikayet) endpoint'i — post/yorum için
- [ ] Actuator/monitoring ekle — health check, temel metrikler
- [ ] JPA/Hikari/mail-executor ayarlarını production trafiğine göre tune et
- [ ] Temel entegrasyon testleri + CI pipeline

## Web (SagliktanWeb)

- [ ] "Bu tıbbi tavsiye değildir" uyarısını post/detay sayfalarında görünür şekilde göster
- [ ] Gizlilik Politikası + KVKK Aydınlatma Metni sayfası oluştur, footer'a link ekle
- [ ] Kayıt formuna KVKK onay checkbox'ı ekle (backend'deki zorunlu alanla eşleşecek)
- [ ] Post/yorum için "şikayet et" butonu ekle
- [ ] Boş grup/alt grup ekranları için "henüz içerik yok" empty state tasarımı
- [ ] Paylaşılabilirlik için temel SEO/meta ayarları (og:image, meta description) gözden geçir

## Sıra Önerisi

1. JWT/secret rotasyonu
2. KVKK metinleri + onay akışı (backend + web birlikte)
3. Grup üyeliği kontrolü, rate limiting, CORS
4. İçerik seed + rapor etme
5. Monitoring, tuning, testler
