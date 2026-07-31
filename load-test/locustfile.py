"""
Sağlıktan - Locust yük testi senaryosu.

Ne yapar: gerçek bir kullanıcının olası davranışını simüle eder - kayıt
olur, giriş yapar, seed hastalık grubuna katılır, sonra tekrar tekrar grup/
alt grup/gönderi gezinir, arama yapar, reaksiyon verir ve ara sıra yeni
gönderi/yorum oluşturur. Task ağırlıkları (weight) gerçek kullanım oranını
yansıtacak şekilde seçildi: okuma (listeleme/görüntüleme) yazmadan çok daha
sık - bir topluluk sitesinde kullanıcıların çoğu zamanı okumakla geçer.

Yerel çalıştırma:
    pip install -r load-test/requirements.txt
    locust -f load-test/locustfile.py --host http://localhost:8080
    # Tarayıcıda http://localhost:8089 açılır, kullanıcı sayısı ve
    # spawn rate orada girilir.

Headless (arayüzsüz, örn. script/CI içinden) çalıştırma:
    locust -f load-test/locustfile.py --host http://localhost:8080 \
        --headless -u 20 -r 2 -t 2m --html load-test-report.html

ÖNEMLİ - backend bu iki test-only env değişkeniyle ayakta olmalı (CI'daki
.github/workflows/load-test.yml zaten bunları ayarlıyor; yerelde elle
çalıştırıyorsan application-secrets.properties'e ya da env'e ekle):
  APP_TESTING_AUTO_VERIFY_EMAIL=true
      Locust'un yarattığı kullanıcılar e-posta doğrulama linkine tıklamadan
      hemen giriş yapabilsin diye.
  APP_TESTING_RATE_LIMIT_MULTIPLIER=1000
      Yükün tamamı tek bir makineden/IP'den geldiği için gerçek rate
      limiter (bkz. RateLimitFilter) bunu saldırı sanıp her isteği 429 ile
      keser - bu da testi anlamsızlaştırır (uygulama mantığını değil, rate
      limiter'ı ölçmüş oluruz).
Bu iki değişken PROD/staging'de KESİNLİKLE tanımlanmamalı - aynı gerekçeyle
SagliktanWeb reposundaki e2e.yml'de de sadece CI'da kullanılıyor.
"""
import random
import string
import threading
import time

from locust import HttpUser, task, between

# Farklı simüle kullanıcılar arasında paylaşılan, o ana kadar oluşturulmuş/
# görülmüş gönderi id'leri - yorum/görüntüleme/reaksiyon task'larının
# rastgele bir gönderi üzerinde çalışabilmesi için. Locust tek process'te
# çok sayıda "user" greenlet'i çalıştırdığından basit bir lock yeterli.
_known_post_ids = []
_lock = threading.Lock()

SEARCH_TERMS = ["tedavi", "deneyim", "merhaba", "soru", "tanı", "destek"]


def _remember_post(post_id):
    with _lock:
        if post_id not in _known_post_ids:
            _known_post_ids.append(post_id)
        if len(_known_post_ids) > 500:
            _known_post_ids.pop(0)


def _random_post_id():
    with _lock:
        return random.choice(_known_post_ids) if _known_post_ids else None


def _random_letters(n=6):
    return "".join(random.choices(string.ascii_lowercase, k=n))


class SagliktanUser(HttpUser):
    # Gerçek kullanıcılar art arda tıklamaz, sayfalar arasında okur/düşünür -
    # her task sonrası 1-4 saniye bekleme normal gezinmeyi simüle eder
    # (saldırı/bot trafiği değil).
    wait_time = between(1, 4)

    def on_start(self):
        stamp = f"{int(time.time() * 1000)}{random.randint(1000, 9999)}"
        self.email = f"locust.{stamp}@example.com"
        self.password = "YukTesti123!"
        # ValidName kuralına uysun diye (bkz. NameValidator) - sadece harf,
        # "test/deneme/mal" gibi yasaklı kelimeler yok.
        first_name = "Deniz"
        last_name = f"Kullanici{_random_letters()}"

        with self.client.post(
            "/api/auth/register",
            json={
                "email": self.email,
                "password": self.password,
                "firstName": first_name,
                "lastName": last_name,
                "kvkkConsent": True,
            },
            name="/api/auth/register",
            catch_response=True,
        ) as resp:
            if resp.status_code != 201:
                resp.failure(f"Kayıt başarısız: {resp.status_code} {resp.text[:200]}")
                return

        with self.client.post(
            "/api/auth/login",
            json={"email": self.email, "password": self.password},
            name="/api/auth/login",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"Giriş başarısız: {resp.status_code} {resp.text[:200]}")
                return
            token = resp.json().get("accessToken")
            self.client.headers.update({"Authorization": f"Bearer {token}"})

        # Seed hastalık grubuna (V3__seed_initial_content.sql - "Retinitis
        # Pigmentosa") katıl ki post/yorum oluşturabilsin (grup üyeliği
        # zorunlu, bkz. PostServiceImpl/CommentServiceImpl).
        self.sub_group_id = None
        groups_resp = self.client.get("/api/disease-groups", name="/api/disease-groups")
        groups = groups_resp.json() if groups_resp.status_code == 200 else []
        if not groups:
            return
        disease_group_id = groups[0]["id"]
        self.client.post(
            f"/api/disease-groups/{disease_group_id}/join",
            name="/api/disease-groups/[id]/join",
        )
        sub_resp = self.client.get(
            f"/api/disease-groups/{disease_group_id}/sub-groups",
            name="/api/disease-groups/[id]/sub-groups",
        )
        sub_groups = sub_resp.json() if sub_resp.status_code == 200 else []
        if sub_groups:
            self.sub_group_id = sub_groups[0]["id"]

    # ---------- Okuma ağırlıklı task'lar (gerçek trafiğin çoğunluğu) ----------

    @task(6)
    def browse_posts(self):
        if not self.sub_group_id:
            return
        resp = self.client.get(
            f"/api/sub-groups/{self.sub_group_id}/posts?page=0&size=10",
            name="/api/sub-groups/[id]/posts",
        )
        if resp.status_code == 200:
            for post in resp.json().get("content", []):
                _remember_post(post["id"])

    @task(5)
    def view_post_detail(self):
        post_id = _random_post_id()
        if not post_id:
            return
        self.client.get(f"/api/posts/{post_id}", name="/api/posts/[id]")
        self.client.get(
            f"/api/posts/{post_id}/comments?page=0&size=10",
            name="/api/posts/[id]/comments",
        )

    @task(2)
    def browse_groups(self):
        self.client.get("/api/disease-groups", name="/api/disease-groups")

    @task(2)
    def search(self):
        query = random.choice(SEARCH_TERMS)
        self.client.get(f"/api/search?q={query}", name="/api/search")

    # ---------- Hafif yazma: reaksiyon ----------

    @task(3)
    def react_to_post(self):
        post_id = _random_post_id()
        if not post_id:
            return
        value = random.choice(["HELPFUL", "NOT_HELPFUL"])
        self.client.put(
            f"/api/posts/{post_id}/reactions",
            json={"value": value},
            name="/api/posts/[id]/reactions",
        )

    # ---------- Ağır yazma: yeni gönderi/yorum ----------

    @task(1)
    def create_post(self):
        if not self.sub_group_id:
            return
        with self.client.post(
            f"/api/sub-groups/{self.sub_group_id}/posts",
            json={
                "title": f"Yük testi gönderisi {_random_letters(8)}",
                "content": "Bu gönderi Locust yük testi tarafından otomatik oluşturuldu.",
            },
            name="/api/sub-groups/[id]/posts (create)",
            catch_response=True,
        ) as resp:
            if resp.status_code == 201:
                _remember_post(resp.json()["id"])
            else:
                resp.failure(f"Gönderi oluşturulamadı: {resp.status_code}")

    @task(2)
    def create_comment(self):
        post_id = _random_post_id()
        if not post_id:
            return
        self.client.post(
            f"/api/posts/{post_id}/comments",
            json={"content": f"Yük testi yorumu {_random_letters(8)}", "parentCommentId": None},
            name="/api/posts/[id]/comments (create)",
        )
