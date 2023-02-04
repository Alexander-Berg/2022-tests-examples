package ru.yandex.auto.vin.decoder.manager.vin

import auto.carfax.common.utils.tracing.Traced
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxOptionId
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.data_provider.GibddDataProvider
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.model.state.StateUtils
import ru.yandex.auto.vin.decoder.model.{MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.vertis.mockito.MockitoSupport

class PartnerStateUpdateManagerSpec
  extends AnyWordSpecLike
  with MockitoSupport
  with MockedFeatures
  with Matchers
  with BeforeAndAfterAll {

  when(features.GibddProvidersTrafficDistribution).thenReturn(gibddTrafficDistributionFeature())
  when(features.AutocodeMain).thenReturn(enabledFeature)
  when(features.BurnOutCheckburo).thenReturn(disabledFeature)

  private val manager = new PartnerStateUpdateManager(features)
  private val jdm = VinCode("01234567")
  private val commonVin = VinCode("WBAUE11040E238571")
  implicit private val t = Traced.empty
  implicit private val trigger = PartnerRequestTrigger.Unknown

  "ReportByDefinitionManager" should {
    "trigger autocode update for jdm" in {
      val state = WatchingStateHolder(jdm, StateUtils.getNewState, 0)
      val customProviders =
        NonEmptyList.of(GibddDataProvider.ADAPERIO, GibddDataProvider.MEGA_PARSER, GibddDataProvider.CHECKBURO)
      manager
        .updateForOrder(jdm, state, customProviders.some)
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.Main.id)) should not be empty
    }

    "not trigger update for updating state" in {
      val state = {
        val stateBuilder = StateUtils.getNewState.toBuilder
        stateBuilder.getCheckburoStateBuilder.getReportBuilder(CheckburoReportType.Wanted).setShouldProcess(true)
        WatchingStateHolder(commonVin, stateBuilder.build(), 0)
      }
      val customProviders =
        NonEmptyList.of(GibddDataProvider.ADAPERIO, GibddDataProvider.MEGA_PARSER, GibddDataProvider.CHECKBURO)
      manager.updateForOrder(commonVin, state, customProviders.some) shouldBe empty
    }
  }
}
