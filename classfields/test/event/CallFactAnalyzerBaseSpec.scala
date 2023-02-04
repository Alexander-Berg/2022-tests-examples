package ru.yandex.vertis.billing.event

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignCallDao
import ru.yandex.vertis.billing.event.call.{CallFactAnalyzer, RealtyCallFactAnalyzer}
import ru.yandex.vertis.billing.event.call.CallFactAnalyzer.Context
import ru.yandex.vertis.billing.model_core.BaggagePayload.CallWithResolution
import ru.yandex.vertis.billing.model_core.Resolution.Manually
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.billing.model_core.gens.{
  сallWithResolutionGen,
  BaggageGen,
  CampaignHeaderGen,
  TeleponyCallFactGenCallType,
  TeleponyCallFactGenCallTypes,
  TeleponyCallFactGenParams
}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Random, Success}

/**
  * Base methods for specs on [[CallFactAnalyzer]]
  *
  * @author ruslansd
  */
trait CallFactAnalyzerBaseSpec extends AnyWordSpec with Matchers with MockitoSupport {

  protected def analyzer: CallFactAnalyzer

  def analyze(
      b: Baggage,
      p: BaggagePayload.CallWithResolution,
      workPolicy: Option[Timetable]
    )(implicit context: Context) =
    analyzer.analyze(b.copy(payload = p, context = EventContext(workPolicy)))(context)

  def resolutionOk(call: CallWithResolution) =
    call.copy(resolution = ResolutionsVector(Manually(Resolution.Statuses.Pass)))

  def resolutionFail(call: CallWithResolution) =
    call.copy(resolution = ResolutionsVector(Manually(Resolution.Statuses.Fail)))

  def resolutionDefault(call: CallWithResolution) =
    call.copy(resolution = ResolutionsVector())

  def busyLineCall(call: CallWithResolution) =
    call.copy(fact =
      call.fact
        .withDuration(0.seconds)
        .withWaitDuration(0.seconds)
    )

  def valuableMissedCall(call: CallWithResolution) =
    call.copy(fact =
      call.fact
        .withDuration(0.seconds)
        .withWaitDuration(20.seconds)
    )

  def notValuableMissedCall(call: CallWithResolution) =
    call.copy(fact =
      call.fact
        .withDuration(0.seconds)
        .withWaitDuration(15.seconds)
    )

  def valuableCall(call: CallWithResolution) =
    call.copy(fact = call.fact.withDuration(60.seconds))

  def shortCallWithOldShow(call: CallWithResolution) =
    call.copy(fact = call.fact.withDuration(35.seconds), phoneShowTime = call.fact.timestamp.minusHours(4))

  def valuableShortCall(call: CallWithResolution) =
    call.copy(fact = call.fact.withDuration(35.seconds), phoneShowTime = call.fact.timestamp.minusHours(2))

  def notValuableCall(call: CallWithResolution) =
    call.copy(fact = call.fact.withDuration(20.seconds))

  def campaignCallDao(facts: Iterable[CampaignCallFact]) = {

    val m = mock[CampaignCallDao]
    stub(m.getValuableIncomings _) { case interval: DateTimeInterval =>
      Future.successful {
        facts
          .filter(f => interval.contains(f.fact.timestamp) && f.revenue > 0)
          .map(IncomingForCampaign.apply)
          .toSet
      }
    }
    m
  }

  def currentDayCallGen(callType: TeleponyCallFactGenCallType): Gen[CallWithResolution] =
    for {
      call <- сallWithResolutionGen(TeleponyCallFactGenParams(callType))
      shift <- Gen.chooseNum(0L, 1.day.toMillis)
      time = DateTime.now().withTimeAtStartOfDay().plus(shift)
      result = call.copy(fact = call.fact.withTimestamp(time), resolution = ResolutionsVector())
    } yield result

  def currentDayCallWithDefaultWorkTimeGen(callType: TeleponyCallFactGenCallType): Gen[CallWithResolution] =
    for {
      call <- currentDayCallGen(callType)
      timetable = defaultWorkTime(call.fact)
      withWorkTime =
        if (timetable.contains(call.fact.timestamp)) {
          call
        } else {
          val time = call.fact.timestamp.withHourOfDay(13).plusMinutes(Random.nextInt(30))
          call.copy(fact = call.fact.withTimestamp(time))
        }
    } yield withWorkTime

  def currentDayCallNotWithDefaultWorkTimeGen(callType: TeleponyCallFactGenCallType): Gen[CallWithResolution] =
    for {
      call <- currentDayCallGen(callType)
      timetable = defaultWorkTime(call.fact)
      withWorkTime =
        if (timetable.contains(call.fact.timestamp)) {
          val time = call.fact.timestamp.withHourOfDay(23).plusMinutes(Random.nextInt(30))
          call.copy(fact = call.fact.withTimestamp(time))
        } else {
          call
        }
    } yield withWorkTime

  val AlwaysWork = CallFactAnalyzerBaseSpec.AlwaysWork
  val AlwaysNotWork = CallFactAnalyzerBaseSpec.AlwaysNotWork

  def defaultWorkTime: Timetable = RealtyCallFactAnalyzer.fallbackTimetable(DateTime.now())
  def defaultWorkTime(call: CallFact): Timetable = RealtyCallFactAnalyzer.fallbackTimetable(call.timestamp)

  def baggageGen(isEnabled: Boolean = true): Gen[Baggage] =
    for {
      header <- CampaignHeaderGen
      baggage <- BaggageGen
    } yield baggage.copy(header = header.copy(settings = header.settings.copy(isEnabled = isEnabled)))

}

object CallFactAnalyzerBaseSpec {
  import MockitoSupport.{?, mock, when}

  val AlwaysWork = {
    val m = mock[Timetable]
    when(m.contains(?)).thenReturn(true)
    when(m.toString).thenReturn("AlwaysWork")
    m
  }

  val AlwaysNotWork = {
    val m = mock[Timetable]
    when(m.contains(?)).thenReturn(false)
    when(m.toString).thenReturn("AlwaysNotWork")
    m
  }
}
