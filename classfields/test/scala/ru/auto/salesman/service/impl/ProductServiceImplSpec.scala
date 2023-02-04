package ru.auto.salesman.service.impl

import cats.data.{NonEmptyList, NonEmptySet}
import org.joda.time.DateTime
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Filter.ForOfferActivatedProducts
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.OfferStatuses._
import ru.auto.salesman.model.ProductId.{isQuota, Placement, QuotaPlacementCarsNew}
import ru.auto.salesman.model.{AdsRequestType, OfferCategories, ProductId}
import ru.auto.salesman.service.BillingService
import ru.auto.salesman.service.GoodsDecider.Action.{Deactivate, NoAction}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason._
import ru.auto.salesman.service.GoodsDecider.NoActionReason.{
  GetCallCarsUsedCampaignError,
  UnexpectedCallCampaignInactivityReason
}
import ru.auto.salesman.service.GoodsDecider.ProductContext
import ru.auto.salesman.service.impl.DeciderUtilsSpec._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.GoodRecordGen
import ru.auto.salesman.test.model.gens.{campaignHeaderGen, GoodsNameGen}
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.yandex.vertis.billing.Model.InactiveReason._
import ru.yandex.vertis.generators.ProducerProvider.asProducer

class ProductServiceImplSpec extends BaseSpec {

  private val billingService = mock[BillingService]
  private val goodsDao = mock[GoodsDao]
  private val categorizedGoodsDao = mock[GoodsDao]

  private val paymentModelFactory =
    TestPaymentModelFactory.withMockRegions(Set(regionWithCallCarsNew))

  private val goodsDaoProvider =
    new GoodsDaoProviderImpl(goodsDao, categorizedGoodsDao)

  private val productService =
    new ProductServiceImpl(
      billingService,
      goodsDaoProvider,
      paymentModelFactory
    )

  import productService.{checkActivationAllowed, checkActivationAllowedByOffer}

  "checkActivationAllowed" should {

    "return NoAction" when {

      "got quota" in {
        val request =
          testRequest.copy(context = ProductContext(QuotaPlacementCarsNew))
        checkActivationAllowed(request, testClient) should matchPattern(
          nonDestructiveDeactivatePf(ProlongUnavailable(QuotaPlacementCarsNew))
        )
      }

      "got calls placement && calls campaign is inactive due to unexpected reason" in {
        forAll(
          campaignHeaderGen(inactiveReasonGen = Some(UNKNOWN_INACTIVE_REASON))
        ) { inactiveCallCampaign =>
          (billingService.getCallCampaign _)
            .expects(testCallsPlacementClient)
            .returningZ(Some(inactiveCallCampaign))
          val result = checkActivationAllowed(
            testCallsPlacementRequest,
            testCallsPlacementClient
          ).e.left.value
          result shouldBe NoAction(
            UnexpectedCallCampaignInactivityReason(UNKNOWN_INACTIVE_REASON)
          )
        }
      }

      "got single with calls placement && error on call:cars:used campaign receiving" in {
        val exception = new RuntimeException("error")
        (billingService.getProductCampaign _)
          .expects(testSingleWithCallsPlacementClient, ProductId.CallCarsUsed)
          .throwingZ(exception)
        val result = checkActivationAllowed(
          testSingleWithCallsPlacementRequest,
          testSingleWithCallsPlacementClient
        ).e.left.value
        result shouldBe NoAction(GetCallCarsUsedCampaignError(exception))
      }
    }

    "return Deactivate" when {
      "got already active Fresh" in {
        val request = testRequest.copy(
          context = ProductContext(ProductId.Fresh),
          offerBillingDeadline = Some(now())
        )
        checkActivationAllowed(request, testClient) should
          matchPattern(deactivatePf(DeadlineExpired(ProductId.Fresh)))
      }

      "got already active Reset" in {
        val request = testRequest.copy(
          context = ProductContext(ProductId.Reset),
          offerBillingDeadline = Some(now())
        )
        checkActivationAllowed(request, testClient) should
          matchPattern(deactivatePf(DeadlineExpired(ProductId.Reset)))
      }

      "got already active Turbo" in {
        val request = testRequest.copy(
          context = ProductContext(ProductId.Turbo),
          offerBillingDeadline = Some(now())
        )
        checkActivationAllowed(request, testClient) should
          matchPattern(deactivatePf(DeadlineExpired(ProductId.Turbo)))
      }

      "placement model == calls && calls campaign not found" in {
        (billingService.getCallCampaign _)
          .expects(testCallsPlacementClient)
          .returningZ(None)
        val result = checkActivationAllowed(
          testCallsRequest,
          testCallsPlacementClient
        ).e.left.value
        result shouldBe Deactivate(
          NoCallCampaign,
          offerStatusPatch = Some(Hidden)
        )
      }

      "placement model == singleWithCalls && call:cars:used campaign not found" in {
        (billingService.getProductCampaign _)
          .expects(testSingleWithCallsPlacementClient, ProductId.CallCarsUsed)
          .returningZ(None)
        val result = checkActivationAllowed(
          testSingleWithCallsRequest,
          testSingleWithCallsPlacementClient
        ).e.left.value
        result shouldBe Deactivate(
          NoCallCarsUsedCampaign,
          offerStatusPatch = Some(Hidden)
        )
      }

      "got calls placement && calls campaign is manually disabled" in {
        forAll(campaignHeaderGen(inactiveReasonGen = Some(MANUALLY_DISABLED))) {
          inactiveCallCampaign =>
            (billingService.getCallCampaign _)
              .expects(testCallsPlacementClient)
              .returningZ(Some(inactiveCallCampaign))
            val result = checkActivationAllowed(
              testCallsPlacementRequest,
              testCallsPlacementClient
            ).e.left.value
            result shouldBe Deactivate(
              InactiveCallCampaign(MANUALLY_DISABLED),
              offerStatusPatch = Some(Hidden)
            )
        }
      }

      "got single with calls placement && call:cars:used campaign is manually disabled" in {
        forAll(campaignHeaderGen(inactiveReasonGen = Some(MANUALLY_DISABLED))) {
          inactiveCallCampaign =>
            (billingService.getProductCampaign _)
              .expects(testSingleWithCallsPlacementClient, ProductId.CallCarsUsed)
              .returningZ(Some(inactiveCallCampaign))
            val result = checkActivationAllowed(
              testSingleWithCallsPlacementRequest,
              testSingleWithCallsPlacementClient
            ).e.left.value
            result shouldBe Deactivate(
              InactiveCallCarsUsedCampaign(MANUALLY_DISABLED),
              offerStatusPatch = Some(Hidden)
            )
        }
      }

      "got calls placement && calls campaign is inactive due to no enough funds" in {
        forAll(campaignHeaderGen(inactiveReasonGen = Some(NO_ENOUGH_FUNDS))) {
          inactiveCallCampaign =>
            (billingService.getCallCampaign _)
              .expects(testCallsPlacementClient)
              .returningZ(Some(inactiveCallCampaign))
            val result = checkActivationAllowed(
              testCallsPlacementRequest,
              testCallsPlacementClient
            ).e.left.value
            result shouldBe Deactivate(
              InactiveCallCampaign(NO_ENOUGH_FUNDS),
              offerStatusPatch = Some(Expired)
            )
        }
      }

      "got calls placement && calls campaign is inactive due to deposit limit exceeded" in {
        forAll(
          campaignHeaderGen(inactiveReasonGen = Some(DEPOSIT_LIMIT_EXCEEDED))
        ) { inactiveCallCampaign =>
          (billingService.getCallCampaign _)
            .expects(testCallsPlacementClient)
            .returningZ(Some(inactiveCallCampaign))
          val result = checkActivationAllowed(
            testCallsPlacementRequest,
            testCallsPlacementClient
          ).e.left.value
          result shouldBe Deactivate(
            InactiveCallCampaign(DEPOSIT_LIMIT_EXCEEDED),
            offerStatusPatch = Some(Expired)
          )
        }
      }

      "got calls placement && calls campaign not found" in {
        (billingService.getCallCampaign _)
          .expects(testCallsPlacementClient)
          .returningZ(None)
        val result = checkActivationAllowed(
          testCallsPlacementRequest,
          testCallsPlacementClient
        ).e.left.value
        result shouldBe Deactivate(
          NoCallCampaign,
          offerStatusPatch = Some(Hidden)
        )
      }

      "got singleWithCalls placement && call:cars:used campaign not found" in {
        (billingService.getProductCampaign _)
          .expects(testSingleWithCallsPlacementClient, ProductId.CallCarsUsed)
          .returningZ(None)
        val result = checkActivationAllowed(
          testSingleWithCallsPlacementRequest,
          testSingleWithCallsPlacementClient
        ).e.left.value
        result shouldBe Deactivate(
          NoCallCarsUsedCampaign,
          offerStatusPatch = Some(Hidden)
        )
      }
    }

    "return next" when {
      "got any active product except Fresh, Reset, Turbo and placement" in {
        forAll(GoodsNameGen) { product =>
          whenever(
            product != ProductId.Fresh && product != Placement &&
            product != ProductId.Turbo && !isQuota(product) &&
            product != ProductId.Reset
          ) {
            val request = testRequest.copy(
              context = ProductContext(product),
              offerBillingDeadline = Some(now())
            )
            checkActivationAllowed(request, testClient).value
          }
        }
      }

      "got any new product" in {
        forAll(GoodsNameGen) { product =>
          whenever(product != Placement && !isQuota(product)) {
            val request =
              testRequest.copy(
                context = ProductContext(product),
                offerBillingDeadline = None
              )
            checkActivationAllowed(request, testClient).value
          }
        }
      }

      "no action for placement and not ready for new billing client" in {
        val request = testRequest.copy(
          context = ProductContext(Placement),
          offerBillingDeadline = Some(now())
        )
        val client = testClient.copy(singlePayment = Set.empty[AdsRequestType])
        checkActivationAllowed(request, client) should matchPattern(
          nonDestructiveDeactivatePf(ProlongUnavailable(request.product))
        )
      }

      "got active placement" in {
        val request = testRequest.copy(
          context = ProductContext(Placement),
          offerBillingDeadline = Some(now())
        )
        checkActivationAllowed(request, testClient).get should be(())
      }

      "placement model == calls && calls campaign is active" in {
        (billingService.getCallCampaign _)
          .expects(testCallsPlacementClient)
          .returningZ(Some(activeCallCampaign))
        val result =
          checkActivationAllowed(
            testCallsPlacementRequest,
            testCallsPlacementClient
          )
        result.value shouldBe (())
      }

      "placement model == singleWithCalls && call:cars:used campaign is active" in {
        (billingService.getProductCampaign _)
          .expects(testSingleWithCallsPlacementClient, ProductId.CallCarsUsed)
          .returningZ(Some(activeCallCarsUsedCampaign))
        val result =
          checkActivationAllowed(
            testSingleWithCallsPlacementRequest,
            testSingleWithCallsPlacementClient
          )
        result.value shouldBe (())
      }

      "prolong calls placement && calls campaign is active" in {
        val prolongationRequest =
          testCallsPlacementRequest.copy(
            offerBillingDeadline = Some(DateTime.now())
          )
        (billingService.getCallCampaign _)
          .expects(testCallsPlacementClient)
          .returningZ(Some(activeCallCampaign))
        val result =
          checkActivationAllowed(prolongationRequest, testCallsPlacementClient)
        result.value shouldBe (())
      }

      "prolong singleWithCalls placement && call:cars:used campaign is active" in {
        val prolongationRequest =
          testSingleWithCallsPlacementRequest.copy(
            offerBillingDeadline = Some(DateTime.now())
          )
        (billingService.getProductCampaign _)
          .expects(testSingleWithCallsPlacementClient, ProductId.CallCarsUsed)
          .returningZ(Some(activeCallCarsUsedCampaign))
        val result =
          checkActivationAllowed(prolongationRequest, testSingleWithCallsPlacementClient)
        result.value shouldBe (())
      }
    }
  }

  "checkActivationAllowedByOffer" should {

    "return next" when {
      "got special-offer for cars used" in {
        val product = ProductId.Special
        val offer = testCarsUsedOffer

        val expectedDaoFilter =
          ForOfferActivatedProducts(
            testOfferId,
            OfferCategories.Cars,
            NonEmptySet.one(ProductId.Turbo: ProductId)(cats.Order.fromOrdering)
          )

        (goodsDao.get _)
          .expects(expectedDaoFilter)
          .returningT(Nil)

        checkActivationAllowedByOffer(product, offer).value shouldBe (())
      }
    }

    "return Deactivate" when {

      "got special-offer for cars new" in {
        val product = ProductId.Special
        val offer = testCarsNewOffer

        checkActivationAllowedByOffer(product, offer) should matchPattern {
          nonDestructiveDeactivatePf(ProlongUnavailable(product))
        }
      }

      "got special-offer activated via package" in {
        val product = ProductId.Special
        val offer = testCarsUsedOffer

        val expectedDaoFilter =
          ForOfferActivatedProducts(
            testOfferId,
            OfferCategories.Cars,
            NonEmptySet.one(ProductId.Turbo: ProductId)(cats.Order.fromOrdering)
          )

        val returnGoods = Iterable(
          GoodRecordGen.next.copy(
            product = product,
            offerId = testOfferId,
            offerHash = testOfferHash
          )
        )

        (goodsDao.get _)
          .expects(expectedDaoFilter)
          .returningT(returnGoods)

        val expected = NonEmptyList.of(product)

        checkActivationAllowedByOffer(product, offer) should matchPattern {
          deactivatePf(AlreadyActivatedByPackages(expected))
        }
      }
    }

  }
}
