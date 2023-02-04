package ru.yandex.realty.providers;

import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
import ru.auto.test.api.realty.ApiVos2;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.yandex.realty.config.RealtyApiConfig;

import javax.inject.Provider;

public class Vos2ApiProdProvider implements Provider<ApiVos2> {

    @Inject
    private RealtyApiConfig config;

    @Override
    public ApiVos2 get() {
        return ApiVos2.vos2(ApiVos2.Config.vos2Config().withReqSpecSupplier(
                () -> new RequestSpecBuilder().setBaseUri(config.getVos2ProdUri()).addFilter(new AllureLoggerFilter())));
    }
}
