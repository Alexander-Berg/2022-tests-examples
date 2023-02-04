package ru.auto.salesman.service.impl

import cats.data.NonEmptySet
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Section.{NEW, USED}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.client.GuardianClient.{HoldState, HoldStates}
import ru.auto.salesman.dao.GoodsDao.Filter.ForOfferActivatedProducts
import ru.auto.salesman.dao.{AdsRequestDao, BadgeDao, GoodsDao}
import ru.auto.salesman.environment._
import ru.auto.salesman.model.OfferCategories.Cars
import ru.auto.salesman.model.OfferStatuses.{Hidden, OfferStatus}
import ru.auto.salesman.model.payment_model.PaymentModel.SingleWithCalls
import ru.auto.salesman.model.{OfferId, PromocoderUser, _}
import ru.auto.salesman.service.GoodsDecider.Action.{Activate, Deactivate, NoAction}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason._
import ru.auto.salesman.service.GoodsDecider.NoActionReason.{
  CampaignResolutionError,
  GetActivePackagesError,
  PriceExtractionError
}
import ru.auto.salesman.service.GoodsDecider.{
  BadgeContext,
  DeactivateReason,
  ProductContext,
  Response => DeciderResponse
}
import ru.auto.salesman.service.PromocoderFeatureService.LoyaltyArgs
import ru.auto.salesman.service._
import ru.auto.salesman.service.impl.GoodsDeciderImpl.{GoodApplyTerms, ZeroPrice}
import ru.auto.salesman.service.products.price.GoodApplyTermsService
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.{BaseSpec, ScalamockCallHandlers, TestException}
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval, RequestContext}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.Cost.{Constraints, PerIndexing}
import ru.yandex.vertis.billing.Model.Good.Custom
import ru.yandex.vertis.billing.Model.InactiveReason.MANUALLY_DISABLED
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.billing.model.Versions
import zio.{Task, ZIO}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

//noinspection TypeAnnotation
class GoodsDeciderImplSpec extends BaseSpec {

  import GoodsDeciderImplSpec._

  implicit val rc = AutomatedContext("test")

  "GoodsDecider" should {

    "return NoAction" when {

      val successNoActionPf: PartialFunction[Any, _] = {
        case Success(
              DeciderResponse(
                NoAction(_),
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None
              )
            ) =>
      }

      "can't resolve client" in new DeciderMocking {
        override lazy val clientSource =
          whenClientSource(Task.fail(testException))
        decider.apply(testRequest) should matchPattern(successNoActionPf)
      }

      "client doesn't have needed single payment tariff turned on" in new DeciderMocking {
        val clientWithoutTariff =
          testClient.copy(singlePayment = Set.empty[AdsRequestType])
        override lazy val clientSource = whenClientSource(clientWithoutTariff)
        decider
          .apply(testRequest)
          .success
          .value
          .action shouldBe a[Deactivate]
      }

      "failed to get single ads request" in new DeciderMocking {
        override lazy val adsRequestDao =
          whenAdsRequestDao(testClientId, testAdsRequestType)(
            Failure(testException)
          )
        decider.apply(testRequest) should matchPattern(successNoActionPf)
      }

      "failed to resolve campaign" in new DeciderMocking {
        override val campaignCreationService =
          whenCampaignCreationService(Failure(testException), testProduct)
        decider.apply(testRequest) should matchPattern(successNoActionPf)
      }

      "failed to resolve campaign synchronously" in new DeciderMocking {
        override val campaignCreationService =
          stub[CampaignCreationService]
        (campaignCreationService.getOrCreate _)
          .when(*)
          .throwingT(new TestException)
        val action = decider
          .apply(testRequest)
          .success
          .value
          .action
        action should matchPattern { case NoAction(CampaignResolutionError(_)) =>
        }
        (campaignCreationService.getOrCreate _).verify(*)
      }

      "there is no price in request and price estimation request failed" in new DeciderMocking {
        override lazy val productsPriceService = stub[GoodApplyTermsService]
        (productsPriceService
          .getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
            _: RequestContext
          ))
          .when(*, *, testActiveStart, *)
          .throwingT(new TestException)
        val result = decider
          .apply(testRequest)
          .success
          .value
          .action
        result should matchPattern { case NoAction(PriceExtractionError(_)) =>
        }
        (productsPriceService
          .getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
            _: RequestContext
          ))
          .verify(*, *, testActiveStart, *)
      }

      "failed to hold" in new DeciderMocking {
        override lazy val holdSource =
          whenHoldSource(Failure(testException), testProduct, testGoodPrice)
        decider.apply(testRequest) should matchPattern(successNoActionPf)
      }

      "failed to get activated turbo for special" in new DeciderMocking {
        override lazy val goodsDao = stub[GoodsDao]
        val e = new TestException
        (goodsDao.get _)
          .when(
            ForOfferActivatedProducts(
              testOfferId,
              OfferCategories.Cars,
              NonEmptySet
                .one(ProductId.Turbo: ProductId)(cats.Order.fromOrdering)
            )
          )
          .throwingT(e)
        decider
          .apply(specialRequest)
          .success
          .value
          .action match {
          case action: NoAction =>
            action.reason shouldBe GetActivePackagesError(e)
          case action => fail(s"expected NoAction, got $action")
        }
      }

      "failed to generate hold transaction id" in {
        // with current implementation we can't check transactionId generation failure
      }

      "failed to set deadline to OfferBilling" in {
        // with current implementation we can't check setActiveDeadline failure
      }

      "failed to set timestamp to OfferBilling" in {
        // with current implementation we can't check setTimestamp failure
      }

      "failed to build OfferBilling" in {
        // with current implementation we can't check OfferBilling build failure
      }

      "deadline for good is undefined" in {
        // goods' deadlines are defined in ru.auto.salesman.model.TermOfGoods
      }

      "failed to get promocode feature" in new DeciderMocking {
        override lazy val promocoderSource =
          whenPromocoderSource(ZIO.fail(testException))
        override lazy val categorizedGoodsDao = stub[GoodsDao]
        (categorizedGoodsDao.get _).when(*).returningT(Nil)
        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]
        value.action shouldBe an[NoAction]
      }
    }

    "return Deactivate" when {
      def verifyDeactivatePf(
          reason: DeactivateReason,
          request: GoodsDecider.Request = testRequest,
          offerStatus: OfferStatus = OfferStatuses.Expired
      ): PartialFunction[Any, _] = {
        case Success(
              DeciderResponse(
                Deactivate(
                  `reason`,
                  Some(`offerStatus`),
                  /* deactivateOtherGoods = */ true
                ),
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None
              )
            ) =>
      }

      "no single ads request" in new DeciderMocking {
        override lazy val adsRequestDao =
          whenAdsRequestDao(testClientId, testAdsRequestType)(Success(None))
        val result =
          decider.apply(testRequest)
        result should matchPattern(
          verifyDeactivatePf(AdsRequestForbidden, offerStatus = Hidden)
        )
      }

      "client is not active" in new DeciderMocking {
        val client = testClient.copy(isActive = false)
        override lazy val clientSource = whenClientSource(Task.succeed(client))
        val result =
          decider.apply(testRequest)
        result should matchPattern(verifyDeactivatePf(InactiveClient))
      }

      "hold return NotEnoughFunds" in new DeciderMocking {
        override lazy val holdSource =
          whenHoldSource(HoldStates.NotEnoughFunds, testProduct)
        val result =
          decider.apply(testRequest)
        result should matchPattern(verifyDeactivatePf(NotEnoughFunds))
      }

      "got already active Fresh" in new DeciderMocking {
        val request = testRequest.copy(
          context = ProductContext(ProductId.Fresh),
          offerBillingDeadline = Some(testActivateDate.plusHours(24))
        )
        val result = decider.apply(request)
        result should matchPattern(
          verifyDeactivatePf(DeadlineExpired(ProductId.Fresh), request)
        )
      }

      "got already active Reset" in new DeciderMocking {
        val request = testRequest.copy(
          context = ProductContext(ProductId.Reset),
          offerBillingDeadline = Some(testActivateDate.plusHours(24))
        )
        val result = decider.apply(request)
        result should matchPattern(
          verifyDeactivatePf(DeadlineExpired(ProductId.Reset), request)
        )
      }

      "NotEnoughFunds and no promocode feature" in new DeciderMocking {
        override lazy val promocoderSource =
          whenPromocoderSource(List.empty[FeatureInstance])

        override lazy val holdSource =
          whenHoldSource(HoldStates.NotEnoughFunds, testProduct)
        val result =
          decider.apply(testRequest)
        result should matchPattern(verifyDeactivatePf(NotEnoughFunds))
      }

      "placement model == calls && calls campaign is inactive" in new DeciderMocking {
        override lazy val clientSource = whenClientSource(testCallsClient)
        override lazy val billingService =
          whenGetCallCampaign(Some(manuallyDisabledCallCampaign))
        val result =
          decider
            .apply(testCallsRequest)
            .success
            .value
            .action
        result shouldBe Deactivate(
          InactiveCallCampaign(MANUALLY_DISABLED),
          offerStatusPatch = Some(Hidden)
        )
      }

      "not allowed activate turbo for region" in new DeciderMocking {
        override lazy val clientSource =
          whenClientSource(testClient.copy(regionId = testRegionId))

        override lazy val featureService = whenFeatureService(Set(testRegionId))

        val expected =
          Deactivate(
            ProductNotAllowedForRegion(turboRequest.context.product, testRegionId),
            offerStatusPatch = None,
            deactivateOtherGoods = false
          )

        decider
          .apply(turboRequest)
          .success
          .value
          .action shouldEqual expected
      }

      "not allowed activate fresh for region" in new DeciderMocking {
        override lazy val clientSource =
          whenClientSource(testClient.copy(regionId = testRegionId))

        override lazy val featureService = whenFeatureService(Set(testRegionId))

        val expected =
          Deactivate(
            ProductNotAllowedForRegion(freshRequest.context.product, testRegionId),
            offerStatusPatch = None,
            deactivateOtherGoods = false
          )

        decider
          .apply(freshRequest)
          .success
          .value
          .action shouldEqual expected
      }
    }

    "return Activate" when {
      // данный интервал времени нужен исключительно чтобы тесты в teamCity не факапили
      // изначально была 1 минута но в teamCity не успевало там выполнялось за 107 секунд
      // поэтому увеличил изначально с запасом
      // +- 3.minute, чтобы тест не флапал.
      val intervalTimeForTest = 3.minute.toMillis

      "everything goes well" in new DeciderMocking {
        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"unexpected $other, expected Activate")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)
        knownCampaign.getHold should
          be(
            HoldUtils
              .getHoldId(
                testOfferId,
                testOfferCategory.protoParent,
                testProduct,
                None,
                testActiveStart
              )
              .get
          )

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe testCampaignHeader.getId
        header.getName shouldBe testCampaignHeader.getName
        header.getOrder shouldBe testCampaignHeader.getOrder
        header.getOwner shouldBe testCampaignHeader.getOwner
        header.getSettings shouldBe testCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(testProduct)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true

        val perIndexing = cost.getPerIndexing
        perIndexing.getUnits shouldBe testGoodPrice
        perIndexing.hasConstraints shouldBe true // todo not sure if it is correct

        offerBilling.getPayloadMap.asScala("offerId") shouldBe testOffer.getId
      }

      "there is no custom price in request and everything goes well" in new DeciderMocking {
        val value: GoodsDecider.Response =
          decider
            .apply(testRequest)
            .success
            .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)
        knownCampaign.getHold should
          be(
            HoldUtils
              .getHoldId(
                testOfferId,
                testOfferCategory.protoParent,
                testProduct,
                None,
                testActiveStart
              )
              .get
          )

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe testCampaignHeader.getId
        header.getName shouldBe testCampaignHeader.getName
        header.getOrder shouldBe testCampaignHeader.getOrder
        header.getOwner shouldBe testCampaignHeader.getOwner
        header.getSettings shouldBe testCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(testProduct)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true

        val perIndexing = cost.getPerIndexing
        perIndexing.getUnits shouldBe testGoodPrice
        perIndexing.hasConstraints shouldBe true // todo not sure if it is correct
      }

      "campaign should be created synchronously and everything goes well" in new DeciderMocking {
        val source = CampaignSource(
          "salesman",
          testBalanceClientId,
          testBalanceAgencyId,
          testAccountId,
          testProduct,
          None
        )
        override val campaignCreationService =
          stub[CampaignCreationService]
        (campaignCreationService.getOrCreate _)
          .when(source)
          .returningT(testCampaignHeader)
        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)
        knownCampaign.getHold should
          be(
            HoldUtils
              .getHoldId(
                testOfferId,
                testOfferCategory.protoParent,
                testProduct,
                None,
                testActiveStart
              )
              .get
          )

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe testCampaignHeader.getId
        header.getName shouldBe testCampaignHeader.getName
        header.getOrder shouldBe testCampaignHeader.getOrder
        header.getOwner shouldBe testCampaignHeader.getOwner
        header.getSettings shouldBe testCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(testProduct)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true

        val perIndexing = cost.getPerIndexing
        perIndexing.getUnits shouldBe testGoodPrice
        perIndexing.hasConstraints shouldBe true // todo not sure if it is correct

        (campaignCreationService.getOrCreate _).verify(source)
      }

      "everything goes well for premium product for cars new goods" in new DeciderMocking {
        val testPremiumProduct = ProductId.Premium
        val premiumCampaignHeader =
          campaignHeaderBuilder(testPremiumProduct).build()
        val request = GoodsDecider.Request(
          testClientId,
          testOffer.toBuilder.setSection(Section.NEW).build(),
          ProductContext(testPremiumProduct),
          testActivateDate,
          None
        )

        override val campaignCreationService =
          whenCampaignCreationService(premiumCampaignHeader, testPremiumProduct)
        override lazy val holdSource =
          whenHoldSource(HoldStates.Ok, testPremiumProduct, testGoodPrice)
        override lazy val promocoderSource =
          whenPromocoderSource(List.empty[FeatureInstance])

        val value = decider
          .apply(request)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)
        knownCampaign.getHold should
          be(
            HoldUtils
              .getHoldId(
                testOfferId,
                testOfferCategory.protoParent,
                request.product,
                None,
                testActiveStart
              )
              .get
          )

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe premiumCampaignHeader.getId
        header.getName shouldBe premiumCampaignHeader.getName
        header.getOrder shouldBe premiumCampaignHeader.getOrder
        header.getOwner shouldBe premiumCampaignHeader.getOwner
        header.getSettings shouldBe premiumCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(request.product)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true

        val perIndexing = cost.getPerIndexing
        perIndexing.getUnits shouldBe testGoodPrice
        perIndexing.hasConstraints shouldBe true // todo not sure if it is correct
      }

      "everything goes well with zero price: no holding & promocode requesting" in new DeciderMocking {
        val testPremiumProduct = ProductId.Premium
        val premiumCampaignHeader =
          campaignHeaderBuilder(testPremiumProduct).build()
        val request = GoodsDecider.Request(
          testClientId,
          testOffer.toBuilder.setSection(Section.NEW).build(),
          ProductContext(testPremiumProduct),
          testActivateDate,
          None
        )

        override val campaignCreationService =
          whenCampaignCreationService(premiumCampaignHeader, testPremiumProduct)
        override lazy val productsPriceService = stub[GoodApplyTermsService]
        (productsPriceService
          .shouldIgnoreDeliveryRegions(
            _: ClientId,
            _: ProductId,
            _: ApiOfferModel.Offer
          ))
          .when(*, *, *)
          .returningT(false)
        (productsPriceService
          .getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
            _: RequestContext
          ))
          .when(*, *, testActiveStart, *)
          .returningT(GoodApplyTerms.zeroPriceUntil(testDeadline))

        val value = decider
          .apply(request)
          .success
          .value

        value shouldBe an[DeciderResponse]

        value.action match {
          case _: Activate => ()
          case other => fail(s"expected activate, got $other")
        }

        value.price.value shouldBe 0

        (holdSource.hold _).verify(*, *, *, *, *, *).never()
        (promocoderSource.getFeaturesForUser _)
          .verify(*)
          .never()
      }

      "with promocode feature" in new DeciderMocking {
        val featureId = s"$testProduct:promo_salesman-test:96eb92e69602f216"
        val featureTag = ProductId.alias(testProduct)
        val featurePayload = FeaturePayload(FeatureUnits.Items)
        val featureCount = FeatureCount(10L, FeatureUnits.Items)
        val origin = FeatureOrigin("f_orig")
        val feature = FeatureInstance(
          featureId,
          origin,
          featureTag,
          testUser.toString,
          featureCount,
          now(),
          now.plusDays(2),
          featurePayload
        )

        override lazy val promocoderSource = whenPromocoderSource(List(feature))
        override lazy val promocoderFeatureService = stub[PromocoderFeatureService]
        (promocoderFeatureService
          .modifyPrice(
            _: List[FeatureInstance],
            _: ProductId,
            _: Funds,
            _: Long,
            _: Option[ApiOfferModel.Offer],
            _: Option[LoyaltyArgs]
          ))
          .when(*, *, *, *, *, *)
          .returningZ(
            ModifiedPrice(
              0L,
              List(
                PriceModifierFeature(
                  feature,
                  FeatureCount(1L, FeatureUnits.Items),
                  testGoodPrice
                )
              )
            )
          )

        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)
        knownCampaign.getHold shouldBe empty

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe testCampaignHeader.getId
        header.getName shouldBe testCampaignHeader.getName
        header.getOrder shouldBe testCampaignHeader.getOrder
        header.getOwner shouldBe testCampaignHeader.getOwner
        header.getSettings shouldBe testCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(testProduct)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true

        val perIndexing = cost.getPerIndexing
        perIndexing.getUnits shouldBe 0
        perIndexing.hasConstraints shouldBe true // todo not sure if it is correct
      }

      "with discount feature" in new DeciderMocking {
        val featureId = s"$testProduct:promo_salesman-test:96eb92e69602f216"
        val featureTag = ProductId.alias(testProduct)
        val featurePayload = FeaturePayload(
          FeatureUnits.Items,
          discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 50))
        )
        val featureCount = FeatureCount(10L, FeatureUnits.Items)
        val origin = FeatureOrigin("f_orig")
        val feature = FeatureInstance(
          featureId,
          origin,
          featureTag,
          testUser.toString,
          featureCount,
          now(),
          now.plusDays(2),
          featurePayload
        )
        val priceWithDiscount =
          PriceService.priceWithDiscount(testGoodPrice, 50)

        override lazy val promocoderSource = whenPromocoderSource(List(feature))
        override lazy val promocoderFeatureService = stub[PromocoderFeatureService]
        (promocoderFeatureService
          .modifyPrice(
            _: List[FeatureInstance],
            _: ProductId,
            _: Funds,
            _: Long,
            _: Option[ApiOfferModel.Offer],
            _: Option[LoyaltyArgs]
          ))
          .when(*, *, *, *, *, *)
          .returningZ(
            ModifiedPrice(
              priceWithDiscount,
              List(
                PriceModifierFeature(
                  feature,
                  FeatureCount(1L, FeatureUnits.Items),
                  testGoodPrice - priceWithDiscount
                )
              )
            )
          )

        override lazy val holdSource =
          whenHoldSource(HoldStates.Ok, testProduct, priceWithDiscount)

        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)

        knownCampaign.getHold should not be empty

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe testCampaignHeader.getId
        header.getName shouldBe testCampaignHeader.getName
        header.getOrder shouldBe testCampaignHeader.getOrder
        header.getOwner shouldBe testCampaignHeader.getOwner
        header.getSettings shouldBe testCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(testProduct)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true
      }

      "with promocode and discount features" in new DeciderMocking {
        val featureId = s"$testProduct:promo_salesman-test:96eb92e69602f216"
        val featureTag = ProductId.alias(testProduct)
        val featurePayload = FeaturePayload(FeatureUnits.Items)
        val featureCount = FeatureCount(10L, FeatureUnits.Items)
        val origin = FeatureOrigin("f_orig")
        val feature = FeatureInstance(
          featureId,
          origin,
          featureTag,
          testUser.toString,
          featureCount,
          now(),
          now.plusDays(2),
          featurePayload
        )
        val discountFeature = feature.copy(
          id = feature + "2",
          payload = FeaturePayload(
            FeatureUnits.Items,
            discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 50))
          )
        )
        val priceWithDiscount =
          PriceService.priceWithDiscount(testGoodPrice, 50)

        override lazy val promocoderSource =
          whenPromocoderSource(List(feature, discountFeature))
        override lazy val promocoderFeatureService = stub[PromocoderFeatureService]
        (promocoderFeatureService
          .modifyPrice(
            _: List[FeatureInstance],
            _: ProductId,
            _: Funds,
            _: Long,
            _: Option[ApiOfferModel.Offer],
            _: Option[LoyaltyArgs]
          ))
          .when(*, *, *, *, *, *)
          .returningZ(
            ModifiedPrice(
              0L,
              List(
                PriceModifierFeature(
                  discountFeature,
                  FeatureCount(1L, FeatureUnits.Items),
                  priceWithDiscount
                ),
                PriceModifierFeature(
                  feature,
                  FeatureCount(1L, FeatureUnits.Items),
                  testGoodPrice - priceWithDiscount
                )
              )
            )
          )

        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val activate = value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }

        val offerBilling = activate.offerBilling
        offerBilling.hasKnownCampaign shouldBe true

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getIsActive shouldBe true
        knownCampaign.getActiveDeadline shouldBe testDeadline.getMillis
        // Оплата должна была пройти вчера (см. testRequest), но по факту
        // происходит сегодня. В таком случае OfferBilling.timestamp
        // проставляем текущей датой (см. DeciderUtils.getBillingTimestamp).
        // +- intervalTimeForTest, чтобы тест не флапал.
        val millis = now().getMillis
        offerBilling.getTimestamp shouldBe (millis +- intervalTimeForTest)
        knownCampaign.getActiveStart should be(millis +- intervalTimeForTest)
        knownCampaign.getHold shouldBe empty

        knownCampaign.hasCampaign shouldBe true

        val header = knownCampaign.getCampaign
        header.getId shouldBe testCampaignHeader.getId
        header.getName shouldBe testCampaignHeader.getName
        header.getOrder shouldBe testCampaignHeader.getOrder
        header.getOwner shouldBe testCampaignHeader.getOwner
        header.getSettings shouldBe testCampaignHeader.getSettings
        header.hasProduct shouldBe true

        val product = header.getProduct
        val goods = product.getGoodsList.asScala.toSet
        goods should have size 1

        val good = goods.head
        good.hasCustom shouldBe true

        val custom = good.getCustom
        custom.getId shouldBe ProductId.alias(testProduct)
        custom.hasCost shouldBe true

        val cost = custom.getCost
        cost.hasPerIndexing shouldBe true

        val perIndexing = cost.getPerIndexing
        perIndexing.getUnits shouldBe 0
        perIndexing.hasConstraints shouldBe true // todo not sure if it is correct
      }

      "without badges Placement" in new DeciderMocking {
        override lazy val badgeDao =
          whenBadgeDao(Some("some-badges-tag"))

        val value = decider
          .apply(testRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val nextTimestamp =
          DeciderUtils.getCurrentActivateDate(
            testRequest.firstActivateDate,
            None
          )
        val nextTimestampVal = nextTimestamp.getMillis / 1000

        val expectedHoldId =
          s"$testOfferId:${ProductId.alias(testProduct)}:$nextTimestampVal"
        value.holdId shouldBe Some(expectedHoldId)

        value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }
      }

      "without badges Badges" in new DeciderMocking {
        val testProduct = ProductId.Badge
        val testHeader = campaignHeaderBuilder(testProduct).build()

        override val campaignCreationService =
          whenCampaignCreationService(testHeader, testProduct)
        override lazy val holdSource =
          whenHoldSource(HoldStates.Ok, testProduct, testGoodPrice)

        val request =
          testRequest.copy(context = BadgeContext(testGoodsId))
        val value = decider
          .apply(request)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val nextTimestamp =
          DeciderUtils.getCurrentActivateDate(
            testRequest.firstActivateDate,
            None
          )
        val nextTimestampVal = nextTimestamp.getMillis / 1000

        val expectedHoldId =
          s"$testOfferId:${ProductId.alias(testProduct)}:$nextTimestampVal"
        value.holdId shouldBe Some(expectedHoldId)

        value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }
      }

      "with badges Badges" in new DeciderMocking {
        val testProduct = ProductId.Badge
        val testHeader = campaignHeaderBuilder(testProduct).build()

        override val campaignCreationService =
          whenCampaignCreationService(testHeader, testProduct)
        override lazy val holdSource =
          whenHoldSource(HoldStates.Ok, testProduct, price = testGoodPrice)
        override lazy val badgeDao = whenBadgeDao(Some("some-badge"))

        val value = decider
          .apply(badgeRequest)
          .success
          .value

        value shouldBe an[DeciderResponse]

        val nextTimestamp =
          DeciderUtils.getCurrentActivateDate(
            testRequest.firstActivateDate,
            None
          )
        val nextTimestampVal = nextTimestamp.getMillis / 1000

        val expectedHoldHash = "15a25357f5ff642da2e690c0c1dbaaaa"
        val expectedHoldId =
          s"$testOfferId:${ProductId.alias(testProduct)}:$expectedHoldHash:$nextTimestampVal"
        value.holdId shouldBe Some(expectedHoldId)

        value.action match {
          case a: Activate => a
          case other => fail(s"expected activate, got $other")
        }
      }
    }

    "correctly process activateDate from 'future'" in new DeciderMocking {
      val otherActivateDate = FirstActivateDate(now().plusMinutes(1))
      override lazy val productsPriceService = stub[GoodApplyTermsService]
      (productsPriceService
        .getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
          _: RequestContext
        ))
        .when(*, *, ActivateDate(otherActivateDate.asDateTime), *)
        .returningT(
          GoodApplyTerms(
            testGoodPrice,
            deliveryRegionsPrice = ZeroPrice,
            goodDeadline = otherActivateDate + 1.day
          )
        )
      (productsPriceService
        .shouldIgnoreDeliveryRegions(
          _: ClientId,
          _: ProductId,
          _: ApiOfferModel.Offer
        ))
        .when(*, *, *)
        .returningT(false)
      val otherRequest =
        testRequest.copy(firstActivateDate = otherActivateDate)
      val value = decider
        .apply(otherRequest)
        .success
        .value

      value.action match {
        case activate: Activate =>
          val offerBilling = activate.offerBilling
          offerBilling.getTimestamp shouldBe otherActivateDate.getMillis
          offerBilling.getKnownCampaign.getActiveStart shouldBe otherActivateDate.getMillis
          val otherDeadline = otherActivateDate + testProductLifetime
          offerBilling.getKnownCampaign.getActiveDeadline shouldBe otherDeadline.getMillis
        case other => fail(s"expected activate, got $other")
      }
    }

    "handle activation in the same day" in new DeciderMocking {
      val otherActivateDate = FirstActivateDate(
        now().withTimeAtStartOfDay().plusHours(1)
      ) // 01:00 today
      override lazy val productsPriceService = stub[GoodApplyTermsService]
      (productsPriceService
        .getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
          _: RequestContext
        ))
        .when(*, *, *, *)
        .returningT(GoodApplyTerms(testGoodPrice, 0, otherActivateDate + 1.day))
      (productsPriceService
        .shouldIgnoreDeliveryRegions(
          _: ClientId,
          _: ProductId,
          _: ApiOfferModel.Offer
        ))
        .when(*, *, *)
        .returningT(false)
      val otherRequest = testRequest.copy(firstActivateDate = otherActivateDate)
      val value = decider
        .apply(otherRequest)
        .success
        .value

      value.action match {
        case activate: Activate =>
          activate.activateDate.asDateTime shouldEqual otherActivateDate.asDateTime
          activate.deadline shouldEqual otherActivateDate.plusDays(1)
        case other => fail(s"expected activate, got $other")
      }
    }

    "handle activation without ads request check for SingleWithCalls payment model" in new DeciderMocking {
      val otherRequest = testRequest.copy(offer =
        testRequest.offer.toBuilder.setSection(Section.USED).build()
      )

      override lazy val billingService: BillingService = whenGetCallCarsUsedCampaign(
        Some(activeCallCarsUsedCampaign)
      )

      override lazy val adsRequestDao = neverAdsRequestDao()

      override lazy val clientSource =
        //mocked regionId for singleWithCalls payment model
        whenClientSource(testClient.copy(regionId = RegionId(3228)))

      val value = decider
        .apply(otherRequest)
        .success
        .value

      value.action.isInstanceOf[Activate] shouldBe true
    }

    "ignore not allowed products for SingleWithCalls payment model" in new DeciderMocking {
      val topRequest = testRequest.copy(
        offer = testRequest.offer.toBuilder.setSection(Section.USED).build(),
        context = ProductContext(ProductId.Top)
      )

      override lazy val adsRequestDao = neverAdsRequestDao()

      override lazy val clientSource =
        //mocked regionId for singleWithCalls payment model
        whenClientSource(testClient.copy(regionId = RegionId(3228)))

      val expected =
        Deactivate(
          ProductNotAllowedForPaymentModel(ProductId.Top, SingleWithCalls),
          offerStatusPatch = None,
          deactivateOtherGoods = false
        )

      decider
        .apply(topRequest)
        .success
        .value
        .action shouldEqual expected
    }

    "handle activation after midnight" in new DeciderMocking {
      val otherActivateDate = FirstActivateDate(
        now().withTimeAtStartOfDay().minusMinutes(1)
      ) // 23:59 previous day
      override lazy val productsPriceService = stub[GoodApplyTermsService]
      (productsPriceService
        .getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
          _: RequestContext
        ))
        .when(*, *, *, *)
        .returningT(GoodApplyTerms(testGoodPrice, 0, otherActivateDate + 1.day))
      (productsPriceService
        .shouldIgnoreDeliveryRegions(
          _: ClientId,
          _: ProductId,
          _: ApiOfferModel.Offer
        ))
        .when(*, *, *)
        .returningT(false)
      val otherRequest = testRequest.copy(firstActivateDate = otherActivateDate)
      val value = decider
        .apply(otherRequest)
        .success
        .value

      value.action match {
        case activate: Activate =>
          activate.activateDate.asDateTime shouldEqual otherActivateDate.asDateTime
          activate.deadline shouldEqual otherActivateDate.plusDays(1)
        case other => fail(s"expected activate, got $other")
      }
    }
  }

}

//noinspection TypeAnnotation
object GoodsDeciderImplSpec {

  import scala.concurrent.duration._

  // +10 minutes adds ability to write off money and start activation yesterday
  val yesterdayTime: DateTime = now().minusHours(24).plusMinutes(10)
  val testClientId: ClientId = 1
  val testAgencyId: Option[AgencyId] = None
  val testOfferId: OfferId = 2
  val testOfferCategory = Cars
  val testOfferSection = USED

  val testOfferPriceRur = 1000000
  val TestOfferPriceKop = 100000000L

  val testOffer =
    offerGen(
      autoruOfferIdGen(testOfferId),
      Category.CARS,
      offerSectionGen = testOfferSection,
      PriceInfoGen = priceInfoGen(testOfferPriceRur.toDouble, OfferCurrencies.RUR)
    ).next

  val deliveryRegionsCount =
    testOffer.getDeliveryInfo.getDeliveryRegionsList.size()

  val testCallsOffer: ApiOfferModel.Offer =
    testOffer.toBuilder.setSection(NEW).build()

  val testOfferPriceUsd = 12500

  val testOfferUsd =
    testOffer.toBuilder
      .setPriceInfo(
        priceInfoGen(testOfferPriceUsd.toDouble, OfferCurrencies.USD).next
      )
      .build()

  val testOfferPriceEur = 10000

  val testOfferEur =
    testOffer.toBuilder
      .setPriceInfo(
        priceInfoGen(testOfferPriceEur.toDouble, OfferCurrencies.EUR).next
      )
      .build()

  val testProduct: ProductId = ProductId.Placement
  val testBalanceClientId: BalanceClientId = 3
  val testBalanceAgencyId: Option[BalanceClientId] = None
  val testCategorizedClientId = Some(123L)
  val testAccountId: AccountId = 4
  val testAmount: Funds = 10000
  val testActivateDate: FirstActivateDate = FirstActivateDate(yesterdayTime)
  val testProductLifetime = 24.hours
  val testActiveStart: ActivateDate = ActivateDate(yesterdayTime)
  val testDeadline: DateTime = yesterdayTime.plusHours(24)
  val testUser = PromocoderUser(testClientId, UserTypes.ClientUser)
  val testGoodsId: GoodsId = 1L

  val testGoodPrice: Funds = 2000

  val testException = new Exception
  val today = DateTimeInterval.currentDay

  val testRequest = GoodsDecider.Request(
    testClientId,
    testOffer,
    ProductContext(testProduct),
    testActivateDate,
    None
  )

  val badgeRequest = GoodsDecider.Request(
    testClientId,
    testOffer,
    BadgeContext(testGoodsId),
    testActivateDate,
    offerBillingDeadline = None
  )

  val testCallsRequest =
    testRequest.copy(
      context = ProductContext(ProductId.Premium),
      offer = testCallsOffer
    )

  val specialRequest = testRequest.copy(context = ProductContext(ProductId.Special))

  val freshRequest = testRequest.copy(context = ProductContext(ProductId.Fresh))

  val turboRequest = testRequest.copy(context = ProductContext(ProductId.Turbo))

  private def campaignHeaderBuilder(productId: ProductId) = {
    val customerId = CustomerId
      .newBuilder()
      .setVersion(Versions.CUSTOMER_ID)
      .setClientId(testBalanceClientId)
    val owner = CustomerHeader
      .newBuilder()
      .setVersion(Versions.CUSTOMER_HEADER)
      .setId(customerId)
    testBalanceAgencyId.foreach(customerId.setAgencyId)
    val order = Order
      .newBuilder()
      .setVersion(Versions.ORDER)
      .setId(22)
      .setOwner(customerId)
      .setText(" ")
      .setCommitAmount(testAmount)
      .setApproximateAmount(testAmount)
    val good = {
      val constraints = Constraints
        .newBuilder()
        .setCostType(CostType.COSTPERINDEXING)
      val perIndexing = PerIndexing
        .newBuilder()
        .setConstraints(constraints)
      val cost = Cost
        .newBuilder()
        .setVersion(Versions.COST)
        .setPerIndexing(perIndexing)
      val custom = Custom
        .newBuilder()
        .setCost(cost)
        .setId(ProductId.alias(productId))
      Good
        .newBuilder()
        .setVersion(Versions.GOOD)
        .setCustom(custom)
    }
    val product = Model.Product
      .newBuilder()
      .setVersion(Versions.PRODUCT)
      .addGoods(good)
    val settings = CampaignSettings
      .newBuilder()
      .setVersion(Versions.CAMPAIGN_SETTINGS)
      .setIsEnabled(true)
    CampaignHeader
      .newBuilder()
      .setVersion(Versions.CAMPAIGN_HEADER)
      .setId(randomAlphanumericString(5))
      .setOrder(order)
      .setProduct(product)
      .setSettings(settings)
      .setOwner(owner)
  }

  val testRegionId: RegionId = RegionId(1)
  val testCityId: CityId = CityId(1123L)
  val testIsActive: Boolean = true
  val testIsReadyForNewBilling = true
  val testSinglePayment = Set(AdsRequestTypes.CarsUsed)

  val testClient = DetailedClient(
    testClientId,
    testAgencyId,
    testBalanceClientId,
    testBalanceAgencyId,
    testCategorizedClientId,
    None,
    testRegionId,
    testCityId,
    testAccountId,
    testIsActive,
    firstModerated = true,
    testSinglePayment
  )

  val testAccount = testClient.accountId
  val testProductKey = testClient.productKey

  val testCampaignSource = CampaignSource(
    AutoruUserId,
    testBalanceClientId,
    testBalanceAgencyId,
    testAccount,
    testProduct,
    testProductKey
  )

  val testCallsClient = testClient.copy(paidCallsAvailable = true)

  val testAdsRequestType = AdsRequestTypes.CarsUsed
  val testAdsRequest = AdsRequestDao.Record(testClientId, testAdsRequestType)

  val activeCallCampaign: CampaignHeader =
    campaignHeaderGen(inactiveReasonGen = None).next

  val activeCallCarsUsedCampaign: CampaignHeader =
    campaignHeaderGen(inactiveReasonGen = None).next

  val manuallyDisabledCallCampaign: CampaignHeader =
    campaignHeaderGen(inactiveReasonGen = Some(MANUALLY_DISABLED)).next

  trait DeciderMocking extends MockFactory with ScalamockCallHandlers {
    val testCampaignHeader = campaignHeaderBuilder(testProduct).build()

    val campaignCreationService: CampaignCreationService = { (_: CampaignSource) =>
      Try(testCampaignHeader)
    }

    lazy val clientSource = whenClientSource(testClient)

    lazy val holdSource = whenHoldSource(HoldStates.Ok, testProduct)

    lazy val promocoderSource = whenPromocoderSource(List.empty)

    lazy val badgeDao = whenBadgeDao(None)

    lazy val adsRequestDao =
      whenAdsRequestDao(testClientId, testAdsRequestType)(
        Success(Some(testAdsRequest))
      )
    lazy val billingService = stub[BillingService]

    lazy val goodsDao = {
      val s = stub[GoodsDao]
      (s.get _).when(*).returningT(Nil)
      s
    }
    lazy val categorizedGoodsDao = stub[GoodsDao]

    lazy val productsPriceService = {
      val s = stub[GoodApplyTermsService]
      (s.shouldIgnoreDeliveryRegions(
        _: ClientId,
        _: ProductId,
        _: ApiOfferModel.Offer
      )).when(*, *, *)
        .returningT(false)
      (s.getTerms(_: GoodsDecider.Request, _: RegionId, _: ActivateDate)(
        _: RequestContext
      )).when(*, *, testActiveStart, *)
        .returningT(
          GoodApplyTerms(
            testGoodPrice,
            deliveryRegionsPrice = ZeroPrice,
            goodDeadline = testDeadline
          )
        )
      s
    }

    lazy val promocoderFeatureService = {
      val s = stub[PromocoderFeatureService]
      (s.modifyPrice(
        _: List[FeatureInstance],
        _: ProductId,
        _: Funds,
        _: Long,
        _: Option[ApiOfferModel.Offer],
        _: Option[LoyaltyArgs]
      )).when(*, *, *, *, *, *)
        .returningZ(ModifiedPrice(testGoodPrice, List.empty))
      s
    }

    lazy val paymentModelFactory =
      TestPaymentModelFactory.withMockRegions(Set(RegionId(3228)))

    lazy val goodsDaoProvider =
      new GoodsDaoProviderImpl(goodsDao, categorizedGoodsDao)

    lazy val productService =
      new ProductServiceImpl(
        billingService,
        goodsDaoProvider,
        paymentModelFactory
      )

    lazy val featureService = whenFeatureService(Set.empty[RegionId])

    lazy val decider =
      new GoodsDeciderImpl(
        clientSource,
        holdSource,
        promocoderSource,
        badgeDao,
        adsRequestDao,
        productsPriceService,
        productService,
        promocoderFeatureService,
        campaignCreationService,
        paymentModelFactory,
        featureService
      )

    protected def whenClientSource(
        response: Task[DetailedClient]
    ): DetailedClientSource = {
      val clientSource = stub[DetailedClientSource]
      (clientSource.unsafeResolve _)
        .when(testClientId, /* withDeleted = */ false)
        .returning(response)
      clientSource
    }

    protected def whenClientSource(
        client: DetailedClient
    ): DetailedClientSource =
      whenClientSource(Task.succeed(client))

    protected def whenGetCallCampaign(
        campaign: Option[CampaignHeader]
    ): BillingService = {
      val billingService = stub[BillingService]
      (billingService.getCallCampaign _).when(*).returningZ(campaign)
      billingService
    }

    protected def whenGetCallCarsUsedCampaign(
        campaign: Option[CampaignHeader]
    ): BillingService = {
      val billingService = stub[BillingService]
      (billingService.getProductCampaign _)
        .when(*, ProductId.CallCarsUsed)
        .returningZ(campaign)
      billingService
    }

    protected def whenCampaignCreationService(
        response: Try[CampaignHeader],
        product: ProductId
    ): CampaignCreationService = {
      val campaignCreationService = stub[CampaignCreationService]
      val source = CampaignSource(
        AutoruUserId,
        testBalanceClientId,
        testBalanceAgencyId,
        testAccountId,
        product
      )

      (campaignCreationService.getOrCreate _)
        .when(source)
        .returning(response)
      campaignCreationService
    }

    protected def whenCampaignCreationService(
        campaign: CampaignHeader,
        product: ProductId
    ): CampaignCreationService =
      whenCampaignCreationService(Success(campaign), product)

    protected def whenHoldSource(
        response: Try[HoldState],
        product: ProductId,
        price: Long
    ): HoldSource = {
      // todo use * because transactionId may be generated inside, should test for it
      val holdSource = stub[HoldSource]
      (holdSource.hold _)
        .when(
          testBalanceClientId,
          testBalanceAgencyId,
          *,
          product,
          testAccountId,
          price
        )
        .returning(response)
      holdSource
    }

    protected def whenHoldSource(
        holdState: HoldState,
        product: ProductId,
        price: Long = testGoodPrice
    ): HoldSource =
      whenHoldSource(Success(holdState), product, price)

    protected def whenPromocoderSource(
        response: Task[List[FeatureInstance]]
    ): PromocoderSource = {
      val promocoderSource = stub[PromocoderSource]
      (promocoderSource.getFeaturesForUser _)
        .when(testUser)
        .returning(response)
      promocoderSource
    }

    protected def whenPromocoderSource(
        features: List[FeatureInstance]
    ): PromocoderSource =
      whenPromocoderSource(ZIO.succeed(features))

    protected def whenBadgeDao(response: Option[String]): BadgeDao = {
      val s = stub[BadgeDao]
      (s.getBadge _)
        .when(testGoodsId, testOfferId, testOfferCategory)
        .returningZ(response)
      s
    }

    protected def whenAdsRequestDao(
        clientId: ClientId,
        requestType: AdsRequestType
    )(response: Try[Option[AdsRequestDao.Record]]): AdsRequestDao = {
      val adsRequestDao = stub[AdsRequestDao]
      (adsRequestDao.get _)
        .when(clientId, requestType)
        .returning(response)
      adsRequestDao
    }

    protected def neverAdsRequestDao(): AdsRequestDao = {
      val adsRequestDao = stub[AdsRequestDao]
      (adsRequestDao.get _)
        .when(*, *)
        .never()
      adsRequestDao
    }

    protected def whenFeatureService(regionIds: Set[RegionId]) = {
      val s = stub[DealerFeatureService]
      (s.regionsDealerDisabledTurbo _).when().returning(regionIds)
      (s.regionsDealerDisabledBoostAndPremium _).when().returning(regionIds)
      s
    }

  }
}
