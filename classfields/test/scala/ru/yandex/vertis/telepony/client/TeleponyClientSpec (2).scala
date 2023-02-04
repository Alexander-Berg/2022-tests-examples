package ru.yandex.vertis.telepony.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import ru.yandex.vertis.http.HostPort
import ru.yandex.vertis.telepony.http.HttpClientImpl
import ru.yandex.vertis.telepony.{SampleHelper, SpecBase}
import ru.yandex.vertis.telepony.model.{ObjectId, Phone, RedirectKey, Tag, TypedDomains}
import ru.yandex.vertis.telepony.service.RedirectServiceV2.CreateRequest
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder

import scala.concurrent.duration._

/**
  * @author neron
  */
class TeleponyClientSpec extends TestKit(ActorSystem("TeleponyClientSpec")) with SpecBase {

  implicit val am = Materializer(system)
  implicit val ec = Threads.lightWeightTasksEc

  private val httpClient =
    new HttpClientImpl("default-spec-client", PipelineBuilder.buildSendReceive(proxy = None, maxConnections = 2), None)

  lazy val client = new TeleponyClientImpl(
    httpClient,
    HostPort("telepony-api-int.vrts-slb.test.vertis.yandex.net", 80),
    TypedDomains.autoru_def
  )

  "TeleponyClientImpl" should {
    "send request" in {
      val request = CreateRequest(
        key = RedirectKey(ObjectId("unit-test"), Phone("+79817757575"), Tag.Empty),
        geoId = None,
        phoneType = None,
        ttl = Some(1.minute),
        antiFraudOptions = Set.empty,
        preferredOperator = None,
        operatorNumber = None,
        options = None
      )
      client.getOrCreate(request)(SampleHelper.rc).futureValue
    }
  }

}
