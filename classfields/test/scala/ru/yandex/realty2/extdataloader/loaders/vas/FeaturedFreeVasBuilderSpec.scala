package ru.yandex.realty2.extdataloader.loaders.vas

import com.google.protobuf.util.Timestamps
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.vas.SiteVasEntry
import ru.yandex.realty.util.Partners.SamoletPartnerId
import ru.yandex.realty.proto.unified.offer.images.{MdsImageId, RealtyPhotoInfo, UnifiedImages}
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextProvider}

@RunWith(classOf[JUnitRunner])
class FeaturedFreeVasBuilderSpec extends SpecBase {
  val searchContextProvider = mock[SearchContextProvider[SearchContext]]

  val fetcher =
    new FeaturedFreeVasBuilder(searchContextProvider)

  trait FeaturedFreeVasBuilderFixture {
    val siteVasEntry = SiteVasEntry(1234L, 236L, None, None)
    val samoletSiteVasEntry = SiteVasEntry(SamoletPartnerId, 236L, None, None)

    val spbOffers = buildOffers(Regions.SPB_AND_LEN_OBLAST, 100)
    val mskOffers = buildOffers(Regions.MSK_AND_MOS_OBLAST, 50)
    val novosibirskOffers = buildOffers(Regions.NOVOSIBIRSK, 60)
    val otherGeoOffers = buildOffers(Regions.KRASNODAR, 200)
    val samoletOffers = buildOffers(Regions.SPB_AND_LEN_OBLAST, 300, SamoletPartnerId)

    val searchContextMock = toMockFunction1(
      searchContextProvider
        .doWithContext(_: SearchContext => List[Offer])
    )

    def buildOffers(regionId: Int, size: Int, partnerId: Long = siteVasEntry.partnerId): List[Offer] = {
      val result = for {
        offerId <- Range(0, size)
        offer = new Offer()
      } yield {
        offer.setId(offerId)
        offer.setPartnerId(partnerId)
        val buildingInfo = new BuildingInfo()
        buildingInfo.setSiteId(siteVasEntry.siteId)
        offer.setBuildingInfo(buildingInfo)
        offer.setActive(true)
        val location = new Location()
        location.setSubjectFederation(regionId, 1)
        offer.setLocation(location)
        offer.setRelevance(1.0f)
        offer.setPhotos(
          UnifiedImages
            .newBuilder()
            .addImage(
              RealtyPhotoInfo
                .newBuilder()
                .setExternalUrl("http://example.org/akme.jpg")
                .setMdsId(
                  MdsImageId
                    .newBuilder()
                    .setKnownNamespace(MdsImageId.KnownNamespace.REALTY)
                    .setGroup(1)
                    .setName("abc")
                )
                .setCreated(Timestamps.fromMillis(System.currentTimeMillis()))
            )
            .build(),
          10
        )
        offer
      }
      result.toList
    }
  }

  "FeaturedFreeVasFetcher in siteFeaturing" should {
    "promote 75% offers from MSK" in new FeaturedFreeVasBuilderFixture {
      searchContextMock.expects(*).returning(mskOffers)
      val result = fetcher.siteFeaturing(siteVasEntry)
      result.getPromotionOfferIdsList.size shouldBe (mskOffers.size * 0.75d).round
      result.getRaiseOfferIdsList.size shouldBe 0
      result.getPremiumOfferIdsList.size shouldBe 0
    }

    "promote 75% offers from SPB" in new FeaturedFreeVasBuilderFixture {
      searchContextMock.expects(*).returning(spbOffers)
      val result = fetcher.siteFeaturing(siteVasEntry)
      result.getPromotionOfferIdsList.size shouldBe (spbOffers.size * 0.75d).round
      result.getRaiseOfferIdsList.size shouldBe 0
      result.getPremiumOfferIdsList.size shouldBe 0
    }

    "apply promotion + raising + premium to 100% offers from Novosibirsk" in new FeaturedFreeVasBuilderFixture {
      searchContextMock.expects(*).returning(novosibirskOffers)
      val result = fetcher.siteFeaturing(siteVasEntry)
      result.getPromotionOfferIdsList.size shouldBe novosibirskOffers.size
      result.getRaiseOfferIdsList.size shouldBe novosibirskOffers.size
      result.getPremiumOfferIdsList.size shouldBe novosibirskOffers.size
    }

    "apply promotion + raising to 60% offers from other regions" in new FeaturedFreeVasBuilderFixture {
      searchContextMock.expects(*).returning(otherGeoOffers)
      val result = fetcher.siteFeaturing(siteVasEntry)
      result.getPromotionOfferIdsList.size shouldBe (otherGeoOffers.size * 0.6d).round
      result.getRaiseOfferIdsList.size shouldBe (otherGeoOffers.size * 0.6d).round
      result.getPremiumOfferIdsList.size shouldBe 0
    }

    "apply promotion + raising + premium to 100% offers from samolet special feed" in new FeaturedFreeVasBuilderFixture {
      searchContextMock.expects(*).returning(samoletOffers)
      val result = fetcher.siteFeaturing(samoletSiteVasEntry)
      result.getPromotionOfferIdsList.size shouldBe samoletOffers.size
      result.getRaiseOfferIdsList.size shouldBe samoletOffers.size
      result.getPremiumOfferIdsList.size shouldBe samoletOffers.size
    }
  }
}
