package ru.yandex.auto.vin.decoder.partners.uremont.misc

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.hydra.HydraClientStub
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class UremontMiscClientIntTest extends AnyFunSuite with MockitoSupport {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty

  private val rateLimiter = RateLimiter.create(10)

  val remoteService = new RemoteHttpService(
    "uremont_info",
    new HttpEndpoint("api.inttgroup.com", 443, "https")
  )

  private val hydraClient = new HydraClientStub(2)
  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  val client = new UremontMiscClient(
    service = remoteService,
    token = "54cggvn8unv2qabgv3wyurkqty5zjrrn",
    hydraClient,
    partnerEventClient = partnerEventClient,
    rateLimiter
  )

  test("get info") {
    val vin = VinCode("Z94CB41BBFR243184")
    val res = client.fetch(vin).await

    assert(res.status == 200)
  }

}
