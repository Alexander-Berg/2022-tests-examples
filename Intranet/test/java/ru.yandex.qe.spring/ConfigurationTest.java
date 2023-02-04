package ru.yandex.qe.spring;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * User: bgleb
 */
@ContextConfiguration(locations = {"classpath:spring/qe-plugin-spring.xml"})
@ExtendWith(SpringExtension.class)
public class ConfigurationTest {

    @Inject
    private ConversionService conversionService;

    @Test
    void config_ok() {
        Assertions.assertNotNull(conversionService);
    }

}
