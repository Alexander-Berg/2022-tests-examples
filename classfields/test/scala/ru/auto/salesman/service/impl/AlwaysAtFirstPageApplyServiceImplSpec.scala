package ru.auto.salesman.service.impl

import org.scalacheck.Gen
import org.scalacheck.Gen.const
import org.scalatest.BeforeAndAfterEach
import ru.auto.api.ApiOfferModel.Category._
import ru.auto.salesman.client.PhpCabinetClient.PostServicesRequest
import ru.auto.salesman.client.SearcherClient.OfferPositionRequest
import ru.auto.salesman.client.{PhpCabinetClient, SearcherClient}
import ru.auto.salesman.model.ProductId.Fresh
import ru.auto.salesman.model.autostrategies.AutostrategyApplyResult.{Applied, NotApplied}
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.searcher.OfferSorts._
import ru.auto.salesman.model.{AutoruDealer, ServicePlaces}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{
  alwaysAtFirstPagePayloadGen,
  searcherPositionGen,
  OfferModelGenerators
}
import ru.auto.salesman.util.offer._
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

class AlwaysAtFirstPageApplyServiceImplSpec
    extends BaseSpec
    with BeforeAndAfterEach
    with OfferModelGenerators {

  private val searcherClient = mock[SearcherClient]

  private val phpCabinetClient = mock[PhpCabinetClient]

  private val service =
    new AlwaysAtFirstPageApplyServiceImpl(searcherClient, phpCabinetClient)

  implicit private val requestContext: RequestContext = AutomatedContext("test")

  private val offerPositionMock = toMockFunction2 {
    (searcherClient
      .offerPosition(_: OfferPositionRequest)(_: RequestContext))
  }

  "Always at first page apply service" should {

    "apply autostrategy for mark-model" in {
      forAll(
        alwaysAtFirstPagePayloadGen(forMarkModelListing = Some(true)),
        offerGen(offerCategoryGen = CARS),
        searcherPositionGen(onFirstPage = false)
      ) { (payload, offer, notFirstPagePosition) =>
        offerPositionMock
          .expects(
            OfferPositionRequest(offer, withSuperGen = false, sort = FreshDesc),
            *
          )
          .returningT(Some(notFirstPagePosition))
        (phpCabinetClient
          .postServices(_: PostServicesRequest)(_: RequestContext))
          .expects(
            PostServicesRequest(
              AutoruOfferId(offer.getId).id,
              AutoruDealer(offer.getUserRef).id,
              CARS.toString,
              Fresh,
              ServicePlaces.AlwaysAtFirstPageAutostrategy
            ),
            *
          )
          .returningT(true)
        service.applyAutostrategy(payload, offer).success.value shouldBe Applied
      }
    }

    "apply autostrategy for mark-model-gen" in {
      forAll(
        alwaysAtFirstPagePayloadGen(
          forMarkModelListing = Some(false),
          forMarkModelGenerationListing = Some(true)
        ),
        offerGen(offerCategoryGen = CARS),
        searcherPositionGen(onFirstPage = false)
      ) { (payload, offer, notFirstPagePosition) =>
        offerPositionMock
          .expects(
            OfferPositionRequest(offer, withSuperGen = true, sort = FreshDesc),
            *
          )
          .returningT(Some(notFirstPagePosition))
        (phpCabinetClient
          .postServices(_: PostServicesRequest)(_: RequestContext))
          .expects(
            PostServicesRequest(
              AutoruOfferId(offer.getId).id,
              AutoruDealer(offer.getUserRef).id,
              CARS.toString,
              Fresh,
              ServicePlaces.AlwaysAtFirstPageAutostrategy
            ),
            *
          )
          .returningT(true)
        service.applyAutostrategy(payload, offer).success.value shouldBe Applied
      }
    }

    "apply autostrategy for moto/trucks" in {
      forAll(
        alwaysAtFirstPagePayloadGen(),
        offerGen(offerCategoryGen = Gen.oneOf(Seq(MOTO, TRUCKS))),
        searcherPositionGen(onFirstPage = false)
      ) { (payload, offer, notFirstPagePosition) =>
        val positionRequest = OfferPositionRequest(
          offer,
          withSuperGen = true,
          sort = CreateDateDesc
        )
        offerPositionMock
          .expects(positionRequest, *)
          .returningT(Some(notFirstPagePosition))
          .noMoreThanOnce()
        offerPositionMock
          .expects(positionRequest.copy(withSuperGen = false), *)
          .returningT(Some(notFirstPagePosition))
          .noMoreThanOnce()
        (phpCabinetClient
          .postServices(_: PostServicesRequest)(_: RequestContext))
          .expects(
            PostServicesRequest(
              AutoruOfferId(offer.getId).id,
              AutoruDealer(offer.getUserRef).id,
              offer.categoryCode,
              Fresh,
              ServicePlaces.AlwaysAtFirstPageAutostrategy
            ),
            *
          )
          .returningT(true)
        service.applyAutostrategy(payload, offer).success.value shouldBe Applied
      }
    }

    "not apply if on first page" in {
      forAll(
        alwaysAtFirstPagePayloadGen(),
        offerGen(offerCategoryGen = CARS),
        searcherPositionGen(onFirstPage = true)
      ) { (payload, offer, firstPagePosition) =>
        offerPositionMock
          .expects(*, *)
          .returningT(Some(firstPagePosition))
          .noMoreThanTwice()
        service
          .applyAutostrategy(payload, offer)
          .success
          .value shouldBe NotApplied
      }
    }

    "not apply if searcher didn't returned position" in {
      forAll(
        alwaysAtFirstPagePayloadGen(),
        offerGen(offerCategoryGen = CARS)
      ) { (payload, offer) =>
        offerPositionMock
          .expects(*, *)
          .returningT(None)
          .noMoreThanTwice()
        service
          .applyAutostrategy(payload, offer)
          .success
          .value shouldBe NotApplied
      }
    }

    "return not applied if php returned false" in {
      forAll(
        alwaysAtFirstPagePayloadGen(),
        offerGen(offerCategoryGen = CARS),
        searcherPositionGen(onFirstPage = false)
      ) { (payload, offer, notFirstPagePosition) =>
        offerPositionMock
          .expects(*, *)
          .returningT(Some(notFirstPagePosition))
          .noMoreThanTwice()
        (phpCabinetClient
          .postServices(_: PostServicesRequest)(_: RequestContext))
          .expects(
            PostServicesRequest(
              AutoruOfferId(offer.getId).id,
              AutoruDealer(offer.getUserRef).id,
              CARS.toString,
              Fresh,
              ServicePlaces.AlwaysAtFirstPageAutostrategy
            ),
            *
          )
          .returningT(false)
        service
          .applyAutostrategy(payload, offer)
          .success
          .value shouldBe NotApplied
      }
    }

    "fail to apply autostrategy for user's offer" in {
      forAll(
        alwaysAtFirstPagePayloadGen(),
        offerGen(offerCategoryGen = CARS, userRefGen = userRefGen),
        searcherPositionGen(onFirstPage = false)
      ) { (payload, offer, notFirstPagePosition) =>
        offerPositionMock
          .expects(*, *)
          .returningT(Some(notFirstPagePosition))
          .noMoreThanTwice()
        service
          .applyAutostrategy(payload, offer)
          .failure
          .exception shouldBe an[UnexpectedUserRefException]
      }
    }

    "fail to apply autostrategy for offer with wrong category" in {
      forAll(
        alwaysAtFirstPagePayloadGen(),
        offerGen(offerCategoryGen = CATEGORY_UNKNOWN),
        searcherPositionGen(onFirstPage = false)
      ) { (payload, offer, notFirstPagePosition) =>
        offerPositionMock
          .expects(*, *)
          .returningT(Some(notFirstPagePosition))
          .noMoreThanTwice()
        service
          .applyAutostrategy(payload, offer)
          .failure
          .exception shouldBe an[InvalidOfferCategoryException]
      }
    }
  }
}
