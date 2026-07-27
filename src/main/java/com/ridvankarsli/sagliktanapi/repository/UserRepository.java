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
}
