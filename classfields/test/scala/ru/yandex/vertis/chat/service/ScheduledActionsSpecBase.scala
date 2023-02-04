package ru.yandex.vertis.chat.service

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.action.ActionGenerators._
import ru.yandex.vertis.chat.action.{Action, ActionTypes, ScheduledAction}
import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

/**
  * Specs on [[ScheduledActions]].
  *
  * @author dimas
  */
trait ScheduledActionsSpecBase extends SpecBase {

  def scheduledActions: ScheduledActions

  private def withAction(testName: String, testCode: (Action, FiniteDuration, Int) => Any) {
    val toSchedule = action.next
    println(s"$testName toSchedule = $toSchedule")
    val delay = 1.second
    val attempts = Random.nextInt(3) + 1
    try {
      testCode(toSchedule, delay, attempts)
    } finally scheduledActions
      .forget(toSchedule)
      .futureValue
  }

  private def wait(period: FiniteDuration): Unit = {
    //wait a bit longer than need :)
    Thread.sleep(period.toMillis + 100)
  }

  "ScheduledActions" should {
    "provide matured actions of type" in {
      val action = techSupportPollNotification.next
      val delay = 1.second
      val attempts = Random.nextInt(3) + 1
      try {
        val start = System.currentTimeMillis()

        scheduledActions
          .schedule(action, delay, attempts)
          .futureValue

        if (System.currentTimeMillis() - start < delay.toMillis) {
          scheduledActions.getMatured.futureValue should be(empty)
          scheduledActions
            .getMatured(ActionTypes.TechSupportPollNotification)
            .futureValue should be(empty)
          wait(delay)
        }

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
        scheduledActions
          .getMatured(ActionTypes.TechSupportPollNotification)
          .futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
        scheduledActions
          .getMatured(ActionTypes.UserNotification)
          .futureValue should be(empty)
      } finally scheduledActions
        .forget(action)
        .futureValue
    }

    "provide matured actions" in withAction(
      "provide matured action",
      (action, delay, attempts) => {
        val start = System.currentTimeMillis()

        scheduledActions
          .schedule(action, delay, attempts)
          .futureValue

        if (System.currentTimeMillis() - start < delay.toMillis) {
          scheduledActions.getMatured.futureValue should be(empty)
          wait(delay)
        }

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
      }
    )

    "not schedule already scheduled action" in withAction(
      "not schedule already scheduled action",
      (action, delay, attempts) => {

        scheduledActions
          .schedule(action, delay, attempts)
          .futureValue

        scheduledActions
          .schedule(action, delay * 2, attempts)
          .futureValue

        wait(delay)

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
      }
    )

    "reschedule already scheduled action" in withAction(
      "reschedule already scheduled action",
      (action, delay, attempts) => {

        scheduledActions
          .schedule(action, delay, attempts)
          .futureValue

        scheduledActions
          .reschedule(action, delay * 3, attempts)
          .futureValue

        wait(delay)

        scheduledActions.getMatured.futureValue should be(empty)

        wait(delay * 2)

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
      }
    )

    "rescheduleIfExists already scheduled action to execute earlier" in withAction(
      "rescheduleIfExists already scheduled action to execute earlier",
      (action, delay, attempts) => {

        scheduledActions
          .schedule(action, delay, attempts)
          .futureValue

        scheduledActions
          .rescheduleIfExists(action, delay * 3, attempts)
          .futureValue shouldBe true

        wait(delay)

        scheduledActions.getMatured.futureValue should be(empty)

        wait(delay * 2)

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
      }
    )

    "not rescheduleIfExists non existing action" in withAction(
      "not rescheduleIfExists non existing action",
      (action, delay, attempts) => {
        scheduledActions
          .rescheduleIfExists(action, delay, attempts)
          .futureValue shouldBe false

        wait(delay)

        scheduledActions.getMatured.futureValue should be(empty)
      }
    )

    "reschedule non existing action" in withAction(
      "reschedule non existing action",
      (action, delay, attempts) => {
        scheduledActions
          .reschedule(action, delay, attempts)
          .futureValue

        wait(delay)

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
      }
    )

    "reschedule already scheduled action to execute earlier" in withAction(
      "reschedule already scheduled action to execute earlier",
      (action, delay, attempts) => {

        scheduledActions
          .schedule(action, delay * 3, attempts)
          .futureValue

        scheduledActions
          .reschedule(action, delay, attempts)
          .futureValue

        wait(delay)

        scheduledActions.getMatured.futureValue should matchPattern {
          case Seq(ScheduledAction(`action`, _, `attempts`)) =>
        }
      }
    )

    "forget scheduled action" in withAction(
      "forget scheduled action",
      (action, delay, attempts) => {

        scheduledActions
          .schedule(action, delay, attempts)
          .futureValue

        scheduledActions
          .forget(action)
          .futureValue

        wait(delay)

        scheduledActions.getMatured.futureValue should be(empty)

      }
    )
  }

}
