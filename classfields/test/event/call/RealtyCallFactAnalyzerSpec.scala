package ru.yandex.vertis.billing.event.call

import org.joda.time.DateTimeConstants
import ru.yandex.vertis.billing.event.CallFactAnalyzerBaseSpec
import ru.yandex.vertis.billing.event.call.CallFactAnalyzer.{Context, Verdict}
import ru.yandex.vertis.billing.model_core.BaggagePayload.{EventSource, EventSources}
import ru.yandex.vertis.billing.model_core.CampaignCallFact.{DetailedStatuses, Statuses}
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{
  BaggageGen,
  PhoneGen,
  Producer,
  TeleponyCallFactGenCallType,
  TeleponyCallFactGenCallTypes
}
import ru.yandex.vertis.billing.settings.RealtyComponents
import ru.yandex.vertis.billing.util.DateTimeUtils
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.concurrent.duration.DurationInt
import scala.util.{Success, Try}

/**
  * @author ruslansd
  */
class RealtyCallFactAnalyzerSpec extends CallFactAnalyzerBaseSpec {

  private val whiteList = PhoneGen.next(2).map(_.value).toSet

  val analyzer = new RealtyCallFactAnalyzer(RealtyComponents, whiteList)

  private def withStatus(s: CallResult)(p: BaggagePayload.CallWithResolution) = {
    val f = p.fact.asInstanceOf[TeleponyCallFact].copy(result = s)
    p.copy(fact = f)
  }

  private def withSource(es: EventSource)(p: BaggagePayload.CallWithResolution) =
    p.copy(source = es)

  override def analyze(
      b: Baggage,
      p: BaggagePayload.CallWithResolution,
      workPolicy: Option[Timetable]
    )(implicit context: Context): Try[Verdict] = {
    val (nb, np) =
      if (p.source == EventSources.PhoneShows) {
        (b, p)
      } else {
        workPolicy match {
          case Some(policy) if policy.contains(p.fact.timestamp) =>
            val h = b.header.copy(settings = b.header.settings.copy(isEnabled = true))
            val np = p.copy(phoneShowTime = p.fact.timestamp.minusDays(1))
            (b.copy(header = h, payload = np), np)
          case _ =>
            val h = b.header.copy(settings = b.header.settings.copy(isEnabled = false))
            val np = p.copy(phoneShowTime = p.fact.timestamp.withTimeAtStartOfDay())
            (b.copy(header = h, payload = np), np)
        }
      }
    super.analyze(nb, np, workPolicy)
  }

  "CallFactAnalyzer" should {

    "correctly work with Unknown status" in {
      implicit val context = Context()
      val baggage = BaggageGen.next

      answeredCallOk(TeleponyCallFactGenCallTypes.Any).foreach { case (call, policy) =>
        val p = withSource(EventSources.PhoneShows)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Ok
        if (CallFactAnalyzer.getCompositeResolution(call.resolution).contains(Resolution.Statuses.Pass)) {
          verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
        } else {
          verdict.detailed shouldBe DetailedStatuses.Ok
        }
      }

      longCallOk(TeleponyCallFactGenCallTypes.Any).foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Ok
        if (CallFactAnalyzer.getCompositeResolution(call.resolution).contains(Resolution.Statuses.Pass)) {
          verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
        } else {
          verdict.detailed shouldBe DetailedStatuses.Ok
        }
      }

      EventSources.values.foreach { es =>
        allSuspicious(TeleponyCallFactGenCallTypes.Any).foreach { case (call, policy) =>
          val p = withSource(es)(call)
          analyze(baggage, withStatus(CallResults.Unknown)(p), policy) should matchPattern {
            case Success(Verdict(Statuses.Suspicious, 0L, _)) =>
          }
        }
      }
    }

    "correctly handle campaign history calls" in {
      implicit val context = Context()
      val baggage = BaggageGen.next
      val shortCallOk = longCallOk(TeleponyCallFactGenCallTypes.Any).map { case (call, policy) =>
        (call.copy(fact = call.fact.withDuration(40.seconds)), policy)
      }
      shortCallOk.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Ok
        if (CallFactAnalyzer.getCompositeResolution(call.resolution).contains(Resolution.Statuses.Pass)) {
          verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
        } else {
          verdict.detailed shouldBe DetailedStatuses.Ok
        }
      }
    }

    "correctly work with non Billing Statuses " in {
      implicit val context = Context()
      val baggage = BaggageGen.next
      RealtyCallFactAnalyzer.NonBillingStatus.foreach { ts =>
        EventSources.values.foreach { es =>
          all(TeleponyCallFactGenCallTypes.Any).foreach { case (call, policy) =>
            val p = withSource(es)(call)
            analyze(baggage, withStatus(ts)(p), policy) should matchPattern {
              case Success(Verdict(Statuses.Suspicious, 0L, _)) =>
            }
          }
        }
      }
    }

    "correctly work with BusyLine statuses" in {
      implicit val context = Context()
      val baggage = BaggageGen.next
      Seq(CallResults.BusyCallee).foreach { ts =>
        EventSources.values.foreach { es =>
          workTime(defaultOk(TeleponyCallFactGenCallTypes.Any)).foreach { case (call, policy) =>
            val p = withSource(es)(call)
            analyze(baggage, withStatus(ts)(p), policy) should matchPattern {
              case Success(Verdict(Statuses.Ok, _, DetailedStatuses.Ok)) =>
            }
          }

          workTime(passedOk(TeleponyCallFactGenCallTypes.Any)).foreach { case (call, policy) =>
            val p = withSource(es)(call)
            analyze(baggage, withStatus(ts)(p), policy) should matchPattern {
              case Success(Verdict(Statuses.Ok, _, DetailedStatuses.ManuallySucceeded)) =>
            }
          }

          notWorkTime(defaultOk(TeleponyCallFactGenCallTypes.Any) ++ allSuspicious(TeleponyCallFactGenCallTypes.Any))
            .foreach { case (call, policy) =>
              val p = withSource(es)(call)
              analyze(baggage, withStatus(ts)(p), policy) should matchPattern {
                case Success(Verdict(Statuses.Suspicious, 0L, _)) =>
              }
            }
        }
      }
    }

    "correctly work with MaybeBilling statuses" in {
      implicit val context = Context()
      val baggage = BaggageGen.next

      RealtyCallFactAnalyzer.MaybeBillingStatus.foreach { ts =>
        EventSources.values.foreach { es =>
          nonAnswered(allOk(TeleponyCallFactGenCallTypes.Any)).foreach { case (call, policy) =>
            val p = withSource(es)(call.copy(fact = call.fact.withWaitDuration(3.seconds)))
            val verdict = analyze(baggage, withStatus(ts)(p), policy).get
            verdict shouldBe Verdict(Statuses.Suspicious, 0L, DetailedStatuses.NonBilledTeleponyStatus)
          }

          nonAnswered(defaultOk(TeleponyCallFactGenCallTypes.Any)).foreach { case (call, policy) =>
            val p = withSource(es)(call.copy(fact = call.fact.withWaitDuration(5.seconds)))
            val verdict = analyze(baggage, withStatus(ts)(p), policy).get
            verdict.status shouldBe Statuses.Ok
            verdict.detailed shouldBe DetailedStatuses.Ok
          }

          nonAnswered(passedOk(TeleponyCallFactGenCallTypes.Any)).foreach { case (call, policy) =>
            val p = withSource(es)(call.copy(fact = call.fact.withWaitDuration(5.seconds)))
            val verdict = analyze(baggage, withStatus(ts)(p), policy).get
            verdict.status shouldBe Statuses.Ok
            verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
          }
        }
      }
    }

    "correctly generate fallback timetable" in {
      val weekdayTime = LocalTimeInterval.apply("09:00", "20:00")
      val weekendTime = LocalTimeInterval.apply("10:00", "19:00")
      val timetable = RealtyCallFactAnalyzer.fallbackTimetable(DateTimeUtils.now())
      val week = DateTimeUtils.wholeWeek(DateTimeUtils.now())
      Iterator.iterate(week.from)(_.plusHours(1)).takeWhile(_.isBefore(week.to)).foreach { time =>
        val day = time.getDayOfWeek
        if (day < DateTimeConstants.SATURDAY) {
          val contains =
            !weekdayTime.from.isAfter(time.toLocalTime) && !weekdayTime.to.isBefore(time.toLocalTime)
          timetable.contains(time) shouldBe contains
        } else {
          val contains =
            !weekendTime.from.isAfter(time.toLocalTime) && !weekendTime.to.isBefore(time.toLocalTime)
          timetable.contains(time) shouldBe contains
        }
      }
    }

    "work correctly with RepeatedOnHistory calls" in {
      val incoming = "8121234567"
      val baggage = baggageGen().next.copy(eventTime = now())
      val campaign = baggage.header

      implicit val context = Context(
        Set(IncomingForCampaign(campaign.id, incoming))
      )

      val repeated = answeredCallOk(TeleponyCallFactGenCallTypes.Any).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }

      repeated.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.RepeatedOnHistory
      }

      val suspicious = allSuspicious(TeleponyCallFactGenCallTypes.Any).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }

      suspicious.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Suspicious
        verdict.revenue shouldBe 0L
      }
    }

    "work correctly with RepeatedOnCurrentDay calls" in {
      val incoming = "8121234567"
      val baggage: Baggage = baggageGen().next.copy(eventTime = now())

      val currentDay = {
        val fact = currentDayCallGen(TeleponyCallFactGenCallTypes.Any).next
        fact.copy(fact = fact.fact.withIncoming(incoming), revenue = 10L)
      }
      implicit val context = Context(currentIntervalIncomings =
        collection.mutable.HashSet(IncomingForCampaign(baggage.snapshot, currentDay.fact))
      )

      val repeated = answeredCallOk(TeleponyCallFactGenCallTypes.Any).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }
      repeated.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.RepeatedOnCurrentDay
      }

      val suspicious = allSuspicious(TeleponyCallFactGenCallTypes.Any).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }
      suspicious.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Suspicious
        verdict.revenue shouldBe 0L
      }
    }

    "work correctly whitelist incoming" in {
      val incoming = whiteList.head
      val baggage = baggageGen().next.copy(eventTime = now())
      val campaign = baggage.header

      implicit val context = Context(
        Set(IncomingForCampaign(campaign.id, incoming))
      )

      val repeated = answered(allOk(TeleponyCallFactGenCallTypes.Any)).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }

      repeated.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Ok
        verdict.detailed shouldBe DetailedStatuses.WhiteListed
      }

      val suspicious = allSuspicious(TeleponyCallFactGenCallTypes.Any).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }

      suspicious.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Suspicious
        verdict.revenue shouldBe 0L
      }
    }

    "make suspicious non answered calls from whitelist" in {
      val incoming = whiteList.head
      val baggage = baggageGen().next

      implicit val context = Context()

      val notAnswered = nonAnswered(
        defaultOk(TeleponyCallFactGenCallTypes.Any) ++ defaultShortCallOk(TeleponyCallFactGenCallTypes.Any)
      ).map { case (call, policy) =>
        (call.copy(fact = call.fact.withIncoming(incoming)), policy)
      }

      notAnswered.foreach { case (call, policy) =>
        val p = withSource(EventSources.CampaignHistory)(call)
        val verdict = analyze(baggage, withStatus(CallResults.Unknown)(p), policy).get
        verdict.status shouldBe Statuses.Suspicious
        verdict.detailed shouldBe DetailedStatuses.NoAnswer
        verdict.revenue shouldBe 0L
      }
    }

    "zero duration calls with Unknown status are suspicious and not valuable" in {
      implicit val context = Context()
      val baggage = BaggageGen.next

      val missedCall = resolutionDefault(valuableMissedCall(currentDayCallGen(TeleponyCallFactGenCallTypes.Any).next))
      val busyLine = resolutionDefault(busyLineCall(currentDayCallGen(TeleponyCallFactGenCallTypes.Any).next))

      for (unknownStatus <- Seq(CallResults.Unknown, CallResults.UnknownResult)) {
        val verdictMissedCall = analyze(baggage, withStatus(unknownStatus)(missedCall), Some(AlwaysWork)).get
        val verdictBusyLineCall = analyze(baggage, withStatus(unknownStatus)(busyLine), Some(AlwaysWork)).get

        verdictMissedCall.status shouldBe Statuses.Suspicious
        verdictMissedCall.detailed shouldBe DetailedStatuses.NotValuable
        verdictMissedCall.revenue shouldBe 0L

        verdictBusyLineCall.status shouldBe Statuses.Suspicious
        verdictBusyLineCall.detailed shouldBe DetailedStatuses.NotValuable
        verdictBusyLineCall.revenue shouldBe 0L
      }
    }

    "correctly handle short calls" in {
      implicit val context = Context()
      val baggage = BaggageGen.next

      val shortCall = resolutionDefault(notValuableCall(currentDayCallGen(TeleponyCallFactGenCallTypes.Any).next))
      val verdictShortCall = analyze(baggage, withStatus(CallResults.Unknown)(shortCall), Some(AlwaysWork)).get

      verdictShortCall.status shouldBe Statuses.Suspicious
      verdictShortCall.detailed shouldBe DetailedStatuses.ShortCall
      verdictShortCall.revenue shouldBe 0L
    }

  }

  private def answered(calls: Seq[(BaggagePayload.CallWithResolution, Option[Timetable])]) =
    calls.filter { case (p, _) => p.fact.duration > 0.seconds }

  private def nonAnswered(calls: Seq[(BaggagePayload.CallWithResolution, Option[Timetable])]) =
    calls.filter { case (p, _) => p.fact.duration == 0.seconds }

  private def allOk(callType: TeleponyCallFactGenCallType) =
    answeredCallOk(callType) ++ defaultBusyLineAndMissedCallOk(callType)

  private def longCallOk(callType: TeleponyCallFactGenCallType) = defaultOk(callType) ++ passedOk(callType)

  private def answeredCallOk(callType: TeleponyCallFactGenCallType) =
    longCallOk(callType) ++ defaultShortCallOk(callType)

  private def allSuspicious(callType: TeleponyCallFactGenCallType) =
    defaultSuspicious(callType) ++ failSuspicious(callType)

  private def all(callType: TeleponyCallFactGenCallType) = allOk(callType) ++ allSuspicious(callType)

  private def notWorkTime(it: Iterable[(BaggagePayload.CallWithResolution, Option[Timetable])]) =
    it.filter { case (_, wp) => wp.contains(AlwaysNotWork) }

  private def workTime(it: Iterable[(BaggagePayload.CallWithResolution, Option[Timetable])]) =
    it.filter { case (_, wp) => wp.contains(AlwaysWork) }

  private def defaultSuspicious(callType: TeleponyCallFactGenCallType) = List(
    (resolutionDefault(busyLineCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionDefault(busyLineCall(currentDayCallNotWithDefaultWorkTimeGen(callType).next)), None),
    (resolutionDefault(notValuableCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionDefault(notValuableCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionDefault(notValuableCall(currentDayCallGen(callType).next)), None),
    (resolutionDefault(valuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionDefault(valuableMissedCall(currentDayCallNotWithDefaultWorkTimeGen(callType).next)), None),
    (resolutionDefault(notValuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionDefault(notValuableMissedCall(currentDayCallGen(callType).next)), None),
    (resolutionDefault(notValuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysWork))
  )

  private def defaultShortCallOk(callType: TeleponyCallFactGenCallType) = List(
    (resolutionDefault(valuableShortCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionDefault(valuableShortCall(currentDayCallGen(callType).next)), None),
    (resolutionDefault(shortCallWithOldShow(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionDefault(shortCallWithOldShow(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionDefault(shortCallWithOldShow(currentDayCallGen(callType).next)), None),
    (resolutionDefault(valuableShortCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork))
  )

  private def defaultBusyLineAndMissedCallOk(callType: TeleponyCallFactGenCallType) = List(
    (resolutionDefault(busyLineCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionDefault(valuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysWork))
  )

  private def defaultOk(callType: TeleponyCallFactGenCallType) =
    List(
      (resolutionDefault(valuableCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
      (resolutionDefault(valuableCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
      (resolutionDefault(valuableCall(currentDayCallGen(callType).next)), None),
      (resolutionDefault(valuableCall(currentDayCallGen(callType).next)), None)
    )

  private def passedOk(callType: TeleponyCallFactGenCallType) = List(
    (resolutionOk(busyLineCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(busyLineCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(valuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(valuableMissedCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(notValuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(notValuableMissedCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(notValuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionOk(busyLineCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionOk(valuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionOk(valuableCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionOk(valuableCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(valuableCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(valuableCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(notValuableCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(notValuableCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionOk(notValuableCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(valuableShortCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(valuableShortCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionOk(valuableShortCall(currentDayCallGen(callType).next)), None),
    (resolutionOk(shortCallWithOldShow(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(shortCallWithOldShow(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionOk(shortCallWithOldShow(currentDayCallGen(callType).next)), None)
  )

  private def failSuspicious(callType: TeleponyCallFactGenCallType) = List(
    (resolutionFail(busyLineCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(busyLineCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(valuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(valuableMissedCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(busyLineCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionFail(valuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionFail(valuableCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionFail(valuableCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(valuableCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(valuableCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(notValuableCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(notValuableCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionFail(notValuableCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(valuableShortCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(valuableShortCall(currentDayCallGen(callType).next)), Some(AlwaysWork)),
    (resolutionFail(valuableShortCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(shortCallWithOldShow(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(shortCallWithOldShow(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(shortCallWithOldShow(currentDayCallGen(callType).next)), None),
    (resolutionFail(notValuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysNotWork)),
    (resolutionFail(notValuableMissedCall(currentDayCallGen(callType).next)), None),
    (resolutionFail(notValuableMissedCall(currentDayCallGen(callType).next)), Some(AlwaysWork))
  )

}
