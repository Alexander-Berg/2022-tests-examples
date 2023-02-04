package services.amogus.storage.test.amoclient

import amogus.model.amo._
import amogus.model.company.Company
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import amogus.storage.amoClient.Codecs.CompanyEncoder

class CompanyCodecsTest extends AnyWordSpecLike with Matchers {

  "CompanyCodecsTest" should {
    "company should be encoded as expected" in {
      val inputField: Company = Company(
        id = 1L,
        name = Some("Some company"),
        responsibleUserId = Some(2L),
        customFieldsValues = Seq(
          AmoField(
            idOrCode = Left(1L),
            values = Seq(
              CustomFieldMultitextValue(123, "some_value"),
              CustomFieldTextValue("another_value"),
              CustomFieldEnumValue(456, "enum_value")
            )
          ),
          AmoField(
            idOrCode = Left(2L),
            values = Seq.empty[CustomFieldEnumValue]
          )
        )
      )

      val requestJsonToAmo = parse("""
         {
           "id" : 1,
           "name" : "Some company",
           "responsible_user_id" : 2,
           "custom_fields_values" : [
             {
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
             },
             {
               "field_id" : 2,
               "values" : null
             }
           ]}""".stripMargin).getOrElse(???)

      inputField.asJson shouldBe requestJsonToAmo
    }

    "company with empty optional fields should be encoded as expected" in {
      val inputField: Company = Company(
        id = 1L,
        name = None,
        responsibleUserId = None,
        customFieldsValues = Seq(
          AmoField(
            idOrCode = Left(1L),
            values = Seq(
              CustomFieldMultitextValue(123, "some_value"),
              CustomFieldTextValue("another_value"),
              CustomFieldEnumValue(456, "enum_value")
            )
          ),
          AmoField(
            idOrCode = Left(2L),
            values = Seq.empty[CustomFieldEnumValue]
          )
        )
      )

      val requestJsonToAmo = parse("""
         {
           "id" : 1,
           "custom_fields_values" : [
             {
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
             },
             {
               "field_id" : 2,
               "values" : null
             }
           ]}""".stripMargin).getOrElse(???)

      inputField.asJson shouldBe requestJsonToAmo
    }
  }
}
