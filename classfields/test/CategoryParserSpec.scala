package ru.yandex.vertis.general.bonsai.model.test

import general.bonsai.category_model.{Category, CategoryState}
import general.bonsai.lang_model.{Linguistics, NameForms}
import ru.yandex.vertis.general.bonsai.model.{FieldIsReadOnly, FieldMalformed, FieldRequired, MessageValidationError}
import ru.yandex.vertis.general.bonsai.model.validation.{ApiModelParser, CategoryParser}
import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test.{testM, _}

object CategoryParserSpec extends DefaultRunnableSpec {
  val validator: ApiModelParser[Category, Category] = CategoryParser.create("category")

  val validCategory = Category(
    uriPart = "part_-",
    name = "name",
    shortName = "short-name",
    uniqueName = "unique-name",
    linguistics = Some(Linguistics()),
    nameForms = Some(
      NameForms(
        accusativeSingular = "accusativeSingular",
        accusativePlural = "accusativePlural",
        genitivePlural = "genitivePlural"
      )
    ),
    state = CategoryState.DEFAULT
  )

  def assertFieldMissing(category: Category, fieldName: String) = {
    assertM(validator.required(Some(category)).run)(
      fails(
        isSubtype[MessageValidationError](
          hasField(
            "errors",
            _.errors.head,
            isSubtype[FieldRequired](Assertion.hasField("message", _.fieldName, equalTo(fieldName)))
          )
        )
      )
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CategoryValidator")(
      testM("return error when whole message is missing")(
        assertM(validator.required(None).run)(
          fails(
            isSubtype[MessageValidationError](
              hasField(
                "errors",
                _.errors.head,
                isSubtype[FieldRequired](Assertion.hasField("message", _.message, containsString("category")))
              )
            )
          )
        )
      ),
      testM("pass validation when category is valid") {
        assertM(validator.required(Some(validCategory)).run)(
          succeeds(anything)
        )
      },
      testM("return error when id is set")(
        assertM(validator.required(Some(validCategory.copy(id = "test"))).run)(
          fails(
            isSubtype[MessageValidationError](
              hasField(
                "errors",
                _.errors.head,
                isSubtype[FieldIsReadOnly](Assertion.hasField("message", _.message, containsString("id")))
              )
            )
          )
        )
      ),
      testM("return error when version is set and uri_part is not")(
        assertM(validator.required(Some(validCategory.copy(version = 10, uriPart = ""))).run)(
          fails(
            isSubtype[MessageValidationError](
              hasField("errors", _.errors.length, equalTo(2))
            )
          )
        )
      ),
      testM("return error when uri_part is invalid") {
        assertM(validator.required(Some(validCategory.copy(uriPart = "abc_- &"))).run)(
          fails(
            isSubtype[MessageValidationError](
              hasField(
                "errors",
                _.errors.head,
                isSubtype[FieldMalformed](Assertion.hasField("name", _.fieldName, equalTo("uriPart")))
              )
            )
          )
        )
      },
      testM("return error when name is missing")(
        assertFieldMissing(validCategory.copy(name = ""), "name")
      ),
      testM("return error when short name is missing")(
        assertFieldMissing(validCategory.copy(shortName = ""), "shortName")
      ),
      testM("return error when linguistics is missing")(
        assertFieldMissing(validCategory.copy(linguistics = None), "linguistics")
      ),
      testM("return error when name forms is missing")(
        assertFieldMissing(validCategory.copy(nameForms = None), "nameForms")
      ) @@ ignore,
      testM("return error when some of name forms are missing")(
        assertFieldMissing(
          validCategory.copy(nameForms = validCategory.nameForms.map(_.copy(accusativePlural = ""))),
          "nameForms.accusativePlural"
        )
      ) @@ ignore,
      testM("return error when state is unset")(
        assertM {
          validator
            .required(Some(validCategory.copy(state = CategoryState.UNSET)))
            .mapError(_.errors.toChunk)
            .run
        }(fails(exists(isSubtype[FieldMalformed](hasField("fieldName", _.fieldName, equalTo("state"))))))
      )
    )
}
