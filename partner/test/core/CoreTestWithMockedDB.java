package ru.yandex.partner.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.partner.core.configuration.BalanceTestConfiguration;
import ru.yandex.partner.libs.multistate.configration.MultistateConfiguration;
import ru.yandex.partner.test.db.MockedMysqlTestConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true"},
        classes = {TestApplication.class, MockedMysqlTestConfiguration.class, BalanceTestConfiguration.class,
                MultistateConfiguration.class}
)

public @interface CoreTestWithMockedDB {
}
