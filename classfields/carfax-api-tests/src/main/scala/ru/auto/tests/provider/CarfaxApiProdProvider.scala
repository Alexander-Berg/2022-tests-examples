package ru.auto.tests.provider

import com.google.inject.Inject
import ru.auto.tests.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.CarfaxApiConfig
import ru.auto.tests.ra.RequestSpecSupplier.{baseReqSpecBuilder, getLogConfig}
import RequestSpecSupplier.baseRestAssuredConfig

class CarfaxApiProdProvider extends Provider[ApiClient] {

  @Inject
  private val config: CarfaxApiConfig = null

  override def get: ApiClient = {
    val carfaxApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(config.getCarfaxApiProdURI)
        .setConfig(baseRestAssuredConfig.logConfig(getLogConfig(config.isRestAssuredLoggerEnabled)))
    )

    ApiClient.api(carfaxApiConfig)
  }
}
