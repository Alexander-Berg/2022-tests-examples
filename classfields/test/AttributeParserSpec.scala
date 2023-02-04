package ru.yandex.vertis.general.bonsai.model.test

import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.attribute_model.{AddFormSettings, AttributeDefinition, DictionarySettings, NumberSettings}
import general.bonsai.lang_model.{Linguistics, NameForms}
import ru.yandex.vertis.general.bonsai.model.{
  AttributeInvalidAddFormSettings,
  DuplicateAttributeDictionaryKeys,
  FieldIsReadOnly,
  FieldMalformed,
  FieldRequired,
  FieldRequiredBecause,
  FieldValidationError,
  MessageValidationError
}
import ru.yandex.vertis.general.bonsai.model.validation.{ApiModelParser, AttributeParser}
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test.{testM, _}

object AttributeParserSpec extends DefaultRunnableSpec {
  val validator: ApiModelParser[AttributeDefinition, AttributeDefinition] = AttributeParser.create("attribute")

  val validAttribute = AttributeDefinition(
    uriPart = "part_-",
    name = "name",
    uniqueName = "unique-name",
    linguistics = Some(Linguistics()),
    nameForms = Some(
      NameForms(
        accusativeSingular = "accusativeSingular",
        accusativePlural = "accusativePlural",
        genitivePlural = "genitivePlural"
      )
    ),
    attributeSettings = AttributeDefinition.AttributeSettings.NumberSettings(NumberSettings(0, 100)),
    addFormSettings = Some(AddFormSettings(AddFormSettings.AddFormControl.INPUT))
  )

  def hasError(assertion: Assertion[FieldValidationError]): Assertion[Any] = {
    isSubtype[MessageValidationError](
      hasField(
        "errors",
        _.errors.head,
        assertion
      )
    )
  }

  def isFieldRequired(fieldNameAssertion: Assertion[String]): Assertion[Any] = {
    isSubtype[FieldRequired](Assertion.hasField("fieldName", _.fieldName, fieldNameAssertion))
  }

  def isFieldMalformed(
      fieldName: Assertion[String],
      comment: Assertion[String] = Assertion.anything): Assertion[Any] = {
    isSubtype[FieldMalformed](
      Assertion.hasField[FieldMalformed, String]("fieldName", _.fieldName, fieldName) &&
        Assertion.hasField[FieldMalformed, String]("comment", _.comment, comment)
    )
  }

  def assertValidationFailure(
      attribute: AttributeDefinition
    )(assertion: Assertion[MessageValidationError]): ZIO[Any, Nothing, TestResult] = {
    assertM(validator.required(Some(attribute)).run)(
      fails(
        assertion
      )
    )
  }

  def assertFieldMissing(attribute: AttributeDefinition, fieldName: String): ZIO[Any, Nothing, TestResult] = {
    assertValidationFailure(attribute) {
      hasError(isFieldRequired(equalTo(fieldName)))
    }
  }

  def assertFieldMissingBecause(attribute: AttributeDefinition, fieldName: String): ZIO[Any, Nothing, TestResult] = {
    assertValidationFailure(attribute) {
      hasError(
        isSubtype[FieldRequiredBecause](Assertion.hasField("message", _.fieldName, equalTo(fieldName)))
      )
    }
  }

  def assertInvalidFormSettings(attribute: AttributeDefinition): ZIO[Any, Nothing, TestResult] = {
    assertValidationFailure(attribute) {
      hasError(
        isSubtype[AttributeInvalidAddFormSettings](Assertion.anything)
      )
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AttributeValidator")(
      testM("return error when whole message is missing")(
        assertM(validator.required(None).run)(
          fails(
            hasError(
              isFieldRequired(containsString("attribute"))
            )
          )
        )
      ),
      testM("pass validation when attribute is valid") {
        assertM(validator.required(Some(validAttribute)).run)(
          succeeds(anything)
        )
      },
      testM("return error when id is set")(
        assertValidationFailure(validAttribute.copy(id = "test"))(
          hasError(
            isSubtype[FieldIsReadOnly](Assertion.hasField("message", _.message, containsString("id")))
          )
        )
      ),
      testM("return error when version is set and uri_part is not")(
        assertM(validator.required(Some(validAttribute.copy(version = 10, uriPart = ""))).run)(
          fails(
            isSubtype[MessageValidationError](
              hasField("errors", _.errors.length, equalTo(2))
            )
          )
        )
      ),
      testM("return error when uri_part is invalid") {
        assertM(validator.required(Some(validAttribute.copy(uriPart = "abc_-&"))).run)(
          fails(
            hasError(
              isFieldMalformed(fieldName = equalTo("uriPart"))
            )
          )
        )
      },
      testM("return error when name is missing")(
        assertFieldMissing(validAttribute.copy(name = ""), "name")
      ),
      testM("return error when linguistics is missing")(
        assertFieldMissing(validAttribute.copy(linguistics = None), "linguistics")
      ),
      testM("return error when name forms is missing")(
        assertFieldMissing(validAttribute.copy(nameForms = None), "nameForms")
      ) @@ ignore,
      testM("return error when some of name forms are missing")(
        assertFieldMissing(
          validAttribute.copy(nameForms = validAttribute.nameForms.map(_.copy(accusativePlural = ""))),
          "nameForms.accusativePlural"
        )
      ) @@ ignore,
      testM("return error when addFormSettings are missing or undefined and attribute is required")(
        assertFieldMissingBecause(validAttribute.copy(isRequired = true, addFormSettings = None), "addFormSettings")
      ),
      testM("return error when dictionary value does not have key") {
        assertFieldMissing(
          validAttribute.withDictionarySettings(
            DictionarySettings(allowedValues = Seq(DictionaryValue(key = "", name = "name", wizardName = "name")))
          ),
          "dictionarySettings.allowedValues[0].key"
        )
      },
      testM("return error when dictionary value have key with url unsafe symbols") {
        assertValidationFailure(
          validAttribute.withDictionarySettings(
            DictionarySettings(allowedValues = Seq(DictionaryValue(key = "key&", name = "name", wizardName = "name")))
          )
        ) {
          hasError(
            isFieldMalformed(fieldName = equalTo("dictionarySettings.allowedValues[0].key"))
          )
        }
      },
      testM("return error when dictionary value does not have name") {
        assertFieldMissing(
          validAttribute.withDictionarySettings(
            DictionarySettings(allowedValues = Seq(DictionaryValue(key = "key", name = "", wizardName = "name")))
          ),
          "dictionarySettings.allowedValues[0].name"
        )
      },
      testM("return error when dictionary value does not have wizardName") {
        assertFieldMissing(
          validAttribute.withDictionarySettings(
            DictionarySettings(allowedValues = Seq(DictionaryValue(key = "key", name = "name", wizardName = "")))
          ),
          "dictionarySettings.allowedValues[0].wizardName"
        )
      },
      testM("return error when dictionary keys are duplicated") {
        assertValidationFailure(
          validAttribute.withDictionarySettings(
            DictionarySettings(allowedValues =
              Seq(
                DictionaryValue(key = "key", name = "name", wizardName = "name"),
                DictionaryValue(key = "key", name = "other_name", wizardName = "other_name")
              )
            )
          )
        ) {
          hasError(
            isSubtype[DuplicateAttributeDictionaryKeys](
              Assertion.hasField[DuplicateAttributeDictionaryKeys, String]("key", _.key, equalTo("key")) &&
                Assertion.hasField[DuplicateAttributeDictionaryKeys, Seq[String]](
                  "fieldNames",
                  _.fieldNames,
                  hasSameElements(
                    Seq("dictionarySettings.allowedValues[0].key", "dictionarySettings.allowedValues[1].key")
                  )
                )
            )
          )
        }
      },
      testM("return error when has empty attributeSettings") {
        assertFieldMissing(
          validAttribute.withAttributeSettings(AttributeDefinition.AttributeSettings.Empty),
          "attributeSettings"
        )
      }
    )
}
