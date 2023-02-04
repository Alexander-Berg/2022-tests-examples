package ru.auto.tests.publicapi.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j;
import org.aeonbits.owner.Config.Key;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.publicapi.config.PublicApiConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
public class PublicApiConfigModule extends AbstractModule {

    @Provides
    @Singleton
    public PublicApiConfig providePublicApiConfig() {
        PublicApiConfig config = ConfigFactory.create(PublicApiConfig.class,
                (Properties) System.getProperties().clone(), System.getenv());

        Properties properties = new Properties();
        config.fill(properties);
        Set<String> propertiesNames = Arrays.stream(PublicApiConfig.class.getDeclaredMethods())
                .map(m -> m.getDeclaredAnnotation(Key.class).value()).collect(Collectors.toSet());
        properties.entrySet().removeIf(e -> !propertiesNames.contains(e.getKey()));
        String path = "target/allure-results/environment.properties";
        try {
            properties.store(new FileOutputStream(path), null);
        } catch (IOException ignored) {
            log.warn(String.format("%s not found", path));
        }
        return config;
    }

    @Override
    protected void configure() {

    }
}
