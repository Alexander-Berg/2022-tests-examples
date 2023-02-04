package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.restassured.builder.RequestSpecBuilder;
import ru.auto.test.api.realty.ApiPromo;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.yandex.realty.config.RealtyApiConfig;

/**
 * @author kurau (Yuri Kalinin)
 */
public class PromoApiModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public ApiPromo providesApiPromo(RealtyApiConfig config) {
        return ApiPromo.promo(ApiPromo.Config.promoConfig()
                .withReqSpecSupplier(() -> new RequestSpecBuilder()
                        .setBaseUri(config.getPromoUri())
                        .addFilter(new AllureLoggerFilter())));
    }
}
