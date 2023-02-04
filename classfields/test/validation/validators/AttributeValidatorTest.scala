package ru.yandex.vertis.general.gost.logic.test.validation.validators

import java.time.Instant

import general.bonsai.attribute_model.AttributeDefinition.AttributeSettings
import general.bonsai.attribute_model.{AttributeDefinition, DictionarySettings, StringSettings}
import general.bonsai.category_model.{Category, CategoryAttribute}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.common.model.user.OwnerId.UserId
import ru.yandex.vertis.general.gost.logic.validation.AttributesValidator
import ru.yandex.vertis.general.gost.model.Draft.DraftId
import ru.yandex.vertis.general.gost.model.attributes.AttributeValue._
import ru.yandex.vertis.general.gost.model.attributes._
import ru.yandex.vertis.general.gost.model.validation.ValidatedEntity
import ru.yandex.vertis.general.gost.model.validation.attributes._
import ru.yandex.vertis.general.gost.model.{CriticalValidationError, Draft}
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object AttributeValidatorTest extends DefaultRunnableSpec {

  val testCategory =
    Category("", attributes = Seq(CategoryAttribute(attributeId = "brand_EkyvVh", isRequiredOverride = Some(true))))

  val testAttributes = Seq(
    AttributeDefinition(
      id = "brand_EkyvVh",
      attributeSettings = AttributeSettings.StringSettings(StringSettings())
    )
  )

  val categories = Seq(
    testCategory
  )

  val testDictionary = BonsaiSnapshot(categories, testAttributes)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AttributesDraftValidator")(
      testM("Validate correct attributes") {
        for {
          draft <- ZIO.effectTotal(createDraft(Attribute("brand_EkyvVh", 3270, StringValue("BRAND"))))
          errors <-
            AttributesValidator
              .validate(ValidatedEntity.fromDraft(draft, UserId(0L)), testCategory, testDictionary)
        } yield assert(errors.toList)(isEmpty)
      },
      testM("Validate attribute required") {
        for {
          draft <- ZIO.effectTotal(createDraft())
          errors <-
            AttributesValidator
              .validate(ValidatedEntity.fromDraft(draft, UserId(0L)), testCategory, testDictionary)
        } yield assert(errors.toList.headOption)(isSome(equalTo(AttributeValueRequired("brand_EkyvVh", ""))))
      },
      testM("Validate unexpected attributes") {
        for {
          draft <- ZIO.effectTotal(
            createDraft(
              Attribute("brand_EkyvVh", 3270, StringValue("BRAND")),
              Attribute("wrong_attribute", 3270, StringValue("BRAND"))
            )
          )
          errors <-
            AttributesValidator.validate(
              ValidatedEntity.fromDraft(draft, UserId(0L)),
              testCategory,
              testDictionary
            )
        } yield assert(errors.toList.headOption)(isSome(equalTo(AttributeValueUnexpected("wrong_attribute"))))
      },
      testM("Validate wrong attribute type") {
        for {
          draft <- ZIO.effectTotal(createDraft(Attribute("brand_EkyvVh", 3270, BooleanValue(true))))
          errors <-
            AttributesValidator
              .validate(ValidatedEntity.fromDraft(draft, UserId(0L)), testCategory, testDictionary)
        } yield assert(errors.toList.headOption)(isSome(isSubtype[WrongAttributeType](anything)))
      }
    )

  private def createDraft(attributes: Attribute[AttributeValue]*) = {
    Draft
      .empty(DraftId("id"), Instant.now(), None, None, None)
      .copy(
        attributes = Attributes(attributes.map(AttributeConverter.toDraftAttribute))
      )
  }
}
