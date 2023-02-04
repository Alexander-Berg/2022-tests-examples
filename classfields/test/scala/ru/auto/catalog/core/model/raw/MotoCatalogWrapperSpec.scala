package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.RawFilterWrapper.Mode
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.model.api.ApiModel.DetailMode
import ru.auto.catalog.core.testkit.syntax._
import ru.auto.catalog.model.api.ApiModel.RawCatalog

import scala.jdk.CollectionConverters._

class MotoCatalogWrapperSpec extends BaseSpec {

  private val catalog = TestMotoCardCatalogWrapper

  "MotoCatalogWrapper" should {
    // Search logic and response details are checked elsewhere, here we only need a sanity check to make sure we didn't miss something.
    "extract exact subcategory" in {
      val filter =
        RawFilterWrapper(
          SUBCATEGORY,
          MODEL,
          Mode.Exact,
          DetailMode.FULL,
          subcategory = Some(subcategory"motorcycle"),
          mark = Some(mark"HUSQVARNA"),
          model = Some(model"LT_610_E")
        )
      val result = catalog.filter(filter)(Right(RawCatalog.getDefaultInstance)).getOrElse(sys.error("right expected"))

      val subcategories = result.getSubcategoryMap.asScala
      subcategories.keySet shouldEqual Set("motorcycle")
      val marks = subcategories.values.head.getMarkMap.asScala
      marks.keySet shouldEqual Set("HUSQVARNA")
      val models = marks.values.head.getModelMap.asScala
      models.keySet shouldEqual Set("LT_610_E")
    }
  }
}
