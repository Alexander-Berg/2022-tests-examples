package ru.auto.tests.realty.vos2.provider;

import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.config.Vos2ApiConfig;

import javax.inject.Provider;

import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.http.ContentType.JSON;
import static ru.auto.tests.realty.vos2.GsonObjectMapper.gson;

public class Vos2ApiProvider implements Provider<ApiClient> {

    @Inject
    private Vos2ApiConfig publicApiConfig;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(
                        () -> new RequestSpecBuilder().setContentType(JSON)
                                .setBaseUri(publicApiConfig.getVos2ApiTestingURI())
                                .setConfig(config().objectMapperConfig(objectMapperConfig()
                                        .defaultObjectMapper(gson()))
                                        .logConfig(LogConfig.logConfig()
                                                .enableLoggingOfRequestAndResponseIfValidationFails()))
                                .addFilter(new AllureLoggerFilter())
                );
        return ApiClient.api(autoruApiConfig);
    }
}
