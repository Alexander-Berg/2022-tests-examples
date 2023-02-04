package ru.yandex.verba.core.model.domain


import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.model.Entity
import ru.yandex.verba.core.util.Logging

import scala.util.Try

/**
  * User: Vladislav Dolbilov (darl@yandex-team.ru)
  * Date: 25.04.13 0:21
  */
class ServiceTest extends AnyFreeSpec with Matchers with Logging {

  object validJson extends BePropertyMatcher[String] {

    import org.json4s.jackson.JsonMethods._

    def apply(str: String) = {
      BePropertyMatchResult(matches = Try(parse(str)).isSuccess, "json")
    }
  }

  "Service" - {
    "should be" - {
      "convertible to json" in {
        val service = Service(
          "auto",
          "Auto",
          Seq(
            Entity(1L, "marks", "Marks", 1L, Path.Empty / "marks"),
            Entity(2L, "color", "Color", 1L, Path.Empty / "color"),
            Entity(3L, "in_stock", "In stock", 1L, Path.Empty / "in_stock"),
            Entity(4L, "seller_city", "Seller city", 1L, Path.Empty / "seller_city")
          )
        )
        service.asJsonString should be(validJson)
      }
    }
  }

}
