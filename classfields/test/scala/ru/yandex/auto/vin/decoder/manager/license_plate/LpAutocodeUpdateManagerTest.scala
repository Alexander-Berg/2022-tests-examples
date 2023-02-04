package ru.yandex.auto.vin.decoder.manager.license_plate

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.auto.vin.decoder.manager.licenseplate.LpAutocodeUpdateManager
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.model.scheduler.{cs, RichWatchingStateUpdate}
import ru.yandex.auto.vin.decoder.model.state.StateUtils.getNewStateUpdate
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{AutocodeReport, CompoundState}
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.service.licenseplate.LicensePlateUpdateService
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

class LpAutocodeUpdateManagerTest
  extends AnyWordSpecLike
  with MockitoSupport
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val source = AutocodeReport.Source.INTERNAL

  val lpUpdateService = mock[LicensePlateUpdateService]
  val lpAutocodeUpdateManager = new LpAutocodeUpdateManager(lpUpdateService)

  val lp = LicensePlate("A777AA111")

  def makeHolder(state: WatchingStateUpdate[CompoundState]): WatchingStateHolder[LicensePlate, CompoundState] = {
    WatchingStateHolder(lp, state.state, System.currentTimeMillis() + 1000L)
  }

  Set(AutocodeReportType.Identifiers).foreach { autocodeReportType =>
    s"LicensePlateAutocodeUpdateManager" should {
      s"process $autocodeReportType report" when {
        "report is old" in {
          val b = AutocodeReport
            .newBuilder()
            .setReportArrived(55)
            .setReportType(autocodeReportType.id)

          val state = getNewStateUpdate
          val res = LpAutocodeUpdateManager.processReport(b.build(), state, source)
          res.isDefined shouldBe true
          res.get.state.getAutocodeState.findReport(autocodeReportType.id).exists(_.getShouldProcess) shouldBe true
          res.get.delay.isFinite shouldBe true
        }

        "report is empty" in {
          val b = AutocodeReport.newBuilder().setReportType(autocodeReportType.id)
          val state = getNewStateUpdate
          val res = LpAutocodeUpdateManager.processReport(b.build(), state, source)
          res.isDefined shouldBe true
          res.get.state.getAutocodeState.findReport(autocodeReportType.id).exists(_.getShouldProcess) shouldBe true
          res.get.delay.isFinite shouldBe true
        }

        "report is ok" in {
          val b = AutocodeReport
            .newBuilder()
            .setReportArrived(System.currentTimeMillis())
            .setRequestSent(1)
            .setReportType(autocodeReportType.id)
          val state = getNewStateUpdate
          val res = LpAutocodeUpdateManager.processReport(b.build(), state, source)
          res.isDefined shouldBe false
        }
      }

      s"force process $autocodeReportType report" when {
        "report is ok" in {
          val b = AutocodeReport
            .newBuilder()
            .setRequestSent(1)
            .setReportArrived(System.currentTimeMillis())
            .setReportType(autocodeReportType.id)
          val state = getNewStateUpdate
          val res = LpAutocodeUpdateManager.forceProcessReport(b.build(), state, source)
          res.isDefined shouldBe true
          res.get.delay.isFinite shouldBe true
          res.get.state.getAutocodeState.findReport(autocodeReportType.id).exists(_.getForceUpdate) shouldBe true
        }

        "report is unfinished" in {
          val b = AutocodeReport
            .newBuilder()
            .setReportType(autocodeReportType.id)
            .setRequestSent(System.currentTimeMillis())
            .setReportArrived(System.currentTimeMillis() - 1000)
          val state = getNewStateUpdate
          val res = LpAutocodeUpdateManager.forceProcessReport(b.build(), state, source)
          res.isDefined shouldBe false
        }
      }
    }

    it should {
      s"update $autocodeReportType" when {
        "autocode invalid" in {
          val state = getNewStateUpdate.withBuilderUpdate { st =>
            st.getAutocodeStateBuilder.setInvalid(true)
            ()
          }
          val holder = makeHolder(state)
          val res = lpAutocodeUpdateManager.update(holder, source)
          res.isDefined shouldBe false
        }

        "no autocode state" in {
          val holder = makeHolder(getNewStateUpdate)
          val res = lpAutocodeUpdateManager.update(holder, source)
          res.isDefined shouldBe true
          res.get.state.getAutocodeState
            .findReport(AutocodeReportType.Identifiers.id)
            .exists(_.getShouldProcess) shouldBe true
          res.get.delay.isFinite shouldBe true
        }

        "no autocode report" in {
          val holder = makeHolder(getNewStateUpdate.withBuilderUpdate { st =>
            st.getAutocodeStateBuilder
            ()
          })
          val res = lpAutocodeUpdateManager.update(holder, source)
          res.isDefined shouldBe true
          res.get.state.getAutocodeState
            .findReport(AutocodeReportType.Identifiers.id)
            .exists(_.getShouldProcess) shouldBe true
          res.get.delay.isFinite shouldBe true
        }
      }

      s"force update $autocodeReportType" when {
        "autocode invalid" in {
          val state = getNewStateUpdate.withBuilderUpdate { st =>
            st.getAutocodeStateBuilder.setInvalid(true)
            ()
          }
          val holder = makeHolder(state)
          val res = lpAutocodeUpdateManager.update(holder, source)
          res.isDefined shouldBe false
        }

        "no autocode state" in {
          val holder = makeHolder(getNewStateUpdate)
          val res = lpAutocodeUpdateManager.update(holder, source)
          res.isDefined shouldBe true
          res.get.state.getAutocodeState
            .findReport(AutocodeReportType.Identifiers.id)
            .exists(_.getShouldProcess) shouldBe true
          res.get.delay.isFinite shouldBe true
        }

        "no autocode report" in {
          val holder = makeHolder(getNewStateUpdate.withBuilderUpdate { st =>
            st.getAutocodeStateBuilder
            ()
          })
          val res = lpAutocodeUpdateManager.update(holder, source)
          res.isDefined shouldBe true
          res.get.state.getAutocodeState
            .findReport(AutocodeReportType.Identifiers.id)
            .exists(_.getShouldProcess) shouldBe true
          res.get.delay.isFinite shouldBe true
        }
      }
    }
  }
}
