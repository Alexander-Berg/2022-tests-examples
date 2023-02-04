package services.amogus.storage.test.amoclient

import amogus.model.amo._
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import amogus.storage.amoClient.Codecs.AmoFieldCodec

class AmoFieldCodecsTest extends AnyWordSpecLike with Matchers {

  "AmoFieldCodecsTest" should {
    "amo field with non-empty values array should be encoded as expected" in {
      val inputField: AmoField = AmoField(
        idOrCode = Left(1L),
        values = Seq(
          CustomFieldMultitextValue(123, "some_value"),
          CustomFieldTextValue("another_value"),
          CustomFieldEnumValue(456, "enum_value")
        )
      )

      val requestJsonToAmo = parse("""{
         "field_id" : 1,
         "values" : [
           {
             "enum_id" : 123,
             "value" : "some_value"
           },
           {
             "value" : "another_value"
           },
           {
             "enum_id" : 456,
             "value" : "enum_value"
           }
         ]
       }""".stripMargin).getOrElse(???)

      inputField.asJson shouldBe requestJsonToAmo
    }

    "amo field with empty values array should be encoded as expected" in {
      val inputField: AmoField = AmoField(idOrCode = Left(1L), values = Seq.empty[CustomFieldEnumValue])

      val requestJsonToAmo = parse("""{
         "field_id" : 1,
         "values" : null}""".stripMargin).getOrElse(???)

      inputField.asJson shouldBe requestJsonToAmo
    }
  }
}
