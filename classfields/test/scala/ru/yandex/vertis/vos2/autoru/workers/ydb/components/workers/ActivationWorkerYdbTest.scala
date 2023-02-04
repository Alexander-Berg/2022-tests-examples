package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doNothing
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.YdbQueryExecutor
import ru.yandex.vertis.ydb.skypper.request.RequestContext
import ru.yandex.vertis.ydb.skypper.settings.TransactionSettings
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.{AutoruOfferID, AutoruSale, AutoruSaleStatus, TestUtils}
import ru.yandex.vos2.autoru.services.salesman.PriceService
import ru.yandex.vos2.autoru.services.salesman.SalesmanUserClient.{ActivationPrice, Promocode}
import ru.yandex.vos2.commonfeatures.FeatureRegistryFactory
import ru.yandex.vos2.services.promocoder.PromocoderClient
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Try
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vos2.dao.offers.ng.YdbRequests.getWorkerShardId

class ActivationWorkerYdbTest
  extends AnyWordSpec
  with InitTestDbs
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with MockitoSupport
  with MockHttpClientHelper {

  import ru.yandex.vertis.ydb.skypper.YdbWrapper
  import ru.yandex.vos2.commonfeatures.FeaturesManager

  implicit val traced: Traced = Traced.empty

  val featureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager = new FeaturesManager(featureRegistry)

  private lazy val privateSale = getOfferById(1043045004).toBuilder.clearFlag().build()

  val ydbMocked = mock[YdbWrapper]

  val priceService = mock[PriceService]
  val offerDao = components.offerVosDao
  val offersWriter = components.offersWriter
  val promocoderClient = mock[PromocoderClient]

  val worker: YdbWorkerTestImpl = new ActivationWorkerYdb(
    priceService,
    offersWriter,
    promocoderClient,
    offerDao
  ) with YdbWorkerTestImpl {
    override def ydb = ydbMocked
    override def operational = components.operational
  }

  override def beforeAll(): Unit = {
    initDbs()
  }

  override protected def beforeEach(): Unit = {
    components.autoruSalesDao.setStatus(
      id = AutoruOfferID.parse(privateSale.getOfferID).id,
      expectedStatuses = Seq(),
      newStatus = AutoruSaleStatus.STATUS_WAITING_ACTIVATION
    )
  }

  abstract private class Fixture {
    val offer: Offer
  }

  "Activation Stage" should {

    "rewrite check" in new Fixture {
      val creationTs = new DateTime("2020-04-01T10:15:45.123+03:00").getMillis
      override val offer: Offer =
        getOfferById(1043270830L).toBuilder
          .setTimestampCreate(creationTs)
          .setTimestampUpdate(creationTs)
          .setOfferID(privateSale.getOfferID)
          .setOfferIRef(privateSale.getOfferIRef)
          .setOfferAutoru(privateSale.getOfferAutoru)
          .setUserRef(privateSale.getUserRef)
          .clearFlag()
          .addFlag(OfferFlag.OF_NEED_ACTIVATION)
          .build()

      val shardId = getWorkerShardId(offer.getOfferID)
      val nextCheck = new DateTime

      val duration = FiniteDuration(30, SECONDS)
      when(priceService.getActivationPrice(?)(?)).thenReturn {
        Try(ActivationPrice(price = 0, duration, Some(Promocode("featureId", count = 3))))
      }

      when(promocoderClient.decrementFeature(ArgumentMatchers.eq("featureId"), ArgumentMatchers.eq(3))(?)).thenReturn(1)

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[Option[DateTime]](eqq("get-current-next-check"))(?, ?)(?))
            .thenReturn(Iterator(None))
          doNothing().when(mockedExecutor).updatePrepared(eqq("write-activation_ydb-result"))(?, ?)
          doNothing().when(mockedExecutor).updatePrepared(eqq("update-worker-next-check"))(?, ?)
          executor(mockedExecutor)

      }

      worker.processOffer(offer, shardId, offer.getOfferID, nextCheck, None)

    }

    "activate free offer with promocode" in new Fixture {
      val creationTs = new DateTime("2020-04-01T10:15:45.123+03:00").getMillis
      override val offer: Offer =
        TestUtils
          .createOffer(creationTs)
          .setOfferID(privateSale.getOfferID)
          .setOfferIRef(privateSale.getOfferIRef)
          .setOfferAutoru(privateSale.getOfferAutoru)
          .setUserRef(privateSale.getUserRef)
          .addFlag(OfferFlag.OF_NEED_ACTIVATION)
          .build()

      val duration = FiniteDuration(30, SECONDS)
      when(priceService.getActivationPrice(?)(?)).thenReturn {
        Try(ActivationPrice(price = 0, duration, Some(Promocode("featureId", count = 3))))
      }

      when(promocoderClient.decrementFeature(ArgumentMatchers.eq("featureId"), ArgumentMatchers.eq(3))(?)).thenReturn(1)

      assert(worker.shouldProcess(offer, None).shouldProcess)
      val result = worker.process(offer, None)
      val newOffer = result.updateOfferFunc.get(offer)

      val expectedExpireTs: Long = new DateTime("2020-04-01T10:16:15.123+03:00").getMillis

      newOffer.getTimestampWillExpire shouldBe expectedExpireTs
      newOffer.getFlagList.contains(OfferFlag.OF_NEED_ACTIVATION) shouldBe false
      newOffer.getFlagList.contains(OfferFlag.OF_INACTIVE) shouldBe false

      val service: PaidService = newOffer.getOfferAutoru.getServicesList.get(0)
      service.getServiceType shouldBe ServiceType.ADD
      service.getExpireDate shouldBe expectedExpireTs
      service.getIsActive shouldBe true

      val oldSale: AutoruSale = components.autoruSalesDao.getOffer(privateSale.getOfferIRef).value
      oldSale.expireDate.getMillis shouldBe (expectedExpireTs / 1000) * 1000
    }

    "don't activate offer with placement price > 0 and promocode" in new Fixture {
      val creationTs = new DateTime("2020-04-01T10:15:45.123+03:00").getMillis
      override val offer: Offer = TestUtils
        .createOffer(creationTs)
        .build()

      val duration = FiniteDuration(30, SECONDS)
      when(priceService.getActivationPrice(?)(?)).thenReturn {
        Try(ActivationPrice(price = 100, duration, Some(Promocode("featureId", count = 1))))
      }

      val result = worker.process(offer, None)

      val expectedExpireTs: Long = new DateTime("2020-06-01T10:15:45.123+03:00").getMillis
      result.updateOfferFunc shouldBe None
      offer.getTimestampWillExpire shouldBe expectedExpireTs
      offer.getFlagList.contains(OfferFlag.OF_NEED_ACTIVATION) shouldBe false
    }
  }

}
