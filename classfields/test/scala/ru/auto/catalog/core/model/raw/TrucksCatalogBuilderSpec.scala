package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.trucks.TrucksCatalogBuilder
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.core.util.Resources
import ru.auto.catalog.model.api.ApiModel.{DetailMode, ErrorMode}
import ru.yandex.auto.message.CatalogSchema.CatalogDataMessage
import ru.auto.catalog.core.testkit.syntax._
import ru.auto.catalog.core.testkit.verbaTrucks
import ru.yandex.auto.message.TrucksCatalogSchema.TrucksCatalogCardMessage

class TrucksCatalogBuilderSpec extends BaseSpec with Resources {
  private val okCard = resources.toProto[TrucksCatalogCardMessage]("/raw/trucks.catalog_card.correct.json")
  private val emptyOptionsData = CatalogDataMessage.getDefaultInstance

  "TrucksCatalogBuilder" should {
    "fill verba_id" in {
      val builder = new TrucksCatalogBuilder(verbaTrucks)
      builder.addCard(okCard)
      val catalog = builder.build(emptyOptionsData)
      val result = catalog
        .filter(
          RawFilterWrapper(
            from = SUBCATEGORY,
            to = MODEL,
            mode = RawFilterWrapper.Mode.SubTree,
            detailMode = DetailMode.SHORT,
            errorMode = ErrorMode.FAIL_FAST,
            mark = Some(mark"CITROEN")
          )
        )(EmptyCatalog)
        .getOrElse(sys.error("right expected"))

      val subcategoryCard = result.getSubcategoryOrThrow("LCV")
      subcategoryCard.getEntity().getVerbaId() shouldBe 11598470

      val markCard = subcategoryCard.getMarkOrThrow("CITROEN")
      markCard.getEntity().getVerbaId() shouldBe 11603436

      val modelCard = markCard.getModelOrThrow("NEMO")
      modelCard.getEntity().getVerbaId() shouldBe 11603453
    }
  }
}
