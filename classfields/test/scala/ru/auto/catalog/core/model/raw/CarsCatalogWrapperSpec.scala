package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.RawFilterWrapper.Mode
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.model.api.ApiModel.{DetailMode, RawCatalog}
import ru.auto.catalog.core.testkit.syntax._

import scala.jdk.CollectionConverters._

class CarsCatalogWrapperSpec extends BaseSpec {

  private val catalog = TestCardCatalogWrapper

  "CarsCatalogWrapper" should {
    // Search logic and response details are checked elsewhere, here we only need a sanity check to make sure we didn't miss something.
    "produce data from all layers" in {
      val filter = RawFilterWrapper(
        MARK,
        COMPLECTATION,
        Mode.Exact,
        DetailMode.SHORT,
        mark = Some(mark"BMW"),
        model = Some(model"X1"),
        superGen = Some(superGeneration"5017453"),
        configuration = Some(configuration"5018134"),
        techParam = Some(techParam"7150206"),
        complectation = Some(complectation"5018171")
      )
      val result = catalog.filter(filter)(Right(RawCatalog.getDefaultInstance)).getOrElse(sys.error("right expected"))

      result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
      result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet shouldEqual Set("X1")
      result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
      result.getConfigurationMap.asScala.keys.toSet shouldEqual Set("5018134")
      result.getTechParamMap.asScala.keys.toSet shouldEqual Set("7150206")
      result.getComplectationMap.asScala.keys.toSet shouldEqual Set("5018171")
    }
  }
}
