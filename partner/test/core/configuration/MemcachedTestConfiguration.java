package ru.yandex.partner.core.configuration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.partner.libs.memcached.MemcachedService;

@TestConfiguration
public class MemcachedTestConfiguration {

    @Bean
    public MemcachedService memcachedService() {
        return Mockito.mock(MemcachedService.class);
    }

}
