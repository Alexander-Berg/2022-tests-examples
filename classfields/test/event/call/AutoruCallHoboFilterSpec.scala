package ru.yandex.vertis.billing.event.call

import ru.yandex.vertis.billing.model_core.CampaignCallFact.Statuses
import ru.yandex.vertis.billing.model_core.Resolution.Manually
import ru.yandex.vertis.billing.model_core.gens.CallComplaintGen
import ru.yandex.vertis.billing.model_core.{Resolution, ResolutionsVector}

import scala.concurrent.duration.DurationInt

/**
  * @author ruslansd
  */
class AutoruCallHoboFilterSpec extends CallHoboFilterSpec {

  protected lazy val filter: CallHoboFilter = new AutoruCallHoboFilter(callModerationSettings)

  protected def shouldPassWithLongDuration: Boolean = true
  protected def shouldPassWithLongDurationFailedStatus: Boolean = true

  "AutoruHoboTaskFilter" should {

    "manual resolution is not null" in {
      next(45.seconds, Statuses.values, ResolutionsVector(Manually(Resolution.Statuses.Pass)))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }
    "repeated" in {
      next(45.seconds, Iterable(Statuses.RepeatedOnHistory, Statuses.RepeatedOnCurrentDay))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }

    "duration > 0" in {
      next(1.seconds, Iterable(Statuses.Suspicious))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe true
        }
    }

    "duration == 0" in {
      next(0.seconds, Statuses.values)
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }

    "should moderate" in {
      next(45.seconds, Iterable(Statuses.Suspicious))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe true
        }
    }

    "not moderate calls by disabled campaigns" in {
      next(45.seconds, Statuses.values, enabled = Some(false))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }

    "duration < 60 duration > 30 Ok" in {
      next(40.seconds, Iterable(Statuses.Ok))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe true
        }
    }

    "moderate calls with specific tag" in {
      next(
        90.seconds,
        Iterable(Statuses.Ok),
        tag = Some(s"tag1k=tag1v&source=${AutoruCallHoboFilter.SpecificTag}")
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe true
      }
    }

    "moderate calls only with specific tag" in {
      next(
        90.seconds,
        Iterable(Statuses.Ok),
        tag = Some("notSpecificTag")
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "not moderate already moderated" in {
      next(
        45.seconds,
        Statuses.values,
        ResolutionsVector(Manually(Resolution.Statuses.Pass)),
        tag = Some(s"tag1k=tag1v&source=${AutoruCallHoboFilter.SpecificTag}")
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    // regress test for bug described in https://st.yandex-team.ru/VSBILLING-4989#61900d9fdd9e61046ba084d1
    "moderate recent call if moderation period is quite long" in {
      val filter = new AutoruCallHoboFilter(callModerationSettings.copy(moderationPeriod = 42.days))
      next(1.seconds, Iterable(Statuses.Suspicious))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe true
        }
    }

    "not moderate calls for used cars with default price" in {
      next(45.seconds, Statuses.values, tag = Some("section=USED"), revenue = Some(100))
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }

    "not moderate calls for used cars with default price even if complained" in {
      next(
        45.seconds,
        Statuses.values,
        complaint = CallComplaintGen.sample,
        tag = Some("section=USED"),
        revenue = Some(100)
      )
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }

    "not moderate short calls (wait+duration < 20 seconds)" in {
      next(1.seconds, Iterable(Statuses.Suspicious), waitDuration = 18.seconds)
        .foreach { b =>
          filter.isShouldBeModerated(b) shouldBe false
        }
    }
  }

}
