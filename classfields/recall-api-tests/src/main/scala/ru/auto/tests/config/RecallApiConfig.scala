package ru.auto.tests.config

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.{DefaultValue, Key}
import java.net.URI

@Config.Sources(Array("classpath:testing.properties"))
trait RecallApiConfig extends Accessible {

  @Key("recall.api.testing.uri")
  @DefaultValue("http://recalls-api-http-api.vrts-slb.test.vertis.yandex.net/api/v1/")
  def getRecallApiTestingURI: URI

  @Key("recall.api.release.uri")
  @DefaultValue("http://recalls-api-production-http-api.vrts-slb.test.vertis.yandex.net/api/v1/")
  def getRecallApiProdURI: URI

  @Key("rest.assured.logger.enabled")
  @DefaultValue("true")
  def isRestAssuredLoggerEnabled: Boolean
}
