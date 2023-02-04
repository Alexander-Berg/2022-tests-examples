package ru.yandex.payments.tvmlocal.testing.micronaut;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.io.ResourceLoader;

public class TvmlocalConfigurationPropertySourceLoader implements PropertySourceLoader {
    private final TvmlocalConfigurationPropertySource source = new TvmlocalConfigurationPropertySource();

    @Override
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader) {
        return Optional.of(source);
    }

    @Override
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader,
                                            ActiveEnvironment activeEnvironment) {
        return Optional.of(source);
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) {
        return source.asMap();
    }
}
