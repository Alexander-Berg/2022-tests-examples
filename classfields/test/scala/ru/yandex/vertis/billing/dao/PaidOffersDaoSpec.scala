package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.{PaidOffer, PartnerOfferId}
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.Success

/**
  * Specs on [[PaidOffersDao]]
  */
trait PaidOffersDaoSpec extends AnyWordSpec with Matchers {

  protected def paidOffersDao: PaidOffersDao

  "PaidOffersDao" should {
    val offerId = PartnerOfferId("partner1", "111111")
    val campaign = "123"
    val time = now()

    val paidOffer = PaidOffer(offerId, campaign, time)
    val paidOffer2 = PaidOffer(offerId, campaign, time.minusMinutes(2))
    val paidOfferFromFuture =
      PaidOffer(offerId, campaign, time.plusMinutes(5))

    "prove offer isn't marked" in {
      paidOffersDao.isPaid(paidOffer) should be(Success(false))
    }
    "add offer as marked" in {
      paidOffersDao.markPaid(Iterable(paidOffer)) should be(Success(()))
    }
    "prove offer is marked" in {
      paidOffersDao.isPaid(paidOffer) should be(Success(true))
    }
    "add offer repeatedly" in {
      paidOffersDao.markPaid(Iterable(paidOffer)) should be(Success(()))
    }
    "prove offer is still marked" in {
      paidOffersDao.isPaid(paidOffer) should be(Success(true))
    }
    "prove offer with other date isn't marked" in {
      paidOffersDao.isPaid(paidOffer2) should be(Success(false))
    }
    "add offer from future" in {
      paidOffersDao.markPaid(Iterable(paidOfferFromFuture)) should be(Success(()))
    }
    "remove older than now " in {
      paidOffersDao.deleteOlderThan(time.plusMinutes(1)) should be(Success(()))
    }
    "prove only offer from future is marked" in {
      paidOffersDao.isPaid(paidOffer) should be(Success(false))
      paidOffersDao.isPaid(paidOfferFromFuture) should be(Success(true))
    }
  }
}
