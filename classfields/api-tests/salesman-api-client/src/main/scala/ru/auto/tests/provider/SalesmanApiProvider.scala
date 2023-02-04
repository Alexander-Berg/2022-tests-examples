package ru.auto.tests.provider

import com.github.viclovsky.swagger.coverage.SwaggerCoverageRestAssured
import com.google.inject.Inject
import ru.auto.tests.ApiClient
import javax.inject.Provider
import ru.auto.tests.config.SalesmanApiConfig
import ru.auto.tests.provider.RequestSpecSupplier.getLogConfig
import ru.auto.tests.provider.RequestSpecSupplier.baseReqSpecBuilder
import ru.auto.tests.provider.RequestSpecSupplier.baseRestAssuredConfig

class SalesmanApiProvider extends Provider[ApiClient] {

  @Inject
  private val config: SalesmanApiConfig = null

  def get: ApiClient = {
    val autoruApiConfig = ApiClient.Config.apiConfig.reqSpecSupplier(() =>
      baseReqSpecBuilder(
        config.getSalesmanApiTestingURI,
        config.getSalesmanApiVersion
      )
        .setConfig(
          baseRestAssuredConfig.logConfig(
            getLogConfig(config.isRestAssuredLoggerEnabled)
          )
        )
        .addFilter(new SwaggerCoverageRestAssured)
    )

    ApiClient.api(autoruApiConfig)
  }
}
