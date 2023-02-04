package ru.auto.tests.config

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.{DefaultValue, Key}
import java.net.URI

@Config.Sources(Array("classpath:testing.properties"))
trait CarfaxApiConfig extends Accessible {

  @Key("carfax.api.testing.uri")
  @DefaultValue("http://carfax-api-http.vrts-slb.test.vertis.yandex.net/api/v1/")
  def getCarfaxApiTestingURI: URI

  @Key("carfax.api.release.uri")
  @DefaultValue("http://carfax-api-production-http.vrts-slb.test.vertis.yandex.net/api/v1/")
  def getCarfaxApiProdURI: URI

  @Key("rest.assured.logger.enabled")
  @DefaultValue("true")
  def isRestAssuredLoggerEnabled: Boolean
}
