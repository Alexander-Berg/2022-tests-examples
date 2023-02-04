package ru.yandex.vertis.billing.event.call

import ru.yandex.vertis.billing.model_core.CampaignCallFact.Statuses
import ru.yandex.vertis.billing.model_core.gens.{CallComplaintGen, Producer}

import scala.concurrent.duration.DurationInt

/**
  * @author ruslansd
  */
class RealtyCommercialCallHoboFilterSpec extends CallHoboFilterSpec {

  override protected def filter: CallHoboFilter =
    new RealtyCommercialCallHoboFilter(callModerationSettings)

  protected def shouldPassWithLongDuration: Boolean = false
  protected def shouldPassWithLongDurationFailedStatus: Boolean = false

  "RealtyCommercialCallHoboFilter" should {
    "not pass with complaint and duration = 0" in {
      next(
        0.seconds,
        Iterable(Statuses.Ok),
        complaint = Some(CallComplaintGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "pass short call after moderation start" in {
      next(
        20.seconds,
        Statuses.values -- Statuses.Repeated
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe true
      }
    }

    "do not pass calls less than 30 seconds" in {
      next(
        40.seconds,
        Statuses.values -- Statuses.Repeated
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "not pass call on disabled campaigns" in {
      next(
        20.seconds,
        Statuses.values -- Statuses.Repeated,
        enabled = Some(false)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

  }
}
