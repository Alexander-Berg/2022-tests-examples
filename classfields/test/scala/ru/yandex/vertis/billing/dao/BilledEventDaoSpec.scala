package ru.yandex.vertis.billing.dao

import org.joda.time.{DateTime, LocalDate}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.BillingEvent.{BilledCallFact, Transaction}
import ru.yandex.vertis.billing.dao.BilledEventDao.{BilledEventInfoWithPayload, ModifiedSinceBatched, WithDate}
import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.Division.Components.Indexing
import ru.yandex.vertis.billing.model_core.Division.Locales
import ru.yandex.vertis.billing.model_core.Division.Projects.AutoRu
import ru.yandex.vertis.billing.model_core.{BilledEventDivision, BilledEventInfo}
import ru.yandex.vertis.billing.util.BilledEventsTestingHelpers
import ru.yandex.vertis.billing.util.CollectionUtils.RichSeq
import ru.yandex.vertis.billing.util.TestingHelpers.RichBilledEventInfoWithPayload
import slick.jdbc.MySQLProfile.api._

import scala.util.{Random, Try}

class BilledEventDaoSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with BilledEventsTestingHelpers {

  private val TotalEventsCount = 600

  private val DivisionWithBilledEventPayloadMap = makeDivisionWithBilledEventPayloadMap(TotalEventsCount)

  private val eventDaoResolver = testingEventDaoResolver(eventStorageDatabase)

  private val billedEventDaoResolver =
    BilledEventDivisionDaoResolver.forSupported(eventStorageDatabase, eventStorageDatabase)

  protected def removeAmountsAndEpoch(event: BilledEventInfoWithPayload): BilledEventInfoWithPayload = {
    val source = event.billedEventInfo
    val changed = source.copy(
      actual = 0,
      expected = 0,
      epoch = None
    )
    event.copy(billedEventInfo = changed)
  }

  private def change(event: BilledEventInfo): BilledEventInfo = {
    Random.nextInt(3) match {
      case 0 =>
        event.copy(actual = event.actual + 1)
      case 1 =>
        event.copy(expected = event.expected + 1)
      case 2 =>
        event.copy(
          actual = event.actual + 1,
          expected = event.expected + 1
        )
      case _ =>
        event.callfact match {
          case Some(callFact) =>
            val changedTag = s"${callFact.getTag}#test"
            val changedCallFact = callFact.toBuilder.setTag(changedTag).build()
            event.copy(callfact = Some(changedCallFact))
          case None =>
            val tr = event.transaction
            val changedComment = s"${tr.getComment}#test"
            val changedTr = tr.toBuilder.setComment(changedComment).build()
            event.copy(transaction = changedTr)
        }
    }
  }

  private def checkEpochChange(
      expected: Iterable[BilledEventInfoWithPayload],
      actual: Iterable[BilledEventInfoWithPayload]): Unit = {
    expected.size shouldBe actual.size
    val expectedMap = expected.groupBy(_.billedEventInfo.id)
    val actualMap = actual.groupBy(_.billedEventInfo.id)
    expectedMap.foreach { case (id, expectedEvents) =>
      expectedEvents.size shouldBe 1
      val expectedEvent = expectedEvents.head
      val actualEvents = actualMap(id)
      val actualEvent = actualEvents.head
      actualEvents.size shouldBe 1
      actualEvent.billedEventInfo.epoch.get should be > expectedEvent.billedEventInfo.epoch.get
      actualEvent.payload shouldBe expectedEvent.payload
    }
  }

  "BilledEventDaoSpec" should {
    "work correctly" in {
      DivisionWithBilledEventPayloadMap.foreach { case (division, info) =>
        val preparedInfo = removePartOfPayloads(info, 4)
        val divisionWithPayload = (division, preparedInfo.flatMap(_.payload))

        val actual = for {
          _ <- fillEventDaos(eventDaoResolver, Set(divisionWithPayload))
          billedEventDao <- billedEventDaoResolver.resolve(division)
          billedEvents = preparedInfo.map(_.billedEventInfo)
          _ <- billedEventDao.write(billedEvents)
          buffer <- billedEventDao.getWithPayload(ModifiedSinceBatched(0L, None, billedEvents.size))
          _ <- Try(require(buffer.toSeq.map(_.billedEventInfo.epoch.get).isOrdered, "Unordered result"))
          _ <- billedEventDao.write(billedEvents)
          notChangedBuffer <- billedEventDao.getWithPayload(ModifiedSinceBatched(0L, None, billedEvents.size))
          _ = checkEpochChange(buffer, notChangedBuffer)
          changedBilledEvents = billedEvents.map(change)
          _ <- billedEventDao.write(changedBilledEvents)
          changedBuffer <- billedEventDao.getWithPayload(ModifiedSinceBatched(0L, None, billedEvents.size))
          _ = checkEpochChange(buffer, changedBuffer)
        } yield buffer.map(_.withoutEpoches)
        val expected = preparedInfo.map(_.withoutEpoches)
        expected.toSeq should contain theSameElementsAs actual.get.toSeq
      }
    }
    "clean up divisions" in {
      billedEventDaoResolver.all.foreach { case (_, dao) =>
        def getAll: Iterable[BilledEventInfo] = {
          val filterAll = ModifiedSinceBatched(0L, batchSize = TotalEventsCount)
          dao.get(filterAll).get
        }

        val all = getAll
        val allDays = all.map(_.timestamp.toLocalDate).toSet.toSeq
        val valuableDays = allDays.take(10)
        valuableDays.foldLeft(Set.empty[LocalDate]) { case (processedDays, day) =>
          dao.delete(WithDate(day)).get

          val days = processedDays + day

          val expected = all.filter(e => !days.contains(e.timestamp.toLocalDate))
          val actual = getAll

          actual should contain theSameElementsAs expected

          days
        }
      }
    }

    "write callfact as string" in {
      val event = billedEventInfo(callFact = Some(BilledCallFact.newBuilder().setTag("test_value").build()))
      for {
        dao <- billedEventDaoResolver.resolve(BilledEventDivision(AutoRu, Locales.Ru, Indexing))
        _ = eventStorageDatabase.runSync(sql"DELETE FROM autoru_ru_offer_indexing_billed_event".asUpdate)
        _ <- dao.write(List(event))
        callfactString = eventStorageDatabase.runSync(
          sql"SELECT callfact_string FROM autoru_ru_offer_indexing_billed_event".as[String].head
        )
      } yield callfactString should (include("tag") and include("test_value"))
    }.get

    "write transaction as string" in {
      val event = billedEventInfo(Transaction.newBuilder().setOrderId(10423).build())
      for {
        dao <- billedEventDaoResolver.resolve(BilledEventDivision(AutoRu, Locales.Ru, Indexing))
        _ = eventStorageDatabase.runSync(sql"DELETE FROM autoru_ru_offer_indexing_billed_event".asUpdate)
        _ <- dao.write(List(event))
        callfactString = eventStorageDatabase.runSync(
          sql"SELECT transaction_string FROM autoru_ru_offer_indexing_billed_event".as[String].head
        )
      } yield callfactString should (include("orderId") and include("10423"))
    }.get
  }

  private def billedEventInfo(
      transaction: Transaction = Transaction.getDefaultInstance,
      callFact: Option[BilledCallFact] = None) =
    BilledEventInfo(
      rawEventId = None,
      rawEventOfferId = None,
      callfactId = Some("test_id"),
      timestamp = new DateTime(100500),
      expected = 100500L,
      actual = 100500L,
      transaction,
      callFact,
      epoch = None
    )
}
