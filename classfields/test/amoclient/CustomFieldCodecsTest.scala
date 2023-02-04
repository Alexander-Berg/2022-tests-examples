package services.amogus.storage.test.amoclient

import amogus.model.amo._
import io.circe.Encoder.encodeSeq
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CustomFieldCodecsTest extends AnyWordSpecLike with Matchers {

  "CustomFieldCodecsTest" should {
    import amogus.storage.amoClient.Codecs.CustomFieldValueCodec

    "all custom field types should be encoded as expected" in {
      val inputField: Seq[CustomFieldValue] = Seq(
        CustomFieldTextValue("simple_value"),
        CustomFieldEnumValue.ofId(1234683L),
        CustomFieldEnumValue.ofValue("enum_value_instead_of_id"),
        CustomFieldEnumValue(38562195, "enum_value_with_id"),
        CustomFieldMultitextValue(5927869, "multitext_value")
      )

      val requestJsonToAmo = parse("""[
                               |  {
                               |    "value": "simple_value"
                               |  },
                               |  {
                               |    "enum_id": 1234683
                               |  },
                               |  {
                               |    "value": "enum_value_instead_of_id"
                               |  },
                               |  {
                               |    "enum_id": 38562195,
                               |    "value": "enum_value_with_id"
                               |  },
                               |  {
                               |    "enum_id": 5927869,
                               |    "value": "multitext_value"
                               |  }
                               |]""".stripMargin).getOrElse(???)

      inputField.asJson shouldBe requestJsonToAmo
    }

    "all custom field types should be decoded as expected" in {
      val requestStringFromAmo = parse("""[
                                        |  {
                                        |    "type": "some_type",
                                        |    "value": "simple_value"
                                        |  },
                                        |  {
                                        |    "type": "select",
                                        |    "enum_id": 38562195,
                                        |    "value": "enum_value_with_id"
                                        |  },
                                        |  {
                                        |    "type": "multitext",
                                        |    "enum_id": 5927869,
                                        |    "value": "multitext_value"
                                        |  },
                                        |  {
                                        |    "value": "text_without_type"
                                        |  },
                                        |  {
                                        |    "value": 1234567890
                                        |  },
                                        |  {
                                        |    "value": true
                                        |  }
                                        |]""".stripMargin).getOrElse(???)

      val expectedParsedResult: Seq[CustomFieldValue] = Seq(
        CustomFieldTextValue("simple_value"),
        CustomFieldEnumValue(38562195, "enum_value_with_id"),
        CustomFieldMultitextValue(5927869, "multitext_value"),
        CustomFieldTextValue("text_without_type"),
        CustomFieldNumericValue(1234567890),
        CustomFieldCheckboxValue(true)
      )

      requestStringFromAmo.as[Seq[CustomFieldValue]] shouldBe Right(expectedParsedResult)
    }
  }

}
