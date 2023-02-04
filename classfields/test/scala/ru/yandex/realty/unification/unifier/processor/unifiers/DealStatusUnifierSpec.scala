package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.generator.RawOfferGenerator
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.offer.{BuildingState, CategoryType, DealStatus, FlatType, IndexingError, OfferType}
import ru.yandex.realty.model.sites.{SaleStatus, Site}
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.vertis.generators.ProducerProvider._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DealStatusUnifierSpec extends AsyncSpecBase with MockFactory with Matchers with OneInstancePerTest {

  private val sitesService = mock[SitesGroupingService]

  private val unifier = new DealStatusUnifier(sitesService)

  implicit val trace: Traced = Traced.empty

  "DealStatusUnifier" should {

    "do nothing if deal status was already set" in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.DIRECT_RENT)
        )
        .next

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue
    }

    "do nothing if offer is from feed" in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.UNKNOWN),
          isFromFeed = false
        )
        .next

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

      offer.getTransaction.getDealStatus shouldEqual DealStatus.UNKNOWN
    }

    s"set ${DealStatus.PRIMARY_SALE} for unfinished new building" in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.UNKNOWN),
          buildingState = Some(BuildingState.UNFINISHED),
          offerType = Some(OfferType.SELL),
          categoryType = Some(CategoryType.APARTMENT),
          flatType = Some(FlatType.NEW_FLAT)
        )
        .next

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

      offer.getTransaction.getDealStatus shouldEqual DealStatus.PRIMARY_SALE
    }

    s"set ${DealStatus.PRIMARY_SALE} for unfinished new secondary building" in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.UNKNOWN),
          buildingState = Some(BuildingState.UNFINISHED),
          offerType = Some(OfferType.SELL),
          categoryType = Some(CategoryType.APARTMENT),
          flatType = Some(FlatType.NEW_SECONDARY)
        )
        .next

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

      offer.getTransaction.getDealStatus shouldEqual DealStatus.PRIMARY_SALE
    }

    s"set ${DealStatus.PRIMARY_SALE_OF_SECONDARY} for on-sale new building " in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.UNKNOWN),
          buildingState = Some(BuildingState.BUILT),
          offerType = Some(OfferType.SELL),
          categoryType = Some(CategoryType.APARTMENT),
          flatType = Some(FlatType.NEW_FLAT)
        )
        .next

      val site = mock[Site]
      (site.getSaleStatus _).expects().anyNumberOfTimes().returning(SaleStatus.ON_SALE)
      (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

      offer.getTransaction.getDealStatus shouldEqual DealStatus.PRIMARY_SALE_OF_SECONDARY
    }

    s"set ${DealStatus.SALE} for sold-out new building" in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.UNKNOWN),
          buildingState = Some(BuildingState.BUILT),
          offerType = Some(OfferType.SELL),
          categoryType = Some(CategoryType.APARTMENT),
          flatType = Some(FlatType.NEW_FLAT)
        )
        .next

      val site = mock[Site]
      (site.getSaleStatus _).expects().anyNumberOfTimes().returning(SaleStatus.SOLD)
      (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

      offer.getTransaction.getDealStatus shouldEqual DealStatus.SALE
    }

    s"add error if deal status was not determined for new building" in {
      val rawOffer = RawOfferGenerator.rawOfferGen().next
      val offer = OfferModelGenerators
        .offerGen(
          dealStatus = Some(DealStatus.UNKNOWN),
          buildingState = Some(BuildingState.BUILT),
          offerType = Some(OfferType.SELL),
          categoryType = Some(CategoryType.APARTMENT),
          flatType = Some(FlatType.NEW_FLAT)
        )
        .next

      val site = mock[Site]
      (site.getSaleStatus _).expects().anyNumberOfTimes().returning(SaleStatus.UNKNOWN)
      (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

      unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

      offer.getOfferState.getErrors.asScala.exists(
        error =>
          error.getError == IndexingError.SITE_OFFER_BASE_FIELD_NOT_SET &&
            error.getDebugInfo == "DealStatusUnifier: Deal status was not determined"
      ) shouldBe (true)
    }

  }

}
