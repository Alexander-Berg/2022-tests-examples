package ru.yandex.vertis.telepony.client.passport

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.http.HttpClientImpl
import ru.yandex.vertis.telepony.service.PassportClient
import ru.yandex.vertis.telepony.service.impl.passport.PassportClientImpl
import ru.yandex.vertis.telepony.service.logging.LoggingPassportClient
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder

/**
  * @author neron
  */
@Ignore
class PassportClientImplIntSpec extends TestKit(ActorSystem("PassportClientImplSpec")) with PassportClientSpec {

  implicit val am = ActorMaterializer()
  implicit val ec = Threads.lightWeightTasksEc

  private val httpClient =
    new HttpClientImpl("default-spec-client", PipelineBuilder.buildSendReceive(proxy = None, maxConnections = 2), None)

  override def passportClient: PassportClient =
    new PassportClientImpl(
      httpClient = httpClient,
      host = "passport-api-01-sas.test.vertis.yandex.net",
      port = 6210,
      basePath = "/api/2.x/auto"
    ) with LoggingPassportClient
}
