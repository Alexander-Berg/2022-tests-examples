package ru.auto.api.model

import org.scalatest.Inspectors
import org.scalatest.prop.TableFor2
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.BaseSpec
import ru.auto.api.model.CategorySelector.{All, Cars, Moto, Trucks}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 25.02.17
  */
class CategorySelectorSpec extends BaseSpec with ScalaCheckPropertyChecks with Inspectors {

  "CategorySelector" should {
    "be extracted from Category" in {
      CategorySelector.from(Category.CARS) shouldBe Cars
      CategorySelector.from(Category.MOTO) shouldBe Moto
      CategorySelector.from(Category.TRUCKS) shouldBe Trucks
    }
    "pattern-matchable" in {
      val data: TableFor2[String, CategorySelector] = Table(
        ("value", "selector"),
        ("cars", Cars),
        ("moto", Moto),
        ("trucks", Trucks),
        ("all", All),
        ("CaRs", Cars)
      )

      forAll(data) { (value, selector) =>
        (value: @unchecked) match {
          case CategorySelector(extracted) => extracted shouldBe selector
        }
      }
    }
    "throw IAE on parsing unknown values" in {
      intercept[IllegalArgumentException] {
        CategorySelector.parse("invalid")
      }
    }

    "round-trip toString -> parse" in {
      forEvery(CategorySelector.values)(selector => CategorySelector.parse(selector.toString) shouldBe selector)
    }
  }
}
