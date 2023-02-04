package ru.yandex.auto.vin.decoder.partners.nissan

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

// todo: разделить тестовые профили на юнит и интеграционные
@Ignore
class DefaultNissanClientIntTest extends AsyncFunSuite with MockitoSupport with BeforeAndAfterAll {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty

  private val rateLimiter = RateLimiter.create(10)

  val remoteService = new RemoteHttpService(
    "nissan",
    new HttpEndpoint("customer360.ru", 443, "https")
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  val client = new DefaultNissanClient(remoteService, "customer360.ru", partnerEventClient, rateLimiter)

  val vins: Seq[String] = Seq(
    "Z8NAJL01048643094",
    "Z8NTBNT32ES011671",
    "Z8NFBAJ11ES021268",
    "VSKCVAD40U0234656",
    "Z8NTANT31CS055576"
  )

  test("get token") {
    client.getToken.map { token =>
      assert(!token.isExpired)
    }
  }

  for (vin <- vins) {
    test(s"get vin data for $vin") {
      client.fetch(CommonVinCode(vin)).map { response =>
        assert(response.identifier.toString == vin)
      }
    }
  }
}
