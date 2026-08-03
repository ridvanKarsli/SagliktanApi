# Sağlıktan – Halka Açılma (Launch) Yol Haritası

`PRODUCTION_ROADMAP.md` ve `PLAN_yeni_ozellikler.md` tamamlandı: JWT/Gmail
rotasyonu, grup üyelik kontrolü, rate limiting, CORS, KVKK onayı, içerik seed,
şikayet sistemi, actuator, reaksiyon/bildirim/admin paneli, E2E suite yeşil.
Bu doküman bir sonraki aşama — kod doğruluğundan çok, gerçek kullanıcı
trafiğini kaldırma ve sorunları görebilme hazırlığı.

## 0. Asıl blocker'lar (site canlı ve doğru çalışsın diye ŞART)

Bu ikisi, 2026-08 tarihli denetimde bulundu, aşağıdaki 1-6 numaralı
maddelerden farklı olarak "iyi olur" değil, "olmazsa kırık" seviyesinde.

- [x] **Frontend'i Vercel'e deploy et.** Teyit edildi (2026-08-03): proje
      Vercel'de, GitHub'a bağlı, `sagliktan.com` canlı ve doğru içeriği
      serviyor.
- [x] **`app.base-url` prod'da doğru domain'i göstersin.** Railway'de
      `APP_BASE_URL=https://api.sagliktan.com` env değişkeni eklendi,
      deploy edildi, servis "Active" (2026-08-03).

## 1. Gözlemlenebilirlik (en yüksek öncelik)

- [x] Hata izleme aracı ekle (Sentry ücretsiz tier — hem backend hem frontend).
      Tamamlandı (2026-08-03): sentry-spring-boot-4-starter + @sentry/react
      eklendi, canlıda test hatasıyla doğrulandı. send-default-pii/userInfo
      bilerek kapalı (KVKK incelemesi tamamlanana kadar).
- [x] Basit uptime/alarm sistemi kur — Better Stack (UptimeRobot yerine;
      UptimeRobot'un ücretsiz planı 2024 sonundan beri ticari kullanım
      yasaklıyor). Tamamlandı (2026-08-03): sagliktan.com ve
      api.sagliktan.com/actuator/health 3 dakikada bir kontrol ediliyor,
      mail bildirimi açık.
- [ ] (Opsiyonel) Yapılandırılmış log + basit log arama — CI'da backend.log'a
      ulaşmanın bu oturumda ne kadar zor olduğunu gördük, production'da bu
      hiç sürdürülebilir değil

## 2. E-posta altyapısı

- [ ] Gmail SMTP'den transactional email servisine geç (Resend / Brevo /
      Postmark) — Gmail'in günlük gönderim limiti ve otomatik gönderimde
      hesap askıya alma riski var, kayıt/şifre sıfırlama akışının omurgası
      bu maile bağlı. **Bilinçli olarak erteledi (2026-08-03): "gmail yeter,
      onu yapmayalım sonra"** — şimdilik Gmail SMTP ile devam ediliyor.
- [ ] Yeni servisle kayıt doğrulama + şifre sıfırlama akışını uçtan uca test et

## 3. Barındırma kapasitesi

- [ ] Render/Vercel planlarını gözden geçir — backend'in "always-on" bir
      planda olduğunu teyit et (ücretsiz tier 15 dk hareketsizlik sonrası
      uyur, ilk kullanıcı isteğinde 30-60sn beyaz ekran riski)
- [ ] Hikari pool (şu an max 10) ve task executor (2-5 thread) ayarlarının
      beklenen ilk kullanıcı sayısına yeteceğini teyit et

## 4. Hukuki / KVKK inceliği

- [ ] Bir avukata danışarak: kullanıcıların paylaştığı içerik (kendi
      hastalığı/semptomları) KVKK'da "özel nitelikli kişisel veri"
      sayılıyor mu, mevcut genel KVKK onay checkbox'ı bunu kapsıyor mu,
      yoksa ayrı/daha spesifik bir açık rıza mı gerekiyor — teyit et
      (bu konuda kesin hüküm veremem, sadece platform türü gereği flag
      ediyorum)

## 5. Küçük cilalar (acil değil, ilk kullanıcı geri bildirimlerinden sonra da olur)

- [ ] `og:image` / Twitter card meta etiketleri ekle (şu an sadece
      `description` + `theme-color` var — link paylaşınca sosyal medyada
      düz metin görünüyor)
- [ ] Vite build'deki "500kB üstü chunk" uyarısını code-splitting ile azalt
      (mobilde ilk yükleme hızlanır)

## 6. Soft launch önerisi

- [ ] Direkt herkese açmak yerine küçük, kontrollü bir grupla (20-50 kişi,
      tek bir hastalık grubuyla) 1-2 hafta pilot yap — gerçek kullanım
      altında ne kırılıyor gör, barındırma limitlerini gerçek trafikle sına
- [ ] İlk haftalarda topluluk sessiz kalmasın diye 1-2 grupta siz/moderatör
      ilk içeriği başlatın

## Sıra Önerisi

1. `app.base-url` teyidi/düzeltmesi (5 dk, Railway panelinden)
2. Frontend'i Vercel'e deploy et
3. Gözlemlenebilirlik (Sentry + uptime) — bundan sonraki her adımda göz
   olarak lazım
4. E-posta altyapısı — kayıt akışı buna bağlı, en kırılgan nokta
5. Barındırma kapasitesi teyidi
6. KVKK hukuki inceleme (paralel yürütülebilir, avukat cevabı beklenirken
   diğer adımlara devam edilebilir)
7. Soft launch
8. Küçük cilalar
