package com.assetmind.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.assetmind")
@EnableJpaRepositories(basePackages = {"com.assetmind.infrastructure.persistence", "com.assetmind.infrastructure.security"})
@EntityScan(basePackages = {"com.assetmind.infrastructure.persistence", "com.assetmind.infrastructure.security"})
@EnableScheduling
@EnableCaching
public class AssetmindApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetmindApplication.class, args);
    }
}

