package ru.auto.tests.provider

import com.google.inject.Inject
import ru.auto.tests.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.SalesmanApiConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseReqSpecBuilder
import ru.auto.tests.provider.RequestSpecSupplier.getLogConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseRestAssuredConfig

/** Created by vicdev on 18.10.17.
  */
class SalesmanApiProdProvider extends Provider[ApiClient] {

  @Inject
  private val config: SalesmanApiConfig = null

  def get: ApiClient = {
    val autoruApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(
        config.getSalesmanApiProdURI,
        config.getSalesmanApiVersion
      )
        .setConfig(
          baseRestAssuredConfig.logConfig(
            getLogConfig(config.isRestAssuredLoggerEnabled)
          )
        )
    )

    ApiClient.api(autoruApiConfig)
  }
}
