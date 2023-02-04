package ru.yandex.vertis.parsing.parsers

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ScrapingHubJsonUtilsTest extends FunSuite {
  test("getFields") {
    val json = Json.obj(
      "f1" -> 1,
      "f2" -> "str",
      "f3" -> Json.obj(
        "f4" -> 2,
        "f5" -> "str2",
        "f6" -> Json.obj(
          "f7" -> 3
        )
      )
    )
    assert(ScrapingHubJsonUtils.getFields(json) == List("f1", "f2", "f3.f4", "f3.f5", "f3.f6.f7"))
  }
}
