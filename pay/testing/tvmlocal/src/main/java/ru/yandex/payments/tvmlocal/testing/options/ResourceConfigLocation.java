package ru.yandex.payments.tvmlocal.testing.options;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import lombok.SneakyThrows;
import lombok.val;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ResourceConfigLocation implements ConfigLocation {
    private final Path path;

    @SneakyThrows
    public ResourceConfigLocation(String resourceName) {
        val fileName = UUID.randomUUID().toString();
        val configFile = File.createTempFile(fileName, ".conf");
        configFile.deleteOnExit();

        try (val configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (configStream == null) {
                throw new IllegalArgumentException("Resource '" + resourceName + "' not found on the classpath");
            }
            Files.copy(configStream, configFile.toPath(), REPLACE_EXISTING);
        }

        this.path = configFile.toPath();
    }

    @Override
    public Path resolvePath() {
        return path;
    }
}
