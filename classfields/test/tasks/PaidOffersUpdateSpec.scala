package ru.yandex.vertis.billing.tasks

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.impl.jdbc._
import ru.yandex.vertis.billing.event.{EventsProviders, HandleTryReader, UnsafePaidOfferRecordsReader}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.PaidOffersService
import ru.yandex.vertis.billing.service.impl.PaidOffersServiceImpl
import ru.yandex.vertis.billing.settings.RealtyCommercialTasksServiceComponents

import scala.collection.Iterable
import scala.util.{Success, Try}

/**
  * Tests for statistics update task
  *
  * @author alesavin
  */
class PaidOffersUpdateSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with EventsProviders {

  trait AccumulatePaidOffersService extends PaidOffersService {

    def acc: Iterable[PaidOffer]

    abstract override def markPaid(offers: Iterable[PaidOffer]): Try[Unit] = {
      acc ++ offers
      super.markPaid(offers)
    }
  }

  val paid = List[PaidOffer]()

  val paidOffersDao = new JdbcPaidOffersDao(billingDualDatabase)

  val paidOffersService = new PaidOffersServiceImpl(paidOffersDao) with AccumulatePaidOffersService {
    def acc = paid
  }

  private val PaidOffersRecordsReader =
    new HandleTryReader(
      new UnsafePaidOfferRecordsReader(
        sortedRandomEventsReader(100),
        RealtyCommercialTasksServiceComponents.getOfferId
      )
    )

  private val PaidOffersRecordsUpdate =
    new PaidOffersUpdateTask(
      paidOffersService,
      PaidOffersRecordsReader,
      PaidOffersUpdateTask.ByIndexingDescriptor
    )

  "PaidOffersUpdateTask" should {
    "update paid offers based on gens" in {
      PaidOffersRecordsUpdate.execute() match {
        case Success(_) =>
          paid.find(r => !paidOffersService.isPaidToday(r.offerId, r.campaignId).get) should be(None)
        case other => fail(s"Unpredicted: $other")
      }
    }
  }
}
