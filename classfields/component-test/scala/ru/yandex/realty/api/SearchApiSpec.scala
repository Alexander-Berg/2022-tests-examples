package ru.yandex.realty.api

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AbstractBnbSearcherComponentTestSpec
import ru.yandex.realty.componenttest.data.sites.Site_1754609
import ru.yandex.realty.componenttest.data.sites.Site_73030
import ru.yandex.realty.model.message.ExtDataSchema.SiteApartmentTypeNamespace.SiteApartmentType
import ru.yandex.realty.render.search.RenderableSearchResponse

@RunWith(classOf[JUnitRunner])
class SearchApiSpec extends AbstractBnbSearcherComponentTestSpec {

  "GET /newbuildingSearch" should {

    "return newbuilding by ID" in {
      val siteId = Site_73030.Id
      Get(s"/newbuildingSearch?siteId=$siteId") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[RenderableSearchResponse]
          val newbuilding = response.result.items.head
          newbuilding.id should be(siteId)
          newbuilding.buildingFeatures.apartmentType == SiteApartmentType.APARTMENTS_AND_FLATS
        }
    }

    "return sites with PLUS_4 rooms" in {

      Get("/newbuildingSearch?roomsTotal=PLUS_4") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[RenderableSearchResponse]
          response.result.items.map(_.id).exists(_ == Site_1754609.Id) should be(true)
        }

    }
  }

}
