package ru.auto.tests.realtyapi.provider;

import com.github.viclovsky.swagger.coverage.FileSystemOutputWriter;
import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.realtyapi.config.RealtyApiConfig;
import ru.auto.tests.realtyapi.v2.ApiClient;

import java.nio.file.Paths;

import static ru.auto.tests.realtyapi.provider.ReqSpecSupplier.baseReqSpecBuilder;
import static ru.auto.tests.realtyapi.provider.ReqSpecSupplier.baseRestAssuredConfig;
import static ru.auto.tests.realtyapi.provider.ReqSpecSupplier.getLogConfig;

public class RealtyApiV2Provider implements Provider<ApiClient> {

    @Inject
    private RealtyApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config realtyApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> baseReqSpecBuilder(config.getRealtyApiTestingURI(), config.getRealtyApiV2Path())
                        .setConfig(baseRestAssuredConfig().logConfig(getLogConfig(config.isRestAssuredLoggerEnabled())))
                        .addFilter(new SwaggerCoverageRestAssured(new FileSystemOutputWriter(Paths.get("swagger-coverage-output-v2")))));
        return ApiClient.api(realtyApiConfig);
    }
}
