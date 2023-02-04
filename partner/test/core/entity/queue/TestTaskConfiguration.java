package ru.yandex.partner.core.entity.queue;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestTaskConfiguration {
    @Bean
    TestTaskFactory testTaskFactory() {
        return new TestTaskFactory();
    }

    @Bean
    TestNonConcurrentTaskFactory testNonConcurrentTaskFactory() {
        return new TestNonConcurrentTaskFactory();
    }
}
