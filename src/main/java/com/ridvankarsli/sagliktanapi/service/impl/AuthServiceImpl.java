package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceAlreadyExistsException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.exception.UnauthorizedException;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetailsService;
import com.ridvankarsli.sagliktanapi.security.JwtService;
import com.ridvankarsli.sagliktanapi.service.AuthService;
import com.ridvankarsli.sagliktanapi.service.AuthTokens;
import com.ridvankarsli.sagliktanapi.service.EmailService;
import com.ridvankarsli.sagliktanapi.util.EmailValidator;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(15);
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final EmailValidator emailValidator;
    private final SecureRandom secureRandom = new SecureRandom();

    // SADECE E2E/test ortamı için: true olduğunda kayıt anında e-posta
    // gerçek SMTP'den gönderilmeden otomatik doğrulanmış sayılır - Playwright
    // gibi otomasyonların gerçek bir e-posta kutusu okuyamamasını aşmak için.
    // Varsayılan (ve production'daki tek geçerli değer) false'tur; application-
    // secrets.properties ya da ortam değişkeni ile elle açılmadıkça hiçbir
    // etkisi yoktur. Bkz. .github/workflows/e2e.yml - sadece orada true.
    @Value("${app.testing.auto-verify-email:false}")
    private boolean autoVerifyEmail;

    // SADECE E2E/test ortamı için: boş değilse ve kaydolan e-posta bu önekle
    // başlıyorsa kullanıcı otomatik ADMIN rolüyle oluşturulur. Admin panel
    // E2E testleri gerçek bir admin oturumu gerektiriyor, prod'da admin
    // bootstrap'ı bilinçli olarak manuel (DB'de elle rol güncelleme) - bu
    // yüzden ayrı, dar kapsamlı bir test kapısı: varsayılan boş string
    // olduğundan (ve production'da asla set edilmediğinden) hiçbir etkisi
    // yoktur. Bkz. .github/workflows/e2e.yml - sadece orada dolu.
    @Value("${app.testing.auto-admin-email-prefix:}")
    private String autoAdminEmailPrefix;

    @Override
    @Transactional
    public User register(String email, String rawPassword, String firstName, String lastName, boolean kvkkConsent) {
        if (!emailValidator.isDeliverable(email)) {
            throw new BadRequestException("Geçersiz e-posta adresi");
        }

        // DTO seviyesinde @AssertTrue zaten bunu zorunlu kılıyor; servis
        // katmanında da tekrar kontrol ediyoruz ki bu metot ileride başka
        // bir yerden (ör. admin/import akışı) çağrılırsa boşlukta kalmasın.
        if (!kvkkConsent) {
            throw new BadRequestException("Kayıt olmak için KVKK Aydınlatma Metni'ni onaylamanız gerekir");
        }

        String code = generateCode();
        LocalDateTime codeExpiresAt = LocalDateTime.now().plus(VERIFICATION_CODE_TTL);
        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (user.isEmailVerified()) {
                throw new ResourceAlreadyExistsException("Bu e-posta zaten kayıtlı");
            }
            // Bu e-posta ile doğrulanmamış eski bir kayıt var — biri (yanlışlıkla
            // ya da kötü niyetle) başkasının e-postasıyla kayıt olup hiç
            // doğrulamamış olabilir. Doğrulanmadığı sürece o e-posta gerçek
            // sahibine sonsuza kadar kapalı kalmasın diye eski kaydın üzerine
            // yeni bilgilerle yazıyoruz (email squatting'i önlemek için).
            user.setPasswordHash(passwordHash);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setVerificationCode(code);
            user.setVerificationCodeExpiresAt(codeExpiresAt);
            user.setKvkkConsentAt(LocalDateTime.now());
        } else {
            boolean grantTestAdmin = !autoAdminEmailPrefix.isBlank() && email.startsWith(autoAdminEmailPrefix);
            user = User.builder()
                    .email(email)
                    .passwordHash(passwordHash)
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(grantTestAdmin ? Role.ADMIN : Role.USER)
                    .emailVerified(false)
                    .verificationCode(code)
                    .verificationCodeExpiresAt(codeExpiresAt)
                    .active(true)
                    .kvkkConsentAt(LocalDateTime.now())
                    .build();
        }

        user = userRepository.save(user);

        if (autoVerifyEmail) {
            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            user = userRepository.save(user);
        } else {
            emailService.sendVerificationCode(user.getEmail(), code);
        }

        return user;
    }

    @Override
    @Transactional
    public void verifyEmail(String email, String code) {
        // NOT: getUserOrThrow yerine bilerek findByEmail kullanıyoruz -
        // e-posta kayıtlı değilse de "Doğrulama kodu hatalı" ile AYNI
        // mesaj/status dönmeli. Aksi halde bu uç, kayıtlı e-postaları
        // 404 ("Kullanıcı bulunamadı") ile açığa çıkaran bir email
        // enumeration kanalı olur (bkz. login/resetPassword'daki aynı düzeltme).
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new BadRequestException("Doğrulama kodu hatalı");
        }

        if (user.isEmailVerified()) {
            return; // idempotent: zaten doğrulanmışsa sessizce çık
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new BadRequestException("Doğrulama kodu hatalı");
        }

        if (user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Doğrulama kodunun süresi dolmuş");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    @Override
    public AuthTokens login(String email, String rawPassword) {
        // NOT: getUserOrThrow yerine findByEmail - e-posta kayıtlı değilse
        // de yanlış şifreyle AYNI 401/mesajı dönmeli, aksi halde bu uç
        // (login) kayıtlı e-postaları 404 ile açığa çıkaran bir email
        // enumeration kanalı olur. forgot-password zaten bu prensiple
        // yazılmıştı (bkz. requestPasswordReset) - burada da uyguluyoruz.
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("E-posta veya şifre hatalı");
        }

        if (!user.isActive()) {
            throw new ForbiddenException("Hesap pasif durumda");
        }

        if (!user.isEmailVerified()) {
            throw new ForbiddenException("E-posta adresi doğrulanmamış");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new AuthTokens(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
        );
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Geçersiz refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // KRİTİK: "type" claim'i kontrol edilmezse bir access token da bu uca
        // refresh token gibi verilebilir - bu da kısa ömürlü olması gereken
        // access token'ın süresiz şekilde yeni token çifti üretmek için
        // kullanılmasına (ömrünü fiilen sınırsız hale getirmesine) yol açardı.
        if (!"refresh".equals(jwtService.extractTokenType(refreshToken)) || !jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Geçersiz veya süresi dolmuş refresh token");
        }

        return new AuthTokens(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
        );
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Mevcut şifre hatalı");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        // Kullanıcı bulunamasa bile aynı şekilde (sessizce) dönülür;
        // aksi halde bu endpoint hangi e-postaların kayıtlı olduğunu
        // dışarıya sızdırır (email enumeration).
        userRepository.findByEmail(email).ifPresent(user -> {
            String code = generateCode();
            user.setResetCode(code);
            user.setResetCodeExpiresAt(LocalDateTime.now().plus(RESET_CODE_TTL));
            userRepository.save(user);
            emailService.sendPasswordResetCode(user.getEmail(), code);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // NOT: getUserOrThrow yerine findByEmail - aynı email enumeration
        // gerekçesiyle (bkz. login/verifyEmail'deki not).
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || user.getResetCode() == null || !user.getResetCode().equals(code)) {
            throw new BadRequestException("Sıfırlama kodu hatalı");
        }

        if (user.getResetCodeExpiresAt() == null || user.getResetCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Sıfırlama kodunun süresi dolmuş");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpiresAt(null);
        userRepository.save(user);
    }

    private String generateCode() {
        int code = secureRandom.nextInt((int) Math.pow(10, VERIFICATION_CODE_LENGTH));
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", code);
    }
}
