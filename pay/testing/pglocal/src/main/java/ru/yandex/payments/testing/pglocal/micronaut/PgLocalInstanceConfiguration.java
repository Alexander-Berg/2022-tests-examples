package ru.yandex.payments.testing.pglocal.micronaut;

import javax.validation.constraints.NotNull;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;

import ru.yandex.payments.testing.pglocal.ServerType;

@Requires(env = Environment.TEST)
@EachProperty(PgLocalConfiguration.CONFIG + ".cluster")
public record PgLocalInstanceConfiguration(@NotNull ServerType role,
                                           @Parameter String name) {
    @ConfigurationInject
    public PgLocalInstanceConfiguration {
    }

    public boolean isMaster() {
        return role == ServerType.MASTER;
    }
}
