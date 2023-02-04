package ru.auto.tests.config

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.{DefaultValue, Key}
import java.net.URI

@Config.Sources(Array("classpath:testing.properties"))
trait SalesmanApiConfig extends Accessible {

  @Key("salesman.api.testing.uri")
  @DefaultValue("http://salesman-tasks-01-sas.test.vertis.yandex.net:1030/api")
  def getSalesmanApiTestingURI: URI

  @Key("salesman.api.release.uri")
  @DefaultValue("http://salesman-tasks-02-sas.test.vertis.yandex.net:1030/api")
  def getSalesmanApiProdURI: URI

  @Key("salesman.api.version")
  @DefaultValue("1.x")
  def getSalesmanApiVersion: String

  @Key("rest.assured.logger.enabled")
  @DefaultValue("true")
  def isRestAssuredLoggerEnabled: Boolean
}
