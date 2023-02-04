package ru.yandex.vertis.billing.tasks

import billing.emon.Model.EventStateId
import com.typesafe.config.ConfigFactory
import org.joda.time.{Duration, Instant}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.EmonEventDao.Event
import ru.yandex.vertis.billing.dao.gens.EmonEventGen
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcEmonEventDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.Epoch
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.tasks.EmonEventsCleanTask.MaxEventAge
import ru.yandex.vertis.billing.util.EmonUtils.RichEventState
import ru.yandex.vertis.billing.util.clean.CleanableEmonEventDao

class EmonEventsCleanTaskSpec
  extends AnyWordSpec
  with AsyncSpecBase
  with JdbcSpecTemplate
  with Matchers
  with BeforeAndAfterEach {

  protected def emonEventDao =
    new JdbcEmonEventDao(eventStorageDatabase) with CleanableEmonEventDao

  protected val task = new EmonEventsCleanTask(emonEventDao)

  override def beforeEach(): Unit = {
    emonEventDao.clean().get
    super.beforeEach()
  }

  "Emon events clean task" should {

    "succesfully run on empty database" in {
      runTask()
    }

    "retain fresh groups" in {
      val events = EmonEventGen.next(100).toList.map(e => e.copy(epoch = freshEventsEpoch.next))
      insert(events)
      runTask()
      val retained = getEvents(events.map(_.event.eventStateId))
      retained.toSet shouldBe events.toSet
    }

    "remove old groups" in {
      val events = EmonEventGen.next(100).toList.map(e => e.copy(epoch = oldEventsEpoch.next))
      insert(events)
      runTask()
      val retained = getEvents(events.map(_.event.eventStateId))
      retained shouldBe Seq.empty
    }

    "don't remove group with fresh event" in {
      val oldEvents = EmonEventGen.next(50).toList.map(e => e.copy(epoch = oldEventsEpoch.next))
      val freshEvents = oldEvents.map(e =>
        e.copy(
          event = e.event.toBuilder.setSnapshotId(e.event.getSnapshotId + 1).build(),
          epoch = freshEventsEpoch.next
        )
      )
      val allEvents = oldEvents ++ freshEvents
      insert(allEvents)
      runTask()
      val retained = getEvents(allEvents.map(_.event.eventStateId))
      retained.toSet shouldBe allEvents.toSet
    }
  }

  private def insert(events: Seq[Event]): Unit = {
    emonEventDao.insert(events).get
  }

  private def getEvents(eventIds: Seq[EventStateId]): Seq[Event] = {
    emonEventDao.getEvents(eventIds).get
  }

  private def runTask(): Unit = {
    task.execute(ConfigFactory.empty()).futureValue
  }

  private def freshEventsEpoch: Gen[Epoch] =
    Gen.choose(Instant.now().minus(MaxEventAge).plus(MaxTestRunTime).getMillis, Instant.now().getMillis)

  private def oldEventsEpoch: Gen[Epoch] =
    Gen.choose(1000, Instant.now().minus(MaxEventAge).getMillis)

  private val MaxTestRunTime = Duration.standardMinutes(5)
}
