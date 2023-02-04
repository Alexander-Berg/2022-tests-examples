package ru.yandex.realty.handlers.developer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.sites.Company
import ru.yandex.realty.sites.SitesGroupingService

@RunWith(classOf[JUnitRunner])
class DeveloperStatisticBuilderSpec extends AsyncSpecBase {

  "DeveloperStatisticBuilderSpec" should {
    "buildStatistic not throwing UnsupportedOperation on empty holders" in {

      val sitesGroupingService = mock[SitesGroupingService]
      val builder = new DeveloperStatisticBuilder(sitesGroupingService)

      val company = new Company(1L)
      val statistic = builder.buildStatistic(company)
      statistic.getAllHouses shouldBe 0
    }
  }
}
