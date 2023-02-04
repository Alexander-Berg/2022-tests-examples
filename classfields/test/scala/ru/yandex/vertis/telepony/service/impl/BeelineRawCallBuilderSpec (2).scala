package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import ru.yandex.vertis.telepony.Specbase
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.BeelineCallEventModelGenerators._
import ru.yandex.vertis.telepony.model.RawCall.Origins
import ru.yandex.vertis.telepony.model.beeline.{CallStates, CallStatusEvent}
import ru.yandex.vertis.telepony.model.{CallResults, Operators, RawCall, RouteResults}

import scala.concurrent.duration._

class BeelineRawCallBuilderSpec extends Specbase {

  private val builder: BeelineRawCallBuilder = new BeelineRawCallBuilder

  "BeelineRawCallBuilderImpl" should {

    "build empty when no events" in {
      builder.build(Seq()) should ===(None)
    }

    "build empty when only routed event (Passed)" in {
      forAll(RoutedEventGen.map(e => e.copy(routeResult = RouteResults.Passed))) { e =>
        builder.build(Seq(e)) should ===(None)
      }
    }

    "build call when only routed event (Blocked)" in {
      forAll(RoutedEventGen.map(e => e.copy(routeResult = RouteResults.Blocked))) { e =>
        val rawCall = builder.build(Seq(e))
        rawCall shouldBe defined
        inside(rawCall) {
          case Some(c) =>
            c.callResult should ===(CallResults.Blocked)
            c.talkDuration should ===(0.seconds)
            c.duration should ===(0.seconds)
            c.recUrl should not be defined
        }
      }
    }

    "build call when routed and terminal events (Blocked)" in {
      forAll(RoutedEventGen.map(e => e.copy(routeResult = RouteResults.Blocked))) { routed =>
        forAll(statusEventGen(Some(true))) { terminal =>
          {
            val rawCall = builder.build(Seq(routed, terminal))
            rawCall shouldBe defined
            inside(rawCall) {
              case Some(c) =>
                c.callResult should ===(CallResults.Blocked)
                c.talkDuration should ===(0.seconds)
                c.duration should ===(math.abs(terminal.time.getMillis - routed.time.getMillis).millis)
                c.recUrl should not be defined
            }
          }
        }
      }
    }

    "build call when only routed event (NoRedirect)" in {
      forAll(RoutedEventGen.map(e => e.copy(routeResult = RouteResults.NoRedirect))) { e =>
        val rawCall = builder.build(Seq(e))
        rawCall shouldBe defined
        inside(rawCall) {
          case Some(c) =>
            c.callResult should ===(CallResults.NoRedirect)
            c.talkDuration should ===(0.seconds)
            c.duration should ===(0.seconds)
            c.recUrl should not be defined
        }
      }
    }

    "build call when routed and terminal events (NoRedirect)" in {
      forAll(RoutedEventGen.map(e => e.copy(routeResult = RouteResults.NoRedirect))) { routed =>
        forAll(statusEventGen(Some(true))) { terminal =>
          val rawCall = builder.build(Seq(routed, terminal))
          rawCall shouldBe defined
          inside(rawCall) {
            case Some(c) =>
              c.callResult should ===(CallResults.NoRedirect)
              c.talkDuration should ===(0.seconds)
              c.duration should ===(math.abs(terminal.time.getMillis - routed.time.getMillis).millis)
              c.recUrl should not be defined
          }
        }
      }
    }

    "build empty when no routed event" in {
      forAll(Gen.listOf(statusEventGen(None))) { events =>
        builder.build(events) should ===(None)
      }
    }

    "build empty when no terminal" in {
      forAll(statusEventGen(Some(false))) { status =>
        val passedRouted = RoutedEventGen.next.copy(routeResult = RouteResults.Passed)
        builder.build(Seq(passedRouted, status)) should ===(None)
      }
    }

    "build call when terminal exists" in {
      forAll(RoutedEventGen, statusEventGen(Some(true))) { (r, s) =>
        builder.build(Seq(r, s)) shouldBe defined
      }
    }

    "build success" in {
      val routed = RoutedEventGen.next.copy(
        routeResult = RouteResults.Passed,
        recording = true
      )
      val callStatusTerminal = CallStatusEvent(
        externalId = routed.externalId,
        callerState = CallStates.Success,
        calleeState = CallStates.Success,
        reason = 0,
        talkDuration = 30.seconds,
        time = routed.time.plusSeconds(45)
      )
      val callOpt = builder.build(Seq(routed, callStatusTerminal))
      callOpt shouldBe defined
      val expectedDuration = (callStatusTerminal.time.getMillis - routed.time.getMillis).millis
      callOpt.get should ===(
        RawCall(
          externalId = routed.externalId,
          source = routed.callerNumber,
          proxy = routed.operatorNumber,
          target = Some(routed.targetNumber),
          startTime = routed.time,
          duration = expectedDuration,
          talkDuration = callStatusTerminal.talkDuration,
          recUrl = Some(routed.recordUrl.toString),
          callResult = CallResults.Success,
          origin = Origins.Online,
          operator = Operators.Beeline
        )
      )
    }

  }

}
