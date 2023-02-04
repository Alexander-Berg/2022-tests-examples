package ru.yandex.partner.core.configuration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.partner.libs.extservice.balance.BalanceService;

@TestConfiguration
public class BalanceTestConfiguration {
    @Bean
    public BalanceService balanceService() {
        return Mockito.mock(BalanceService.class);
    }
}
