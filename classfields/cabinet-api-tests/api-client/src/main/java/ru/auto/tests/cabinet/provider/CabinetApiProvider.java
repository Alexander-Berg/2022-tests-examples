package ru.auto.tests.cabinet.provider;

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import com.google.inject.Inject;
import io.restassured.config.FailureConfig;
import ru.auto.tests.cabinet.config.CabinetApiConfig;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.ra.InternalServerErrorListener;

import javax.inject.Provider;

public class CabinetApiProvider implements Provider<ApiClient> {

    @Inject
    private CabinetApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config cabinetApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> RequestSpecSupplier.baseReqSpecBuilder(config.getCabinetApiTestingURI(), config.getCabinetApiVersion())
                        .setConfig(RequestSpecSupplier.baseRestAssuredConfig()
                                .logConfig(RequestSpecSupplier.getLogConfig(config.isRestAssuredLoggerEnabled()))
                                .failureConfig(FailureConfig.failureConfig().failureListeners(new InternalServerErrorListener())))
                        .addFilter(new SwaggerCoverageRestAssured()));
        return ApiClient.api(cabinetApiConfig);
    }
}
