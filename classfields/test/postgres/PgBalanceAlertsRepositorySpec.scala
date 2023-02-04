package auto.dealers.balance_alerts.storage.postgres.test

import auto.dealers.balance_alerts.storage._
import auto.dealers.balance_alerts.storage.postgres._
import auto.dealers.balance_alerts.storage.testkit.Schema
import common.zio.doobie.testkit.TestPostgresql
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test._
import zio._
import java.time.OffsetDateTime
import auto.dealers.balance_alerts.model.BalanceEventType

object PgBalanceAlertsRepositorySpec extends DefaultRunnableSpec {

  val timestamp = OffsetDateTime.parse("2007-09-03T10:11:30+00:00")

  def spec: ZSpec[Environment, Failure] = {
    val suit = suite("PgBalanceAlertsRepositorySpec")(
      additionTest,
      upsertDiffTest,
      upsertSameTest,
      updateAfterSendTest,
      upsertResetTest,
      updateNonExistentTest
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential

    val env = TestPostgresql.managedTransactor >+> PgBalanceAlertsRepository.live

    suit.provideCustomLayerShared(env)
  }

  lazy val additionTest = testM("Add few alerts") {
    for {
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceAlertsRepository.upsertAlert(2, BalanceEventType.ThreeDaysLeft, timestamp)
      list <- BalanceAlertsRepository.getAlerts
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.ThreeDaysLeft, timestamp),
            (2, BalanceEventType.ThreeDaysLeft, timestamp)
          )
        )
    }
  }

  lazy val upsertDiffTest = testM("Upsertion updates previous entry") {
    for {
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(1))
      list <- BalanceAlertsRepository.getAlerts
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(1))
          )
        )
    }
  }

  lazy val upsertSameTest = testM("Upsertion of the same event type does not affect the entry") {
    for {
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp.plusHours(1))
      list <- BalanceAlertsRepository.getAlerts
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.ThreeDaysLeft, timestamp)
          )
        )
    }
  }

  lazy val updateAfterSendTest = testM("Update affects correct entry") {
    for {
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceAlertsRepository.upsertAlert(2, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceAlertsRepository.update(2, timestamp.plusHours(1))
      list <- BalanceAlertsRepository.getAlerts
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp, ev.lastNotified, ev.notificationsCount))
        .sameElements(
          Seq(
            (1, BalanceEventType.ThreeDaysLeft, timestamp, None, 0),
            (2, BalanceEventType.ThreeDaysLeft, timestamp, Some(timestamp.plusHours(1)), 1)
          )
        )
    }
  }

  lazy val upsertResetTest = testM("Upsert resets the entry") {
    for {
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceAlertsRepository.update(1, timestamp.plusHours(1))
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(2))
      list <- BalanceAlertsRepository.getAlerts
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp, ev.lastNotified, ev.notificationsCount))
        .sameElements(
          Seq(
            (1, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(2), None, 0)
          )
        )
    }
  }

  lazy val updateNonExistentTest = testM("Update non existent entry causes failure") {
    for {
      _ <- BalanceAlertsRepository.upsertAlert(1, BalanceEventType.ThreeDaysLeft, timestamp)
      res <- BalanceAlertsRepository.update(2, timestamp.plusHours(1)).run
    } yield assert(res)(fails(isSubtype[BalanceAlertsRepository.AlertNotFound](anything)))
  }

}
