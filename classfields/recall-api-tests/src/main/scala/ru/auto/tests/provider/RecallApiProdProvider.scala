package ru.auto.tests.provider

import com.google.inject.Inject
import ru.auto.tests.recall.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.RecallApiConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseRestAssuredConfig
import ru.auto.tests.ra.RequestSpecSupplier.{baseReqSpecBuilder, getLogConfig}

class RecallApiProdProvider extends Provider[ApiClient] {

  @Inject
  private val config: RecallApiConfig = null

  override def get: ApiClient = {
    val carfaxApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(config.getRecallApiProdURI)
        .setConfig(baseRestAssuredConfig.logConfig(getLogConfig(config.isRestAssuredLoggerEnabled)))
    )

    ApiClient.api(carfaxApiConfig)
  }
}
