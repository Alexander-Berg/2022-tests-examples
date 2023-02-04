package ru.yandex.vertis.billing.event.call

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.billing.event.CallFactAnalyzerBaseSpec
import ru.yandex.vertis.billing.event.call.CallFactAnalyzer.{Context, Verdict}
import ru.yandex.vertis.billing.event.call.RealtyCommercialCallFactAnalyzerSpec._
import ru.yandex.vertis.billing.model_core.CampaignCallFact.{DetailedStatuses, Statuses}
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core.gens.{
  BaggageGen,
  CampaignHeaderGen,
  Producer,
  TeleponyCallFactGenCallType,
  TeleponyCallFactGenCallTypes
}
import ru.yandex.vertis.billing.model_core.{
  Baggage,
  BaggagePayload,
  CallResult,
  IncomingForCampaign,
  Resolution,
  TeleponyCallFact
}
import ru.yandex.vertis.billing.settings.RealtyComponents
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

/**
  * Spec on [RealtyCommercialCallFactAnalyzer].
  * Call billing strategy in https://st.yandex-team.ru/VSBILLING-3571
  *
  * @author ruslansd
  */
class RealtyCommercialCallFactAnalyzerSpec extends CallFactAnalyzerBaseSpec with BeforeAndAfterEach {

  implicit private val context = Context()

  override protected def beforeEach(): Unit = {
    context.currentIntervalIncomings.clear()
    super.beforeEach()
  }

  override protected def analyzer: CallFactAnalyzer = new RealtyCommercialCallFactAnalyzer(RealtyComponents)

  private def allOk(callType: TeleponyCallFactGenCallType) = defaultOk(callType) ++ passedOk(callType)

  private def allSuspicious(callType: TeleponyCallFactGenCallType) = failSuspicious(callType)

  private def all(callType: TeleponyCallFactGenCallType) = allOk(callType) ++ allSuspicious(callType)

  "RealtyCommercialCallFactAnalyzer" should {

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
      RealtyCommercialCallFactAnalyzer.NonAppropriateForCallPrice.foreach { ts =>
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
          verdict.status shouldBe Statuses.Ok
          verdict.detailed shouldBe DetailedStatuses.Ok
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

    "work correctly with non working stage" in {
      val baggage = baggageGen(false).next
      BillableStatuses.foreach { ts =>
        defaultOk(TeleponyCallFactGenCallTypes.Any).foreach { call =>
          val nc = withStatus(ts)(call)
          val verdict = analyze(baggage, nc, None).get
          verdict.status shouldBe Statuses.Suspicious
          verdict.detailed shouldBe DetailedStatuses.NotWorkTime
        }

        CallResults.values.foreach { ts =>
          allSuspicious(TeleponyCallFactGenCallTypes.Any).foreach { call =>
            val nc = withStatus(ts)(call)
            val verdict = analyze(baggage, nc, None).get
            verdict.status shouldBe Statuses.Suspicious
          }
        }
      }
    }

    "work correctly with non working stage before policy start" in {
      val baggage = baggageGen(false).next
      BillableStatuses.foreach { s =>
        allOk(TeleponyCallFactGenCallTypes.Any).foreach { call =>
          val nc = withStatus(s)(call)
          val verdict = analyze(baggage, nc, None).get
          if (CallFactAnalyzer.getCompositeResolution(call.resolution).contains(Resolution.Statuses.Pass)) {
            verdict.status shouldBe Statuses.Ok
            verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
          } else {
            verdict.status shouldBe Statuses.Suspicious
            verdict.detailed shouldBe DetailedStatuses.NotWorkTime
          }
        }
      }
    }

    "work correctly with non working stage with resolution ok" in {
      val baggage = baggageGen(false).next
      (CallResults.values -- RealtyCommercialCallFactAnalyzer.NonAppropriateForCallPrice).foreach { ts =>
        passedOk(TeleponyCallFactGenCallTypes.Any).foreach { call =>
          val nc = withStatus(ts)(call)
          val verdict = analyze(baggage, nc, None).get
          verdict.detailed shouldBe DetailedStatuses.ManuallySucceeded
          verdict.status shouldBe Statuses.Ok
        }
      }
    }

    "work correctly after StartREALTY1893 timestamp" in {
      val baggage = baggageGen(false).next
      RealtyCommercialCallFactAnalyzer.NonAppropriateForCallPrice.foreach { ts =>
        failSuspicious(TeleponyCallFactGenCallTypes.Any)
          .foreach { call =>
            val nc = withStatus(ts)(call)
            val verdict = analyze(baggage, nc, None).get
            verdict.status shouldBe Statuses.Suspicious
            verdict.detailed shouldBe DetailedStatuses.NonBilledTeleponyStatus
          }
      }
    }

    "work correctly with noAnswers" in {
      val baggage = baggageGen(false).next
      val tss = CallResults.values --
        RealtyCommercialCallFactAnalyzer.NonAppropriateForCallPrice --
        RealtyCommercialCallFactAnalyzer.UnavailableCallee -
        CallResults.Success

      passedOk(TeleponyCallFactGenCallTypes.Any)
        .foreach { call =>
          tss.foreach { ts =>
            val nc = withDuration(0.seconds)(withWaitDuration(2.seconds)(withStatus(ts)(call)))
            val verdict = analyze(baggage, nc, None).get
            verdict.status shouldBe Statuses.Suspicious
            verdict.detailed shouldBe DetailedStatuses.NotValuable
          }
        }
    }

    "work correctly after StartREALTY1893 timestamp with noAnswers" in {
      val baggage = baggageGen(false).next
      val tss = CallResults.values --
        RealtyCommercialCallFactAnalyzer.NonAppropriateForCallPrice --
        RealtyCommercialCallFactAnalyzer.UnavailableCallee -
        CallResults.Success
      passedOk(TeleponyCallFactGenCallTypes.Any)
        .map(beforeTs(RealtyCommercialCallFactAnalyzer.StartVSBILLING4290LogicTimestamp))
        .foreach { call =>
          tss.foreach { ts =>
            val nc = withWaitDuration(2.seconds)(withStatus(ts)(call))
            val verdict = analyze(baggage, nc, None).get
            if (ts == CallResults.StopCaller) {
              verdict.status shouldBe Statuses.Suspicious
              verdict.detailed shouldBe DetailedStatuses.NotValuable
            } else {
              verdict.status shouldBe Statuses.Ok
            }
          }
        }
    }
  }

  private def defaultOk(callType: TeleponyCallFactGenCallType) =
    List(
      resolutionDefault(valuableCall(currentDayCallGen(callType).next)),
      resolutionDefault(busyLineCall(currentDayCallGen(callType).next)),
      resolutionDefault(notValuableCall(currentDayCallGen(callType).next)),
      resolutionDefault(notValuableMissedCall(currentDayCallGen(callType).next))
    )

  private def passedOk(callType: TeleponyCallFactGenCallType) = List(
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

object RealtyCommercialCallFactAnalyzerSpec {

  private val BillableStatuses = Set(CallResults.Success)

  private def withStatus(s: CallResult)(p: BaggagePayload.CallWithResolution) = {
    val f = p.fact.asInstanceOf[TeleponyCallFact].copy(result = s)
    p.copy(fact = f)
  }

  private def withWaitDuration(d: FiniteDuration)(p: BaggagePayload.CallWithResolution) = {
    val f = p.fact.asInstanceOf[TeleponyCallFact].withWaitDuration(d)
    p.copy(fact = f)
  }

  private def withDuration(d: FiniteDuration)(p: BaggagePayload.CallWithResolution) = {
    val f = p.fact.asInstanceOf[TeleponyCallFact].withDuration(d)
    p.copy(fact = f)
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
