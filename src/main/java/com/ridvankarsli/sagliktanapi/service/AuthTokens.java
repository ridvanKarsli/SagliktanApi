package com.ridvankarsli.sagliktanapi.service;

// AuthService'in login/refresh sonucunda döndürdüğü token çifti.
// DTO/Mapping adımında (rapor adım 7) response DTO'suna dönüştürülecek.
public record AuthTokens(String accessToken, String refreshToken) {
}
