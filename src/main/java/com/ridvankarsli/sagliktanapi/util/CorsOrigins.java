package com.ridvankarsli.sagliktanapi.util;

import java.util.Arrays;
import java.util.List;

// app.cors.allowed-origins (ALLOWED_ORIGINS env) - virgülle ayrılmış origin
// listesini parse eden saf fonksiyon. SecurityConfig (Spring CORS filter) ve
// WebSocketConfig (STOMP handshake origin kontrolü) aynı kaynaktan aynı
// mantıkla parse ediyordu (bkz. clean-code audit) - bean'ler paylaşılamıyor
// (iki mekanizma tamamen farklı) ama parsing mantığı burada tek yerde.
public final class CorsOrigins {

    private CorsOrigins() {
    }

    public static List<String> parse(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
