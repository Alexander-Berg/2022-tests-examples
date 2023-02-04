package ru.auto.tests.example.provider;

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured;
import com.google.inject.Inject;
import ru.auto.tests.example.config.ExampleApiConfig;
import ru.auto.tests.example.ApiClient;

import javax.inject.Provider;

public class ExampleApiProvider implements Provider<ApiClient> {

    @Inject
    private ExampleApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> RequestSpecSupplier.baseReqSpecBuilder(config.getExampleApiTestingURI(), config.getExampleApiVersion())
                        .setConfig(RequestSpecSupplier.baseRestAssuredConfig()
                                .logConfig(RequestSpecSupplier.getLogConfig(config.isRestAssuredLoggerEnabled())))
                        .addFilter(new SwaggerCoverageRestAssured()));
        return ApiClient.api(autoruApiConfig);
    }
}
