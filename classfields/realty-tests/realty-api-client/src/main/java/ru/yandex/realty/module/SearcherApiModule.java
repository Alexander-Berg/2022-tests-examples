package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.restassured.builder.RequestSpecBuilder;
import ru.auto.test.api.realty.ApiSearcher;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.yandex.realty.config.RealtyApiConfig;

/**
 * Created by vicdev on 13.04.17.
 */
public class SearcherApiModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public ApiSearcher providesApiSearcher(RealtyApiConfig config) {
        return ApiSearcher.searcher(ApiSearcher.Config.searcherConfig()
                .withReqSpecSupplier(() -> new RequestSpecBuilder()
                        .setBaseUri(config.getSearcherUri())
                        .addFilter(new AllureLoggerFilter())));
    }
}
