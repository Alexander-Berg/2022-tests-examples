package ru.auto.tests.publicapi.provider;

import com.google.inject.Inject;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.config.PublicApiConfig;

import javax.inject.Provider;

import static ru.auto.tests.publicapi.provider.RequestSpecSupplier.baseReqSpecBuilder;
import static ru.auto.tests.publicapi.provider.RequestSpecSupplier.getLogConfig;
import static ru.auto.tests.publicapi.provider.RequestSpecSupplier.baseRestAssuredConfig;

/**
 * Created by vicdev on 18.10.17.
 */
public class PublicApiProdProvider implements Provider<ApiClient> {

    @Inject
    private PublicApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> baseReqSpecBuilder(config.getPublicApiProdURI(), config.getPublicApiVersion())
                                .setConfig(baseRestAssuredConfig()
                                        .logConfig(getLogConfig(config.isRestAssuredLoggerEnabled()))));
        return ApiClient.api(autoruApiConfig);
    }
}
