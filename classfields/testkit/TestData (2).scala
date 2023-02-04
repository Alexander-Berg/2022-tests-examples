package ru.yandex.vertis.general.bonsai.testkit

import general.bonsai.attribute_model.{AddFormSettings, AttributeDefinition, BooleanSettings}
import general.bonsai.public_api.{CategoryData, InternalAttribute}
import scalapb.json4s.JsonFormat

import scala.io.{Codec, Source}

object TestData {

  val Bosonozhki: TestCategory = TestCategory(
    "bosonozhki_Sh6Wjv",
    Seq(
      "brand_EkyvVh",
      "brand_EkyvVh1",
      "brand_EkyvVh2",
      "brand_EkyvVh3",
      "brand_EkyvVh4",
      "brand_EkyvVh5",
      "brand_EkyvVh6"
    )
  )

  def category(id: String): CategoryData = {
    JsonFormat.fromJsonString[CategoryData](Source.fromResource(s"categories/$id.json")(Codec.UTF8).mkString)
  }

  case class TestCategory(categoryId: String, attributeIds: Seq[String])
}
