package ru.auto.tests.provider

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured
import com.google.inject.Inject
import ru.auto.tests.garage.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.GarageApiConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseRestAssuredConfig
import ru.auto.tests.ra.RequestSpecSupplier.{baseReqSpecBuilder, getLogConfig}

class GarageApiProvider extends Provider[ApiClient] {

  @Inject
  private val config: GarageApiConfig = null

  override def get: ApiClient = {
    val carfaxApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(config.getGarageApiTestingURI)
        .setConfig(baseRestAssuredConfig.logConfig(getLogConfig(config.isRestAssuredLoggerEnabled)))
        .addFilter(new SwaggerCoverageRestAssured)
    )

    ApiClient.api(carfaxApiConfig)
  }
}
