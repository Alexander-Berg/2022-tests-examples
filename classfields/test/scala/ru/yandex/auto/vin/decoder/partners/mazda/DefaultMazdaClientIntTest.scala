package ru.yandex.auto.vin.decoder.partners.mazda

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
class DefaultMazdaClientIntTest extends AnyFunSuite {

  private val httpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("siebel.mazda.ru", 443, "https")
    new RemoteHttpService(name = "mazda", endpoint = endpoint)
  }

  private val rateLimiter = RateLimiter.create(10)
  implicit val t = Traced.empty
  implicit private val m = TestOperationalSupport
  implicit private val partnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val partnerEventClient = new NoopPartnerEventManager

  private val client =
    new DefaultMazdaClient("MMR_USER_AUTORU", "MRRussiaAutoRU", httpService, partnerEventClient, rateLimiter)

  test("get data") {
    val vin = CommonVinCode("JAZGG14F681721279")
    Await.result(client.fetch(vin), 1.minutes)
    assert(2 + 2 == 4)
  }
}
