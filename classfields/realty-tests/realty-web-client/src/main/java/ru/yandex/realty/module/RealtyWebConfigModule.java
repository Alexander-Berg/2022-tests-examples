package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.aeonbits.owner.ConfigFactory;
import ru.yandex.realty.config.RealtyTagConfig;
import ru.yandex.realty.config.RealtyWebConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.aeonbits.owner.Config.Key;

public class RealtyWebConfigModule extends AbstractModule {

    @Provides
    @Singleton
    public RealtyWebConfig provideRealtyWebConfig() throws IOException {
        RealtyWebConfig cfg = ConfigFactory.create(RealtyWebConfig.class, System.getProperties(), System.getenv());
        Properties properties = new Properties();
        cfg.fill(properties);
        Set<String> propertiesNames = Arrays.stream(RealtyWebConfig.class.getDeclaredMethods())
                .map(m -> m.getDeclaredAnnotation(Key.class).value()).collect(Collectors.toSet());
        properties.entrySet().removeIf(e -> !propertiesNames.contains(e.getKey()));
        File file = new File("target/allure-results/environment.properties");
        properties.store(new FileOutputStream(file.getName()), null);
        return cfg;
    }

    @Provides
    @Singleton
    public RealtyTagConfig provideRealtyTagConfig() {
        return ConfigFactory.create(RealtyTagConfig.class, System.getProperties(), System.getenv());
    }

    @Override
    protected void configure() {
    }

}
