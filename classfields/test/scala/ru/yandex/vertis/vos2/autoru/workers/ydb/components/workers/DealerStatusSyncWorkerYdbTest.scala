package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.cabinet.DealerAutoru.Dealer.Status
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model.{AutoruOfferID, AutoruSaleStatus, TestUtils}
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

class DealerStatusSyncWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll
  with InitTestDbs {
  implicit val traced: Traced = Traced.empty

  override def beforeAll(): Unit = {
    initDbs()
  }

  private lazy val privateSale = getOfferById(1043045004).toBuilder.clearFlag().build()
  private lazy val dealerSale = getOfferById(1043026846).toBuilder.clearFlag().build()
  private lazy val bannedSale = getOfferById(1044216699).toBuilder.clearFlag().putFlag(OfferFlag.OF_BANNED).build()

  private def createOffer(isDealer: Boolean = true, status: Status = Status.ACTIVE): Offer.Builder = {
    val builder = TestUtils.createOffer(dealer = isDealer)
    builder.getUserBuilder.setAutoruDealerStatus(status)

    builder
  }

  abstract private class Fixture {
    components.autoruSalesDao.setStatus(
      id = AutoruOfferID.parse(dealerSale.getOfferID).id,
      expectedStatuses = Seq(),
      newStatus = AutoruSaleStatus.STATUS_SHOW
    )
    components.autoruSalesDao.setStatus(
      id = AutoruOfferID.parse(bannedSale.getOfferID).id,
      expectedStatuses = Seq(),
      newStatus = AutoruSaleStatus.STATUS_MODERATOR_DELETED
    )

    val worker = new DealerStatusSyncWorkerYdb(
      components.offersWriter
    ) with YdbWorkerTestImpl
  }

  "Offer" should {
    "be skipped" when {
      "seller is not dealer" in new Fixture {
        private val offerBuilder = createOffer(isDealer = false)
        lazy val offer: Offer = offerBuilder.build()

        assert(worker.shouldProcess(offer, None).shouldProcess === false)
      }
      "dealer status is not stopped or deleted" in new Fixture {
        private val offerBuilder = createOffer()
        lazy val offer: Offer = offerBuilder.build()

        assert(worker.shouldProcess(offer, None).shouldProcess === false)
      }
    }
    "be revoked" when {
      "status is stopped" in new Fixture {
        private val offerBuilder = dealerSale.toBuilder
        offerBuilder.getUserBuilder.setAutoruDealerStatus(Status.STOPPED)
        lazy val offer: Offer = offerBuilder.build()

        assert(worker.shouldProcess(offer, None).shouldProcess === true)
        private val result = worker.process(offer, None).updateOfferFunc.get(offer)
        assert(result.getAutoruCompositeStatus == CompositeStatus.CS_INACTIVE)
      }
    }
    "be archived" when {
      "status is deleted" in new Fixture {
        private val offerBuilder = dealerSale.toBuilder
        offerBuilder.getUserBuilder.setAutoruDealerStatus(Status.DELETED)
        lazy val offer: Offer = offerBuilder.build()

        assert(worker.shouldProcess(offer, None).shouldProcess === true)
        private val result = worker.process(offer, None).updateOfferFunc.get(offer)

        assert(result.getAutoruCompositeStatus == CompositeStatus.CS_REMOVED)
      }
    }
  }

}
