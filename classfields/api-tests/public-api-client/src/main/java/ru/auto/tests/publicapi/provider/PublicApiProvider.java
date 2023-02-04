package ru.auto.tests.publicapi.provider;

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import com.google.inject.Inject;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.config.PublicApiConfig;

import javax.inject.Provider;

import static ru.auto.tests.publicapi.provider.RequestSpecSupplier.baseReqSpecBuilder;
import static ru.auto.tests.publicapi.provider.RequestSpecSupplier.getLogConfig;
import static ru.auto.tests.publicapi.provider.RequestSpecSupplier.baseRestAssuredConfig;

public class PublicApiProvider implements Provider<ApiClient> {

    @Inject
    private PublicApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> baseReqSpecBuilder(config.getPublicApiTestingURI(), config.getPublicApiVersion())
                        .setConfig(baseRestAssuredConfig()
                                .logConfig(getLogConfig(config.isRestAssuredLoggerEnabled())))
                        .addFilter(new SwaggerCoverageRestAssured()));
        return ApiClient.api(autoruApiConfig);
    }
}
