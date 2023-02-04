package ru.yandex.partner.core.configuration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutor;
import ru.yandex.partner.libs.extservice.blackbox.BlackboxService;
import ru.yandex.passport.tvmauth.TvmClient;

@TestConfiguration
public class AuthTestConfiguration {
    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean
    public BlackboxRequestExecutor blackboxRequestExecutor() {
        return Mockito.mock(BlackboxRequestExecutor.class);
    }

    @Bean
    public BlackboxService blackboxService(
            BlackboxRequestExecutor blackboxRequestExecutor, TvmClient tvmClient
    ) {
        return new TestBlackboxService(blackboxRequestExecutor, tvmClient);
    }
}
