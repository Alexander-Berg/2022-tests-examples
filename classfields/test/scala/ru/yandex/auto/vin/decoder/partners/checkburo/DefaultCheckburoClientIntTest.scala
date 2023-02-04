package ru.yandex.auto.vin.decoder.partners.checkburo

import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.model.ReadyOrder
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Ignore
class DefaultCheckburoClientIntTest extends AnyFunSuite with MockitoSupport {

  implicit val t: Traced = Traced.empty

  implicit val partnerRequestTrigger = PartnerRequestTrigger.Unknown

  val remoteService = new RemoteHttpService(
    "checkburo",
    new HttpEndpoint("checkburo.com", 443, "https"),
    client = TestHttpUtils.DefaultHttpClient
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  val client = new DefaultCheckburoClient(remoteService, partnerEventClient)

  private def testCase[R](vinString: String, reportType: CheckburoReportType[R], delay: Long = 1000) = {
    val vin = VinCode(vinString)

    val report = (for {
      token <- client.login("test@auto.ru", "123456")
      orderId <- client.createOrder(vin, reportType, token)
      _ <- Future(Thread.sleep(delay))
      result <- client.getOrderResult(vin, orderId, reportType, token)
      _ <- client.logout(token)
    } yield result).await

    report match {
      case report: ReadyOrder[R @unchecked] => report.model
      case _ => throw new RuntimeException(s"ReadyReport is expected to be here")
    }
  }

  test("Test all report types") {
    val types = List(
      CheckburoReportType.Constraints,
      CheckburoReportType.TechData,
      CheckburoReportType.Wanted,
      CheckburoReportType.Accidents,
      CheckburoReportType.RegActions
    )
    types.map(testCase("XUULA69KJB0007676", _)).foreach(println)
  }

  test("Test exact vin and report type") {
    val data = testCase("XUULA69KJB0007676", CheckburoReportType.TechData, delay = 3000)
    println(data)
  }
}
