package ru.yandex.partner.core.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.partner.core.entity.block.container.BlockContainer;

@TestConfiguration
public class TestOperationContainerConfiguration {
    @Bean
    public TestOperationContainerConfigurer<BlockContainer> testOperationContainerConfigurer() {
        return new TestOperationContainerConfigurer<>();
    }

}
