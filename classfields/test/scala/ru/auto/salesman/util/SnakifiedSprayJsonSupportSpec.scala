package ru.auto.salesman.util

import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.SnakifiedSprayJsonSupport._

class SnakifiedSprayJsonSupportSpec extends BaseSpec {

  case class CamelCaseNames(
      name: String,
      camelName: String,
      longCamelName: String,
      superLongCamelName: String
  )

  val camelCaseNamesFormat = jsonFormat4(CamelCaseNames)

  "SnakifiedSprayJsonSupport" should {
    "convert camel case names to snake case names" in {
      val camelCaseNames = CamelCaseNames("arg1", "arg2", "arg3", "arg4")
      val json = camelCaseNamesFormat.write(camelCaseNames)
      val fieldNames = json.asJsObject.fields.map { case (key, _) =>
        key
      }.toSet
      fieldNames shouldBe Set(
        "name",
        "camel_name",
        "long_camel_name",
        "super_long_camel_name"
      )
    }
  }
}
