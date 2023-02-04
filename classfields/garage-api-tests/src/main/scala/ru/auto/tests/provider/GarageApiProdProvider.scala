package ru.auto.tests.provider

import com.google.inject.Inject
import ru.auto.tests.garage.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.GarageApiConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseRestAssuredConfig
import ru.auto.tests.ra.RequestSpecSupplier.{baseReqSpecBuilder, getLogConfig}

class GarageApiProdProvider extends Provider[ApiClient] {

  @Inject
  private val config: GarageApiConfig = null

  override def get: ApiClient = {
    val carfaxApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(config.getGarageApiProdURI)
        .setConfig(baseRestAssuredConfig.logConfig(getLogConfig(config.isRestAssuredLoggerEnabled)))
    )

    ApiClient.api(carfaxApiConfig)
  }
}
