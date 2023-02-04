package ru.auto.salesman.service.impl.user.autoru.price.service

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.Assertion
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.ApiOfferModel.OfferStatus._
import ru.auto.api.ResponseModel
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.VosClient.GetUserOffersQuery
import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.model.user.Goods
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.user.PriceService.EnrichedOffer
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.VosRequestGenerators
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.{ServiceModelGenerators, UserModelGenerators}

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

class FreeOffersCountCalculatorSpec
    extends BaseSpec
    with OfferModelGenerators
    with ServiceModelGenerators
    with UserModelGenerators
    with VosRequestGenerators {

  val vosClient: VosClient = mock[VosClient]
  val goodsDao: GoodsDao = mock[GoodsDao]

  val freeOffersCountCalculator =
    new FreeOffersCountCalculatorImpl(vosClient, goodsDao)

  "FreeOffersCountCalculator" should {

    "use only INACTIVE, REMOVED, EXPIRED statuses" in {
      val statusesToCheck = Seq(INACTIVE, REMOVED, EXPIRED)
      val otherStatuses =
        Seq(
          ACTIVE,
          STATUS_UNKNOWN,
          BANNED,
          NEED_ACTIVATION,
          DRAFT,
          UNRECOGNIZED
        )
      val allKnownStatuses = otherStatuses ++ statusesToCheck
      FreeOffersCountCalculatorImpl.calcQuotaOfferStatuses should contain theSameElementsAs statusesToCheck
      OfferStatus.values() should contain theSameElementsAs allKnownStatuses
    }

    """used free offers = not active offers for last 30 days + active offers -
    active paid offers (when current offer not among this offers)""" in {
      calculateMethodGenerators(includeBaseOfferToActiveOffersList = false) {
        (
            offer,
            paidOffers,
            usingQuotaByGeneration,
            // all offers wasActive=false
            notActiveNeverActivatedOffersListingResponse,
            // all offers wasActive=true
            notActiveAtLeastOnceActivatedOffersListingResponse,
            // all offers wasActive=true
            activeOffersListingResponse
        ) =>
          val notActiveOffersListingResponse =
            notActiveAtLeastOnceActivatedOffersListingResponse.toBuilder
              .addAllOffers(
                notActiveNeverActivatedOffersListingResponse.getOffersList
              )
              .build()
          (vosClient.getUserOffers _)
            .expects(assertArgs { s: GetUserOffersQuery =>
              s.createDateFrom.get.getMillis shouldBe (DateTime
                .now()
                .minusDays(FreeOffersCountCalculatorImpl.freeOfferCheckPeriod)
                .getMillis +- 3.minute.toMillis)
            })
            .returningZ(notActiveOffersListingResponse)
          (vosClient.getUserOffers _)
            .expects(*)
            .returningZ(activeOffersListingResponse)
          (goodsDao.get _).expects(*).returningZ(paidOffers)

          val count = freeOffersCountCalculator
            .calculate(offer, usingQuotaByGeneration)
            .success
            .value
          val atLeastOnceActivatedOffers =
            notActiveAtLeastOnceActivatedOffersListingResponse.getOffersList.asScala ++
            activeOffersListingResponse.getOffersList.asScala

          count shouldBe atLeastOnceActivatedOffers.size - paidOffers.size
      }
    }

    """used free offers = not active offers for last 30 days + active offers -
    current offer(when it's among to nonactive or active offers and was active in the past) -
    active paid offers""" in {
      calculateMethodGenerators(includeBaseOfferToActiveOffersList = true) {
        (
            offer,
            paidOffers,
            usingQuotaByGeneration,
            // all offers wasActive=false
            notActiveNeverActivatedOffersListingResponse,
            // all offers wasActive=true
            notActiveAtLeastOnceActivatedOffersListingResponse,
            // all offers wasActive=true
            activeOffersListingResponse
        ) =>
          val notActiveOffersListingResponse =
            notActiveAtLeastOnceActivatedOffersListingResponse.toBuilder
              .addAllOffers(
                notActiveNeverActivatedOffersListingResponse.getOffersList
              )
              .build()
          (vosClient.getUserOffers _)
            .expects(*)
            .returningZ(notActiveOffersListingResponse)
          (vosClient.getUserOffers _)
            .expects(*)
            .returningZ(activeOffersListingResponse)
          (goodsDao.get _).expects(*).returningZ(paidOffers)

          val count = freeOffersCountCalculator
            .calculate(offer, usingQuotaByGeneration)
            .success
            .value

          val atLeastOnceActivatedOffers =
            notActiveAtLeastOnceActivatedOffersListingResponse.getOffersList.asScala ++
            activeOffersListingResponse.getOffersList.asScala

          count shouldBe atLeastOnceActivatedOffers.size - 1 - paidOffers.size
      }
    }

  }

  private def calculateMethodGenerators(
      includeBaseOfferToActiveOffersList: Boolean
  )(
      f: (
          EnrichedOffer,
          List[Goods],
          Boolean,
          ResponseModel.OfferListingResponse,
          ResponseModel.OfferListingResponse,
          ResponseModel.OfferListingResponse
      ) => Assertion
  ) {
    for {
      offer <- EnrichedOfferGen
      paidOffers <- Gen.listOf(GoodsGen)

      usingQuotaByGeneration <- bool

      offerId = offer.offerId.value

      notActiveNeverActivatedOffersListingResponse <-
        offersListingResponseWithOrWithoutOfferGen(
          offerId,
          offersWereActivated = false
        )

      notActiveAtLeastOnceActivatedOffersListingResponse <-
        offersListingResponseWithOrWithoutOfferGen(
          offerId,
          offersWereActivated = true
        )

      activeOffersListingResponse <- offersListingResponseWithOrWithoutOfferGen(
        offerId,
        offersWereActivated = true,
        includeOfferWithGivenId = includeBaseOfferToActiveOffersList
      )

    } yield
      f(
        offer,
        paidOffers,
        usingQuotaByGeneration,
        notActiveNeverActivatedOffersListingResponse,
        notActiveAtLeastOnceActivatedOffersListingResponse,
        activeOffersListingResponse
      )
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
