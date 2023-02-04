package ru.yandex.vertis.billing.events.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.DivisionDaoResolver._
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcEventDivisionDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.dao.{DuplicationPolicyResolver, EventDivisionDao}
import ru.yandex.vertis.billing.events.service.IndexingEventDailyUniq
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.model_core.{Event, SupportedDivisions}
import ru.yandex.vertis.billing.service.impl.EventStoreServiceImpl
import ru.yandex.vertis.billing.util.TestingHelpers.RichPayload

import scala.util.Success

/**
  * Spec on storing ``AutoRuIndexing`` events
  *
  * @author ruslansd
  */
class EventStoreServiceImplIndexingSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate {

  protected def eventStoreService =
    new EventStoreServiceImpl(
      forSupported(dualDatabase = eventStorageDualDatabase, dpResolver = DuplicationPolicyResolver.WithSkipOnIndexing)
    ) with IndexingEventDailyUniq {

      def resolver: DuplicationPolicyResolver =
        DuplicationPolicyResolver.WithSkipOnIndexing
    }

  private val autoRuEventStorageDao =
    new JdbcEventDivisionDao(eventStorageDualDatabase, SupportedDivisions.AutoRuIndexing.identity)

  private val realtyEventStorageDao =
    new JdbcEventDivisionDao(eventStorageDualDatabase, SupportedDivisions.RealtyCommercialRuIndexing.identity)

  val autoRuEvent = Event(SupportedDivisions.AutoRuIndexing, PayloadGen.next)
  val realtyEvent = Event(SupportedDivisions.RealtyCommercialRuIndexing, PayloadGen.next)

  val fixedRealtyPayload = modify(realtyEvent).payload.withoutEpoch

  private def modify(event: Event): Event =
    DuplicationPolicyResolver.WithSkipOnIndexing.resolve(event.division).modify(event)

  "EventStoreServiceImpl" should {
    "store autoru indexing event with provided time" in {
      eventStoreService.store(Iterable(autoRuEvent)).get

      autoRuEventStorageDao.read(EventDivisionDao.All) match {
        case Success(payloads) if payloads.size == 1 =>
          payloads.head.withoutEpoch should be(autoRuEvent.payload)
        case res => fail(res.toString)
      }
    }

    "store autoru indexing same events only once" in {
      eventStoreService.store(Iterable(autoRuEvent)).get

      autoRuEventStorageDao.read(EventDivisionDao.All) match {
        case Success(payloads) if payloads.size == 1 =>
          payloads.head.withoutEpoch should be(autoRuEvent.payload)
        case res => fail(res.toString)
      }
    }

    "store realty indexing events with start of day time" in {
      eventStoreService.store(Iterable(realtyEvent)).get

      realtyEventStorageDao.read(EventDivisionDao.All) match {
        case Success(payloads) if payloads.size == 1 =>
          payloads.head.withoutEpoch should be(fixedRealtyPayload)
        case res => fail(res.toString)
      }
    }
  }
}
