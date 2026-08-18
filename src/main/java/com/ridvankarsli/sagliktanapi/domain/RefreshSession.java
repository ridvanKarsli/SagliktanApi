package com.ridvankarsli.sagliktanapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// "Aktif Oturumlar" (görev #305) - her satır bir refresh token'a (JWT
// içindeki "sid" claim'i, bkz. JwtService) karşılık gelir, ham token
// DEĞİL sadece rastgele bir UUID saklanır. Bkz. V19 migration'daki
// "KAPSAM NOTU" - access token'lar bu tablodan bağımsız stateless kalır.
@Entity
@Table(name = "refresh_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId;

    // User-Agent'tan türetilen kaba bir etiket (ör. "Chrome · Windows") -
    // bkz. util/UserAgentParser. Ham User-Agent string'i saklanmıyor, hem
    // gereksiz hem de zamanla anlamsızlaşan bir veri.
    @Column(name = "device_label")
    private String deviceLabel;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;
}
