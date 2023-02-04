package ru.yandex.vertis.billing.service.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.DivisionDaoResolver._
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.model_core.Division
import ru.yandex.vertis.billing.model_core.gens.{EventGen, Producer}

import scala.util.{Failure, Success}

/**
  * Specs on [[EventStoreServiceImpl]]
  *
  * @author alesavin
  */
class EventStoreServiceImplSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate {

  protected def eventStoreService =
    new EventStoreServiceImpl(forSupported(eventStorageDualDatabase))

  "EventStoreServiceImpl" should {

    val events = EventGen.next(1000).toList
    val unsupported =
      EventGen.next(1).map(_.copy(division = Division("test", "test", "test"))).toList

    "store any valid events" in {
      eventStoreService.store(events) match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "fail if unsupported event" in {
      eventStoreService.store(unsupported) match {
        case Failure(e) => info(s"Done ${e.getMessage}")
        case other => fail(s"Unexpected $other")
      }
    }
    "fail if any unsupported event in batch" in {
      eventStoreService.store(events ++ unsupported) match {
        case Failure(e) => info(s"Done ${e.getMessage}")
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
