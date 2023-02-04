package auto.dealers.balance_alerts.storage.postgres.test

import auto.dealers.balance_alerts.storage.BalanceEventsRepository
import auto.dealers.balance_alerts.storage.postgres.PgBalanceEventsRepository
import auto.dealers.balance_alerts.model.{BalanceEvent, BalanceEventType}
import auto.dealers.balance_alerts.storage.testkit.Schema
import common.zio.doobie.testkit.TestPostgresql
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test._
import zio._
import zio.interop.catz._
import java.time.OffsetDateTime
import java.util.UUID

object PgBalanceEventsRepositorySpec extends DefaultRunnableSpec {

  val timestamp = OffsetDateTime.parse("2007-09-03T10:11:30+00:00")

  def spec: ZSpec[Environment, Failure] = {
    val suit = suite("PgBalanceEventsRepositorySpec")(
      additionTest,
      deletionTest
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential

    val env = TestPostgresql.managedTransactor >+> PgBalanceEventsRepository.live

    suit.provideCustomLayerShared(env)
  }

  lazy val additionTest = testM("Add few events") {
    for {
      _ <- BalanceEventsRepository.addEvent(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceEventsRepository.addEvent(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceEventsRepository.addEvent(2, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(1))
      list <- BalanceEventsRepository.getEvents
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.ThreeDaysLeft, timestamp),
            (1, BalanceEventType.ThreeDaysLeft, timestamp),
            (2, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(1))
          )
        )
    }
  }

  lazy val deletionTest = testM("Delete event") {
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- BalanceEventsRepository.addEvent(1, BalanceEventType.ThreeDaysLeft, timestamp)
      id <- sql"SELECT id FROM balance_events".query[UUID].unique.transact(xa)
      _ <- BalanceEventsRepository.addEvent(1, BalanceEventType.ThreeDaysLeft, timestamp)
      _ <- BalanceEventsRepository.addEvent(2, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(1))
      _ <- BalanceEventsRepository.deleteEvent(id)
      list <- BalanceEventsRepository.getEvents
    } yield assertTrue {
      list
        .map(ev => (ev.dealerId, ev.balanceEventType, ev.timestamp))
        .sameElements(
          Seq(
            (1, BalanceEventType.ThreeDaysLeft, timestamp),
            (2, BalanceEventType.ZeroDaysLeft, timestamp.plusHours(1))
          )
        )
    }
  }

}
