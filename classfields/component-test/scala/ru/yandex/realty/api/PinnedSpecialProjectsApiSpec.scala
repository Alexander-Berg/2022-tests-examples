package ru.yandex.realty.api

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AbstractBnbSearcherComponentTestSpec
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.render.search.RenderableSearchResponse

@RunWith(classOf[JUnitRunner])
class PinnedSpecialProjectsApiSpec extends AbstractBnbSearcherComponentTestSpec {

  "GET /newbuildingSearch" should {

    "response with pinned sites in sbp region" in {
      Get(s"/newbuildingSearch?rgid=${NodeRgid.SPB_AND_LEN_OBLAST}") ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = entityAs[RenderableSearchResponse]
          response.result.pinnedItems.size shouldBe 1
          response.result.pinnedItems.head.id shouldBe Site_57547.Id
        }
    }

    "response with pinned sites in child geo of spb region" in {
      Get(s"/newbuildingSearch?rgid=${NodeRgid.SPB}&parkingType=UNDERGROUND") ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = entityAs[RenderableSearchResponse]
          response.result.pinnedItems.size shouldBe 1
          response.result.pinnedItems.head.id shouldBe Site_57547.Id
        }
    }

    "response with no pinned sites because of geo " in {
      Get(s"/newbuildingSearch?rgid=${NodeRgid.NIZHNY_NOVGOROD_OBLAST}") ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = entityAs[RenderableSearchResponse]
          response.result.pinnedItems shouldBe empty
        }
    }

    "response with no pinned sites because of filters " in {
      Get(s"/newbuildingSearch?rgid=${NodeRgid.SPB_AND_LEN_OBLAST}&parkingType=OPEN") ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = entityAs[RenderableSearchResponse]
          response.result.pinnedItems shouldBe empty
        }
    }
  }

}
