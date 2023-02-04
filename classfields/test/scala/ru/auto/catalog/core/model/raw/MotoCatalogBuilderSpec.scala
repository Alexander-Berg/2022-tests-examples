package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.moto.MotoCatalogBuilder
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.core.util.Resources
import ru.auto.catalog.model.api.ApiModel.{DetailMode, ErrorMode}
import ru.yandex.auto.message.CatalogSchema.CatalogDataMessage
import ru.auto.catalog.core.testkit.syntax._
import ru.auto.catalog.core.testkit.verbaMoto
import ru.yandex.auto.message.MotoCatalogSchema.MotoCatalogCardMessage

class MotoCatalogBuilderSpec extends BaseSpec with Resources {
  private val okCard = resources.toProto[MotoCatalogCardMessage]("/raw/moto.catalog_card.correct.json")
  private val emptyOptionsData = CatalogDataMessage.getDefaultInstance

  "MotoCatalogBuilder" should {
    "fill verba_id" in {
      val builder = new MotoCatalogBuilder(verbaMoto)
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
            mark = Some(mark"HONDA")
          )
        )(EmptyCatalog)
        .getOrElse(sys.error("right expected"))

      val subcategoryCard = result.getSubcategoryOrThrow("atv")
      subcategoryCard.getEntity().getVerbaId() shouldBe 11144164

      val markCard = subcategoryCard.getMarkOrThrow("HONDA")
      markCard.getEntity().getVerbaId() shouldBe 11144191

      val modelCard = markCard.getModelOrThrow("ATC_200X")
      modelCard.getEntity().getVerbaId() shouldBe 11150248
    }
  }
}
