package com.ridvankarsli.sagliktanapi.config;

import com.ridvankarsli.sagliktanapi.security.JwtHandshakeChannelInterceptor;
import com.ridvankarsli.sagliktanapi.util.CorsOrigins;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Gerçek zamanlı bildirimler için STOMP over WebSocket. JWT doğrulaması
// burada değil, JwtHandshakeChannelInterceptor'da (CONNECT frame'inde)
// yapılıyor - bkz. o sınıf. CORS allowlist SecurityConfig ile aynı kaynaktan
// (app.cors.allowed-origins / ALLOWED_ORIGINS env) okunuyor - tek yerden
// yönetmek için, ama WebSocket origin kontrolü Spring CORS filter'ından
// tamamen ayrı bir mekanizma olduğu için burada ayrıca enjekte ediliyor.
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeChannelInterceptor jwtHandshakeChannelInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = CorsOrigins.parse(allowedOriginsRaw).toArray(String[]::new);
        registry.addEndpoint("/ws").setAllowedOrigins(origins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Sunucudan istemciye tek yönlü yayın kanalı - istemcinin sunucuya
        // uygulama mesajı göndermesi gerekmiyor (bu yüzden application
        // destination prefix / @MessageMapping controller'ı yok).
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtHandshakeChannelInterceptor);
    }
}
