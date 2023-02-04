package ru.yandex.auto.vin.decoder.partners.suzuki

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class SuzukiClientIntTest extends AnyFunSuite {

  val service = new RemoteHttpService(
    "suzuki",
    new HttpEndpoint("api.suzuki-motor.ru", 443, "https")
  )

  private val partnerEventClient = new NoopPartnerEventManager
  implicit val t = Traced.empty

  val client =
    new SuzukiClient(service, "Token 27c5a1c8a9a336e654fe89585a7fa80532238921", partnerEventClient)

  implicit val partnerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger.Unknown

  test("vehicle") {
    val vin = VinCode("JSAJTD54V00630279")
    val res = client.getVehicle(vin).await

    assert(res.model.get.vin === "JSAJTD54V00630279")
  }

  test("vehicle not found") {
    val vin = VinCode("ZSAJTD54V00630279")
    val res = client.getVehicle(vin).await

    assert(res.model.isEmpty)
    assert(res.raw.toString() === "[]")
  }

  test("vehicle-model") {
    val vin = VinCode("JSAJTD54V00630279")
    val res = client.getVehicleModel(vin, 111).await

    assert(res.model.get.id === 111)
  }

  test("vehicle-model not found") {
    val vin = VinCode("ZSAJTD54V00630279")
    val res = client.getVehicleModel(vin, -1).await

    assert(res.model.isEmpty)
    assert(res.raw.toString() === "[]")
  }

}
