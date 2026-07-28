package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Gelişmiş arama: ad/soyada göre kişi arama (bkz. V7 migration,
    // search_vector kolonu). Hesabı kapatılmış (active = false) kullanıcılar
    // sonuçlara girmez.
    @Query(
            value = "SELECT * FROM users u WHERE u.active = true " +
                    "AND u.search_vector @@ plainto_tsquery('turkish', :query)",
            countQuery = "SELECT count(*) FROM users u WHERE u.active = true " +
                    "AND u.search_vector @@ plainto_tsquery('turkish', :query)",
            nativeQuery = true)
    Page<User> search(@Param("query") String query, Pageable pageable);

    // Admin paneli: public search'ün aksine pasif kullanıcıları da içerir,
    // tam metin arama yerine basit ILIKE (admin araması nadir/küçük hacimli,
    // full-text index gerektirmez). q/active/role parametrelerinden herhangi
    // biri null ise o filtre uygulanmaz.
    @Query(
            value = "SELECT * FROM users u WHERE " +
                    "(CAST(:q AS text) IS NULL OR :q = '' " +
                    "   OR u.email ILIKE CONCAT('%', CAST(:q AS text), '%') " +
                    "   OR u.first_name ILIKE CONCAT('%', CAST(:q AS text), '%') " +
                    "   OR u.last_name ILIKE CONCAT('%', CAST(:q AS text), '%')) " +
                    "AND (CAST(:active AS boolean) IS NULL OR u.active = :active) " +
                    "AND (CAST(:role AS text) IS NULL OR u.role = :role) " +
                    "ORDER BY u.created_at DESC",
            countQuery = "SELECT count(*) FROM users u WHERE " +
                    "(CAST(:q AS text) IS NULL OR :q = '' " +
                    "   OR u.email ILIKE CONCAT('%', CAST(:q AS text), '%') " +
                    "   OR u.first_name ILIKE CONCAT('%', CAST(:q AS text), '%') " +
                    "   OR u.last_name ILIKE CONCAT('%', CAST(:q AS text), '%')) " +
                    "AND (CAST(:active AS boolean) IS NULL OR u.active = :active) " +
                    "AND (CAST(:role AS text) IS NULL OR u.role = :role)",
            nativeQuery = true)
    Page<User> adminSearch(
            @Param("q") String q, @Param("active") Boolean active, @Param("role") String role, Pageable pageable);
}
