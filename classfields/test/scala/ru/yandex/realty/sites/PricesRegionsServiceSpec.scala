package ru.yandex.realty.sites

import com.google.protobuf.Int32Value
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.abram.proto.api.prices.regions.{
  SitePriceRegion,
  SitePricesDistance,
  SitePricesDistances,
  SitesPricesRegionsResponse
}
import ru.yandex.realty.clients.abram.AbramClient
import ru.yandex.realty.model.sites.BuildingClass
import ru.yandex.realty.proto.{BuildingClass => BuildingClassResponse}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PricesRegionsServiceSpec extends AsyncSpecBase {

  "PricesRegionFetcher " should {
    "get costs info from abram client and add zero distance if no distance specified " in {
      val abramClient = mock[AbramClient]
      val pricesWithoutDistances: Future[SitesPricesRegionsResponse] = Future.successful(
        SitesPricesRegionsResponse
          .newBuilder()
          .addSitesPricesRegions(
            SitePriceRegion
              .newBuilder()
              .setGeoId(1)
              .setBuildingClass(BuildingClassResponse.BUILDING_CLASS_ELITE)
              .setBid(4)
              .setSpecialPartnerPrice(Int32Value.newBuilder().setValue(5))
              .build()
          )
          .build()
      )

      (abramClient.getSitesPricesRegions(_: Traced)).expects(*).returning(pricesWithoutDistances).once()
      val fetcher = new PricesRegionsService(abramClient)
      val costsMap = fetcher.sitesPrices()
      val prices = costsMap.regionPrices
        .get(1)
        .flatMap(_.classPrices.get(BuildingClass.ELITE))
        .orNull
      prices.distancePrices should have size (1)
      prices.distancePrices.get(0).orNull.getSpecialPartnerPrice.getValue should be(5)
    }

    "parse costs info from abram client " in {
      val abramClient = mock[AbramClient]
      val prices: Future[SitesPricesRegionsResponse] = Future.successful(
        SitesPricesRegionsResponse
          .newBuilder()
          .addSitesPricesRegions(
            SitePriceRegion
              .newBuilder()
              .setGeoId(1)
              .setBuildingClass(BuildingClassResponse.BUILDING_CLASS_BUSINESS)
              .setBid(10)
              .setSpecialPartnerPrice(Int32Value.newBuilder().setValue(11))
              .setSitePricesDistances(
                SitePricesDistances
                  .newBuilder()
                  .addSitePricesDistance(SitePricesDistance.newBuilder().setFrom(0).setPrice(10))
                  .addSitePricesDistance(SitePricesDistance.newBuilder().setFrom(1).setPrice(8))
                  .addSitePricesDistance(SitePricesDistance.newBuilder().setFrom(3).setPrice(6))
                  .addSitePricesDistance(SitePricesDistance.newBuilder().setFrom(5).setPrice(3))
              )
          )
          .build()
      )
      (abramClient.getSitesPricesRegions(_: Traced)).expects(*).returning(prices).once()
      val fetcher = new PricesRegionsService(abramClient)
      val costsMap = fetcher.sitesPrices()
      val pricesMap = costsMap.regionPrices
        .get(1)
        .flatMap(_.classPrices.get(BuildingClass.BUSINESS))
        .orNull
      pricesMap.distancePrices should have size (4)
      pricesMap.distancePrices.get(3).orNull.getSpecialPartnerPrice.getValue should be(6)
    }
  }
}
