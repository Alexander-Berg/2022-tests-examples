package ru.yandex.auto.vin.decoder.model.scheduler

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.{MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.avilon.Avilon
import ru.yandex.auto.vin.decoder.partners.carprice.Carprice
import ru.yandex.auto.vin.decoder.partners.wilgood.Wilgood
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.mockito.MockitoSupport

class WatchingStateUpdateTest extends AnyFunSuite with MockitoSupport with MockedFeatures {
  implicit val partnerRequestTrigger = PartnerRequestTrigger.Unknown

  when(features.Carprice).thenReturn(enabledFeature)
  when(features.Wilgood).thenReturn(enabledFeature)
  when(features.Avilon).thenReturn(enabledFeature)

  val emptyStateUpdate = WatchingStateUpdate.defaultSync(CompoundState.newBuilder().build())

  test("update partners") {
    val stateUpdate = emptyStateUpdate.withPartnersUpdate(features, VinCode("WBAUE11040E238571"))(
      Carprice,
      Wilgood,
      Avilon
    )
    assert(stateUpdate.state.hasCarpriceState)
    assert(stateUpdate.state.getCarpriceState.getShouldProcess)
    assert(stateUpdate.state.hasWilgoodState)
    assert(stateUpdate.state.getWilgoodState.getShouldProcess)
    assert(stateUpdate.state.hasAvilonState)
    assert(stateUpdate.state.getAvilonState.getShouldProcess)
  }
}
