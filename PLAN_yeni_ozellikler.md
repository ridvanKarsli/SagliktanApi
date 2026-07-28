# Sağlıktan – Yeni Özellikler Teknik Planı

Kapsam: Bildirim sistemi (gerçek zamanlı), Faydalı/Faydalı Değil reaksiyon sistemi,
içerik şikayeti (mevcut, admin paneline bağlanacak), Admin paneli.

**Kapsam dışı:** Resim ekleme — kullanıcının kararıyla iptal edildi (moderasyon zorluğu).

Sıralama: önce backend (SagliktanApi), sonra frontend (SagliktanWeb). Her adım
kendi içinde commit edilebilir bir birim; her adım bitince özet + git komutları
verilecek, sıradaki adıma geçilecek.

---

## Mevcut Durum (denetimden)

- **Şikayet sistemi zaten var:** `ContentReport` entity, `/api/posts/{id}/report`
  ve `/api/comments/{id}/report` uçları, frontend'de `PostDetail.jsx` içinde tam
  çalışan şikayet dialogu (hem post hem yorum için). Bu adım için **yeni kod
  yazılmayacak**, sadece admin panelinden yönetilebilir hale getirilecek
  (durum: PENDING/REVIEWED, yeni: REJECTED).
- **Reaksiyon, bildirim, admin paneli: hiçbiri yok.** Sıfırdan yazılacak.
- Mevcut `NotificationContext.jsx` / `LumoNotification.jsx` bir toast (anlık
  uyarı) bileşenidir, kalıcı/okunabilir bildirim sistemi değildir — karıştırılmasın.

---

## Mimari Kararlar

- **Bildirim iletimi:** WebSocket (STOMP over WebSocket, `spring-boot-starter-websocket`).
  JWT doğrulaması STOMP `CONNECT` frame'indeki `Authorization: Bearer <token>`
  header'ı üzerinden, bir `ChannelInterceptor` ile yapılacak (mevcut stateless
  JWT modeliyle tutarlı, session/cookie kullanılmayacak). Bağlantı koptuğunda/ilk
  açılışta kaçırılan bildirimleri yakalamak için ayrıca normal REST uçları da
  olacak (liste + okunmamış sayısı + okundu işaretleme).
- **Reaksiyon:** `ContentReport` ile aynı polimorfik desen (`targetType` + `targetId`),
  kullanıcı başına hedef başına tek kayıt (unique constraint), `HELPFUL` /
  `NOT_HELPFUL` arası geçiş yapılabilir, kaldırılabilir.
- **Admin paneli:** Yeni `AdminController`, sınıf seviyesinde
  `@PreAuthorize("hasRole('ADMIN')")`. Kullanıcı listesinde asla `passwordHash`,
  doğrulama/sıfırlama kodları dönülmeyecek — yeni bir `AdminUserResponse` DTO'su.

---

## BACKEND (SagliktanApi) — sırayla

### Adım B1 — Reaksiyon sistemi
- `Reaction` entity + `V8__add_reactions.sql` (user_id, target_type, target_id,
  value enum[HELPFUL, NOT_HELPFUL], unique(user_id, target_type, target_id))
- `ReactionRepository`, `ReactionService`/`Impl` (react/toggle, kaldır, sayaç + kullanıcının kendi reaksiyonu)
- `PostResponse` / `CommentResponse`'a `helpfulCount`, `notHelpfulCount`, `myReaction` alanları
- `PostController` / `CommentController`'a `PUT /api/posts/{id}/reactions`,
  `DELETE /api/posts/{id}/reactions` (ve yorum karşılıkları)
- IDOR kontrolü: sadece kendi reaksiyonun değişir/silinir (path'te targetId var, body'de userId yok — principal'dan alınır)

### Adım B2 — Bildirim sistemi (WebSocket)
- `spring-boot-starter-websocket` bağımlılığı
- `Notification` entity + `V9__add_notifications.sql` (recipient_id, actor_id, type, target_type, target_id, message, read, created_at)
- `WebSocketConfig` (STOMP endpoint `/ws`, JWT handshake/CONNECT doğrulama interceptor'ı, CORS allowlist SecurityConfig ile aynı origin'ler)
- `NotificationService`: bildirim oluşturup hem DB'ye yazacak hem `SimpMessagingTemplate.convertAndSendToUser(...)` ile anlık push edecek
- Tetikleyiciler: yeni yorum → post sahibine; yoruma yanıt → üst yorum sahibine (kendi içeriğine kendi aksiyonun bildirim üretmez)
- REST fallback: `GET /api/notifications` (sayfalı), `GET /api/notifications/unread-count`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`
- IDOR kontrolü: bir kullanıcı sadece kendi bildirimlerini görür/okur

### Adım B3 — Admin paneli backend
- `ReportStatus`'a `REJECTED` eklenir; `ContentReport`'a `resolvedBy`/`resolvedAt` (`V10__extend_reports.sql`)
- `AdminController` (+`AdminUserResponse`, `AdminReportResponse` DTO'ları):
  - `GET /api/admin/stats` — toplam kullanıcı/post/yorum, bekleyen rapor sayısı
  - `GET /api/admin/users` — sayfalı + arama (email/isim) + filtre (aktif/pasif, rol)
  - `PUT /api/admin/users/{id}` — rol değiştir, aktif/pasif yap, profil alanlarını düzenle
  - `GET /api/admin/reports` — sayfalı + status filtresi, raporlanan içeriğin önizlemesi + sahibi
  - `PUT /api/admin/reports/{id}` — durum güncelle (REVIEWED/REJECTED), opsiyonel aksiyon: içeriği sil / kullanıcıyı pasifleştir

### Adım B4 — Yeni uçların güvenlik kontrolü
- B1–B3'te eklenen her endpoint için authorization/IDOR/DTO-sızıntı taraması (bu oturumun ilk yarısındaki denetimle aynı yöntem)

---

## FRONTEND (SagliktanWeb) — sırayla

### Adım F1 — API servis katmanı
`services/api.js`'e B1–B3'teki yeni uçların karşılıkları

### Adım F2 — Reaksiyon arayüzü
`PostCard`, `PostDetail`, yorum bileşeni: "Faydalı / Faydalı Değil" butonları + sayaçlar, kullanıcının kendi seçimi vurgulanır

### Adım F3 — Bildirim arayüzü
`@stomp/stompjs` ile WebSocket bağlantısı (login sonrası açılır, JWT ile), üst barda zil ikonu + okunmamış rozeti, açılır liste/sayfa, okundu işaretleme, bağlantı koptuğunda REST fallback ile senkronize olma

### Adım F4 — Admin paneli arayüzü
Yeni `/admin` route'u (sadece ADMIN role, route guard), sekmeler: Dashboard (istatistikler), Raporlar (listele + durum güncelle + aksiyon), Kullanıcılar (listele/ara + düzenle)

### Adım F5 — Uçtan uca kontrol
Build doğrulaması, manuel senaryo listesi, mümkünse yeni Playwright senaryoları (reaksiyon, bildirim, admin paneli temel akışları)

---

## Çalışma şekli

Her adımda: kodu yazarım → mümkün olduğunca statik/manuel doğrularım (backend'de
Maven'i sandbox'ta çalıştıramıyorum, gerçek derleme doğrulaması push sonrası CI'da
olacak) → sana özet + `git add/commit/push` komutlarını veririm → onayını
beklemeden bir sonraki adıma geçerim, sen istediğin an durdurabilirsin.
