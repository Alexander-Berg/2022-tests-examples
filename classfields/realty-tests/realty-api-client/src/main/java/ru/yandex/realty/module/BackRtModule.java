package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.restassured.builder.RequestSpecBuilder;
import ru.auto.test.api.realty.ApiRealtyBack;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.yandex.realty.config.RealtyApiConfig;

/**
 * Created by vicdev on 30.06.17.
 */
public class BackRtModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public ApiRealtyBack providesRealtyBack(RealtyApiConfig config) {
        return ApiRealtyBack.realtyback(ApiRealtyBack.Config.realtybackConfig().withReqSpecSupplier(
                () -> new RequestSpecBuilder().setBaseUri(config.getRealtyBackUri()).addFilter(new AllureLoggerFilter())));
    }
}
