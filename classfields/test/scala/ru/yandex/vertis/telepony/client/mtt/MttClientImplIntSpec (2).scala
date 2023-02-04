package ru.yandex.vertis.telepony.client.mtt

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.model.Credentials
import ru.yandex.vertis.telepony.service.impl.mtt.{
  MttCallRecordClientImpl,
  MttClientImpl,
  MttGlobalApiClientImpl,
  MttPhoneClientImpl
}
import ru.yandex.vertis.telepony.service.logging.LoggingMttClient
import ru.yandex.vertis.telepony.service.metered.MeteredMttClient
import ru.yandex.vertis.telepony.service.mtt.{SkippingMttClient, ThrottledMttClient}
import ru.yandex.vertis.telepony.service.{MttCallRecordClient, MttClient, MttGlobalApiClient, MttPhoneClient}
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}
import ru.yandex.vertis.telepony.util.{Limiter, RateLimiter, TestPrometheusComponent, Threads}

import scala.concurrent.duration.DurationInt

/**
  * @author neron
  */
@Ignore
class MttClientImplIntSpec extends TestKit(ActorSystem("MttClientImplSpec")) with MttClientSpec {

  implicit val am = Materializer(system)

  override val client: MttClient = MttClientImplIntSpec.createMttClient
}

object MttClientImplIntSpec extends TestPrometheusComponent {

  // You can find credentials in yav.yandex-team.ru
  private val CustomerName = "110000346"
  private val WebApiCommonCredentials = Credentials(username = "???", password = "???")
  private val RecordsCredentials = Credentials(username = "???", password = "???")
  private val GApiCredentials = Credentials(username = "???", password = "???")

  def createMttPhoneClient(implicit am: Materializer): MttPhoneClient = {
    val sendReceive = PipelineBuilder.buildSendReceiveWithCredentials(
      auth = WebApiCommonCredentials,
      proxy = None,
      maxConnections = 2
    )
    val httpClient = HttpClientBuilder.fromSendReceive("mtt-client-spec", sendReceive)

    new MttPhoneClientImpl(
      httpClient = httpClient,
      baseUrl = "https://webapicommon.mtt.ru:443/index.php",
      newBaseUrl = "https://api.mtt.ru:443/ipcr/",
      archiveUrl = "https://webapicommon.mtt.ru:443/archive/index.php",
      archiveTimestamp = DateTime.parse("2020-11-19T00:00:00+03:00"),
      customerName = CustomerName,
      eventUrl = "https://mtt_user:mtt_password@telepony.test.vertis.yandex.net/reactive-api/operators/mtt",
      100.millis
    )(Threads.lightWeightTasksEc, am)
  }

  def createMttCallRecordClient(
      implicit
      am: Materializer): MttCallRecordClient = {
    val sendReceive = PipelineBuilder.buildSendReceiveWithCredentials(
      auth = RecordsCredentials,
      proxy = None,
      maxConnections = 2
    )
    val httpClient = HttpClientBuilder.fromSendReceive("mtt-client-spec", sendReceive)
    new MttCallRecordClientImpl(
      httpClient = httpClient,
      baseUrl = "https://rc.mtt.ru:443",
      100.millis
    )(Threads.lightWeightTasksEc, am)
  }

  def createMttGlobalBlackListClient(
      implicit
      am: Materializer): MttGlobalApiClient = {
    val sendReceive =
      PipelineBuilder.buildSendReceiveWithCredentials(auth = GApiCredentials, proxy = None, maxConnections = 2)
    val httpClient = HttpClientBuilder.fromSendReceive("mtt-client-spec", sendReceive)

    new MttGlobalApiClientImpl(
      httpClient = httpClient,
      baseUrl = "https://gapi.mtt.ru:6443/v1/api",
      CustomerName,
      100.millis
    )(Threads.lightWeightTasksEc, am)
  }

  def createMttClient(implicit am: Materializer): MttClient =
    new MttClientImpl(createMttPhoneClient, createMttCallRecordClient, createMttGlobalBlackListClient)
      with ThrottledMttClient
      with SkippingMttClient
      with MeteredMttClient
      with LoggingMttClient
      with PrometheusProvider {
      override val limiter: Limiter = new RateLimiter(10, 1)

      override def maxQueueCount: Int = 5
    }
}
