package ru.auto.tests.example.provider;

import com.google.inject.Inject;
import ru.auto.tests.example.config.ExampleApiConfig;
import ru.auto.tests.example.ApiClient;

import javax.inject.Provider;

import static ru.auto.tests.example.provider.RequestSpecSupplier.baseReqSpecBuilder;
import static ru.auto.tests.example.provider.RequestSpecSupplier.getLogConfig;
import static ru.auto.tests.example.provider.RequestSpecSupplier.baseRestAssuredConfig;

/**
 * Created by vicdev on 18.10.17.
 */
public class ExampleApiProdProvider implements Provider<ApiClient> {

    @Inject
    private ExampleApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> baseReqSpecBuilder(config.getExampleApiProdURI(), config.getExampleApiVersion())
                                .setConfig(baseRestAssuredConfig()
                                        .logConfig(getLogConfig(config.isRestAssuredLoggerEnabled()))));
        return ApiClient.api(autoruApiConfig);
    }
}
