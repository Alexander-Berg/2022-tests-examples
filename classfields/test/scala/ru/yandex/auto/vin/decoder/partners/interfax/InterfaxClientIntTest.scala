package ru.yandex.auto.vin.decoder.partners.interfax

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class InterfaxClientIntTest extends AnyFunSuite {

  private val (host, username, password) = ("sparkgatetest.interfax.ru", "", "")

  private val client = {
    val endpoint = new HttpEndpoint(host, 443, "https")
    val service = new RemoteHttpService("interfax", endpoint)
    val limiter = RateLimiter.create(100)
    new InterfaxClient(service, username, password, new NoopPartnerEventManager, limiter)
  }

  private val vin = VinCode("JTJBERBZ402030467")
  implicit private val t = Traced.empty
  implicit private val trigger = PartnerRequestTrigger.Unknown

  test("Can make request") {
    val data = client.checkVin(vin).await
    println(data)
  }
}
