package ru.yandex.vertis.vsquality.techsupport.service.bot

import cats.syntax.either._
import cats.instances.either._
import RecoverAllErrorsScenario._
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author potseluev
  */
class RecoverAllErrorsScenarioSpec extends SpecBase {

  type DummyScenario = Scenario[Either[Unit, *], Unit, Unit, Unit, Unit]

  implicit private val ctx: Unit = ()

  private val alwaysFailingScenario: DummyScenario =
    new DummyScenario {

      override def transit(currentStateId: Unit, command: Unit)(implicit ctx: Unit): Either[Unit, Option[Unit]] =
        ().asLeft

      override def getState(stateId: Unit)(implicit ctx: Unit): Either[Unit, Option[Unit]] =
        ().asLeft

      override def startFrom: Unit = ()
    }

  "RecoveredScenario.transit" should {
    "return None instead of error" in {
      assume(alwaysFailingScenario.transit((), ()).isLeft)
      alwaysFailingScenario.recoverAllErrors.transit((), ()) shouldBe Right(None)
    }
  }

  "RecoveredScenario.getState" should {
    "return None instead of error" in {
      assume(alwaysFailingScenario.getState(()).isLeft)
      alwaysFailingScenario.recoverAllErrors.getState(()) shouldBe Right(None)
    }
  }
}
