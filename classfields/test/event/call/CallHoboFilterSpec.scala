package ru.yandex.vertis.billing.event.call

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.CampaignCallFact.Statuses
import ru.yandex.vertis.billing.model_core.Resolution.Manually
import ru.yandex.vertis.billing.model_core.callcenter.CallCenterCallId
import ru.yandex.vertis.billing.model_core.gens.{
  BaggageGen,
  CallCenterCallIdGen,
  CallComplaintGen,
  CallWithStatusBaggagePayloadGen,
  Producer
}
import ru.yandex.vertis.billing.model_core.{CallComplaint, CampaignCallFact, Resolution, ResolutionsVector}
import ru.yandex.vertis.billing.settings.CallModifySettings
import ru.yandex.vertis.billing.util.DateTimeUtils

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

/**
  * @author ruslansd
  */
trait CallHoboFilterSpec extends AnyWordSpec with Matchers {

  protected def filter: CallHoboFilter

  protected def shouldPassWithLongDuration: Boolean
  protected def shouldPassWithLongDurationFailedStatus: Boolean

  protected def callModerationSettings: CallModifySettings = CallModifySettings(
    7.days,
    7.days,
    21.days
  )

  private val tooOldTimestamp = {
    DateTimeUtils.now().minusMillis(callModerationSettings.moderationPeriod.plus(1.day).toMillis.toInt)
  }

  "HoboTaskFilter" should {

    "duration >= 60 status fail" in {
      next(65.seconds, Iterable(Statuses.Suspicious)).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe shouldPassWithLongDurationFailedStatus
      }
    }

    "duration < 60 duration > 30 Suspicious" in {
      next(40.seconds, Iterable(Statuses.Suspicious)).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe shouldPassWithLongDuration
      }
    }
    "duration = 0" in {
      next(0.seconds, Statuses.values).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "duration >= 60 and status = OK" in {
      next(65.seconds, Iterable(Statuses.Ok)).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "manual resolution is not null" in {
      next(
        45.seconds,
        Statuses.values,
        ResolutionsVector(Manually(Resolution.Statuses.Pass))
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "repeated" in {
      next(
        45.seconds,
        Iterable(Statuses.RepeatedOnHistory, Statuses.RepeatedOnCurrentDay)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "pass with complaint" in {
      next(
        60.seconds,
        Iterable(Statuses.Ok),
        complaint = Some(CallComplaintGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe true
      }
    }

    "not pass with complaint and repeated" in {
      next(
        60.seconds,
        Iterable(Statuses.RepeatedOnCurrentDay),
        complaint = Some(CallComplaintGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "not pass with complaint and already moderated" in {
      next(
        60.seconds,
        Iterable(Statuses.Ok),
        ResolutionsVector(Manually(Resolution.Statuses.Pass)),
        complaint = Some(CallComplaintGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "not pass too old calls" in {
      next(
        60.seconds,
        Iterable(Statuses.Ok),
        complaint = Some(CallComplaintGen.next),
        timestamp = Some(tooOldTimestamp)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "pass call center call" in {
      next(65.seconds, Iterable(Statuses.Ok)).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "not pass with complaint and already moderated call center call" in {
      next(
        60.seconds,
        Iterable(Statuses.Ok),
        ResolutionsVector(Manually(Resolution.Statuses.Pass)),
        complaint = Some(CallComplaintGen.next),
        callCenterCallId = Some(CallCenterCallIdGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

    "not pass too old call center call" in {
      next(
        60.seconds,
        Iterable(Statuses.Ok),
        complaint = Some(CallComplaintGen.next),
        timestamp = Some(tooOldTimestamp),
        callCenterCallId = Some(CallCenterCallIdGen.next)
      ).foreach { b =>
        filter.isShouldBeModerated(b) shouldBe false
      }
    }

  }

  protected def next(
      duration: FiniteDuration,
      statuses: Iterable[CampaignCallFact.Status],
      resolution: ResolutionsVector = ResolutionsVector(),
      timestamp: Option[DateTime] = None,
      complaint: Option[CallComplaint] = None,
      enabled: Option[Boolean] = None,
      tag: Option[String] = None,
      callCenterCallId: Option[CallCenterCallId] = None,
      revenue: Option[Long] = None,
      waitDuration: FiniteDuration = 20.seconds) = {
    val p = CallWithStatusBaggagePayloadGen.next.copy(
      complaint = complaint,
      callCenterCallId = callCenterCallId,
      revenue = revenue.getOrElse(Random.between(1000, 100000))
    )
    val f = p.fact
      .withDuration(duration)
      .withWaitDuration(waitDuration)
      .withTimestamp(timestamp.getOrElse(p.fact.timestamp))
      .withTag(tag)
    val header = {
      val h = BaggageGen.next.header.copy(epoch = Some(DateTime.now().getMillis))
      h.copy(settings = h.settings.copy(isEnabled = enabled.getOrElse(true)))
    }
    statuses.map { s =>
      BaggageGen.next.copy(payload = p.copy(fact = f, status = s, resolution = resolution), header = header)
    }
  }

}
