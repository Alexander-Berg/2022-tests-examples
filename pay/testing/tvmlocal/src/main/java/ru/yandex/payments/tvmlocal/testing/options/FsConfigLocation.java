package ru.yandex.payments.tvmlocal.testing.options;

import java.nio.file.Path;

public record FsConfigLocation(Path path) implements ConfigLocation {
    @Override
    public Path resolvePath() {
        return path;
    }
}
