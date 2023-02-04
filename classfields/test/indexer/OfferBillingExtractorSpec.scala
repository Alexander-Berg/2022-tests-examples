package ru.yandex.vertis.billing.indexer

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.indexer.status.DeciderImpl
import ru.yandex.vertis.billing.model_core.OfferBilling.{Active, Inactive, KnownCampaign, UnknownCampaign}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{OfferIdGen, Producer}
import ru.yandex.vertis.billing.status.DefaultActiveDeadlineResolver

import scala.util.Success

/**
  * Specs on [[OfferBillingExtractor]].
  *
  * @author dimas
  */
class OfferBillingExtractorSpec extends AnyWordSpec with Matchers {

  private val decider = new DeciderImpl(paidOffers, limitHelper, DefaultActiveDeadlineResolver)
  private val extractor = new OfferBillingExtractor(campaigns, decider)

  "OfferBillingExtractor" should {
    "build KnownCampaign for exists campaign" in {
      val source = BindingSources.Api
      val variant = Variant(EnabledCampaign.id, isDeleted = false, source)

      val billing = extractor.getOfferBilling(offer1, variant).get

      billing match {
        case KnownCampaign(_, _, `EnabledCampaign`, `source`, _, _) =>
        case other => fail(s"Unexpected billing $billing")
      }
    }

    "build UnknownCampaign for non-exists campaign" in {
      val source = BindingSources.Api
      val variant = Variant(NonExistsCampaignId, isDeleted = false, source)

      val billing = extractor.getOfferBilling(offer1, variant).get

      billing match {
        case UnknownCampaign(_, _, NonExistsCampaignId, `source`) =>
        case other => fail(s"Unexpected billing $billing")
      }
    }

    "fail to build billing for failed campaign" in {
      val source = BindingSources.Api
      val variant = Variant(FailedCampaignId, isDeleted = false, source)

      intercept[NoSuchElementException] {
        extractor.getOfferBilling(offer1, variant).get
      }
    }

    "select None for deleted variants" in {
      val offerId = OfferIdGen.next
      val deletedVariants = OfferVariants(offerId)
        .put(Variant(EnabledCampaign.id, isDeleted = true, BindingSources.Api))
        .put(Variant(NonExistsCampaignId, isDeleted = true, BindingSources.Feed))
      extractor.getOfferBilling(deletedVariants) should be(Success(None))
    }

    "select single non-deleted variant" in {
      val offerId = offer1
      val source = BindingSources.Api
      val variants = OfferVariants(offerId)
        .put(Variant(EnabledCampaign.id, isDeleted = false, source))
        .put(Variant(NonExistsCampaignId, isDeleted = true, BindingSources.Feed))
      extractor.getOfferBilling(variants) match {
        case Success(Some(KnownCampaign(_, _, `EnabledCampaign`, `source`, _, _))) =>
        case other => fail(s"Unexpected billing $other")
      }
    }

    "prefer API-binding to FEED-binding" in {
      val offerId = offer2
      val variants = OfferVariants(offerId)
        .put(Variant(EnabledCampaign.id, isDeleted = false, BindingSources.Api))
        .put(Variant(NonExistsCampaignId, isDeleted = false, BindingSources.Feed))
      extractor.getOfferBilling(variants) match {
        case Success(Some(KnownCampaign(_, _, `EnabledCampaign`, BindingSources.Api, _, _))) =>
        case other => fail(s"Unexpected billing $other")
      }
    }

    "prefer API-binding to FEED-binding even if there is unknown campaign" in {
      val offerId = offer1
      val variants = OfferVariants(offerId)
        .put(Variant(NonExistsCampaignId, isDeleted = false, BindingSources.Api))
        .put(Variant(EnabledCampaign.id, isDeleted = false, BindingSources.Feed))
      extractor.getOfferBilling(variants) match {
        case Success(Some(UnknownCampaign(_, _, NonExistsCampaignId, BindingSources.Api))) =>
        case other => fail(s"Unexpected billing $other")
      }
    }

    "build inactive KnownCampaign for campaign without funds and paid offer" in {
      val source = BindingSources.Api
      val variant = Variant(NoFundsPerDayCampaign.id, isDeleted = false, source)

      val billing = extractor.getOfferBilling(offer1, variant).get
      billing match {
        case KnownCampaign(_, _, NoFundsPerDayCampaign, `source`, Inactive(_), _) =>
        case other => fail(s"Unexpected billing $billing")
      }
    }

    "build inactive KnownCampaign for campaign without funds" in {
      val source = BindingSources.Api
      val variant = Variant(NoFundsPerDayCampaign.id, isDeleted = false, source)

      val billing = extractor.getOfferBilling(offer2, variant).get
      billing match {
        case KnownCampaign(_, _, NoFundsPerDayCampaign, `source`, Inactive(_), _) =>
        case other => fail(s"Unexpected billing $billing")
      }
    }
  }

}
