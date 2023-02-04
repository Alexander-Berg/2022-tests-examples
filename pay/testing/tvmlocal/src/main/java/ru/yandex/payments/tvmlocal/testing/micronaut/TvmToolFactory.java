package ru.yandex.payments.tvmlocal.testing.micronaut;

import java.util.OptionalInt;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import lombok.val;

import ru.yandex.payments.tvmlocal.testing.BinarySandboxSource;
import ru.yandex.payments.tvmlocal.testing.TvmTool;
import ru.yandex.payments.tvmlocal.testing.options.TvmToolOptions;

@Factory
@Requires(beans = TvmlocalConfiguration.class)
public class TvmToolFactory {
    private static final BinarySandboxSource TVMTOOL_SOURCE = new BinarySandboxSource();

    @Context
    @Bean(preDestroy = "stop")
    public TvmTool tvmtool(TvmlocalConfiguration configuration,
                           @Value("${tvmlocal.tvmtool-port}") int port,
                           @Value("${tvmlocal.tvmtool-auth-token}") String authToken) {
        val options = new TvmToolOptions(OptionalInt.of(port), configuration.configLocation(),
                configuration.mode(), configuration.env(), authToken);
        return TvmTool.start(TVMTOOL_SOURCE, options);
    }
}
