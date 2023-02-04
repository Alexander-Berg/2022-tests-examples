package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.restassured.builder.RequestSpecBuilder;
import ru.auto.test.api.realty.ApiBnbsearcher;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.yandex.realty.config.RealtyApiConfig;

/**
 * Created by vicdev on 01.08.17.
 */
public class BnbSearcherApiModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public ApiBnbsearcher providesRealtyBack(RealtyApiConfig config) {
        return ApiBnbsearcher.bnbsearcher(ApiBnbsearcher.Config.bnbsearcherConfig().withReqSpecSupplier(
                () -> new RequestSpecBuilder().setBaseUri(config.getBnbSearcherUri()).addFilter(new AllureLoggerFilter())));
    }
}
