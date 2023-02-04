package ru.auto.salesman.service.impl

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.OfferStatus.INACTIVE
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.environment
import ru.auto.salesman.model.{
  ActivateDate,
  AutoruDealer,
  FirstActivateDate,
  OfferCategory,
  Slave
}
import ru.auto.salesman.model.OfferStatuses.Expired
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.service._
import ru.auto.salesman.service.DealerProductApplyService.NotAppliedReason.{
  PermanentError,
  TemporaryError
}
import ru.auto.salesman.service.DealerProductApplyService.ProductApplyResult.{
  Applied,
  NotApplied
}
import ru.auto.salesman.service.GoodsDecider.{Context, ProductContext, Request, Response}
import ru.auto.salesman.service.GoodsDecider.Action.{Activate, Deactivate, NoAction}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.{
  DealerNotOwnOffer,
  InactiveClient
}
import ru.auto.salesman.service.GoodsDecider.NoActionReason.GetAdsRequestError
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.test.dao.gens.GoodsDeciderGenerators
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.auto.salesman.util.offer.RichOffer

class DealerProductApplyServiceImplSpec extends BaseSpec {

  private val vosClient = mock[VosClient]
  private val goodsDao = mock[GoodsDao]
  private val goodsDaoProvider = mock[GoodsDaoProvider]
  private val goodsDecider = mock[GoodsDecider]

  private val activateService = mock[GoodsActivateService]
  private val activateServiceProvider = mock[GoodsActivateServiceProvider]

  private val service =
    new DealerProductApplyServiceImpl(
      vosClient,
      goodsDaoProvider,
      goodsDecider,
      activateServiceProvider
    )

  implicit private val rc: RequestContext = AutomatedContext("test")

  "DealerProductApplyServiceImplSpec" should {

    "fail on offer not found" in {
      forAll(autoruOfferIdGen(), ProductIdGen, AutoruDealerGen, optFundsGen) {
        (offerId, product, dealer, customPrice) =>
          (vosClient.getOptOffer _).expects(offerId, Slave).returningZ(None)
          service
            .applyProduct(offerId, product, dealer, customPrice)
            .success
            .value should matchPattern { case NotApplied(PermanentError(_)) =>
          }
      }
    }

    "fail non-placement on inactive offer" in {
      forAll(autoruOfferIdGen(), minSuccessful(10)) { offerId =>
        forAll(
          nonPlacementProductIdGen,
          NotActiveOfferGen.map(_.toBuilder.setId(offerId.toString).build),
          AutoruDealerGen,
          optFundsGen,
          minSuccessful(10)
        ) { (product, offer, dealer, customPrice) =>
          (vosClient.getOptOffer _)
            .expects(offerId, Slave)
            .returningZ(Some(offer))
          service
            .applyProduct(offerId, product, dealer, customPrice)
            .success
            .value should matchPattern { case NotApplied(PermanentError(_)) =>
          }
        }
      }
    }

    "activate" in {
      val testStart = environment.now()
      val nonHashedId = 1071400570L
      forAll(
        autoruOfferIdGen(nonHashedId),
        AutoruDealerGen,
        minSuccessful(10)
      ) { (offerId, dealer) =>
        forAll(
          ProductIdGen,
          offerGen(
            offerIdGen = Gen.const(offerId),
            userRefGen = Gen.const(dealer.toString)
          ),
          GoodsDeciderGenerators.activateGen(),
          optFundsGen,
          minSuccessful(10)
        ) { (product, offer, activate, customPrice) =>
          (vosClient.getOptOffer _)
            .expects(offerId, Slave)
            .returningZ(Some(offer))
          var deciderActivateDate = Option.empty[FirstActivateDate]
          (goodsDecider
            .apply(_: Request)(_: RequestContext))
            .expects(
              argAssert { request: Request =>
                request.clientId shouldBe dealer.id
                request.offer shouldBe offer
                request.product shouldBe product
                request.customPrice shouldBe customPrice
                deciderActivateDate = Some(request.firstActivateDate)
                request.firstActivateDate.isAfter(testStart) shouldBe true
                request.firstActivateDate
                  .isBefore(testStart.plusMinutes(1)) shouldBe true
                request.offerBillingDeadline shouldBe None
                ()
              },
              *
            )
            .returningT(Response(activate))
          (goodsDaoProvider.chooseDao _)
            .expects(offer.getCategory)
            .returning(goodsDao)
          (activateServiceProvider.chooseService _)
            .expects(offer.getCategory)
            .returning(activateService)
          (activateService
            .activate(
              _: Activate,
              _: Context,
              _: AutoruOfferId,
              _: OfferCategory,
              _: AutoruDealer,
              _: ActivateDate,
              _: GoodsActivator
            )(_: RequestContext))
            .expects(
              activate,
              ProductContext(product),
              offerId,
              offer.offerCategory,
              dealer,
              argAssert { activateDate: ActivateDate =>
                activateDate.asDateTime shouldBe deciderActivateDate.value.asDateTime
              },
              *,
              *
            )
            .returningT(())
          service
            .applyProduct(offerId, product, dealer, customPrice)
            .success
            .value shouldBe Applied
        }
      }
    }

    "activate placement for inactive offer" in {
      val testStart = environment.now()
      val nonHashedId = 1071400570L
      forAll(
        autoruOfferIdGen(nonHashedId),
        AutoruDealerGen,
        minSuccessful(10)
      ) { (offerId, dealer) =>
        forAll(
          offerGen(
            offerIdGen = Gen.const(offerId),
            statusGen = INACTIVE,
            userRefGen = Gen.const(dealer.toString)
          ),
          GoodsDeciderGenerators.activateGen(),
          optFundsGen,
          minSuccessful(10)
        ) { (offer, activate, customPrice) =>
          val product = Placement
          (vosClient.getOptOffer _)
            .expects(offerId, Slave)
            .returningZ(Some(offer))
          var deciderActivateDate = Option.empty[FirstActivateDate]
          (goodsDecider
            .apply(_: Request)(_: RequestContext))
            .expects(
              argAssert { request: Request =>
                request.clientId shouldBe dealer.id
                request.offer shouldBe offer
                request.product shouldBe product
                request.customPrice shouldBe customPrice
                deciderActivateDate = Some(request.firstActivateDate)
                request.firstActivateDate.isAfter(testStart) shouldBe true
                request.firstActivateDate
                  .isBefore(testStart.plusMinutes(1)) shouldBe true
                request.offerBillingDeadline shouldBe None
                ()
              },
              *
            )
            .returningT(Response(activate))
          (goodsDaoProvider.chooseDao _)
            .expects(offer.getCategory)
            .returning(goodsDao)
          (activateServiceProvider.chooseService _)
            .expects(offer.getCategory)
            .returning(activateService)
          (activateService
            .activate(
              _: Activate,
              _: Context,
              _: AutoruOfferId,
              _: OfferCategory,
              _: AutoruDealer,
              _: ActivateDate,
              _: GoodsActivator
            )(_: RequestContext))
            .expects(
              activate,
              ProductContext(product),
              offerId,
              offer.offerCategory,
              dealer,
              argAssert { activateDate: ActivateDate =>
                activateDate.asDateTime shouldBe deciderActivateDate.value.asDateTime
              },
              *,
              *
            )
            .returningT(())
          service
            .applyProduct(offerId, product, dealer, customPrice)
            .success
            .value shouldBe Applied
        }
      }
    }

    "return permanent error" in {
      forAll(autoruOfferIdGen(), AutoruDealerGen, minSuccessful(10)) {
        (offerId, dealer) =>
          forAll(
            ProductIdGen,
            offerGen(
              offerIdGen = Gen.const(offerId),
              userRefGen = Gen.const(dealer.toString)
            ),
            optFundsGen,
            minSuccessful(10)
          ) { (product, offer, customPrice) =>
            (vosClient.getOptOffer _)
              .expects(offerId, Slave)
              .returningZ(Some(offer))
            (goodsDecider
              .apply(_: Request)(_: RequestContext))
              .expects(*, *)
              .returningT(
                Response(
                  Deactivate(InactiveClient, offerStatusPatch = Some(Expired))
                )
              )
            service
              .applyProduct(offerId, product, dealer, customPrice)
              .success
              .value shouldBe NotApplied(PermanentError(InactiveClient))
          }
      }
    }

    "return temporary error" in {
      forAll(autoruOfferIdGen(), AutoruDealerGen, minSuccessful(10)) {
        (offerId, dealer) =>
          forAll(
            ProductIdGen,
            offerGen(
              offerIdGen = Gen.const(offerId),
              userRefGen = Gen.const(dealer.toString)
            ),
            optFundsGen,
            minSuccessful(10)
          ) { (product, offer, customPrice) =>
            (vosClient.getOptOffer _)
              .expects(offerId, Slave)
              .returningZ(Some(offer))
            (goodsDecider
              .apply(_: Request)(_: RequestContext))
              .expects(*, *)
              // don't care about exact reason here for now
              .returningT(
                Response(
                  NoAction(GetAdsRequestError(new TestException("test reason")))
                )
              )
            service
              .applyProduct(offerId, product, dealer, customPrice)
              .success
              .value shouldBe NotApplied(
              TemporaryError(
                "GetAdsRequestError(ru.auto.salesman.test.TestException: test reason)"
              )
            )
          }
      }
    }

    "return permanent error if dealer doesn't own offer" in {
      forAll(autoruOfferIdGen(), DifferentDealersGen, minSuccessful(10)) {
        (offerId, differentDealers) =>
          val dealer = differentDealers.dealer1
          val offerDealer = differentDealers.dealer2
          forAll(
            ProductIdGen,
            offerGen(
              offerIdGen = Gen.const(offerId),
              userRefGen = Gen.const(offerDealer.toString)
            ),
            optFundsGen,
            minSuccessful(10)
          ) { (product, offer, customPrice) =>
            (vosClient.getOptOffer _)
              .expects(offerId, Slave)
              .returningZ(Some(offer))
            service
              .applyProduct(offerId, product, dealer, customPrice)
              .success
              .value shouldBe NotApplied(
              PermanentError(
                DealerNotOwnOffer(dealer, offerId, offer.getUserRef)
              )
            )
          }
      }
    }
  }
}
