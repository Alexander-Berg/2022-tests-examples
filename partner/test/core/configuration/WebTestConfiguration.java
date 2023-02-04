package ru.yandex.partner.core.configuration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.partner.core.entity.block.type.tags.TagService;

@TestConfiguration
public class WebTestConfiguration {

    @Bean
    @Primary
    public TagService tagService() {
        return Mockito.mock(TagService.class);
    }

}
