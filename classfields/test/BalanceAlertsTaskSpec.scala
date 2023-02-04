package auto.dealers.balance_alerts.tasks.test

import auto.common.metrics.src.Metrics
import auto.dealers.balance_alerts.logic.testkit.NotificationServiceMock
import auto.dealers.balance_alerts.model.BalanceEvent
import auto.dealers.balance_alerts.model.testkit.gens.{alertsGen, dealerEventsMapGen, eventGen}
import auto.dealers.balance_alerts.storage.testkit.BalanceAlertsRepositoryMock
import auto.dealers.balance_alerts.storage.testkit.BalanceEventsRepositoryMock
import auto.dealers.balance_alerts.tasks.BalanceAlertsTask
import auto.dealers.balance_alerts.tasks.BalanceAlertsTask.BalanceAlertsTaskConfig
import common.tagged.tag.@@
import common.zio.clock.MoscowClock
import common.zio.logging.Logging
import io.prometheus.client.Gauge
import zio.{Has, NonEmptyChunk, ZIO, ZLayer}
import zio.test.{assertM, checkM, DefaultRunnableSpec, Gen, ZSpec}
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock.MockClock

import java.time.{LocalDate, LocalTime, ZonedDateTime}

object BalanceAlertsTaskSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("BalanceAlertsTask")(
      doNothingAtNight,
      doNothingIfNoEventsAndAlerts,
      processNewEvents
    )

  val doNothingAtNight = testM("do nothing outside working hours") {

    val env =
      Logging.live ++ outsideWorkingHours ++ BalanceAlertsRepositoryMock.empty ++ BalanceEventsRepositoryMock.empty ++ emptyNotificationService

    assertM(zExecuteTask)(isUnit).provideLayer(env)
  }

  val doNothingIfNoEventsAndAlerts = testM("not proceed if there are no events and alerts") {

    val env = Logging.live ++ workingHours ++ noAlertsRepo ++ noEventsRepo ++ emptyNotificationService

    assertM(zExecuteTask)(isUnit).provideLayer(env)
  }

  val processNewEvents = testM("process new events") {

    checkM(dealerEventsMapGen()) { eventsMap =>
      val allEvents = eventsMap.values.flatten.toSeq

      val eventsGet = BalanceEventsRepositoryMock.GetEvents(value(allEvents))
      val eventsDelete =
        eventsMap.values
          .map { dealerEvents =>
            BalanceEventsRepositoryMock.DeleteEvents(
              equalTo(
                NonEmptyChunk
                  .fromIterableOption(dealerEvents.map(_.id))
                  .get
              ),
              unit
            )
          }
          .reduce(_ && _)

      val alertsGet =
        BalanceAlertsRepositoryMock.GetAlerts(value(Seq.empty))
      val alertsUpates =
        eventsMap
          .map { case (_, events) =>
            events.maxBy(_.timestamp)
          }
          .map { lastEvent =>
            BalanceAlertsRepositoryMock.UpsertAlert(
              equalTo((lastEvent.dealerId, lastEvent.balanceEventType, lastEvent.timestamp)),
              unit
            )
          }
          .reduce(_ && _)

      val eventsRepo = eventsGet ++ eventsDelete
      val alertsRepo = alertsUpates ++ alertsGet

      val env = Logging.live ++ workingHours ++ alertsRepo ++ eventsRepo ++ emptyNotificationService

      assertM(zExecuteTask)(isUnit).provideLayer(env)
    }
  }

  val processAlerts = testM("process alerts") {

    checkM(Gen.listOf(alertsGen())) { alerts =>
      val alertsGet =
        BalanceAlertsRepositoryMock.GetAlerts(value(alerts.toSeq))

      val now = getDateTime(defaultDate, startTime)

      val alertsProcessing = alerts
        .map { alert =>
          NotificationServiceMock.Notify(
            equalTo(alert),
            unit
          ) ++
            BalanceAlertsRepositoryMock.Update(
              equalTo((alert.dealerId, now)),
              unit
            )
        }
        .reduce(_ && _)

      val env = Logging.live ++ workingHours ++ noEventsRepo ++ alertsGet ++ alertsProcessing

      assertM(zExecuteTask)(isUnit).provideLayer(env)
    }
  }

  val startTime = LocalTime.of(6, 0)
  val finishTime = LocalTime.of(21, 0)

  val maxAlerts = 3

  val defaultConfig = ZLayer.succeed(
    BalanceAlertsTaskConfig(
      runFrom = startTime,
      runUntil = finishTime,
      runEveryHours = 3
    )
  )

  val taskEnv = defaultConfig ++ Metrics.queueTotal

  val zExecuteTask = BalanceAlertsTask.make.provideLayer(taskEnv).flatMap(_.program)

  val defaultDate = LocalDate.of(2020, 1, 1)

  def getDateTime(date: LocalDate, time: LocalTime) =
    ZonedDateTime
      .of(date, time, MoscowClock.timeZone)
      .toOffsetDateTime

  val workingHours = MockClock.CurrentDateTime(
    value(getDateTime(defaultDate, startTime))
  )

  val outsideWorkingHours = MockClock.CurrentDateTime {
    value(getDateTime(defaultDate, startTime.minusHours(2)))
  }

  val noAlertsRepo = BalanceAlertsRepositoryMock.GetAlerts(value(Seq.empty))
  val noEventsRepo = BalanceEventsRepositoryMock.GetEvents(value(Seq.empty))
  val emptyNotificationService = NotificationServiceMock.empty

}
