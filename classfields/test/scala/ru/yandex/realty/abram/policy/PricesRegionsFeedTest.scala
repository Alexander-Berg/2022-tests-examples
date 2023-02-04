package ru.yandex.realty.abram.policy

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import org.scalatest.PrivateMethodTester
import ru.yandex.realty.model.sites.BuildingClass

@RunWith(classOf[JUnitRunner])
class PricesRegionsFeedTest extends SpecBase with PrivateMethodTester {

  "PricesRegionsFeed " should {
    "parse xml feed with distances" in {
      val getSitePrices = PrivateMethod[SitesPricesRegions]('getSitesPrices)
      val result = PricesRegionsFeed invokePrivate getSitePrices("/sites_prices_regions_test.xml")

      val expected = SitesPricesRegions(
        List(
          SitePriceRegionItem(
            213,
            BuildingClass.ECONOM,
            600000,
            Option(1200000),
            Option.empty,
            Option(DateTime.parse("2010-12-12T00:00:00")),
            Option(
              SitePricesDistances(
                List(
                  SitePricesDistance(0, 600000),
                  SitePricesDistance(11, 400000),
                  SitePricesDistance(21, 300000)
                )
              )
            )
          ),
          SitePriceRegionItem(
            213,
            BuildingClass.ELITE,
            1500000,
            Option(1800000),
            Option.empty,
            Option(DateTime.parse("2010-12-12T00:00:00")),
            Option(
              SitePricesDistances(
                List(
                  SitePricesDistance(0, 1500000),
                  SitePricesDistance(8, 1000000),
                  SitePricesDistance(16, 800000)
                )
              )
            )
          )
        )
      )

      result should be(expected)
    }
  }

}
