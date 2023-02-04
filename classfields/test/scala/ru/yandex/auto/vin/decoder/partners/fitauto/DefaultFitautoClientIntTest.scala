package ru.yandex.auto.vin.decoder.partners.fitauto

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Ignore
class DefaultFitautoClientIntTest extends AnyFunSuite {

  private val httpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("servicebook.fitauto.ru", 443, "https")
    new RemoteHttpService(name = "fitauto", endpoint = endpoint)
  }

  implicit val t = Traced.empty
  implicit private val m = TestOperationalSupport
  implicit private val partnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val rateLimiter = RateLimiter.create(10)

  private val partnerEventClient = new NoopPartnerEventManager

  private val client = new DefaultFitautoClient(
    new FitautoConfig(
      "Autoru",
      "O#CewV%KuRl6GjcR5xJ8"
    ),
    httpService,
    partnerEventClient,
    rateLimiter
  )

  test("get data") {
    val vin = CommonVinCode("JMZER893800122961")
    val data = Await.result(client.fetch(vin), 1.minutes)
    data
  }
}
