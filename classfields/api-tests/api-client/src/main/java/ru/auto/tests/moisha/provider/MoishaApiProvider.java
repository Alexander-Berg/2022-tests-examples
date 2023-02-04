package ru.auto.tests.moisha.provider;

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import com.google.inject.Inject;
import ru.auto.tests.moisha.config.MoishaApiConfig;
import ru.auto.tests.moisha.ApiClient;

import javax.inject.Provider;

public class MoishaApiProvider implements Provider<ApiClient> {

    @Inject
    private MoishaApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> RequestSpecSupplier.baseReqSpecBuilder(config.getMoishaApiTestingURI(), config.getMoishaApiVersion())
                        .setConfig(RequestSpecSupplier.baseRestAssuredConfig()
                                .logConfig(RequestSpecSupplier.getLogConfig(config.isRestAssuredLoggerEnabled())))
                        .addFilter(new SwaggerCoverageRestAssured()));
        return ApiClient.api(autoruApiConfig);
    }
}
