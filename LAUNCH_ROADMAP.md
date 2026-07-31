# Sağlıktan – Halka Açılma (Launch) Yol Haritası

`PRODUCTION_ROADMAP.md` ve `PLAN_yeni_ozellikler.md` tamamlandı: JWT/Gmail
rotasyonu, grup üyelik kontrolü, rate limiting, CORS, KVKK onayı, içerik seed,
şikayet sistemi, actuator, reaksiyon/bildirim/admin paneli, E2E suite yeşil.
Bu doküman bir sonraki aşama — kod doğruluğundan çok, gerçek kullanıcı
trafiğini kaldırma ve sorunları görebilme hazırlığı.

## 1. Gözlemlenebilirlik (en yüksek öncelik)

- [ ] Hata izleme aracı ekle (Sentry ücretsiz tier — hem backend hem frontend)
- [ ] Basit uptime/alarm sistemi kur (ör. UptimeRobot, ücretsiz) — backend
      uykuya dalarsa/çökerse haberin olsun
- [ ] (Opsiyonel) Yapılandırılmış log + basit log arama — CI'da backend.log'a
      ulaşmanın bu oturumda ne kadar zor olduğunu gördük, production'da bu
      hiç sürdürülebilir değil

## 2. E-posta altyapısı

- [ ] Gmail SMTP'den transactional email servisine geç (Resend / Brevo /
      Postmark) — Gmail'in günlük gönderim limiti ve otomatik gönderimde
      hesap askıya alma riski var, kayıt/şifre sıfırlama akışının omurgası
      bu maile bağlı
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

1. Gözlemlenebilirlik (Sentry + uptime) — bundan sonraki her adımda göz
   olarak lazım
2. E-posta altyapısı — kayıt akışı buna bağlı, en kırılgan nokta
3. Barındırma kapasitesi teyidi
4. KVKK hukuki inceleme (paralel yürütülebilir, avukat cevabı beklenirken
   diğer adımlara devam edilebilir)
5. Soft launch
6. Küçük cilalar
