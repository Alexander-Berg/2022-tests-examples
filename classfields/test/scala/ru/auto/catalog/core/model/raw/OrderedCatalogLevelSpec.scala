package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import org.scalatest.prop.TableDrivenPropertyChecks._
import ru.auto.catalog.model.api.ApiModel.CatalogLevel

class OrderedCatalogLevelSpec extends BaseSpec {

  "OrderedCatalogLevel" should {
    "have one-to-one correspondence with CatalogLevel" in forAll(
      Table("CatalogLevel", CatalogLevel.values.toIndexedSeq: _*)
    ) { v =>
      OrderedCatalogLevel.fromApi(v).toApi shouldBe v
    }
  }
}
