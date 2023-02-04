package ru.yandex.vertis.telepony.client.mts

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.model.Credentials
import ru.yandex.vertis.telepony.service.MtsClient
import ru.yandex.vertis.telepony.service.impl.mts.MtsClientImpl
import ru.yandex.vertis.telepony.service.logging.LoggingMtsClient
import ru.yandex.vertis.telepony.service.metered.MeteredMtsClient
import ru.yandex.vertis.telepony.service.mts.{SkippingMtsClient, ThrottledMtsClient}
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}
import ru.yandex.vertis.telepony.util.{Limiter, RateLimiter, TestPrometheusComponent, Threads}

import scala.concurrent.duration.DurationInt

/**
  * @author evans
  */
@Ignore
class MtsClientImplIntSpec extends TestKit(ActorSystem("MtsClientImplSpec")) with MtsClientSpec {

  implicit val am: ActorMaterializer = ActorMaterializer()

  override val client: MtsClient = MtsClientImplIntSpec.createMtsClient
}

object MtsClientImplIntSpec extends TestPrometheusComponent {

  def createMtsClient(
      implicit
      actorMaterializer: ActorMaterializer): MtsClient = {
    val sendReceive = PipelineBuilder.buildSendReceiveWithCredentials(
      credentials = Credentials("yndx.telepony.experiments.dev@yandex.ru", "???"),
      proxy = None,
      maxConnections = 2
    )

    val httpClient = HttpClientBuilder.fromSendReceive("mts-client-spec", sendReceive)

    new MtsClientImpl(
      httpClient,
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
  }
}
