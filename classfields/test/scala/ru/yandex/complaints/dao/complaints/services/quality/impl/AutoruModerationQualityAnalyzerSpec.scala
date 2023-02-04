package ru.yandex.complaints.dao.complaints.services.quality.impl

import java.sql.Timestamp

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import ru.yandex.complaints.dao.complaints.Generators._
import ru.yandex.complaints.model.OfferEvent
import ru.yandex.complaints.services.events.OfferEventsService
import ru.yandex.complaints.services.quality.ModerationQualityAnalyzer.Verdict.{Ok, Problem}
import ru.yandex.complaints.services.quality.impl.ModerationQualityAnalyzerImpl
import ru.yandex.complaints.util.SignalRank
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._
import scala.util.Success

/**
  * Spec for [[ModerationQualityAnalyzerImpl]]
  *
  * @author frenki
  */
abstract class AutoruModerationQualityAnalyzerSpec
  extends WordSpec
    with BeforeAndAfter
    with MockitoSupport
    with Matchers {

  private val mockOfferEventService = mock[OfferEventsService]

  private def qa: ModerationQualityAnalyzerImpl = new ModerationQualityAnalyzerImpl(
    mockOfferEventService, options)

  protected def options: ModerationQualityAnalyzerImpl.Options

  private val offer = OfferGen.next
  private val event = OfferEvent.Warn

  before {
    when(mockOfferEventService.getLast(offer.id, event)).thenReturn(Success(None))
  }

  "AutoruModerationQualityAnalyzer" should {

    "return Problem when > 1 'complaintType' complaints in last epoch" in {
      val complaintTime = new Timestamp(new DateTime().minus(1.hours.toMillis).getMillis)
      val complaints = ComplaintsGen.map {
        _.copy(created = complaintTime, ctype = options.complaintType)
      }.next(2)
      qa(offer, complaints) match {
        case Success(Problem(SignalRank.Check, _)) => ()
        case other => fail(s"Unexpected $other")
      }
    }

    "return Ok when complaints have other type" in {
      val complaintTime = new Timestamp(new DateTime().minus(1.hours.toMillis).getMillis)
      val complaints = ComplaintsGen.filter(_.ctype != options.complaintType).map(_.copy(created = complaintTime)).next(2)
      qa(offer, complaints) shouldBe Success(Ok)
    }

    "return Ok when complaints are too old" in {
      val complaintTime = new Timestamp(0)
      val complaints = ComplaintsGen.map {
        _.copy(created = complaintTime, ctype = options.complaintType)
      }.next(2)
      qa(offer, complaints) shouldBe Success(Ok)
    }

    "return Ok when <= 1 related complaints" in {
      val complaintTime = new Timestamp(new DateTime().minus(1.hours.toMillis).getMillis)
      val complaint = ComplaintsGen.map {
        _.copy(created = complaintTime, ctype = options.complaintType)
      }.next
      qa(offer, Seq(complaint)) shouldBe Success(Ok)
    }

    "ignore complaints during 1 hour after last warn" in {
      val lastWarnTime = new DateTime().minus(3.hours.toMillis)
      val complaintTime = new Timestamp(lastWarnTime.plus(30.minutes.toMillis).getMillis)
      when(mockOfferEventService.getLast(offer.id, event)).thenReturn(Success(Some(lastWarnTime)))
      val complaints = ComplaintsGen.map {
        _.copy(created = complaintTime, ctype = options.complaintType)
      }.next(20)
      qa(offer, complaints) shouldBe Success(Ok)
    }

    "return Problem if >= 1 complaint in last epoch" in {
      val lastWarnTime = new DateTime().minus(3.hours.toMillis)
      val complaintTime = new Timestamp(lastWarnTime.plus(90.minutes.toMillis).getMillis)
      when(mockOfferEventService.getLast(offer.id, event)).thenReturn(Success(Some(lastWarnTime)))
      val complaints = ComplaintsGen.map {
        _.copy(created = complaintTime, ctype = options.complaintType)
      }.next(20)
      qa(offer, complaints) match {
        case Success(Problem(SignalRank.Check, _)) => ()
        case other => fail(s"Unexpected $other")
      }
    }
  }
}