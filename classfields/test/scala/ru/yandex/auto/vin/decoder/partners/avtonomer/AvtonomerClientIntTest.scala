package ru.yandex.auto.vin.decoder.partners.avtonomer

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class AvtonomerClientIntTest extends AnyFunSuite {

  implicit val t = Traced.empty
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val remoteService = new RemoteHttpService(
    "avtonomer",
    new HttpEndpoint("platesmania.com", 80, "http")
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager
  val client = new AvtonomerClient(remoteService, "Jg4NAo8Wsf", partnerEventClient)

  test("get photo") {
    val lp = LicensePlate("a001aa98")
    val res = client.getPhoto(lp).await
    assert(res.response.cars.size === 4)
  }

}
