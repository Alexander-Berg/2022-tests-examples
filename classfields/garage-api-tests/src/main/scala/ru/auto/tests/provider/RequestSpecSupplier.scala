package ru.auto.tests.provider

import io.restassured.config.FailureConfig.failureConfig
import io.restassured.config.ObjectMapperConfig.objectMapperConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.config.RestAssuredConfig.config
import ru.auto.tests.commons.restassured.AllureFailureListener
import ru.auto.tests.garage.GsonObjectMapper.gson

object RequestSpecSupplier {

  def baseRestAssuredConfig: RestAssuredConfig = {
    config
      .objectMapperConfig(objectMapperConfig.defaultObjectMapper(gson))
      .failureConfig(failureConfig.failureListeners(new AllureFailureListener))
  }
}
