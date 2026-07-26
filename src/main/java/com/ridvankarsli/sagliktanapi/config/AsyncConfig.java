package com.ridvankarsli.sagliktanapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// E-posta gönderimini (@Async) HTTP isteğini bloklamadan arka planda
// yürütebilmek için gerekli. Eski projede de sendWelcomeMail @Async idi.
@Configuration
@EnableAsync
public class AsyncConfig {
}
