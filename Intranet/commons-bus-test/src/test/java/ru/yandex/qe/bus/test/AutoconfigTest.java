package ru.yandex.qe.bus.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.qe.spring.profiles.Profiles;

/**
 * @author lvovich
 */
@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({"classpath:spring/sample-service.xml"})
public class AutoconfigTest extends AbstractBusJUnit5SpringContextTests {

    @Test
    public void verifyContext() {
        Assertions.assertNotNull(applicationContext);
    }
}
