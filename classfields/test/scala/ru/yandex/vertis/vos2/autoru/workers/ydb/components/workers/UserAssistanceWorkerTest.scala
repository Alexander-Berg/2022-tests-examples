package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.mockito.Mockito.{doNothing, reset, times, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.CommonModel.PaidService.PaymentReason
import ru.auto.salesman.model.user.ApiModel.{ProductPrice, ProductPrices}
import ru.yandex.vertis.baker.components.workdistribution.WorkDistributionData
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.user_assistance.UserAssistanceWorker.{EmptyState, RetryDelay}
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.user_assistance.{UserAssistanceOfferData, UserAssistanceWorker}
import ru.yandex.vertis.ydb.skypper.request.RequestContext
import ru.yandex.vertis.ydb.skypper.settings.TransactionSettings
import ru.yandex.vertis.ydb.skypper.{YdbQueryExecutor, YdbWrapper}
import ru.yandex.vos2
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, PremiumOffer}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruRichOfferBuilder
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.services.salesman.SalesmanUserClient
import ru.yandex.vos2.autoru.utils.time.TimeService
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.commonfeatures.VosFeatureTypes.{VosFeature, WithGeneration}
import ru.yandex.vos2.util.Protobuf

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UserAssistanceWorkerTest extends AnyWordSpec with Matchers with MockitoSupport with InitTestDbs {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val offer: Offer
    val mockedFeatureManager = mock[FeaturesManager]
    val mockedFeature = mock[VosFeature]
    when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
    when(mockedFeature.value).thenReturn(WithGeneration(false, 1))
    val daoMocked = mock[AutoruOfferDao]
    val ydbMocked = mock[YdbWrapper]

    val salesmanUserClient = mock[SalesmanUserClient]
    val timeService = mock[TimeService]

    val userAssistanceWorker = new UserAssistanceWorker(
      ydbMocked,
      components.regionTree,
      salesmanUserClient,
      components.offerFormConverter,
      timeService
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = mockedFeatureManager

      override val ydb: YdbWrapper = ydbMocked
    }
  }

  "UserAssistanceWorkerTest YDB" should {

    "should process offer without state" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)
      testOffer.setOfferAutoru(offerAutoRu)

      override val offer: Offer = testOffer.build()
      assert(userAssistanceWorker.shouldProcess(offer, None).shouldProcess)
    }

    "should process offer with state" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)
      testOffer.setOfferAutoru(offerAutoRu)

      val premiumOffer = PremiumOffer.newBuilder().setIsPremiumOffer(true).setIsAlreadyProcessed(false).build()

      override val offer: Offer = testOffer.build()
      assert(userAssistanceWorker.shouldProcess(offer, Some(Protobuf.toJson(premiumOffer))).shouldProcess)
    }

    "should not process offer that already processed" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoruBuilder
      offerAutoRu.addNotifications(
        Notification.newBuilder().setType(NotificationType.USER_ASSISTANCE).setTimestampCreate(vos2.getNow)
      )
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)

      val premiumOffer = PremiumOffer
        .newBuilder()
        .setIsPremiumOffer(true)
        .setUpdated(Timestamps.fromMillis(vos2.getNow))
        .setIsAlreadyProcessed(true)
        .build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      override val offer: Offer = testOffer.build()
      assert(!userAssistanceWorker.shouldProcess(offer, premiumStr).shouldProcess)
    }

    "should  process offer thean already processed but old" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)

      val premiumOffer = PremiumOffer
        .newBuilder()
        .setIsPremiumOffer(true)
        .setUpdated(Timestamps.fromMillis(vos2.getNow - (2.days.toMillis)))
        .setIsAlreadyProcessed(true)
        .build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      override val offer: Offer = testOffer.build()
      assert(userAssistanceWorker.shouldProcess(offer, premiumStr).shouldProcess)
    }

    "should process offer with preset IsPremiumOffer" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)
      val premiumOffer = PremiumOffer.newBuilder().setIsPremiumOffer(false).build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      testOffer.setOfferAutoru(offerAutoRu)

      override val offer: Offer = testOffer.build()
      assert(!userAssistanceWorker.shouldProcess(offer, premiumStr).shouldProcess)
    }
    "should process offer with preset IsPremiumOffer=true" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)
      testOffer.setPremiumOffer(PremiumOffer.newBuilder().setIsPremiumOffer(true).build())

      val premiumOffer = PremiumOffer.newBuilder().setIsPremiumOffer(true).build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))

      testOffer.setOfferAutoru(offerAutoRu)

      override val offer: Offer = testOffer.build()
      assert(userAssistanceWorker.shouldProcess(offer, premiumStr).shouldProcess)
    }

    "should NOT process offer" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1000).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)

      val premiumOffer = PremiumOffer
        .newBuilder()
        .setIsAlreadyProcessed(true)
        .setUpdated(Timestamps.fromMillis(vos2.getNow))
        .build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))

      testOffer.setOfferAutoru(offerAutoRu)

      override val offer: Offer = testOffer.build()

      assert(!userAssistanceWorker.shouldProcess(offer, premiumStr).shouldProcess)
    }

    "should NOT process offer with already processed feature" in new Fixture {
      private val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoru.toBuilder
      private val seller = offerAutoRu.getSeller.toBuilder
      private val place = seller.getPlace.toBuilder.setGeobaseId(1).build()
      seller.setPlace(place)
      offerAutoRu.setSeller(seller)
      val premiumOffer =
        PremiumOffer
          .newBuilder()
          .setIsAlreadyProcessed(true)
          .setUpdated(Timestamps.fromMillis(vos2.getNow))
          .build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      testOffer.setOfferAutoru(offerAutoRu)

      override val offer: Offer = testOffer.build()

      assert(!userAssistanceWorker.shouldProcess(offer, premiumStr).shouldProcess)
    }

    "With premium offer" in new Fixture {
      val testOffer = createOffer()
      private val offerAutoRu = testOffer.getOfferAutoruBuilder
      override val offer: Offer = testOffer.build()
      private val productPrices = ProductPrices
        .newBuilder()
        .addAllProductPrices(
          Seq(ProductPrice.newBuilder().setPaymentReason(PaymentReason.PREMIUM_OFFER).build()).asJava
        )
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(Iterator.empty)
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      doNothing().when(ydbMocked).updatePrepared(?)(?, ?)(?, ?)
      val res = userAssistanceWorker.process(offer, None)
      val state = (Protobuf.fromJson[PremiumOffer](res.nextState.get))

      assert(
        res.updateOfferFunc.isEmpty &&
          state.getIsPremiumOffer &&
          res.nextCheck.nonEmpty
      )
    }

    "With exception while salesman working" in new Fixture {
      val testOffer = createOffer()
      testOffer.getPremiumOfferBuilder.build()
      override val offer: Offer = testOffer.build()

      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Failure(new Exception("Test")))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      val res = userAssistanceWorker.process(offer, None)
      assert(
        res.updateOfferFunc.isEmpty &&
          res.nextState.isEmpty
      )
      res.nextCheck.get.getMillis shouldBe time.plus(RetryDelay.toMillis).getMillis +- 1000
    }

    "With preset IsPremiumOffer" in new Fixture {
      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)

      val premiumOffer = PremiumOffer
        .newBuilder()
        .setIsPremiumOffer(true)
        .setIsAlreadyProcessed(true)
        .setUpdated(Timestamps.fromMillis(vos2.getNow))
        .build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      doNothing().when(ydbMocked).updatePrepared(?)(?, ?)(?, ?)

      val res = userAssistanceWorker.process(offer, premiumStr)

      assert(
        res.nextState.isEmpty && res.nextCheck.nonEmpty
      )
    }

    "With not premium offer" in new Fixture {
      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      private val productPrices = ProductPrices
        .newBuilder()
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      doNothing().when(ydbMocked).updatePrepared(?)(?, ?)(?, ?)
      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(Iterator.empty)
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val res = userAssistanceWorker.process(offer, None)
      val state = (Protobuf.fromJson[PremiumOffer](res.nextState.get))

      assert(
        state.getIsAlreadyProcessed
          && !state.getIsPremiumOffer
          && res.nextCheck.nonEmpty
      )
    }

    "Inactive offer with premium" in new Fixture {
      private val testOffer = createOffer().addFlag(OfferFlag.OF_INACTIVE)
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().setIsPremiumOffer(true).build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(Iterator.empty)
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      doNothing().when(ydbMocked).updatePrepared(?)(?, ?)(?, ?)
      val res = userAssistanceWorker.process(offer, premiumStr)

      assert(
        res.nextState.get == EmptyState
          && res.nextCheck.isEmpty
      )
    }

    "Inactive offer with premium retry" in new Fixture {
      private val testOffer = createOffer().addFlag(OfferFlag.OF_INACTIVE)
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().setIsPremiumOffer(true).setIsAlreadyProcessed(true).build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      doNothing().when(ydbMocked).updatePrepared(?)(?, ?)(?, ?)
      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(Iterator.empty)
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      val state = (res.nextState)
      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

    "new offer with premium data and sendChat event" in new Fixture {

      reset(mockedFeatureManager)
      reset(mockedFeature)
      when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      private val productPrices = ProductPrices
        .newBuilder()
        .addAllProductPrices(
          Seq(ProductPrice.newBuilder().setPaymentReason(PaymentReason.PREMIUM_OFFER).build()).asJava
        )
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(Iterator.empty)
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      val state = (res.nextState)
      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

    "old offer with premium data and sendChat event" in new Fixture {

      reset(mockedFeatureManager)
      reset(mockedFeature)
      when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      private val productPrices = ProductPrices
        .newBuilder()
        .addAllProductPrices(
          Seq(ProductPrice.newBuilder().setPaymentReason(PaymentReason.PREMIUM_OFFER).build()).asJava
        )
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(Iterator(UserAssistanceOfferData(Seq("1"), notificationSent = false, generation = 1)))
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      val state = (res.nextState)
      val newOffer = res.updateOfferFunc.get(offer)

      assert(newOffer.getOfferAutoru.getNotificationsList.asScala.exists(_.getType == NotificationType.USER_ASSISTANCE))

      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

    "old offer with already in database but without notification" in new Fixture {

      reset(mockedFeatureManager)
      reset(mockedFeature)
      when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      private val productPrices = ProductPrices
        .newBuilder()
        .addAllProductPrices(
          Seq(ProductPrice.newBuilder().setPaymentReason(PaymentReason.PREMIUM_OFFER).build()).asJava
        )
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(
              Iterator(UserAssistanceOfferData(Seq(offer.getOfferID), notificationSent = true, generation = 1))
            )
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      assert(res.updateOfferFunc.isEmpty)

      val state = (res.nextState)
      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

    "old offer with premium data with changed generation" in new Fixture {

      reset(mockedFeatureManager)
      reset(mockedFeature)
      when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
      when(mockedFeature.value).thenReturn(WithGeneration(true, 2))

      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      private val productPrices = ProductPrices
        .newBuilder()
        .addAllProductPrices(
          Seq(ProductPrice.newBuilder().setPaymentReason(PaymentReason.PREMIUM_OFFER).build()).asJava
        )
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(
              Iterator(UserAssistanceOfferData(Seq(offer.getOfferID), notificationSent = false, generation = 1))
            )
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)
          executor(mockedExecutor)

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      val newOffer = res.updateOfferFunc.get(offer)
      assert(newOffer.getOfferAutoru.getNotificationsList.asScala.exists(_.getType == NotificationType.USER_ASSISTANCE))
      val state = (res.nextState)
      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

    "old offer without premium data" in new Fixture {
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      reset(mockedFeatureManager)
      reset(mockedFeature)
      when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      private val testOffer = createOffer()
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List()))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(
              Iterator(UserAssistanceOfferData(Seq(offer.getOfferID), notificationSent = false, generation = 1))
            )
          doNothing().when(mockedExecutor).updatePrepared(eqq("delete-user-assistance-data"))(?, ?)
          val res = executor(mockedExecutor)

          verify(mockedExecutor, times(1)).queryPrepared(?)(?, ?)(?)
          verify(mockedExecutor, times(1)).updatePrepared(?)(?, ?)
          res

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      assert(res.updateOfferFunc.isEmpty)
      val state = (res.nextState)
      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

    "offer with already sent notification without info in ydb" in new Fixture {
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      reset(mockedFeatureManager)
      reset(mockedFeature)
      when(mockedFeatureManager.UserAssistanceMessageSendToChat).thenReturn(mockedFeature)
      when(mockedFeature.value).thenReturn(WithGeneration(true, 1))

      private val testOffer = createOffer()
      testOffer.addNotificationByType(
        Notification.NotificationType.USER_ASSISTANCE,
        isCritical = false
      )
      override val offer: Offer = testOffer.build()
      val premiumOffer = PremiumOffer.newBuilder().build()
      val premiumStr = Some(Protobuf.toJson(premiumOffer))
      val time = new DateTime()
      when(timeService.getNow).thenReturn(time)
      private val productPrices = ProductPrices
        .newBuilder()
        .addAllProductPrices(
          Seq(ProductPrice.newBuilder().setPaymentReason(PaymentReason.PREMIUM_OFFER).build()).asJava
        )
        .build()
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?)).thenReturn(Success(List(productPrices)))

      stub(
        ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
      ) {
        case (_, _, executor, _, _) =>
          val mockedExecutor = mock[YdbQueryExecutor]
          when(mockedExecutor.queryPrepared[UserAssistanceOfferData](eqq("get-user-assistance-data"))(?, ?)(?))
            .thenReturn(
              Iterator(UserAssistanceOfferData(Seq(offer.getOfferID), notificationSent = false, generation = 1))
            )
          doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-user-assistance-data"))(?, ?)

          val res = executor(mockedExecutor)

          verify(mockedExecutor, times(1)).queryPrepared(?)(?, ?)(?)
          verify(mockedExecutor, times(1)).updatePrepared(?)(?, ?)
          res

      }
      val res = userAssistanceWorker.process(offer, premiumStr)
      assert(res.updateOfferFunc.isEmpty)
      val state = (res.nextState)
      assert(!userAssistanceWorker.shouldProcess(offer, state).shouldProcess)
    }

  }

}
