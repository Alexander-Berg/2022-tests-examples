package ru.auto.api.util.search.mappers

import org.scalatest.prop.TableDrivenPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.model.{Paging, Sorting, SortingByField}
import ru.auto.api.search.SearchModel.SearchRequestParameters
import ru.auto.api.ui.UiModel.SellerGroup._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.06.18
  */
class SellerGroupMapperSpec extends BaseSpec with TableDrivenPropertyChecks with TestRequestWithId with MockitoSupport {

  private val mapper = new SellerGroupMapper

  private def newRequest: SearchRequestParameters.Builder = {
    SearchRequestParameters.newBuilder()
  }

  implicit private val sorting: Sorting = SortingByField("field", desc = true)
  implicit private val paging: Paging = Paging.Default

  val enrichData = Table(
    ("seller_group", "dealer_org_type"),
    (Set(ANY_SELLER), Set(1, 2, 4)),
    (Set(PRIVATE), Set(4)),
    (Set(COMMERCIAL), Set(1, 2)),
    (Set(COMMERCIAL, PRIVATE), Set(1, 2, 4))
  )

  val decayData = Table(
    ("dealer_org_type", "seller_group"),
    (Set(1, 2, 4), Set(ANY_SELLER)),
    (Set(1, 4), Set(ANY_SELLER)),
    (Set(4), Set(PRIVATE)),
    (Set(1, 2), Set(COMMERCIAL)),
    (Set(1), Set(COMMERCIAL)),
    (Set(), Set(ANY_SELLER))
  )

  "SellerGroupMapper" should {
    "enrich dealer_org_type" in {
      forEvery(enrichData) { (sellerGroups, dealerOrgTypes) =>
        val searchRequest = newRequest
          .addAllSellerGroup(sellerGroups.asJava)

        val params = mapper.fromApiToSearcher(searchRequest)

        params("dealer_org_type").map(_.toInt) shouldBe dealerOrgTypes
      }
    }

    "fail enrich on invalid seller_group combination" in {
      an[IllegalArgumentException] shouldBe thrownBy {
        mapper.fromApiToSearcher(newRequest.addSellerGroup(ANY_SELLER).addSellerGroup(PRIVATE))
      }
    }

    "decay dealer_org_type to seller_group" in {
      forEvery(decayData) { (dealerOrgTypes, sellerGroups) =>
        val params = Map("dealer_org_type" -> dealerOrgTypes.map(_.toString))
        val builder = newRequest

        mapper.fromSearcherToApi(params, builder)

        builder.getSellerGroupList should contain theSameElementsAs sellerGroups
      }
    }
  }
}
