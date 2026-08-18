package com.ridvankarsli.sagliktanapi.util;

// "Aktif Oturumlar" listesinde (görev #305/#306) kullanıcıya ham User-Agent
// string'i yerine "Chrome · Windows" gibi okunur bir etiket göstermek için
// kaba bir sezgisel ayrıştırıcı - tam bir ua-parser kütüphanesi eklemek bu
// tek amaç için gereksiz bir bağımlılık olurdu. Yanlış/eksik eşleşmede en
// kötü ihtimalle jenerik bir etiket döner, kritik bir davranışı etkilemez.
public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static String toDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Bilinmeyen cihaz";

        String ua = userAgent.toLowerCase();
        String browser = detectBrowser(ua);
        String os = detectOs(ua);

        if (browser == null && os == null) return "Bilinmeyen cihaz";
        if (os == null) return browser;
        if (browser == null) return os;
        return browser + " · " + os;
    }

    private static String detectBrowser(String ua) {
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        // Chrome kontrolü Safari'den ÖNCE gelmeli - Chrome'un UA'sı da
        // "Safari/xxx" içerir (WebKit tabanlı olduğu için).
        if (ua.contains("chrome/") || ua.contains("crios/")) return "Chrome";
        if (ua.contains("firefox/") || ua.contains("fxios/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return null;
    }

    private static String detectOs(String ua) {
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        return null;
    }
}
