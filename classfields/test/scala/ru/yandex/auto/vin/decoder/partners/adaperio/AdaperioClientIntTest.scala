package ru.yandex.auto.vin.decoder.partners.adaperio

import auto.carfax.common.utils.tracing.Traced
import io.lemonlabs.uri.Url
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, Ignore}
import ru.yandex.auto.vin.decoder.model.{CommonVinCode, VinCode}
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType.TechInspections
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

@Ignore
class AdaperioClientIntTest extends AsyncFunSuite with Matchers with BeforeAndAfter with MockitoSupport {

  implicit val m: TestOperationalSupport.type = TestOperationalSupport
  implicit val t = Traced.empty

  implicit val partnerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger.Unknown

  val service = new RemoteHttpService(
    "adaperio",
    new HttpEndpoint("adaperio.ru", 443, "https")
  )

  private val partnerEventClient = new NoopPartnerEventManager

  private val config = AdaperioConfig(
    Url("https://adaperio.ru"),
    "AC24B293-FFCC-4141-B83F-F6EBAC18D281",
    "PqwO9133)1:fxO__4dkS"
  )

  private val client = new AdaperioClient(config, service, partnerEventClient)

  test("successful postOrder and getReport") {
    val vinCode = VinCode("XTA21140064164283")

    val orderId = client.postOrder(AdaperioReportType.TechInspections, vinCode).await

    partnerEventClient.event.getOrderId shouldBe orderId.toString

    val (_, _, adaperioReportResponse) = client.getReport(orderId, AdaperioReportType.TechInspections, vinCode).await

    adaperioReportResponse.orderId shouldBe orderId
  }

  test("get reports") {
    val reports = Map(
      //      Main -> Set(2122446035),
      //      MainUpdate -> Set(1011134511, 1791085992),
      TechInspections -> Set(1893243774)
      //      Taxi -> Set(318616883)
    )

    for {
      (reportType, orders) <- reports
      order <- orders
    } {
      val (_, _, report) = client.getReport(order, reportType, CommonVinCode("")).await
      assert(report.orderId != 0)
    }

    assert(true)
  }
}
