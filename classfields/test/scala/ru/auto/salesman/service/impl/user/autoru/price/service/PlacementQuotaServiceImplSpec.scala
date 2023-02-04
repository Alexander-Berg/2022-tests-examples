package ru.auto.salesman.service.impl.user.autoru.price.service

import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.user.QuotaPlacementDao
import ru.auto.salesman.model.user.PaymentReasons
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.impl.user.autoru.price.service.placement.PlacementQuotaServiceImpl
import ru.auto.salesman.service.user.PriceService.ProductQuota
import ru.auto.salesman.service.user.autoru.price.service.FreeOffersCountCalculator
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.test.{BaseSpec, TestException}

class PlacementQuotaServiceImplSpec extends BaseSpec with ServiceModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val quotaPlacementDao: QuotaPlacementDao = mock[QuotaPlacementDao]
  val vosClient: VosClient = mock[VosClient]

  val freeOffersCountCalculator: FreeOffersCountCalculator =
    mock[FreeOffersCountCalculator]

  val quotaService =
    new PlacementQuotaServiceImpl(
      vosClient,
      quotaPlacementDao,
      freeOffersCountCalculator
    )
  "quotaBySameOffers" should {
    "return new quota for DRAFT if vosClient.hasSameOfferAsDraft return true" in {
      forAll(EnrichedOfferGen, AutoruUserGen) { (enrichedOfferGen, user) =>
        val source = enrichedOfferGen.source.toBuilder
          .setStatus(OfferStatus.DRAFT)
          .build
        val enrichedOffer =
          enrichedOfferGen.copy(source = source, user = Some(user))

        (vosClient.hasSameOfferAsDraft _)
          .expects(
            enrichedOffer.source.getCategory,
            enrichedOffer.offerId,
            enrichedOffer.source.getUserRef
          )
          .returningZ(true)

        quotaService
          .getQuotaBySameOffers(enrichedOffer)
          .success
          .value shouldBe Some(
          ProductQuota(
            size = 0,
            baseQuota = 0,
            reason = Some(PaymentReasons.DuplicateOffer),
            duration = None,
            offerType = None
          )
        )
      }
    }

    "return None for DRAFT if vosClient.hasSameOfferAsDraft return false" in {
      forAll(EnrichedOfferGen, AutoruUserGen) { (enrichedOfferGen, user) =>
        val source = enrichedOfferGen.source.toBuilder
          .setStatus(OfferStatus.DRAFT)
          .build
        val enrichedOffer =
          enrichedOfferGen.copy(source = source, user = Some(user))

        (vosClient.hasSameOfferAsDraft _)
          .expects(
            enrichedOffer.source.getCategory,
            enrichedOffer.offerId,
            enrichedOffer.source.getUserRef
          )
          .returningZ(false)

        quotaService
          .getQuotaBySameOffers(enrichedOffer)
          .success
          .value shouldBe None
      }
    }

    "fail for DRAFT if vosClient.hasSameOfferAsDraft failed" in {
      forAll(EnrichedOfferGen, AutoruUserGen) { (enrichedOfferGen, user) =>
        val source = enrichedOfferGen.source.toBuilder
          .setStatus(OfferStatus.DRAFT)
          .build
        val enrichedOffer =
          enrichedOfferGen.copy(source = source, user = Some(user))
        val testException = new TestException

        (vosClient.hasSameOfferAsDraft _)
          .expects(
            enrichedOffer.source.getCategory,
            enrichedOffer.offerId,
            enrichedOffer.source.getUserRef
          )
          .throwingZ(testException)

        quotaService
          .getQuotaBySameOffers(enrichedOffer)
          .failure
          .exception shouldBe testException
      }
    }

    "return new quota for not draft offer if vosClient.hasSameOffer return true" in {
      forAll(EnrichedOfferGen, AutoruUserGen, OfferStatusNotDraftGen) {
        (enrichedOfferGen, user, notDraftStatus) =>
          val source =
            enrichedOfferGen.source.toBuilder.setStatus(notDraftStatus).build
          val enrichedOffer =
            enrichedOfferGen.copy(source = source, user = Some(user))

          (vosClient.hasSameOffer _)
            .expects(
              enrichedOffer.source.getCategory,
              enrichedOffer.offerId,
              enrichedOffer.source.getUserRef
            )
            .returningZ(true)

          quotaService
            .getQuotaBySameOffers(enrichedOffer)
            .success
            .value shouldBe Some(
            ProductQuota(
              size = 0,
              baseQuota = 0,
              reason = Some(PaymentReasons.DuplicateOffer),
              duration = None,
              offerType = None
            )
          )
      }
    }

    "return None for not draft offer if vosClient.hasSameOffer return false" in {
      forAll(EnrichedOfferGen, AutoruUserGen, OfferStatusNotDraftGen) {
        (enrichedOfferGen, user, notDraftStatus) =>
          val source =
            enrichedOfferGen.source.toBuilder.setStatus(notDraftStatus).build
          val enrichedOffer =
            enrichedOfferGen.copy(source = source, user = Some(user))

          (vosClient.hasSameOffer _)
            .expects(
              enrichedOffer.source.getCategory,
              enrichedOffer.offerId,
              enrichedOffer.source.getUserRef
            )
            .returningZ(false)

          quotaService
            .getQuotaBySameOffers(enrichedOffer)
            .success
            .value shouldBe None
      }
    }

    "fail for not draft offer if vosClient.hasSameOffer failed" in {
      forAll(EnrichedOfferGen, AutoruUserGen, OfferStatusNotDraftGen) {
        (enrichedOfferGen, user, notDraftStatus) =>
          val source =
            enrichedOfferGen.source.toBuilder.setStatus(notDraftStatus).build
          val enrichedOffer =
            enrichedOfferGen.copy(source = source, user = Some(user))
          val testException = new TestException

          (vosClient.hasSameOffer _)
            .expects(
              enrichedOffer.source.getCategory,
              enrichedOffer.offerId,
              enrichedOffer.source.getUserRef
            )
            .throwingZ(testException)

          quotaService
            .getQuotaBySameOffers(enrichedOffer)
            .failure
            .exception shouldBe testException
      }
    }

    "reduceQuotaByUsedFreeOffers" should {
      "set status FREE_LIMIT, when quota.size isn't 0 and used free offers count >= quota size " in {
        forAll(ProductQuotaGen.map(_.copy(size = 2)), bool, EnrichedOfferGen) {
          (quota, usingQuotaByGeneration, enrichedOffer) =>
            (freeOffersCountCalculator.calculate _)
              .expects(*, *)
              .returningZ(2)
              .anyNumberOfTimes()

            val q = quotaService
              .reduceQuotaByUsedFreeOffers(
                quota,
                enrichedOffer,
                usingQuotaByGeneration
              )
              .success
              .value
            q.reason shouldBe Some(PaymentReasons.FreeLimit)
            q.size shouldBe 0
            q.baseQuota shouldBe quota.baseQuota - 2
        }
      }

      "not set status FREE_LIMIT, but reduce baseQuota" in {
        forAll(ProductQuotaGen.map(_.copy(size = 0)), bool, EnrichedOfferGen) {
          (quota, usingQuotaByGeneration, enrichedOffer) =>
            (freeOffersCountCalculator.calculate _)
              .expects(*, *)
              .returningZ(1)
              .anyNumberOfTimes()

            val q = quotaService
              .reduceQuotaByUsedFreeOffers(
                quota,
                enrichedOffer,
                usingQuotaByGeneration
              )
              .success
              .value
            q.baseQuota shouldBe quota.baseQuota - 1
            q.reason shouldBe quota.reason
            q.size shouldBe 0
        }
      }
    }
  }
}
