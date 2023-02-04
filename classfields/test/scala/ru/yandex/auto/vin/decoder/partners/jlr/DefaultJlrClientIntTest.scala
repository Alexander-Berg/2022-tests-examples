package ru.yandex.auto.vin.decoder.partners.jlr

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.partners.event.PartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

// todo: разделить тестовые профили на юнит и интеграционные
@Ignore
class DefaultJlrClientIntTest extends AsyncFunSuite with BeforeAndAfterAll with MockitoSupport {

  private val rateLimiter = RateLimiter.create(10)
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty

  val httpService = new RemoteHttpService(
    "JLR",
    new HttpEndpoint("club.jaguar.ru", 443, "https")
  )

  val partnerEventClient: PartnerEventManager = mock[PartnerEventManager]

  val client = new DefaultJlrClient(httpService, partnerEventClient, rateLimiter)

  test("authenticate") {
    client.authenticate.map { sessionId =>
      assert(sessionId.id.nonEmpty)
      assert(!sessionId.isExpired)
    }
  }

  for (vin <- Seq("SALCA2BN4HH651572", "SALLMAM247A243532")) {
    test(s"getAllRepairsEx for $vin") {
      client.fetch(CommonVinCode(vin)).map { response =>
        assert(response.identifier.toString == vin)
      }
    }
  }
}
