package ru.yandex.vertis.billing.events.tasks

import org.joda.time.LocalDate
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.dao.{
  BilledEventDao,
  BilledEventDivisionDaoResolver,
  DivisionDaoResolver,
  EventDivisionDao
}
import ru.yandex.vertis.billing.model_core.gens.{BilledEventInfoGen, PayloadGen, Producer}
import ru.yandex.vertis.billing.util.DateTimeUtils
import ru.yandex.vertis.billing.util.DateTimeUtils.DateTimeWithDuration

import scala.concurrent.duration.DurationInt

/**
  * Spec on [[CleanupDivisionsTask]]
  *
  * @author ruslansd
  */
class CleanupDivisionsTaskSpec extends AnyWordSpec with TryValues with Matchers with JdbcSpecTemplate {

  private val eventDaoResolver = DivisionDaoResolver.forSupported(eventStorageDualDatabase)

  private val billedEventDaoResolver =
    BilledEventDivisionDaoResolver.forSupported(eventStorageDatabase, eventStorageDatabase)
  private val partitionTTL = 5.days

  private val task = new CleanupDivisionsTask(eventDaoResolver, billedEventDaoResolver, partitionTTL)

  "CleanupDivisionsTask" should {

    "correctly work on empty dao" in {
      task.execute().success.value
    }

    "delete old partitions" in {
      insertEventsForDays(Iterable(deadlinedPartition, today))
      hasDeadlinedPartition shouldBe true
      hasDeadlinedBilledEventPartition shouldBe true

      (task.execute() should be).a(Symbol("Success"))
      hasDeadlinedPartition shouldBe false
      hasDeadlinedBilledEventPartition shouldBe false
    }

    "delete all deadlined partitions" in {
      val deadlinedDates = (1 to 2).map(day => deadlinedPartition.minusDays(day))
      val dates = (1 to 10).map(day => deadlinedPartition.plusDays(day))
      insertEventsForDays(deadlinedDates ++ dates)
      hasDeadlinedPartition shouldBe true
      hasDeadlinedBilledEventPartition shouldBe true

      (task.execute() should be).a(Symbol("Success"))
      hasDeadlinedPartition shouldBe false
      hasDeadlinedBilledEventPartition shouldBe false
    }
  }

  private def today: LocalDate = DateTimeUtils.now().toLocalDate
  private def deadlinedPartition: LocalDate = DateTimeUtils.now().minus(5.days + 1.days).toLocalDate

  private def hasDeadlinedPartition: Boolean = {
    eventDaoResolver.all.exists { case (_, dao) =>
      val earliest = dao.read(EventDivisionDao.Earliest).get.headOption.map(_.timestamp.toLocalDate)
      earliest.exists(ld => !ld.isAfter(deadlinedPartition))
    }
  }

  private def hasDeadlinedBilledEventPartition: Boolean = {
    billedEventDaoResolver.all.exists { case (_, dao) =>
      val earliest = dao.get(BilledEventDao.Earliest).get.headOption.map(_.timestamp.toLocalDate)
      earliest.exists(ld => !ld.isAfter(deadlinedPartition))
    }
  }

  private def insertEventsForDays(dates: Iterable[LocalDate]): Unit = {
    dates.foreach { date =>
      eventDaoResolver.all.foreach { case (_, dao) =>
        val payload = PayloadGen.next.copy(timestamp = date.toDateTimeAtCurrentTime)
        dao.write(Iterable(payload))
      }
      billedEventDaoResolver.all.foreach { case (_, dao) =>
        val payload = BilledEventInfoGen.next.copy(timestamp = date.toDateTimeAtCurrentTime)
        dao.write(Iterable(payload))
      }
    }
  }

}
