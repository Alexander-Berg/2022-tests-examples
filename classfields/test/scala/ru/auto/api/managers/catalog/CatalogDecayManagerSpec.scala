package ru.auto.api.managers.catalog

import scala.jdk.CollectionConverters._
import ru.auto.api.BaseSpec
import ru.auto.api.CatalogModel

class CatalogDecayManagerSpec extends BaseSpec {
  val catalogDecayManager = new CatalogDecayManager

  "decay(Complectation)" should {
    "remove migration-flag from available options" in {
      val before =
        CatalogModel.Complectation
          .newBuilder()
          .addAvailableOptions("foo")
          .addAvailableOptions("migration-flag")
          .addAvailableOptions("bar")
          .build()
      val after = catalogDecayManager.decay(before)
      assert(after.getAvailableOptionsList.asScala == List("foo", "bar"))
    }
  }

  "decay(TechInfo)" should {
    "decay the nested Complectation" in {
      val before =
        CatalogModel.TechInfo
          .newBuilder()
          .setComplectation(
            CatalogModel.Complectation
              .newBuilder()
              .addAvailableOptions("foo")
              .addAvailableOptions("migration-flag")
          )
          .build()
      val after = catalogDecayManager.decay(before)
      assert(after.getComplectation.getAvailableOptionsList.asScala == List("foo"))
    }
  }

  "decay(CatalogEntity)" should {
    "decay the nested Complectation" in {
      val before =
        CatalogModel.CatalogEntity
          .newBuilder()
          .setComplectation(
            CatalogModel.Complectation
              .newBuilder()
              .addAvailableOptions("foo")
              .addAvailableOptions("migration-flag")
          )
          .build()
      val after = catalogDecayManager.decay(before)
      assert(after.getComplectation.getAvailableOptionsList.asScala == List("foo"))
    }
  }

  "decay(CatalogEntity)" should {
    "decay the nested CatalogEntity" in {
      val before =
        CatalogModel.CatalogEntityList
          .newBuilder()
          .addEntities(
            CatalogModel.CatalogEntity
              .newBuilder()
              .setComplectation(
                CatalogModel.Complectation
                  .newBuilder()
                  .addAvailableOptions("foo")
                  .addAvailableOptions("migration-flag")
              )
          )
          .build()
      val after = catalogDecayManager.decay(before)
      assert(after.getEntitiesCount == 1)
      assert(after.getEntities(0).getComplectation.getAvailableOptionsList.asScala == List("foo"))
    }
  }
}
