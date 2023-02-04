package ru.auto.tests.cabinet.provider;

import com.google.inject.Inject;
import ru.auto.tests.cabinet.config.CabinetApiConfig;
import ru.auto.tests.cabinet.ApiClient;

import javax.inject.Provider;

/**
 * Created by vicdev on 18.10.17.
 */
public class CabinetApiProdProvider implements Provider<ApiClient> {

    @Inject
    private CabinetApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config cabinetApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> RequestSpecSupplier.baseReqSpecBuilder(config.getCabinetApiProdURI(), config.getCabinetApiVersion())
                                .setConfig(RequestSpecSupplier.baseRestAssuredConfig()
                                        .logConfig(RequestSpecSupplier.getLogConfig(config.isRestAssuredLoggerEnabled()))));
        return ApiClient.api(cabinetApiConfig);
    }
}
