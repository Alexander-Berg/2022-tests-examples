package ru.yandex.payments.testing.pglocal.micronaut;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Nullable;

import ru.yandex.payments.testing.pglocal.Version;

import static ru.yandex.payments.testing.pglocal.micronaut.PgLocalConfiguration.CONFIG;

@ConfigurationProperties(CONFIG)
@Requires(env = Environment.TEST, property = CONFIG)
public record PgLocalConfiguration(@NotBlank String user,
                                   @NotNull Version pgVersion,
                                   @NotNull Migrations migrations) {
    public static final String CONFIG = "local-pg";
    public static final String DB_NAME_PROPERTY = "local-pg.dbname";

    @ConfigurationInject
    public PgLocalConfiguration {
    }

    @ConfigurationProperties("migrations")
    public static final record Migrations(@Nullable String resourceFolder,
                                          @Nullable Path folder) {
        @ConfigurationInject
        public Migrations(@Nullable String resourceFolder, @Nullable String folder) {
            this(resourceFolder, folder == null ? null : Paths.get(folder));

            if (resourceFolder == null && folder == null) {
                throw new ConfigurationException("'resource-folder' and 'folder' are empty");
            }
        }
    }
}
