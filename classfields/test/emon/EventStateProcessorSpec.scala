package ru.yandex.vertis.billing.emon

import billing.CommonModel
import com.google.protobuf.util.Timestamps
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.gens.ValidEmonEventStateGen
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcEmonEventDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.service.metered.MeteredStub
import ru.yandex.vertis.billing.util.EmonUtils
import ru.yandex.vertis.billing.util.EmonUtils.RichEventState
import ru.yandex.vertis.billing.util.clean.CleanableEmonEventDao
import ru.yandex.vertis.mockito.MockitoSupport

class EventStateProcessorSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  protected def emonEventDao =
    new JdbcEmonEventDao(eventStorageDatabase) with CleanableEmonEventDao

  protected val processor = new EventStateProcessor(emonEventDao) with MeteredStub

  override def beforeEach(): Unit = {
    emonEventDao.clean().get
    super.beforeEach()
  }

  "EventStateProcessor" should {
    "save valid events" in {
      val events = ValidEmonEventStateGen.next(100).toList
      processor.process(events).futureValue
      val inserted = emonEventDao.getEvents(events.map(_.eventStateId)).get
      inserted.map(_.event).toSet shouldBe events.toSet
    }

    "ignore events without price" in {
      val events = ValidEmonEventStateGen.next(100).toList.map { e =>
        val b = e.toBuilder
        b.getPriceBuilder.clear()
        b.build()
      }
      processor.process(events).futureValue
      val inserted = emonEventDao.getEvents(events.map(_.eventStateId)).get
      inserted shouldBe Seq.empty
    }

    "ignore events without balance payer" in {
      val events = ValidEmonEventStateGen.next(100).toList.map { e =>
        val b = e.toBuilder
        b.getEventBuilder.getPayerBuilder.clearBalance()
        b.build()
      }
      processor.process(events).futureValue
      val inserted = emonEventDao.getEvents(events.map(_.eventStateId)).get
      inserted shouldBe Seq.empty
    }

    "ignore events for unsupported project" in {
      val events = ValidEmonEventStateGen.next(100).toList.map { e =>
        val b = e.toBuilder
        b.getEventBuilder.getEventIdBuilder.setProject(CommonModel.Project.UNKNOWN_PROJECT)
        b.build()
      }
      processor.process(events).futureValue
      val inserted = emonEventDao.getEvents(events.map(_.eventStateId)).get
      inserted shouldBe Seq.empty
    }

    "ignore event update for too old events" in {
      val events = ValidEmonEventStateGen.next(100).toList.map { e =>
        val b = e.toBuilder
        val now = System.currentTimeMillis()
        val created = now - 2 * EmonUtils.MaxEventAge.getMillis
        b.getEventBuilder.setTimestamp(Timestamps.fromMillis(created))
        b.setTimestamp(Timestamps.fromMillis(now))
        b.build()
      }
      processor.process(events).futureValue
      val inserted = emonEventDao.getEvents(events.map(_.eventStateId)).get
      inserted shouldBe Seq.empty
    }

    "ignore event update for the same snapshot" in {
      val events = ValidEmonEventStateGen.next(100).toList
      val eventUpdates = events.map { e =>
        val b = e.toBuilder
        b.getPriceBuilder.getResponseBuilder.getRuleBuilder.getPriceBuilder
          .setKopecks(e.getPrice.getResponse.getRule.getPrice.getKopecks + 100)
        b.build()
      }
      processor.process(events).futureValue
      processor.process(eventUpdates).futureValue
      val inserted = emonEventDao.getEvents(eventUpdates.map(_.eventStateId)).get
      inserted.map(_.event).toSet shouldBe events.toSet
    }
  }

}
