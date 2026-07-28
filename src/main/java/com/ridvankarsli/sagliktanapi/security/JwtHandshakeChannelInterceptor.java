package com.ridvankarsli.sagliktanapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

// STOMP CONNECT frame'inde gelen JWT'yi doğrular. WebSocket'in ham HTTP
// upgrade isteği SecurityConfig'de permitAll (native WebSocket API custom
// header/Authorization taşıyamıyor) - gerçek kimlik doğrulama burada, ilk
// STOMP mesajında yapılıyor. Doğrulama başarısız olursa bağlantı reddedilir
// (MessagingException STOMP ERROR frame'i ile sonuçlanır, bağlantı kapanır).
@Component
@RequiredArgsConstructor
public class JwtHandshakeChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("WebSocket bağlantısı için geçerli bir Authorization header gerekli");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Refresh token'ların bu uçta bearer token gibi kullanılmasını
            // engelle - aynı gerekçe JwtAuthenticationFilter'daki gibi.
            if (!"access".equals(jwtService.extractTokenType(token)) || !jwtService.isTokenValid(token, userDetails)) {
                throw new MessagingException("Geçersiz ya da süresi dolmuş token");
            }

            // accessor.setUser(...): convertAndSendToUser'ın hedefi bulabilmesi
            // için Principal.getName() (= e-posta) burada set ediliyor.
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
        } catch (MessagingException e) {
            throw e;
        } catch (RuntimeException e) {
            // Bozuk/süresi dolmuş token (JwtException), ya da token geçerli
            // ama kullanıcı artık yok (UsernameNotFoundException) - hepsi
            // aynı şekilde bağlantı reddiyle sonuçlanmalı.
            throw new MessagingException("Geçersiz token", e);
        }

        return message;
    }
}
