package ru.auto.tests.moisha.provider;

import com.google.inject.Inject;
import ru.auto.tests.moisha.config.MoishaApiConfig;
import ru.auto.tests.moisha.ApiClient;

import javax.inject.Provider;

import static ru.auto.tests.moisha.provider.RequestSpecSupplier.baseReqSpecBuilder;
import static ru.auto.tests.moisha.provider.RequestSpecSupplier.getLogConfig;
import static ru.auto.tests.moisha.provider.RequestSpecSupplier.baseRestAssuredConfig;

/**
 * Created by vicdev on 18.10.17.
 */
public class MoishaApiProdProvider implements Provider<ApiClient> {

    @Inject
    private MoishaApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config autoruApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> baseReqSpecBuilder(config.getMoishaApiProdURI(), config.getMoishaApiVersion())
                                .setConfig(baseRestAssuredConfig()
                                        .logConfig(getLogConfig(config.isRestAssuredLoggerEnabled()))));
        return ApiClient.api(autoruApiConfig);
    }
}
