package ru.auto.salesman.service.placement.validation

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Section.USED
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.payment_model.PaymentModel.{Calls, Quota, Single}
import ru.auto.salesman.model.payment_model.PaymentModel
import ru.auto.salesman.util.offer.RichOffer
import ru.auto.salesman.model.{
  ActivateDate,
  ClientId,
  OfferCategories,
  OfferCurrencies,
  OfferId,
  ProductId,
  QuotaType
}
import ru.auto.salesman.service.GoodsDecider.Action.{Activate, Deactivate, NoAction}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.{
  InactiveClient,
  QuotaExceeded
}
import ru.auto.salesman.service.GoodsDecider.NoActionReason.OfferResolutionError
import ru.auto.salesman.service.GoodsDecider.{DeactivateReason, Request, Response}
import ru.auto.salesman.service.quota_offers.QuotaOffersDecider
import ru.auto.salesman.service.GoodsDecider
import ru.auto.salesman.service.goods.DealerGoodsService
import ru.auto.salesman.service.goods.domain._
import ru.auto.salesman.service.placement.validation.domain.{
  Allowed,
  Forbidden,
  TemporaryError
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{autoruOfferIdGen, OfferModelGenerators}
import ru.auto.salesman.util.CacheControl.NoCache
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.Model.OfferBilling

class DealerGoodsPreparingServiceImplSpec
    extends BaseSpec
    with OfferModelGenerators
    with BeforeAndAfterAll {

  private val testOfferId: OfferId = 2

  private val testOffer = offerGen(
    offerIdGen = autoruOfferIdGen(testOfferId),
    offerCategoryGen = Category.CARS,
    offerSectionGen = USED,
    PriceInfoGen = priceInfoGen(
      priceGen = 1000000.toDouble,
      currencyGen = OfferCurrencies.RUR
    )
  ).next

  private val goodsService = mock[DealerGoodsService]
  private val singleDecider = mock[GoodsDecider]
  private val quotaDecider = mock[QuotaOffersDecider]
  private val vosClient = mock[VosClient]

  private val service = new DealerGoodsPreparingServiceImpl(
    goodsService,
    singleDecider,
    quotaDecider,
    vosClient
  )

  private val testClientId: ClientId = 7
  private val testPlacement: ProductId = ProductId.Placement

  private val activate = Activate(
    activateDate = ActivateDate(DateTime.now()),
    offerBilling = OfferBilling.newBuilder().setVersion(1).build(),
    features = List.empty
  )

  private val inactiveClientDeactivateAction = Deactivate(
    reason = DeactivateReason.InactiveClient,
    offerStatusPatch = None
  )

  private val runtimeException = new RuntimeException

  private val noAction = NoAction(OfferResolutionError(runtimeException))

  private val quotedOfferAddingResult = buildAddingResult(Quota)

  private val singleOfferAddingResultPaid =
    buildAddingResult(Single, isPaid = true)
  private val singleOfferAddingResult = buildAddingResult(Single)

  private val callsOfferAddingResultPaid =
    buildAddingResult(Calls, isPaid = true)

  private val singleDeciderResponseActivate = Response(activate)

  private val singleDeciderResponseInactiveClient = Response(
    inactiveClientDeactivateAction
  )
  private val singleDeciderResponseNoAction = Response(noAction)

  private val testPlacementCarsUsedQuotaType: QuotaType =
    ProductId.QuotaPlacementCarsUsed

  implicit val rc: RequestContext = AutomatedContext("unit-test", NoCache)

  "ActivationPreconditionsValidator service" should {
    // check Quota model

    "validate quoted offer with allowed decision" in {
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(*, *)
        .returningZ(List(quotedOfferAddingResult))
      (quotaDecider.canActivate _).expects(*, *, *).returningZ(true)

      service
        .prepare(testOffer, testPlacement, testClientId)
        .success
        .value shouldBe Allowed
    }

    "validate quoted offer with forbidden decision" in {
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(*, *)
        .returningZ(List(quotedOfferAddingResult))
      (quotaDecider.canActivate _).expects(*, *, *).returningZ(false)

      service
        .prepare(testOffer, testPlacement, testClientId)
        .success
        .value shouldBe Forbidden(
        QuotaExceeded(
          testPlacementCarsUsedQuotaType,
          testOffer.id,
          testClientId
        )
      )
    }
  }

  // check Single | Calls model

  "validate single offer with allowed decision cause it has been already paid" in {
    (goodsService
      .add(_: ClientId, _: List[GoodsRequest]))
      .expects(*, *)
      .returningZ(List(singleOfferAddingResultPaid))

    service
      .prepare(testOffer, testPlacement, testClientId)
      .success
      .value shouldBe Allowed
  }

  "validate calls offer with allowed decision cause it has been already paid" in {
    (goodsService
      .add(_: ClientId, _: List[GoodsRequest]))
      .expects(*, *)
      .returningZ(List(callsOfferAddingResultPaid))

    service
      .prepare(testOffer, testPlacement, testClientId)
      .success
      .value shouldBe Allowed
  }

  "validate single offer with allowed decision it has not been paid (activate)" in {
    (goodsService
      .add(_: ClientId, _: List[GoodsRequest]))
      .expects(*, *)
      .returningZ(List(singleOfferAddingResult))
    (singleDecider
      .applyZIO(_: Request))
      .expects(*)
      .returningZ(singleDeciderResponseActivate)

    service
      .prepare(testOffer, testPlacement, testClientId)
      .success
      .value shouldBe Allowed
  }

  "validate single offer with allowed decision it has not been paid (forbidden)" in {
    (goodsService
      .add(_: ClientId, _: List[GoodsRequest]))
      .expects(*, *)
      .returningZ(List(singleOfferAddingResult))
    (singleDecider
      .applyZIO(_: Request))
      .expects(*)
      .returningZ(singleDeciderResponseInactiveClient)

    service
      .prepare(testOffer, testPlacement, testClientId)
      .success
      .value shouldBe Forbidden(InactiveClient)
  }

  "validate single offer with allowed decision it has not been paid (temporary error)" in {
    (goodsService
      .add(_: ClientId, _: List[GoodsRequest]))
      .expects(*, *)
      .returningZ(List(singleOfferAddingResult))
    (singleDecider
      .applyZIO(_: Request))
      .expects(*)
      .returningZ(singleDeciderResponseNoAction)

    service
      .prepare(testOffer, testPlacement, testClientId)
      .success
      .value shouldBe TemporaryError(OfferResolutionError(runtimeException))
  }

  private def buildAddingResult(
      paymentModel: PaymentModel,
      isPaid: Boolean = false
  ): GoodsAddingResult =
    GoodsAddingResult(
      offerId = testOfferId,
      category = OfferCategories.Cars,
      product = testPlacement,
      createDate = DateTime.now,
      expireDate = if (isPaid) Some(DateTime.now) else None,
      badge = None,
      offerHash = None,
      epoch = None,
      paymentModel = paymentModel,
      addedGoodId = Some(-1)
    )

}
