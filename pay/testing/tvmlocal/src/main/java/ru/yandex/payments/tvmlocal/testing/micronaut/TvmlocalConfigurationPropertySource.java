package ru.yandex.payments.tvmlocal.testing.micronaut;

import java.util.Map;

import io.micronaut.context.env.MapPropertySource;

import ru.yandex.payments.tvmlocal.testing.Utils;
import ru.yandex.payments.tvmlocal.testing.options.TvmToolOptions;

public class TvmlocalConfigurationPropertySource extends MapPropertySource {
    private static final String NAME = "tvmlocal";
    private static final Map<String, Object> PROPERTIES = Map.of(
            NAME + ".tvmtool-auth-token", TvmToolOptions.generateAuthToken(),
            NAME + ".tvmtool-port", Utils.selectRandomPort()
    );

    public TvmlocalConfigurationPropertySource() {
        super(NAME, PROPERTIES);
    }
}
