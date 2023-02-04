package ru.auto.tests.provider

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured
import com.google.inject.Inject
import ru.auto.tests.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.CarfaxApiConfig
import ru.auto.tests.ra.RequestSpecSupplier.{baseReqSpecBuilder, getLogConfig}
import RequestSpecSupplier.baseRestAssuredConfig

class CarfaxApiProvider extends Provider[ApiClient] {

  @Inject
  private val config: CarfaxApiConfig = null

  override def get: ApiClient = {
    val carfaxApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(config.getCarfaxApiTestingURI)
        .setConfig(baseRestAssuredConfig.logConfig(getLogConfig(config.isRestAssuredLoggerEnabled)))
        .addFilter(new SwaggerCoverageRestAssured)
    )

    ApiClient.api(carfaxApiConfig)
  }
}
