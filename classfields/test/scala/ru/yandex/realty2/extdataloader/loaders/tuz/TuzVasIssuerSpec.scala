package ru.yandex.realty2.extdataloader.loaders.tuz

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.abram.CallsTariff
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{CategoryType, Offer, OfferType}
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextProvider}
import ru.yandex.realty.storage.CampaignHeadersStorage
import ru.yandex.realty.proto.offer.{CampaignType, Product, ProductSource}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class TuzVasIssuerSpec extends SpecBase {
  val campaignHeadersProvider = mock[Provider[CampaignHeadersStorage]]
  val searchContextProvider = mock[SearchContextProvider[SearchContext]]
  val issuer = new TuzVasIssuer(campaignHeadersProvider, searchContextProvider)

  trait TuzVasIssuerFixture {

    val searchContextMock = toMockFunction1(
      searchContextProvider
        .doWithContext(_: SearchContext => List[Offer])
    )

    val vosUid = "123456"
    val feedId = 12345789

    val vosOffers = buildOffers(Regions.SPB_AND_LEN_OBLAST, 10)
    val feedOffers = buildOffers(Regions.KRASNODARSKYJ_KRAI, 50)
    val feedSpbOffers = buildOffers(Regions.SPB_AND_LEN_OBLAST, 100)
    val feedMskOffers = buildOffers(Regions.MSK_AND_MOS_OBLAST, 200)

    def buildOffers(regionId: Int, size: Int): List[Offer] = {
      val result = for {
        offerId <- Range(0, size)
        offer = new Offer()
      } yield {
        offer.setId(offerId)
        offer.setRelevance(1.0f)
        offer.setActive(true)
        offer.setOfferType(OfferType.SELL)
        offer.setCategoryType(CategoryType.GARAGE)
        offer.setUid("123456")
        val location = new Location()
        location.setSubjectFederation(regionId, 1)
        offer.setLocation(location)
        offer
      }
      result.toList
    }
  }

  "TuzVasIssuer in feedPartnerFeaturing" should {
    "on Maximum tariff" when {
      "apply 100% promotion + raising and 50% premium to feed offers" in new TuzVasIssuerFixture {
        searchContextMock.expects(*).returning(feedOffers)
        val result = issuer.feedPartnerFeaturing(feedId, CallsTariff.Maximum)
        result.isDefined shouldBe true
        val tuzVasOffers = result.get.getTuzVasOffers
        tuzVasOffers.getPromotionOfferIdsCount shouldBe feedOffers.size
        tuzVasOffers.getRaiseOfferIdsCount shouldBe feedOffers.size
        tuzVasOffers.getPremiumOfferIdsCount shouldBe (feedOffers.size * 0.5f).round
      }

      "apply 75% promotion + raising and 50% premium to feed offers from SPB" in new TuzVasIssuerFixture {
        searchContextMock.expects(*).returning(feedSpbOffers)
        val result = issuer.feedPartnerFeaturing(feedId, CallsTariff.Maximum)
        result.isDefined shouldBe true
        val tuzVasOffers = result.get.getTuzVasOffers
        tuzVasOffers.getPromotionOfferIdsCount shouldBe (feedSpbOffers.size * 0.75f).round
        tuzVasOffers.getRaiseOfferIdsCount shouldBe (feedSpbOffers.size * 0.75f).round
        tuzVasOffers.getPremiumOfferIdsCount shouldBe (feedSpbOffers.size * 0.5f).round
      }

      "apply 75% promotion + raising and 50% premium to feed offers from MSK" in new TuzVasIssuerFixture {
        searchContextMock.expects(*).returning(feedMskOffers)
        val result = issuer.feedPartnerFeaturing(feedId, CallsTariff.Maximum)
        result.isDefined shouldBe true
        val tuzVasOffers = result.get.getTuzVasOffers
        tuzVasOffers.getPromotionOfferIdsCount shouldBe (feedMskOffers.size * 0.75f).round
        tuzVasOffers.getRaiseOfferIdsCount shouldBe (feedMskOffers.size * 0.75f).round
        tuzVasOffers.getPremiumOfferIdsCount shouldBe (feedMskOffers.size * 0.5f).round
      }
    }
  }

  "TuzVasIssuer in vosFeaturing" should {
    "on Maximum tariff" when {
      "apply 100% promotion + raising and 50% premium to user" in new TuzVasIssuerFixture {
        searchContextMock.expects(*).returning(vosOffers)
        val result = issuer.vosFeaturing(vosUid, CallsTariff.Maximum)
        result.isDefined shouldBe true
        val tuzVasOffers = result.get.getTuzVasOffers
        tuzVasOffers.getPromotionOfferIdsCount shouldBe vosOffers.size
        tuzVasOffers.getRaiseOfferIdsCount shouldBe vosOffers.size
        tuzVasOffers.getPremiumOfferIdsCount shouldBe (vosOffers.size * 0.5f).round
      }
    }
  }

}
