package ru.yandex.auto.vin.decoder.partners.allevacuators

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class AllEvacuatorsClientIntTest extends AnyFunSuite {

  implicit val t = Traced.empty
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val remoteService = new RemoteHttpService(
    "all-evacuators",
    new HttpEndpoint("lk.all-evak.ru", 80, "http")
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager
  val client = new AllEvacuatorsClient(remoteService, "evac-599b029dac062", partnerEventClient)

  test("by vin") {
    client.getAllData.await
    assert(2 + 2 == 4)
  }

}
