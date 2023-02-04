package ru.auto.tests.provider

import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.LogConfig
import io.restassured.config.RestAssuredConfig
import ru.auto.tests.commons.restassured.AllureFailureListener
import ru.auto.tests.commons.restassured.AllureLoggerFilter
import java.net.URI
import io.restassured.config.FailureConfig.failureConfig
import io.restassured.config.LogConfig.logConfig
import io.restassured.config.ObjectMapperConfig.objectMapperConfig
import io.restassured.config.RestAssuredConfig.config
import io.restassured.http.ContentType.JSON
import ru.auto.tests.GsonObjectMapper.gson

object RequestSpecSupplier {

  private[provider] def baseReqSpecBuilder(baseUri: URI, basePath: String) =
    new RequestSpecBuilder()
      .setContentType(JSON)
      .setBaseUri(baseUri)
      .setBasePath(basePath)
      .addFilter(new AllureLoggerFilter)

  private[provider] def getLogConfig(loggerEnabled: Boolean): LogConfig = {
    if (loggerEnabled)
      return logConfig.enableLoggingOfRequestAndResponseIfValidationFails
    logConfig
  }

  private[provider] def baseRestAssuredConfig =
    config
      .objectMapperConfig(objectMapperConfig.defaultObjectMapper(gson))
      .failureConfig(failureConfig.failureListeners(new AllureFailureListener))
}

class RequestSpecSupplier private () {}
