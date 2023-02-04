package ru.yandex.partner.coreexperiment.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = "spring.main.allow-bean-definition-overriding=true",
        classes = CoreLibraryTestConfiguration.class
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Transactional
public @interface CoreLibraryTest {
}
