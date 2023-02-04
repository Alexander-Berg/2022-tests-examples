package ru.auto.tests.ra

import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.LogConfig
import io.restassured.config.LogConfig.logConfig
import io.restassured.http.ContentType.JSON
import ru.auto.tests.commons.restassured.AllureLoggerFilter

import java.net.URI

object RequestSpecSupplier {

  def baseReqSpecBuilder(baseUri: URI): RequestSpecBuilder = {
    new RequestSpecBuilder()
      .setContentType(JSON)
      .setBaseUri(baseUri)
      .addFilter(new AllureLoggerFilter)
  }

  def getLogConfig(loggerEnabled: Boolean): LogConfig = {
    if (loggerEnabled) logConfig.enableLoggingOfRequestAndResponseIfValidationFails
    else logConfig
  }
}
