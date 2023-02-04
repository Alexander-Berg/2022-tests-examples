package ru.yandex.vertis.feature.impl

import java.time.Instant

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.feature.model.{SerDe, TypedValue}

class TypedValueSerDeSpec
  extends WordSpec
    with Matchers {

  val serDe: SerDe[TypedValue] = TypedValueSerDe.JsonSerDe

  "TypedValueSerDe" should {
    "correctly serialize-deserialize full TypedValue" in {
      val someTimestamp = Instant.parse("2021-05-20T13:37:16.289Z")
      val typedValue = TypedValue("type", "value", Set("tag1", "tag2"), Some("info"), history = Seq(
        TypedValue.ChangeEvent(timestamp = someTimestamp, value = "value", operator = Some("login"), comment = Some("comment"))
      ))

      val expected =
        s"""
           |{
           |"history":[{"timestamp":"2021-05-20T13:37:16.289Z","value":"value","operator":"login","comment":"comment"}],
           |"tags":["tag1","tag2"],
           |"info":"info",
           |"type":"type",
           |"value":"value"
           |}
       """.stripMargin.replaceAll("\\s", "")

      val serialized = serDe.serialize(typedValue)

      serialized shouldBe expected

      serDe.deserialize(serialized).get shouldBe typedValue
    }

    "correctly serialize-deserialize old TypedValue" in {
      val typedValue = TypedValue("type", "value", Set("tag1", "tag2"), Some("info"))
      val expected =
        s"""
           |{
           |"history":[],
           |"tags":["tag1","tag2"],
           |"info":"info",
           |"type":"type",
           |"value":"value"
           |}
       """.stripMargin.replaceAll("\\s", "")

      val serialized = serDe.serialize(typedValue)

      serialized shouldBe expected

      serDe.deserialize(serialized).get shouldBe typedValue
    }

    "correctly serialize-deserialize TypedValue with empty meta" in {
      val typedValue = TypedValue("type", "value")

      val expected =
        s"""
           |{
           |"history":[],
           |"tags":[],
           |"info":null,
           |"type":"type",
           |"value":"value"
           |}
       """.stripMargin.replaceAll("\\s", "")

      val serialized = serDe.serialize(typedValue)

      serialized shouldBe expected

      serDe.deserialize(serialized).get shouldBe typedValue
    }

    "correctly deserialize TypedValue with no meta set" in {
      val json =
        s"""
           |{
           |"type":"type",
           |"value":"value"
           |}
       """.stripMargin.replaceAll("\\s", "")

      val expected = TypedValue("type", "value")

      serDe.deserialize(json).get shouldBe expected
    }
  }
}
