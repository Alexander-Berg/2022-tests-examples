package ru.yandex.vos2.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.{JsNull, Json}
import ru.yandex.vos2.util.JsonUtils._

@RunWith(classOf[JUnitRunner])
class JsonUtilsTest extends FunSuite {

  test("omit nulls from json") {
    val json = Json.obj(
      "a" -> 1,
      "b" -> JsNull,
      "c" -> 3,
      "d" -> Json.arr(1, 2, 3, JsNull, 5),
      "e" -> Json.obj(
        "f" -> 6,
        "g" -> JsNull,
        "h" -> Json.arr(JsNull, 2, 3, 4, 5)
      ),
      "i" -> Json.obj(
        "j" -> 6,
        "k" -> Json.obj(
          "l" -> 8,
          "m" -> JsNull
        ),
        "n" -> Json.arr(JsNull, JsNull, 3, 4, 5)
      )
    )

    val withoutNulls = json.omitNulls
    assert(withoutNulls == Json.obj(
      "a" -> 1,
      "c" -> 3,
      "d" -> Json.arr(1, 2, 3, 5),
      "e" -> Json.obj(
        "f" -> 6,
        "h" -> Json.arr(2, 3, 4, 5)
      ),
      "i" -> Json.obj(
        "j" -> 6,
        "k" -> Json.obj(
          "l" -> 8
        ),
        "n" -> Json.arr(3, 4, 5)
      )
    ))
  }

}
