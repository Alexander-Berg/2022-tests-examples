package ru.auto.api.model.searcher

import ru.auto.api.BaseSpec
import ru.auto.api.search.SearchModel.CatalogFilter

class CatalogFilterUtilsSpec extends BaseSpec {

  "serialize nameplate_name" in {
    val filter = CatalogFilter.newBuilder().setNameplateName("123").build()
    val serialized = CatalogFilterUtils.serialize(filter)
    serialized shouldBe "nameplate_name=123"
    CatalogFilterUtils.parse(serialized) shouldBe filter
  }

  "serialize nameplate" in {
    val filter = CatalogFilter.newBuilder().setNameplate(123).build()
    val serialized = CatalogFilterUtils.serialize(filter)
    serialized shouldBe "nameplate=123"
    CatalogFilterUtils.parse(serialized) shouldBe filter
  }

  "serialize both nameplate and nameplate_name" in {
    val filter = CatalogFilter.newBuilder().setNameplateName("123").setNameplate(123).build()
    val serialized = CatalogFilterUtils.serialize(filter)
    serialized shouldBe "nameplate=123,nameplate_name=123"
    CatalogFilterUtils.parse(serialized) shouldBe filter
  }

  // todo property-based specs
}
