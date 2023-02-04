package ru.yandex.auto.vin.decoder.manager.vin

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.data_provider.FinesDataProvider
import ru.yandex.auto.vin.decoder.model.scheduler.{cs, RichWatchingStateUpdate}
import ru.yandex.auto.vin.decoder.model.state.StateUtils
import ru.yandex.auto.vin.decoder.model.{MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.{CheckburoReportType, CheckburoUpdateManager}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.StateUpdateHistory.StateUpdateSource
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.utils.features.FinesTrafficDistributionFeature
import ru.yandex.auto.vin.decoder.utils.features.TrafficDistributionFeature.ProviderWeights
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration.DurationInt

class FinesUpdateManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport with MockedFeatures {

  import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._

  val vin = VinCode("ABCDEFGHJKLMNPRST")
  val holder = WatchingStateHolder(vin, StateUtils.getNewState, System.currentTimeMillis)

  val freshReportSamplePeriod = System.currentTimeMillis() - 13.hour.toMillis
  val t: Traced = Traced.empty

  val dealerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger(StateUpdateSource.OFFER_ACTIVE, Some("dealer:123"), Some(123), None, None)

  val userRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger(StateUpdateSource.OFFER_ACTIVE, Some("user:123"), None, None, None)

  when(features.FinesForDealersTrafficDistribution).thenReturn(
    Feature[ProviderWeights[FinesDataProvider]](
      FinesTrafficDistributionFeature.nameForDealers,
      _ =>
        Map(
          FinesDataProvider.AUTOCODE -> 0,
          FinesDataProvider.CHECKBURO -> 100
        )
    )
  )

  when(features.CheckburoFines).thenReturn(
    Feature[Boolean]("", _ => true)
  )

  when(features.AutocodeFines).thenReturn(
    Feature[Boolean]("", _ => true)
  )

  when(features.FinesForUsersTrafficDistribution).thenReturn(
    Feature[ProviderWeights[FinesDataProvider]](
      FinesTrafficDistributionFeature.nameForUsers,
      _ =>
        Map(
          FinesDataProvider.AUTOCODE -> 50,
          FinesDataProvider.CHECKBURO -> 50
        )
    )
  )

  val manager = new FinesUpdateManager(new CheckburoUpdateManager, features)

  "FinesUpdateManager" should {

    "update fines state for dealers" in {
      val update = manager.update(vin, holder.toUpdate)(t, dealerRequestTrigger)

      update.nonEmpty shouldBe true
      println(update)
      getCheckburoState(update.get).get.getShouldProcess shouldBe true
      getAutocodeState(update.get) shouldBe empty
    }

    "do not update fines state for dealers if checkburo is in progress" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getCheckburoStateBuilder
              .getReportBuilder(CheckburoReportType.Fines)
              .setShouldProcess(true)
            ()
          }

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.isEmpty shouldBe true
    }

    "update fines state for dealers even if autocode is in progress" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.Fines)
              .setRequestSent(freshReportSamplePeriod)
            ()
          }

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.nonEmpty shouldBe true
      getCheckburoState(update.get).get.getShouldProcess shouldBe true
      getAutocodeState(update.get).get.getShouldProcess shouldBe false
    }

    "do not update fines state for dealers if there is fresh checkburo response" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getCheckburoStateBuilder
              .getReportBuilder(CheckburoReportType.Fines)
              .setReportArrived(freshReportSamplePeriod)
            ()
          }

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.isEmpty shouldBe true
    }

    "update fines state for dealers even if there is fresh autocode response" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.Fines)
              .setReportArrived(freshReportSamplePeriod)
            ()
          }

      val update = manager.update(vin, upd)(t, dealerRequestTrigger)

      update.nonEmpty shouldBe true
      getCheckburoState(update.get).get.getShouldProcess shouldBe true
      getAutocodeState(update.get).get.getShouldProcess shouldBe false
    }

    "update fines state for users" in {
      val update = manager.update(vin, holder.toUpdate)(t, userRequestTrigger)

      update.nonEmpty shouldBe true

      val autocode = getAutocodeState(update.get).fold(false)(_.getShouldProcess)
      val checkburo = getCheckburoState(update.get).fold(false)(_.getShouldProcess)

      (autocode || checkburo) shouldBe true
      (autocode && checkburo) shouldBe false // Only one partner should be triggered
    }

    "do not update fines state for users if checkburo is in progress" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getCheckburoStateBuilder
              .getReportBuilder(CheckburoReportType.Fines)
              .setShouldProcess(true)
            ()
          }

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }

    "do not update fines state for dealers if autocode is in progress" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.Fines)
              .setRequestSent(freshReportSamplePeriod)
            ()
          }

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }

    "do not update fines state for users if there is fresh checkburo response" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate { st =>
            st.getCheckburoStateBuilder
              .getReportBuilder(CheckburoReportType.Fines)
              .setReportArrived(freshReportSamplePeriod)
          }

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }

    "do not update fines state for users if there is fresh autocode response" in {
      val upd =
        holder.toUpdate
          .withBuilderUpdate(
            _.getAutocodeStateBuilder
              .getReportBuilder(AutocodeReportType.Fines)
              .setReportArrived(freshReportSamplePeriod)
          )

      val update = manager.update(vin, upd)(t, userRequestTrigger)

      update.isEmpty shouldBe true
    }
  }

  private def getCheckburoState(upd: WatchingStateUpdate[CompoundState]) =
    upd.state.getCheckburoState.findReport(CheckburoReportType.Fines.toString)

  private def getAutocodeState(upd: WatchingStateUpdate[CompoundState]) =
    upd.state.getAutocodeState.findReport(AutocodeReportType.Fines.toString)
}
