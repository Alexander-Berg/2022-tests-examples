package ru.auto.api.util.search

import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.model.CategorySelector.{Cars, Trucks}
import ru.auto.api.model.SearcherQuery
import ru.auto.api.model.searcher.SearcherRequest

class SearchesUtilsSpec extends BaseSpec {
  "SearchesUtils" should {
    "generate id" in {
      val id = SearchesUtils.generateId(SearcherRequest(Cars, SearcherQuery("a=b&b=c")))
      id shouldBe "74aa71fc2ee903828fadab4f0adc56a30bb32bbd"
    }

    "generate id which ignore order of parameters" in {
      val id1 = SearchesUtils.generateId(SearcherRequest(Cars, SearcherQuery("a=b&b=c")))
      val id2 = SearchesUtils.generateId(SearcherRequest(Cars, SearcherQuery("b=c&a=b")))
      id1 shouldBe id2
    }

    "use category to generate id" in {
      val id1 = SearchesUtils.generateId(SearcherRequest(Cars, SearcherQuery("a=b&b=c")))
      val id2 = SearchesUtils.generateId(SearcherRequest(Trucks, SearcherQuery("a=b&b=c")))
      id1 should not be id2
    }

    "support exсlude catalog filter if necessary features was provided" in {
      val features = Set(ClientFeature.SEARCH_EXCLUDE_CATALOG_FILTER_CARS)
      SearchesUtils.supportExcludeCatalogFilter(Cars, features) shouldBe true
    }

    "do not support exсlude catalog filter if necessary features wasn't provided" in {
      val features = Set(ClientFeature.SEARCH_EXCLUDE_CATALOG_FILTER_TRUCKS)
      SearchesUtils.supportExcludeCatalogFilter(Cars, features) shouldBe false
    }
  }
}
