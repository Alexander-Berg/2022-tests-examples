package ru.yandex.vertis.billing.event.call

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.billing.event.CallFactAnalyzerBaseSpec
import ru.yandex.vertis.billing.event.call.AutoruCallFactAnalyzerSpec._
import ru.yandex.vertis.billing.event.call.CallFactAnalyzer.{Context, Verdict}
import ru.yandex.vertis.billing.model_core.CampaignCallFact.{DetailedStatuses, Statuses}
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core.gens.{
  PhoneGen,
  Producer,
  TeleponyCallFactGenCallType,
  TeleponyCallFactGenCallTypes
}
import ru.yandex.vertis.billing.model_core.{
  BaggagePayload,
  CallResult,
  IncomingForCampaign,
  Resolution,
  TeleponyCallFact
}
import ru.yandex.vertis.billing.settings.AutoRuComponents
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.Random

/**
  * Spec on [[AutoruCallFactAnalyzer]]
  *
  * @author ruslansd
  */
class AutoruCallFactAnalyzerSpec extends CallFactAnalyzerBaseSpec with BeforeAndAfterEach {

  implicit private val context = Context()

  private val whiteList = PhoneGen.next(2).map(_.value).toSet

  override protected def beforeEach(): Unit = {
    context.currentIntervalIncomings.clear()
    super.beforeEach()
  }

  override protected def analyzer: CallFactAnalyzer = new AutoruCallFactAnalyzer(AutoRuComponents, whiteList)

  private def allOk(callType: TeleponyCallFactGenCallType) = defaultOk(callType) ++ passedOk(callType)

  private def allSuspicious(callType: TeleponyCallFactGenCallType) =
    defaultSuspicious(callType) ++ failSuspicious(callType)
  private def all(callType: TeleponyCallFactGenCallType) = allOk(callType) ++ allSuspicious(callType)

  "AutoruCallAnalyzer" should {

    "correctly work with Unknown status" in {
      val baggage = baggageGen().next
      BillableStatuses.foreach { s =>
        allOk(TeleponyCallFactGenCallTypes.Any).foreach { call =>
          val verdict = analyze(baggage, withStatus(s)(call), None).get
          verdict.status shouldBe Statuses.Ok
        }
      }
    }

    "correctly work with non Billing Statuses " in {
      val baggage = baggageGen().next
      AutoruCallFactAnalyzer.NonBillingStatus.foreach { ts =>
        all(TeleponyCallFactGenCallTypes.Any).foreach { call =>
          val verdict = analyze(baggage, withStatus(ts)(call), None).get
          verdict shouldBe Verdict(Statuses.Suspicious, 0L, DetailedStatuses.NonBilledTeleponyStatus)
        }
      }
    }

    "correctly work with BusyLine, Unavailable statuses" in {
      val baggage = baggageGen().next
      Seq(CallResults.BusyCallee, CallResults.UnavailableCallee).foreach { ts =>
        defaultOk(TeleponyCallFactGenCallTypes.Any).foreach { call =>
          val nc = withStatus(ts)(call)
          val verdict = analyze(baggage, nc, None).get
          verdict shouldBe Verdict(Statuses.Suspicious, 0L, DetailedStatuses.BusyLine)
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

      val repeated =
        allOk(TeleponyCallFactGenCallTypes.Any).map(call => call.copy(fact = call.fact.withIncoming(incoming)))

      BillableStatuses.foreach { s =>
        repeated.foreach { call =>
          val verdict = analyze(baggage, withStatus(s)(call), None).get
          verdict.status shouldBe Statuses.RepeatedOnHistory
        }

        val suspicious = allSuspicious(TeleponyCallFactGenCallTypes.Any).map { call =>
          call.copy(fact = call.fact.withIncoming(incoming))
        }

        suspicious.foreach { call =>
          val verdict = analyze(baggage, withStatus(s)(call), None).get
          verdict.status shouldBe Statuses.Suspicious
          verdict.revenue shouldBe 0L
        }
      }
    }

    "work correctly whitelist incoming" in {
      val incoming = whiteList.head
      val baggage = baggageGen().next.copy(eventTime = now())
      val campaign = baggage.header

      implicit val context = Context(
        Set(IncomingForCampaign(campaign.id, incoming))
      )

      val repeated =
        allOk(TeleponyCallFactGenCallTypes.Any).map(call => call.copy(fact = call.fact.withIncoming(incoming)))

      BillableStatuses.foreach { s =>
        repeated.foreach { call =>
          val verdict = analyze(baggage, withStatus(s)(call), None).get
          verdict.status shouldBe Statuses.Ok
          verdict.detailed shouldBe DetailedStatuses.WhiteListed
        }

        val suspicious = allSuspicious(TeleponyCallFactGenCallTypes.Any).map { call =>
          call.copy(fact = call.fact.withIncoming(incoming))
        }

        suspicious.foreach { call =>
          val verdict = analyze(baggage, withStatus(s)(call), None).get
          verdict.status shouldBe Statuses.Suspicious
          verdict.revenue shouldBe 0L
        }
      }
    }

    "work correctly with non working stage" in {
      val baggage = baggageGen(false).next
      CallResults.values.foreach { ts =>
        (allSuspicious(TeleponyCallFactGenCallTypes.Any) ++ defaultOk(TeleponyCallFactGenCallTypes.Any))
          .map { call =>
            afterTs(AutoruCallFactAnalyzer.NonWorkingPolicyStart)(call)
          }
          .foreach { call =>
            val nc = withStatus(ts)(call)
            val verdict = analyze(baggage, nc, None).get
            verdict.status shouldBe Statuses.Suspicious
            verdict.revenue shouldBe 0L
          }
      }
    }

    "work correctly with non working stage before policy start" in {
      val baggage = baggageGen(false).next
      BillableStatuses.foreach { s =>
        allOk(TeleponyCallFactGenCallTypes.Redirect)
          .map(beforeTs(AutoruCallFactAnalyzer.NonWorkingPolicyStart))
          .foreach { call =>
            val nc = withStatus(s)(call)
            val verdict = analyze(baggage, nc, None).get
            verdict.status shouldBe Statuses.Ok
            if (CallFactAnalyzer.getCompositeResolution(call.resolution).contains(Resolution.Statuses.Pass)) {
              verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
            } else {
              verdict.detailed shouldBe DetailedStatuses.Ok
            }
          }
      }
    }

    "work correctly with non working stage with resolution ok" in {
      val baggage = baggageGen(false).next
      (CallResults.values -- AutoruCallFactAnalyzer.NonBillingStatus).foreach { ts =>
        passedOk(TeleponyCallFactGenCallTypes.Any).map(afterTs(AutoruCallFactAnalyzer.NonWorkingPolicyStart)).foreach {
          call =>
            val nc = withStatus(ts)(call)
            val verdict = analyze(baggage, nc, None).get
            verdict.status shouldBe Statuses.Ok
            verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
        }
      }
    }

  }

  private def defaultSuspicious(callType: TeleponyCallFactGenCallType) = List(
    resolutionDefault(busyLineCall(currentDayCallGen(callType).next)),
    resolutionDefault(notValuableCall(currentDayCallGen(callType).next)),
    resolutionDefault(notValuableMissedCall(currentDayCallGen(callType).next))
  )

  private def defaultOk(callType: TeleponyCallFactGenCallType) =
    List(resolutionDefault(valuableCall(currentDayCallGen(callType).next)))

  private def passedOk(callType: TeleponyCallFactGenCallType) = List(
    resolutionOk(busyLineCall(currentDayCallGen(callType).next)),
    resolutionOk(valuableMissedCall(currentDayCallGen(callType).next)),
    resolutionOk(notValuableMissedCall(currentDayCallGen(callType).next)),
    resolutionOk(valuableCall(currentDayCallGen(callType).next)),
    resolutionOk(notValuableCall(currentDayCallGen(callType).next)),
    resolutionOk(valuableShortCall(currentDayCallGen(callType).next)),
    resolutionOk(shortCallWithOldShow(currentDayCallGen(callType).next))
  )

  private def failSuspicious(callType: TeleponyCallFactGenCallType) = List(
    resolutionFail(busyLineCall(currentDayCallGen(callType).next)),
    resolutionFail(valuableMissedCall(currentDayCallGen(callType).next)),
    resolutionFail(valuableCall(currentDayCallGen(callType).next)),
    resolutionFail(notValuableCall(currentDayCallGen(callType).next)),
    resolutionFail(valuableShortCall(currentDayCallGen(callType).next)),
    resolutionFail(shortCallWithOldShow(currentDayCallGen(callType).next)),
    resolutionFail(notValuableMissedCall(currentDayCallGen(callType).next))
  )
}

object AutoruCallFactAnalyzerSpec {

  private val BillableStatuses = Set(CallResults.Unknown, CallResults.Success, CallResults.UnknownResult)

  private def withStatus(s: CallResult)(p: BaggagePayload.CallWithResolution) = {
    val f = p.fact.asInstanceOf[TeleponyCallFact].copy(result = s)
    p.copy(fact = f)
  }

  private def afterTs(
      timestamp: DateTime
    )(call: BaggagePayload.CallWithResolution): BaggagePayload.CallWithResolution = {
    if (call.fact.timestamp.isAfter(timestamp)) {
      call
    } else {
      val randomTs = timestamp.plusMinutes(Random.nextInt(60 * 24 * 2))
      call.copy(fact = call.fact.withTimestamp(randomTs))
    }
  }

  private def beforeTs(
      timestamp: DateTime
    )(call: BaggagePayload.CallWithResolution): BaggagePayload.CallWithResolution = {
    if (call.fact.timestamp.isBefore(timestamp)) {
      call
    } else {
      val randomTs = timestamp.minusMinutes(Random.nextInt(60 * 24 * 2))
      call.copy(fact = call.fact.withTimestamp(randomTs))
    }
  }
}
