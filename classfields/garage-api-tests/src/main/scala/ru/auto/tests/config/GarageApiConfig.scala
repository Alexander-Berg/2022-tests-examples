package ru.auto.tests.config

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.{DefaultValue, Key}
import java.net.URI

@Config.Sources(Array("classpath:testing.properties"))
trait GarageApiConfig extends Accessible {

  @Key("garage.api.testing.uri")
  @DefaultValue("http://garage-api-http-api.vrts-slb.test.vertis.yandex.net/api/v1/")
  def getGarageApiTestingURI: URI

  @Key("garage.api.release.uri")
  @DefaultValue("http://garage-api-production-http-api.vrts-slb.test.vertis.yandex.net/api/v1/")
  def getGarageApiProdURI: URI

  @Key("rest.assured.logger.enabled")
  @DefaultValue("true")
  def isRestAssuredLoggerEnabled: Boolean
}
