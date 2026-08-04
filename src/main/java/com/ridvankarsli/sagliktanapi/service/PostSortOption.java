package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.exception.BadRequestException;

// Faz 2 adım 1: gönderi listeleme sıralama seçeneği.
// RECENT: created_at DESC (mevcut/varsayılan davranış, geriye dönük uyumlu).
// POPULAR: reaksiyon + kaydedilme sayısı toplamına göre DESC, eşitlikte
// created_at DESC (bkz. PostRepository.findBySubGroupIdOrderByPopularityDesc).
public enum PostSortOption {
    RECENT,
    POPULAR;

    // Controller'daki ?sort= query param'ını parse eder. Boş/eksik değer
    // sessizce RECENT'e (mevcut varsayılan) düşer - eski davranışı bozmaz.
    // Tanınmayan bir değer ise (yazım hatası vb.) sessizce yok saymak yerine
    // 400 döndürür, çağıranın hatasını fark etmesini sağlar.
    public static PostSortOption fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return RECENT;
        }
        try {
            return PostSortOption.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Geçersiz sıralama seçeneği: " + raw);
        }
    }
}
