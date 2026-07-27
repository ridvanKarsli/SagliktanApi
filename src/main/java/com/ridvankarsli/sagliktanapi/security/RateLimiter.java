package com.ridvankarsli.sagliktanapi.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Basit, bağımlılıksız sabit pencereli (fixed window) rate limiter.
// Bellek içi çalışır; Railway'de şu an tek instance koştuğumuz için
// yeterli. Yatay ölçekleme gerekirse (birden fazla instance) Redis
// tabanlı bir çözüme (ör. Bucket4j + Redis) geçilmeli, çünkü bu
// implementasyon instance'lar arasında paylaşılmaz.
@Component
public class RateLimiter {

    private static final long CLEANUP_INTERVAL_MS = Duration.ofMinutes(30).toMillis();

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    public boolean tryConsume(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        maybeCleanup(now, window.toMillis());

        Window w = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.startMillis >= window.toMillis()) {
                w.startMillis = now;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= limit;
        }
    }

    // Eski pencereleri (bir daha erişilmeyen IP'leri) belleği şişirmesin
    // diye periyodik olarak temizler. Kilitsiz, fırsatçı bir temizlik -
    // her isteğe ek yük getirmemek için sadece belirli aralıklarla çalışır.
    private void maybeCleanup(long now, long windowMillis) {
        long last = lastCleanup.get();
        if (now - last < CLEANUP_INTERVAL_MS) {
            return;
        }
        if (!lastCleanup.compareAndSet(last, now)) {
            return;
        }
        long maxAge = Math.max(windowMillis, CLEANUP_INTERVAL_MS);
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis > maxAge);
    }

    private static final class Window {
        volatile long startMillis;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
