package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.RawFilterWrapper.Mode
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.model.api.ApiModel.DetailMode
import ru.auto.catalog.core.testkit.syntax._
import ru.auto.catalog.model.api.ApiModel.RawCatalog

class MarkModelLayerBuilderSpec extends BaseSpec {

  private val catalog = TestCardCatalogWrapper

  "MarkModelLayerBuilder" should {
    "extract descriptions for marks and models with DetailMode.FULL" in {
      val filter =
        RawFilterWrapper(
          MARK,
          MODEL,
          Mode.Exact,
          DetailMode.FULL,
          mark = Some(mark"VOLKSWAGEN"),
          model = Some(model"SPACEFOX")
        )
      val result = catalog
        .filter(filter)(Right(RawCatalog.getDefaultInstance))
        .getOrElse(sys.error("right expected"))

      result.getMarkMap should contain.key("VOLKSWAGEN")
      val volkswagen = result.getMarkMap.get("VOLKSWAGEN")
      volkswagen.getDescription.toLowerCase should include("народный автомобиль")

      volkswagen.getModelMap() should contain.key("SPACEFOX")
      val spacefox = volkswagen.getModelMap().get("SPACEFOX")
      spacefox.getDescription should include("Spacefox появился")
    }
  }
}
