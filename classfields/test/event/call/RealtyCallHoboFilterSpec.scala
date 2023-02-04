package ru.yandex.vertis.billing.event.call

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.model_core.CampaignCallFact.Statuses
import ru.yandex.vertis.billing.model_core.Resolution.Manually
import ru.yandex.vertis.billing.model_core.gens.{CallComplaintGen, Producer}
import ru.yandex.vertis.billing.model_core.{Resolution, ResolutionsVector}

import scala.concurrent.duration.DurationInt

/**
  * @author ruslansd
  */
class RealtyCallHoboFilterSpec extends CallHoboFilterSpec with ScalaCheckPropertyChecks {

  override protected val filter: CallHoboFilter = new RealtyCallHoboFilter(callModerationSettings)

  protected def shouldPassWithLongDuration: Boolean = true
  protected def shouldPassWithLongDurationFailedStatus: Boolean = true

  "RealtyHoboTaskFilter" should {

    "manual resolution is not null" in {
      next(
        60.seconds,
        Statuses.values,
        ResolutionsVector(Manually(Resolution.Statuses.Pass))
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "repeated" in {
      next(
        60.seconds,
        Iterable(Statuses.RepeatedOnHistory, Statuses.RepeatedOnCurrentDay)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "duration < 60 duration > 30 Ok" in {
      next(30.seconds, Iterable(Statuses.Ok)).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "do not sent success calls" in {
      forAll(Gen.choose(0, 30)) { duration =>
        next(duration.seconds, Iterable(Statuses.Ok)).foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
      }
    }

    "sent calls with duration = 0 with complaint" in {
      next(
        0.seconds,
        Iterable(Statuses.Ok),
        complaint = Some(CallComplaintGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe true
      }
    }

    "not sent calls with duration = 0 without complaint" in {
      next(0.seconds, Iterable(Statuses.Ok)).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }
  }
}
