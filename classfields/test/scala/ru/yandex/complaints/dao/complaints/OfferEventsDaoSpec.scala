package ru.yandex.complaints.dao.complaints

import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import ru.yandex.complaints.dao.events.OfferEventsDao
import ru.yandex.complaints.dao.offers.OffersDao
import Generators._
import ru.yandex.complaints.dao.OfferID
import ru.yandex.complaints.dao.events.OfferEventsDao.{EventRecord, Filter}
import ru.yandex.complaints.model.OfferEvent
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.{Failure, Success}

/**
  * Spec for [[OfferEventsDao]]
  *
  * @author potseluev
  */
trait OfferEventsDaoSpec
  extends WordSpec
    with BeforeAndAfter
    with MockitoSupport
    with Matchers {

  def offerEventsDao: OfferEventsDao

  def offersDao: OffersDao

  val FakeEvent = mock[OfferEvent]

  before {
    val notExistedEventId = Int.MaxValue
    when(FakeEvent.value).thenReturn(notExistedEventId)
  }

  "OfferEventsDao" should {

    "return empty result" in {
      val offerId = OfferIdGen.next
      offerEventsDao.get(Filter.ByOfferId(offerId)) shouldBe Success(Seq.empty)
    }

    "fail when try to add event for not existed offer" in {
      val offerId = OfferIdGen.next
      offerEventsDao.add(EventRecord(offerId, OfferEvent.Warn)) match {
        case Failure(_) => ()
        case other => fail(s"Unexpected $other")
      }
    }

    "correct add new event" in {
      val offer = OfferGen.next
      offersDao.create(offer.id, offer.authorId)
      offerEventsDao.add(EventRecord(offer.id, OfferEvent.Warn)) shouldBe Success(())
    }

    "fail when try to add event with unknown type" in {
      val offer = OfferGen.next
      offersDao.create(offer.id, offer.authorId)
      offerEventsDao.add(EventRecord(offer.id, FakeEvent)) match {
        case Failure(_) => ()
        case other => fail(s"Unexpected $other")
      }
    }

    "correct get all events sorted by creation time" in {
      check(OfferEvent.Warn, Filter.ByOfferId)
    }

    "correct get events with filter sorted by creation time" in {
      check(OfferEvent.Warn, Filter.ByOfferIdAndEventType(_, OfferEvent.Warn))
    }

    "return empty result when no matches to filter" in {
      val offer = OfferGen.next
      val n = 10
      offersDao.create(offer.id, offer.authorId)
      (1 to n).foreach(_ => offerEventsDao.add(EventRecord(offer.id, OfferEvent.Warn)))
      val result = offerEventsDao.get(Filter.ByOfferIdAndEventType(offer.id, FakeEvent))
      result shouldBe Success(Seq.empty)
    }

    "return empty events if offer was removed" in {
      val offer = OfferGen.next
      val n = 10
      offersDao.create(offer.id, offer.authorId)
      (1 to n).foreach(_ => offerEventsDao.add(EventRecord(offer.id, OfferEvent.Warn)))

      offersDao.plainRemove(offer.id)

      val result = offerEventsDao.get(Filter.ByOfferId(offer.id))
      result shouldBe Success(Seq.empty)
    }

    def check(event: OfferEvent, filter: OfferID => Filter): Unit = {
      val offer = OfferGen.next
      val n = 10
      offersDao.create(offer.id, offer.authorId)
      (1 to n).foreach(_ => offerEventsDao.add(EventRecord(offer.id, event)))
      val records = offerEventsDao.get(filter(offer.id)).get
      records.size shouldBe n
      records shouldEqual records.sortWith { (r1, r2) =>
        r1.time.isBefore(r2.time)
      }
    }
  }
}
