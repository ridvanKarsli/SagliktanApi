# Yük testi (Locust)

## Bu ne işe yarıyor?

E2E testleri (Playwright) "doğru çalışıyor mu" sorusuna cevap verir - tek
kullanıcı için akışların hatasız işlediğini doğrular. Locust farklı bir
soruya cevap verir: "**aynı anda çok kullanıcı** olunca ne olur?" - kaç
kişi eşzamanlı kullanınca yanıtlar yavaşlamaya başlıyor, hangi uç ilk
tıkanıyor, sunucu kaç istek/saniyeyi kaldırabiliyor.

`locustfile.py`, gerçek bir kullanıcının yapacağı şeyleri simüle eden
sahte "kullanıcılar" tanımlıyor: kayıt olma, giriş yapma, gruba katılma,
gönderi/yorum gezinme, arama, reaksiyon verme, ara sıra yeni gönderi/yorum
paylaşma. Okuma işlemleri (gezinme) yazma işlemlerinden (paylaşma) çok
daha sık - gerçek kullanım oranını yansıtıyor.

## Nasıl çalıştırılır?

**GitHub Actions üzerinden (önerilen, kurulum gerektirmez):**

1. GitHub'da repo → **Actions** sekmesi → sol menüden **Load Test (Locust)**
2. Sağ üstte **Run workflow** → istersen kullanıcı sayısı/süreyi değiştir
   (varsayılan: 20 eşzamanlı kullanıcı, 2 saniyede bir yeni kullanıcı, 2
   dakika sürer) → **Run workflow**
3. Test bitince workflow sayfasındaki **Artifacts** bölümünden
   `load-test-report` dosyasını indir, içindeki `.html` dosyasını
   tarayıcıda aç.

**Yerelde (Docker/Postgres/Java 21 kuruluysa):**

```bash
pip install -r load-test/requirements.txt
locust -f load-test/locustfile.py --host http://localhost:8080
# http://localhost:8089 açılır, kullanıcı sayısı orada girilir
```

## Rapordaki sayıları nasıl okumalı?

- **RPS (requests/second):** sunucunun saniyede kaldırdığı istek sayısı.
  Yüksek olması iyi.
- **Median / 95%ile yanıt süresi:** isteklerin yarısı (median) ya da
  %95'i bu sürede ya da daha hızlı tamamlandı. Bir web sayfası için
  genel kabul gören kaba eşik: 95. yüzdelik ~1000ms altındaysa iyi,
  birkaç saniyeyi buluyorsa kullanıcı yavaşlığı hissetmeye başlar.
- **Failures (%):** başarısız (4xx/5xx veya timeout) isteklerin oranı.
  %0'a yakın olmalı - yüksekse hangi endpoint'te olduğuna (rapordaki
  satır bazlı döküme) bakılmalı.

Bu sayılar mutlak bir "geçti/kaldı" eşiği değil - amaç, kullanıcı sayısı
arttıkça (`users` input'unu 50, 100, 200 yaparak) bu sayıların nasıl
bozulduğunu gözlemlemek ve ilk tıkanan noktayı (genelde veritabanı
sorguları ya da bağlantı havuzu boyutu - bkz. `application.properties`
Hikari ayarları) önceden görebilmek.

## Neden her push'ta değil, sadece elle çalışıyor?

Bu bir doğruluk testi değil, kapasite testi - normal `ci.yml`'i
yavaşlatmasın diye ayrı tutuldu. İstersen `load-test.yml`'e bir
`schedule:` (örn. haftalık) ekleyip düzenli otomatik çalıştırabiliriz.
