package ru.yandex.auto.vin.decoder.state

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.cache.AudatexDealersCache
import ru.yandex.auto.vin.decoder.model.{AudatexDealers, MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.partners.audatex.{Audatex, AudatexAudaHistory}
import ru.yandex.auto.vin.decoder.partners.interfax.InterfaxCheckVin
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.vertis.mockito.MockitoSupport

class PartnerStateScheduleTest extends AnyWordSpecLike with MockitoSupport with MockedFeatures with Matchers {

  implicit private val trigger = PartnerRequestTrigger.Unknown

  private val audatexDealersCache = mock[AudatexDealersCache]
  private val audatexDealer = AudatexPartner(Set(1L), "dealer", Audatex.Credentials("", ""))
  when(audatexDealersCache.get).thenReturn(AudatexDealers(List(audatexDealer)))

  when(features.AudatexAudaHistory).thenReturn(enabledFeature)
  when(features.AudatexFraudCheck).thenReturn(enabledFeature)
  when(features.InterfaxCheckVin).thenReturn(enabledFeature)

  private val vinStrict = VinCode("WBAUE11040E238571")
  private val vinNonStrict = VinCode("WBAUE11040E238WW1")
  private val jdmWithSpace = VinCode("TP12 000650")
  private val jdm = VinCode("TP12000650")

  "Shouldn't schedule Audatex state for JDMs with spaces" in {

    val ps = AudatexAudaHistory(audatexDealersCache)

    schedule(ps, jdmWithSpace).getAudatex.getShouldProcess shouldBe false
    schedule(ps, jdm).getAudatex.getShouldProcess shouldBe true
    schedule(ps, vinStrict).getAudatex.getShouldProcess shouldBe true
    schedule(ps, vinNonStrict).getAudatex.getShouldProcess shouldBe true
  }

  "Shouldn't schedule InterfaxCheckVin state for non-strict vin" in {

    schedule(InterfaxCheckVin, vinStrict).getInterfaxCheckVinState.getShouldProcess shouldBe true
    schedule(InterfaxCheckVin, vinNonStrict).getInterfaxCheckVinState.getShouldProcess shouldBe false
  }

  private def schedule(ps: PartnerState[_], identifier: VinCode): CompoundState = {
    ps.scheduleIfNeeded(
      ScheduleStateParams(features, identifier, mark = None, onlyIfOlderThan = None),
      CompoundState.newBuilder.build
    )
  }
}
