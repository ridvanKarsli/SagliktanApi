package com.ridvankarsli.sagliktanapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: okunmuş bildirimleri periyodik temizleyen
// NotificationCleanupJob gibi @Scheduled metodları etkinleştirir.
@EnableScheduling
@SpringBootApplication
public class SagliktanApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SagliktanApiApplication.class, args);
    }

}
