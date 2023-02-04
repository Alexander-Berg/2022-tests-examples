package ru.yandex.auto.vin.decoder.partners.autocode

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class MosAutocodeClientIntTest extends AnyFunSuite {
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val remoteService = new RemoteHttpService(
    "mos.autocode",
    new HttpEndpoint("avtokodapi.mos.ru", 7105, "http")
  )

  implicit val t = Traced.empty

  private val rateLimiter = RateLimiter.create(10)
  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager
  val client = new MosAutocodeClient(remoteService, partnerEventClient, rateLimiter)

  test("get data") {
    val vin = VinCode("WVWZZZ3CZFE807849")
    val res = client.fetch(vin).await

    assert(res.status == 200)
  }
}
