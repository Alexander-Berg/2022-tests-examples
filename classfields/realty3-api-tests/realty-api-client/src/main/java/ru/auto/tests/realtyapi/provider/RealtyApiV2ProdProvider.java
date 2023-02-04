package ru.auto.tests.realtyapi.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.realtyapi.config.RealtyApiConfig;
import ru.auto.tests.realtyapi.v2.ApiClient;

import static ru.auto.tests.realtyapi.provider.ReqSpecSupplier.baseReqSpecBuilder;
import static ru.auto.tests.realtyapi.provider.ReqSpecSupplier.baseRestAssuredConfig;
import static ru.auto.tests.realtyapi.provider.ReqSpecSupplier.getLogConfig;

public class RealtyApiV2ProdProvider implements Provider<ApiClient> {

    @Inject
    private RealtyApiConfig config;

    @Override
    public ApiClient get() {
        ApiClient.Config realtyApiConfig = ApiClient.Config
                .apiConfig()
                .reqSpecSupplier(() -> baseReqSpecBuilder(config.getRealtyApiProdURI(), config.getRealtyApiV2Path())
                        .setConfig(baseRestAssuredConfig().logConfig(getLogConfig(config.isRestAssuredLoggerEnabled()))));
        return ApiClient.api(realtyApiConfig);
    }
}
