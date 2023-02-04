package ru.yandex.auto.vin.decoder.partners.migalki

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class MigalkiClientIntTest extends AnyFunSuite with MockitoSupport with BeforeAndAfterAll {
  implicit val tracer = Traced.empty
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val service = new RemoteHttpService("migalki", HttpEndpoint("migalki.net", 443, "https"))
  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager
  val rateLimiter = RateLimiter.create(10)
  val client = new MigalkiClient(service, partnerEventClient, "8XfVSsUJfB8HeBeJvzjthJj69OIRLcGr", rateLimiter)

  test("getPlatePhotos - normal response") {
    val lp = LicensePlate("а001мр97")
    val res = client.getData(lp).await

    assert(res.response.cars.nonEmpty)
  }
}
