package ru.yandex.vertis.telepony.client.mts

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.model.{ApiKey, Credentials}
import ru.yandex.vertis.telepony.service.MtsClient
import ru.yandex.vertis.telepony.service.impl.mts
import ru.yandex.vertis.telepony.service.impl.mts.{DualMtsClient, MtsClientImpl}
import ru.yandex.vertis.telepony.service.impl.mts.v5.MtsClientV5Impl
import ru.yandex.vertis.telepony.service.impl.mts.v5.logging.LoggingMtsClientV5
import ru.yandex.vertis.telepony.service.impl.mts.v5.metering.MeteredMtsClientV5
import ru.yandex.vertis.telepony.service.logging.LoggingMtsClient
import ru.yandex.vertis.telepony.service.metered.MeteredMtsClient
import ru.yandex.vertis.telepony.service.mts.v5.{SkippingMtsClientV5, ThrottledMtsClientV5}
import ru.yandex.vertis.telepony.service.mts.{SkippingMtsClient, ThrottledMtsClient}
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}
import ru.yandex.vertis.telepony.util.{Limiter, RateLimiter, TestPrometheusComponent, Threads}

import scala.concurrent.duration.DurationInt

/**
  * @author evans
  */
@Ignore
class MtsClientImplIntSpec extends TestKit(ActorSystem("MtsClientImplSpec")) with MtsClientSpec {

  implicit val am: Materializer = Materializer(system)

  override val client: DualMtsClient = MtsClientImplIntSpec.createMtsClient
}

object MtsClientImplIntSpec extends TestPrometheusComponent {

  def createMtsClient(
      implicit
      actorMaterializer: Materializer): DualMtsClient = {
    val sendReceiveV4 = PipelineBuilder.buildSendReceiveWithCredentials(
      auth = Credentials("yndx.telepony.experiments.dev@yandex.ru", "???"),
      proxy = None,
      maxConnections = 2
    )

    val sendReceiveV5 = PipelineBuilder.buildSendReceiveWithCredentials(
      auth = ApiKey("???"),
      proxy = None,
      maxConnections = 2
    )

    val httpClientForMtsV4 = HttpClientBuilder.fromSendReceive("mts-client-spec", sendReceiveV4)
    val httpClientForMtsV5 = HttpClientBuilder.fromSendReceive("mts-client-spec", sendReceiveV5)

    val mtsClientV4 = new MtsClientImpl(
      httpClientForMtsV4,
      httpClientForMtsV4,
      "aa.mts.ru",
      443,
      100.millis
    )(Threads.lightWeightTasksEc, actorMaterializer)
      with ThrottledMtsClient
      with SkippingMtsClient
      with MeteredMtsClient
      with LoggingMtsClient
      with PrometheusProvider {
      override val limiter: Limiter =
        new RateLimiter(10, 1)

      override def maxQueueCount: Int = 5
    }

    val mtsClientV5 = new MtsClientV5Impl(
      httpClientForMtsV5,
      "aa.mts.ru",
      443,
      100.millis
    )(Threads.lightWeightTasksEc, actorMaterializer)
      with ThrottledMtsClientV5
      with SkippingMtsClientV5
      with MeteredMtsClientV5
      with LoggingMtsClientV5
      with PrometheusProvider {
      override val limiter: Limiter =
        new RateLimiter(10, 1)

      override def maxQueueCount: Int = 5
    }
    mts.DualMtsClient(clientV4 = mtsClientV4, clientV5 = mtsClientV5)
  }
}
