package com.ridvankarsli.sagliktanapi.config;

import com.ridvankarsli.sagliktanapi.security.JwtHandshakeChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Gerçek zamanlı bildirimler için STOMP over WebSocket. JWT doğrulaması
// burada değil, JwtHandshakeChannelInterceptor'da (CONNECT frame'inde)
// yapılıyor - bkz. o sınıf. CORS allowlist SecurityConfig ile aynı
// origin'ler (tek yerden yönetmek için oraya taşınabilir ama WebSocket
// origin kontrolü Spring CORS filter'ından tamamen ayrı bir mekanizma
// olduğu için burada da elle tanımlanması gerekiyor).
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeChannelInterceptor jwtHandshakeChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "https://sagliktan.com",
                        "https://www.sagliktan.com",
                        "http://localhost:3000"
                );
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
