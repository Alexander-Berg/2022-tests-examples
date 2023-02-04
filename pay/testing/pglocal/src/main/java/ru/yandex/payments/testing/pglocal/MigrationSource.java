package ru.yandex.payments.testing.pglocal;

import java.nio.file.Path;
import java.util.Arrays;

public sealed interface MigrationSource {
    String[] flywayPaths();

    record Folder(Path[] paths) implements MigrationSource {
        public Folder(Path path) {
            this(new Path[] { path });
        }

        @Override
        public String[] flywayPaths() {
            return Arrays.stream(paths)
                    .map(path -> "filesystem:" + path.toAbsolutePath())
                    .toArray(String[]::new);
        }
    }

    record ResourceFolder(String[] paths) implements MigrationSource {
        public ResourceFolder(String path) {
            this(new String[] { path });
        }

        @Override
        public String[] flywayPaths() {
            return Arrays.stream(paths)
                    .map(path -> "classpath:" + path)
                    .toArray(String[]::new);
        }
    }
}
