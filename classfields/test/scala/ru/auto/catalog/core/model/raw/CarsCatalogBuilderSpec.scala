package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.cars.CarsCatalogBuilder
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.core.util.Resources
import ru.auto.catalog.model.api.ApiModel.{DetailMode, ErrorMode}
import ru.yandex.auto.message.CatalogSchema.{CatalogCardMessage, CatalogDataMessage}
import ru.auto.catalog.core.testkit.syntax._

class CarsCatalogBuilderSpec extends BaseSpec with Resources {
  private val okCard = resources.toProto[CatalogCardMessage]("/raw/cars.catalog_card.correct.json")
  private val incorrectCard = resources.toProto[CatalogCardMessage]("/raw/cars.catalog_card.incorrect.json")
  private val emptyOptionsData = CatalogDataMessage.getDefaultInstance

  "CarsCatalogBuilder" should {
    "handle valid cards" in {
      val builder = new CarsCatalogBuilder(EmptyCarsSearchTagsInheritanceDecider)

      builder.addCard(okCard)

      val catalog = builder.build(emptyOptionsData)
      catalog
        .filter(
          RawFilterWrapper(
            from = MARK,
            to = COMPLECTATION,
            mode = RawFilterWrapper.Mode.SubTree,
            detailMode = DetailMode.FULL,
            errorMode = ErrorMode.FAIL_FAST,
            mark = Some(mark"ALFA_ROMEO"),
            model = Some(model"146")
          )
        )(EmptyCatalog)
        .getOrElse(sys.error("right expected"))
        .getConfigurationCount shouldBe 1
    }

    // Not sure if this requirement is necessary, but there was a similar test here before the rewrite.
    "keep data from invalid cards" in {
      val builder = new CarsCatalogBuilder(EmptyCarsSearchTagsInheritanceDecider)

      // We get an error when parsing the supergeneration, so lower layers in the card are discarded.
      an[IllegalStateException] shouldBe thrownBy {
        builder.addCard(incorrectCard)
      }

      val catalog = builder.build(emptyOptionsData)
      val result = catalog
        .filter(
          RawFilterWrapper(
            from = MARK,
            to = COMPLECTATION,
            mode = RawFilterWrapper.Mode.SubTree,
            detailMode = DetailMode.FULL,
            errorMode = ErrorMode.FAIL_FAST,
            mark = Some(mark"AUDI"),
            model = Some(model"A3")
          )
        )(EmptyCatalog)
        .getOrElse(sys.error("right expected"))

      // There is one supergeneration that was (partially) parsed.
      result.getSuperGenCount shouldBe 1
      // We didn't get to the configuration
      result.getConfigurationCount shouldBe 0
    }

    "fill verba_id" in {
      val builder = new CarsCatalogBuilder(EmptyCarsSearchTagsInheritanceDecider)
      builder.addCard(okCard)
      val catalog = builder.build(emptyOptionsData)
      val result = catalog
        .filter(
          RawFilterWrapper(
            from = MARK,
            to = COMPLECTATION,
            mode = RawFilterWrapper.Mode.SubTree,
            detailMode = DetailMode.SHORT,
            errorMode = ErrorMode.FAIL_FAST,
            mark = Some(mark"ALFA_ROMEO"),
            model = Some(model"146")
          )
        )(EmptyCatalog)
        .getOrElse(sys.error("right expected"))

      val markCard = result.getMarkOrThrow("ALFA_ROMEO")
      markCard.getEntity().getVerbaId() shouldBe 3137

      val modelCard = markCard.getModelOrThrow("146")
      modelCard.getEntity().getVerbaId() shouldBe 5152

      val superGenCard = result.getSuperGenOrThrow("4993318")
      superGenCard.getEntity().getVerbaId() shouldBe 12574

      val configurationCard = result.getConfigurationOrThrow("20464131")
      configurationCard.getEntity().getVerbaId() shouldBe 10226776

      val techParamCard = result.getTechParamOrThrow("20464218")
      techParamCard.getEntity().getVerbaId() shouldBe 10226923

      val complectationCard = result.getComplectationOrThrow("123123123")
      complectationCard.getEntity().getVerbaId() shouldBe 321321321
    }
  }
}
