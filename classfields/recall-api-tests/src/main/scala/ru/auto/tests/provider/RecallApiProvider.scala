package ru.auto.tests.provider

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured
import com.google.inject.Inject
import ru.auto.tests.recall.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.RecallApiConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseRestAssuredConfig
import ru.auto.tests.ra.RequestSpecSupplier.{baseReqSpecBuilder, getLogConfig}

class RecallApiProvider extends Provider[ApiClient] {

  @Inject
  private val config: RecallApiConfig = null

  override def get: ApiClient = {
    val carfaxApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(config.getRecallApiTestingURI)
        .setConfig(baseRestAssuredConfig.logConfig(getLogConfig(config.isRestAssuredLoggerEnabled)))
        .addFilter(new SwaggerCoverageRestAssured)
    )

    ApiClient.api(carfaxApiConfig)
  }
}
