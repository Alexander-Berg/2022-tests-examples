package ru.yandex.auto.vin.decoder.manager.vin

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.enablers.Emptiness.emptinessOfOption
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.data_provider.TechInspectionDataProvider
import ru.yandex.auto.vin.decoder.model.scheduler.{cs, RichWatchingStateUpdate}
import ru.yandex.auto.vin.decoder.model.state.StateUtils
import ru.yandex.auto.vin.decoder.model.{MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.{CheckburoReportType, CheckburoUpdateManager}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.StateUpdateHistory.StateUpdateSource
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.utils.features.TechInspectionProvidersTrafficDistributionFeature
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._

class TechInspectionUpdateManagerSpec extends AnyWordSpecLike with Matchers with MockitoSupport with MockedFeatures {

  import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._

  val vin = VinCode("ABCDEFGHJKLMNPRST")
  val holder = WatchingStateHolder(vin, StateUtils.getNewState, System.currentTimeMillis)
  val checkburoUpdateManager = new CheckburoUpdateManager

  val t: Traced = Traced.empty

  val dealerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger(StateUpdateSource.OFFER_ACTIVE, Some("dealer:123"), Some(123), None, None)

  val userRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger(StateUpdateSource.OFFER_ACTIVE, Some("user:123"), None, None, None)

  when(features.TechInspectionProvidersTrafficDistributionForDealers).thenReturn(
    Feature[ProviderWeights[TechInspectionDataProvider]](
      TechInspectionProvidersTrafficDistributionFeature.nameForDealers,
      _ =>
        Map(
          TechInspectionDataProvider.AUTOCODE -> 0,
          TechInspectionDataProvider.ADAPERIO -> 100,
          TechInspectionDataProvider.CHECKBURO -> 0
        )
    )
  )

  when(features.TechInspectionProvidersTrafficDistributionForUsers).thenReturn(
    Feature[ProviderWeights[TechInspectionDataProvider]](
      TechInspectionProvidersTrafficDistributionFeature.nameForUsers,
      _ =>
        Map(
          TechInspectionDataProvider.AUTOCODE -> 50,
          TechInspectionDataProvider.ADAPERIO -> 50,
          TechInspectionDataProvider.CHECKBURO -> 50
        )
    )
  )

  val manager = new TechInspectionUpdateManager(checkburoUpdateManager, features)

  "TechInspectionUpdateManager" should {

    "update tech inspection state for dealers" in {
      val update = manager.update(vin, holder.toUpdate)(t, dealerRequestTrigger)

      update.nonEmpty shouldBe true
      println(update)
      getAdaperioState(update.get).get.getShouldProcess shouldBe true
      getAutocodeState(update.get) shouldBe empty
    }

    "do not update tech inspection state for dealers if adaperio is in progress" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAdaperioBuilder.getReportBuilder(AdaperioReportType.TechInspections).setRequestSent(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.isEmpty shouldBe true
    }

    "update tech inspection state for dealers even if autocode is in progress" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.TechInspections)
              .setRequestSent(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.nonEmpty shouldBe true
      getAdaperioState(update.get).get.getShouldProcess shouldBe true
      getAutocodeState(update.get).get.getShouldProcess shouldBe false
    }

    "do not update tech inspection state for dealers if there is fresh adaperio response" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAdaperioBuilder.getReportBuilder(AdaperioReportType.TechInspections).setReportArrived(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.isEmpty shouldBe true
    }

    "update tech inspection state for dealers even if there is fresh autocode response" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.TechInspections)
              .setReportArrived(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.nonEmpty shouldBe true
      getAdaperioState(update.get).get.getShouldProcess shouldBe true
      getAutocodeState(update.get).get.getShouldProcess shouldBe false
    }

    "update tech inspection state for users" in {
      val update = manager.update(vin, holder.toUpdate)(t, userRequestTrigger)

      update.nonEmpty shouldBe true

      val autocode = getAutocodeState(update.get).fold(false)(_.getShouldProcess)
      val adaperio = getAdaperioState(update.get).fold(false)(_.getShouldProcess)
      val checkburo = getCheckburoState(update.get).fold(false)(_.getShouldProcess)

      (autocode || adaperio || checkburo) shouldBe true
      List(autocode, adaperio, checkburo).count(identity) shouldBe 1 // Only one partner should be triggered
    }

    "do not update tech inspection state for users if adaperio is in progress" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAdaperioBuilder.getReportBuilder(AdaperioReportType.TechInspections).setRequestSent(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }

    "do not update tech inspection state for dealers if autocode is in progress" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.TechInspections)
              .setRequestSent(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }

    "do not update tech inspection state for users if there is fresh adaperio response" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAdaperioBuilder.getReportBuilder(AdaperioReportType.TechInspections).setReportArrived(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }

    "do not update tech inspection state for users if there is fresh autocode response" in {
      val oneDayBefpre = System.currentTimeMillis() - 1.day.toMillis
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.TechInspections)
              .setReportArrived(oneDayBefpre)
          )

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }
  }

  private def getCheckburoState(upd: WatchingStateUpdate[CompoundState]) =
    upd.state.getCheckburoState.findReport(CheckburoReportType.Mileages.toString)

  private def getAdaperioState(upd: WatchingStateUpdate[CompoundState]) =
    upd.state.getAdaperio.findReport(AdaperioReportType.TechInspections.toString)

  private def getAutocodeState(upd: WatchingStateUpdate[CompoundState]) =
    upd.state.getAutocodeState.findReport(AutocodeReportType.TechInspections.toString)
}
