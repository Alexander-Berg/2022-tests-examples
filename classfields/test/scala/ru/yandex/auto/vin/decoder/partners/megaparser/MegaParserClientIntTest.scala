package ru.yandex.auto.vin.decoder.partners.megaparser

import auto.carfax.common.utils.config.ProxyConfig
import auto.carfax.common.utils.tracing.Traced
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.scalatest.{BeforeAndAfterAll, Ignore}
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.partners.megaparser.client.MegaParserClient
import ru.yandex.auto.vin.decoder.partners.megaparser.model.rsa.CurrentInsurancesResponse
import ru.yandex.auto.vin.decoder.partners.megaparser.model.{
  MegaParserOrderRequest,
  MegaParserReportRequest,
  MegaParserRsaReportType
}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.config.Environment.config
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultRequestConfig
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class MegaParserClientIntTest extends AnyFunSuite with MockitoSupport with BeforeAndAfterAll {
  implicit val tracer = Traced.empty
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  lazy val requestConf = RequestConfig
    .copy(DefaultRequestConfig)
    .setProxy {
      val proxyConfig = ProxyConfig.fromConfig(config.getConfig("auto-vin-decoder.proxy"))
      new HttpHost(proxyConfig.host, proxyConfig.port)
    }
    .build
  lazy val service = new RemoteHttpService("mega_parser", HttpEndpoint("autoook.ru", 443, "https"), requestConf)
  lazy val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager
  lazy val client = new MegaParserClient(service, partnerEventClient, "") // для работы подставить key

  test("create order") {
    val lp = LicensePlate("H660PH716")
    val reportType = MegaParserRsaReportType.insuranceDetails.eventType
    val orderRequest = MegaParserOrderRequest.lpRsaOrder(lp)
    val res = client.makeOrder(lp, orderRequest, reportType).await

    println(res.queue.statusId)
    assert(res.queue.statusId.nonEmpty)
  }

  test("get report") {
    val lp = LicensePlate("H660PH716")
    val reportRequest =
      MegaParserReportRequest[LicensePlate, CurrentInsurancesResponse](
        "18216EB2EE09A6DC8A519AFFE7CC4E30",
        MegaParserRsaReportType.currentInsurances
      )
    val res = client.getReport(lp, reportRequest).await
    println(res)
    assert(res.raw.nonEmpty)
  }
}
