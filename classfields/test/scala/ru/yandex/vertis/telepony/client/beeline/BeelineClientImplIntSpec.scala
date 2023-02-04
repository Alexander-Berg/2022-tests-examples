package ru.yandex.vertis.telepony.client.beeline

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.model.Credentials
import ru.yandex.vertis.telepony.service.BeelineClient
import ru.yandex.vertis.telepony.service.impl.beeline.BeelineClientImpl
import ru.yandex.vertis.telepony.service.logging.LoggingBeelineClient
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}
import ru.yandex.vertis.telepony.util.monitoring.DummyMeter

import scala.concurrent.duration._

/**
  * @author neron
  */
@Ignore
class BeelineClientImplIntSpec extends TestKit(ActorSystem("BeelineClientImplSpec")) with BeelineClientSpec {

  implicit val am = ActorMaterializer()

  override def scheduler: Scheduler = am.system.scheduler

  override val client: BeelineClient = BeelineClientImplIntSpec.createClient
}

object BeelineClientImplIntSpec {

  def createClient(implicit am: ActorMaterializer): BeelineClient = {
    val requestUrl = "https://cloudcc.beeline.ru/api/request"
    val sendReceive = PipelineBuilder.buildSendReceiveWithCredentials(
      credentials = Credentials(
        username = "yandex-vertis-test", // 79647624288 // 79647624287 79647624415 79647624259 79647624350
        password = "???" // see in yav.yandex-team.ru
      ),
      proxy = None,
      maxConnections = 2,
      futureTimeout = 5000.millis
    )
    val httpClient = HttpClientBuilder.fromSendReceive("beeline-client-spec", sendReceive)

    new BeelineClientImpl(httpClient, () => requestUrl, 100.millis, DummyMeter)(Threads.lightWeightTasksEc, am)
      with LoggingBeelineClient
  }
}
