package postgres

import auto.dealers.amoyak.model.triggers.{TriggerEvent, TriggerEventForClient, TriggerEventType}
import auto.dealers.amoyak.storage.dao.TriggerEventsDao.TriggerEventsFilter
import auto.dealers.amoyak.storage.postgres.PgTriggerEventsDao
import auto.dealers.amoyak.storage.postgres.TriggerToPgEnumMeta._
import cats.data.NonEmptySeq
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits._
import doobie.implicits.legacy.instant.JavaTimeInstantMeta
import doobie.Transactor
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.Assertion.{hasSameElements, isEmpty}
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

import java.time.Instant
import java.time.temporal.ChronoUnit

object PgTriggerEventsDaoSpec extends DefaultRunnableSpec {

  private val currentDateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS) // truncated for tests
  private val previousDateTime = currentDateTime.minus(2, ChronoUnit.DAYS)
  private val dateTimeToRemoveEvents = currentDateTime.minus(1, ChronoUnit.DAYS)

  private val clientToBalanceUpdate = 1L
  private val balanceEventTypeToUpdate = TriggerEventType.BalanceSevenDaysLeft

  private val eventsToCreate = Seq(
    TriggerEvent(
      eventType = balanceEventTypeToUpdate,
      clientId = clientToBalanceUpdate,
      updatedAt = previousDateTime,
      title = "Баланс закончится через 7 дней",
      note = None
    ),
    TriggerEvent(
      eventType = TriggerEventType.VasVolumeDecreased,
      clientId = 2L,
      updatedAt = previousDateTime,
      title = "Упал объем услуг",
      note = Some("В сравнении с прошлой неделей")
    ),
    TriggerEvent(
      eventType = TriggerEventType.AccountBlocked,
      clientId = clientToBalanceUpdate,
      updatedAt = previousDateTime,
      title = "Заблокирован аккаунт",
      note = None
    )
  )

  private val eventsToCreateOrUpdate = Seq(
    TriggerEvent(
      eventType = balanceEventTypeToUpdate,
      clientId = clientToBalanceUpdate,
      updatedAt = currentDateTime,
      title = "Другое сообщение про баланс закончится через 7 дней",
      note = None
    ),
    TriggerEvent(
      eventType = TriggerEventType.OffersCountDecreased,
      clientId = 3L,
      updatedAt = currentDateTime,
      title = "Упало количество объявлений",
      note = Some("В сравнении с прошлой неделей, на N процентов")
    )
  )

  private val resultEventsAfterSecondUpsert = eventsToCreate.filterNot { event =>
    event.clientId == clientToBalanceUpdate && event.eventType == balanceEventTypeToUpdate
  } ++ eventsToCreateOrUpdate

  private def getEventsFromTable(xa: Transactor[Task]): Task[Seq[TriggerEvent]] =
    sql"select event_type, client_id, updated_at, title, note, sent_to_amo from trigger_events"
      .query[TriggerEvent]
      .to[Seq]
      .transact(xa)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTriggerEventsDao")(
      testM("upsert should insert new rows")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          eventsBeforeInsert <- getEventsFromTable(xa)
          _ <- new PgTriggerEventsDao(xa).upsert(eventsToCreate)
          resultAfterUpsert <- getEventsFromTable(xa)
        } yield assert(eventsBeforeInsert)(isEmpty) && assert(resultAfterUpsert)(hasSameElements(eventsToCreate))
      ),
      testM("upsert should update all fields for existing events")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          eventsBeforeAnyInsert <- getEventsFromTable(xa)
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreate)
          resultAfterFirstUpsert <- getEventsFromTable(xa)
          _ <- client.upsert(eventsToCreateOrUpdate)
          resultAfterSecondUpsert <- getEventsFromTable(xa)
        } yield assert(eventsBeforeAnyInsert)(isEmpty) &&
          assert(resultAfterFirstUpsert)(hasSameElements(eventsToCreate)) &&
          assert(resultAfterSecondUpsert)(hasSameElements(resultEventsAfterSecondUpsert))
      ),
      testM("find should return events filtered by type and client ids")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreateOrUpdate)
          allEvents <- getEventsFromTable(xa)
          filteredEvents <- client.find(
            TriggerEventsFilter(
              eventType = NonEmptySeq.of(TriggerEventType.OffersCountDecreased),
              clientIds = Seq(clientToBalanceUpdate)
            )
          )
          filteringResult = eventsToCreateOrUpdate.filter { event =>
            event.eventType == TriggerEventType.OffersCountDecreased && event.clientId == clientToBalanceUpdate
          }
        } yield assert(allEvents)(hasSameElements(eventsToCreateOrUpdate)) &&
          assert(filteredEvents)(hasSameElements(filteringResult))
      ),
      testM("find should return events filtered by event type only if there is empty client ids param")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreateOrUpdate)
          allEvents <- getEventsFromTable(xa)
          eventTypesToFind = Seq(TriggerEventType.VasVolumeDecreased, TriggerEventType.OffersCountDecreased)
          filteredEvents <- client.find(
            TriggerEventsFilter(eventType = NonEmptySeq.fromSeqUnsafe(eventTypesToFind), clientIds = Nil)
          )
          filteringResult = eventsToCreateOrUpdate.filter(event => eventTypesToFind.contains(event.eventType))
        } yield assert(allEvents)(hasSameElements(eventsToCreateOrUpdate)) &&
          assert(filteredEvents)(hasSameElements(filteringResult))
      ),
      testM("find should not return sent to amo events")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreateOrUpdate)
          _ <- client.markExisted(NonEmptySeq.fromSeqUnsafe(eventsToCreateOrUpdate.map(_.toEventForClient)))
          allEvents <- getEventsFromTable(xa)
          eventTypesToFind = Seq(TriggerEventType.VasVolumeDecreased, TriggerEventType.OffersCountDecreased)
          filteredEvents <- client.find(
            TriggerEventsFilter(eventType = NonEmptySeq.fromSeqUnsafe(eventTypesToFind), clientIds = Nil)
          )
        } yield assert(allEvents)(hasSameElements(eventsToCreateOrUpdate.map(_.copy(sentToAmo = true)))) &&
          assert(filteredEvents)(isEmpty)
      ),
      testM("mark existed should mark events by client and event type params")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreateOrUpdate)
          allEvents <- getEventsFromTable(xa)
          _ <- client.markExisted(
            NonEmptySeq.of(TriggerEventForClient(balanceEventTypeToUpdate, clientToBalanceUpdate))
          )
          allEventsWithMarked <- getEventsFromTable(xa)
          resultAfterMarkingExisted = eventsToCreateOrUpdate.collect {
            case event @ TriggerEvent(`balanceEventTypeToUpdate`, `clientToBalanceUpdate`, _, _, _, _) =>
              event.copy(sentToAmo = true)
            case other => other
          }
        } yield assert(allEvents)(hasSameElements(eventsToCreateOrUpdate)) &&
          assert(allEventsWithMarked)(hasSameElements(resultAfterMarkingExisted))
      ),
      testM("remove old events with keeping not sent should remove events and leave not sent to amo ones")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreate)
          _ <- client.upsert(eventsToCreateOrUpdate)
          _ <- client.markExisted(
            NonEmptySeq.of(TriggerEventForClient(TriggerEventType.AccountBlocked, clientToBalanceUpdate))
          )
          _ <- client.removeOldEvents(dateTimeToRemoveEvents, keepNotSentToAmo = true)
          resultEvents <- getEventsFromTable(xa)
          filteredByDateTimeAndExistenceEvents = resultEventsAfterSecondUpsert.filterNot { event =>
            event.updatedAt.isBefore(dateTimeToRemoveEvents) &&
            event.eventType == TriggerEventType.AccountBlocked &&
            event.clientId == clientToBalanceUpdate
          }
        } yield assert(resultEvents)(hasSameElements(filteredByDateTimeAndExistenceEvents))
      ),
      testM("remove old events without keeping not sent should remove all events older than method param")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerEventsDao(xa)
          _ <- client.upsert(eventsToCreate)
          _ <- client.upsert(eventsToCreateOrUpdate)
          _ <- client.removeOldEvents(dateTimeToRemoveEvents, keepNotSentToAmo = false)
          resultEvents <- getEventsFromTable(xa)
          filteredByDateTimeEvents = resultEventsAfterSecondUpsert.filter(_.updatedAt.isAfter(dateTimeToRemoveEvents))
        } yield assert(resultEvents)(hasSameElements(filteredByDateTimeEvents))
      )
    ) @@
      beforeAll(ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie) @@
      after(ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM trigger_events".update.run.transact(xa))) @@
      sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
