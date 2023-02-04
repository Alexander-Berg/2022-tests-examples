package ru.auto.catalog.core.model.raw

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.RawFilterWrapper.Mode
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel._
import ru.auto.catalog.core.testkit._
import ru.auto.catalog.model.api.ApiModel.DetailMode

import ru.auto.catalog.core.testkit.syntax.LiteralIds

class TechParamLayerSpec extends BaseSpec {

  private val layer = TestCardCatalogWrapper.techParamLayer

  "TechParamLayer" should {
    "look up elements by filter nameplate" in {
      val filter =
        RawFilterWrapper(
          TECH_PARAM,
          TECH_PARAM,
          Mode.Exact,
          DetailMode.SHORT,
          nameplateSemanticUrl = Some(nameplateSemanticUrl"23d")
        )
      val Some(criterion) = layer.getCriterion(filter)
      val result = layer.lookup(criterion)

      result shouldBe Set(techParam"7150205", techParam"5018167")
    }

    "look up elements by filter nameplate with matching tech parameter id" in {
      val filter =
        RawFilterWrapper(
          TECH_PARAM,
          TECH_PARAM,
          Mode.Exact,
          DetailMode.SHORT,
          nameplateSemanticUrl = Some(nameplateSemanticUrl"23d"),
          techParam = Some(techParam"7150205")
        )
      val Some(criterion) = layer.getCriterion(filter)
      val result = layer.lookup(criterion)

      result shouldBe Set(techParam"7150205")
    }

    "return empty set for unknown nameplate" in {
      val filter =
        RawFilterWrapper(
          TECH_PARAM,
          TECH_PARAM,
          Mode.Exact,
          DetailMode.SHORT,
          mark = Some(mark"BMW"),
          model = Some(model"X1"),
          nameplateSemanticUrl = Some(nameplateSemanticUrl"no such nameplate")
        )
      val Some(criterion) = layer.getCriterion(filter)
      val result = layer.lookup(criterion)

      result shouldBe empty
    }

    "return empty set for mismatched nameplate and tech parameter id" in {
      val filter =
        RawFilterWrapper(
          TECH_PARAM,
          TECH_PARAM,
          Mode.Exact,
          DetailMode.SHORT,
          nameplateSemanticUrl = Some(nameplateSemanticUrl"23d"),
          // This is from a different model and has different nameplate
          techParam = Some(techParam"21126878")
        )
      val Some(criterion) = layer.getCriterion(filter)
      val result = layer.lookup(criterion)

      result shouldBe empty
    }

    "use nameplate URL rather than name" in {
      val filter =
        RawFilterWrapper(
          TECH_PARAM,
          TECH_PARAM,
          Mode.Exact,
          DetailMode.SHORT,
          mark = Some(mark"KIA"),
          model = Some(model"RIO"),
          // This is different from the nameplate name!
          nameplateSemanticUrl = Some(nameplateSemanticUrl"x_line")
        )

      val Some(criterion) = layer.getCriterion(filter)
      val result = layer.lookup(criterion)

      result shouldBe Set(
        techParam"21126878",
        techParam"21207656",
        techParam"21207691",
        techParam"21126879",
        techParam"21126876",
        techParam"21126877"
      )
    }
  }
}
