package ru.yandex.payments.tvmlocal.testing.micronaut;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import javax.validation.constraints.NotBlank;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.format.MapFormat;

import ru.yandex.payments.tvmlocal.testing.options.ConfigLocation;
import ru.yandex.payments.tvmlocal.testing.options.FsConfigLocation;
import ru.yandex.payments.tvmlocal.testing.options.Mode;
import ru.yandex.payments.tvmlocal.testing.options.ResourceConfigLocation;

import static java.util.Objects.requireNonNullElseGet;
import static ru.yandex.payments.tvmlocal.testing.micronaut.TvmlocalConfiguration.PREFIX;

@ConfigurationProperties(PREFIX)
@Requires(env = Environment.TEST)
@Requires(property = PREFIX)
public record TvmlocalConfiguration(Mode mode,
                                    ConfigLocation configLocation,
                                    Map<String, String> env) {
    public static final String PREFIX = "tvmlocal";
    private static final String CLASSPATH_PREFIX = "classpath:";

    @ConfigurationInject
    public TvmlocalConfiguration(@Bindable(defaultValue = "unittest") Mode mode,
                                 @NotBlank String config,
                                 @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
                                 @Nullable Map<String, String> env) {
        this(
                mode,
                config.startsWith(CLASSPATH_PREFIX)
                        ? new ResourceConfigLocation(config.substring(CLASSPATH_PREFIX.length()))
                        : new FsConfigLocation(Path.of(config)),
                requireNonNullElseGet(env, Collections::emptyMap)
        );
    }
}
