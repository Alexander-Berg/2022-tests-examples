package ru.auto.tests.realty.vos2.provider;

import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.config.Vos2ApiConfig;

import javax.inject.Provider;

import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.http.ContentType.JSON;
import static ru.auto.tests.realty.vos2.GsonObjectMapper.gson;

/**
 * Created by vicdev on 18.10.17.
 */
public class Vos2ApiProdProvider implements Provider<ApiClient> {

    @Inject
    private Vos2ApiConfig publicApiConfig;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(
                        () -> new RequestSpecBuilder().setContentType(JSON)
                                .setBaseUri(publicApiConfig.getVos2ApiProdURI())
                                .setConfig(config().objectMapperConfig(objectMapperConfig()
                                        .defaultObjectMapper(gson())))
                                .addFilter(new AllureLoggerFilter())
                );
        return ApiClient.api(autoruApiConfig);
    }
}
