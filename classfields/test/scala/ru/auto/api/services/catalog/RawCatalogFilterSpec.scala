package ru.auto.api.services.catalog

import ru.auto.api.BaseSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.model.ModelGenerators
import ru.auto.api.search.SearchModel.CatalogFilter

class RawCatalogFilterSpec extends BaseSpec with ScalaCheckPropertyChecks {
  "fromCatalogFilter" should {
    "ignore fields except the legacy set when legacyMode is enabled" in {
      forAll(ModelGenerators.CatalogFilterGen) { filter =>
        val legacyFilter = CatalogFilter
          .newBuilder()
          .setMark(filter.getMark())
          .setModel(filter.getModel())
          .setGeneration(filter.getGeneration())
          .setConfiguration(filter.getConfiguration())
          .setTechParam(filter.getTechParam())
          .setComplectation(filter.getComplectation())
          .build
        val result = RawCatalogFilter.fromCatalogFilter(filter, legacyMode = true)
        val expected = RawCatalogFilter.fromCatalogFilter(legacyFilter, legacyMode = true)
        result shouldBe expected
      }
    }
  }
}
