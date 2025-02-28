package com.caching.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    /**
     * Cache manager bean
     *
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        // Using ConcurrentMapCacheManager for in-memory caching
        return new ConcurrentMapCacheManager("geocoding", "reverse-geocoding");
    }
}
