package ru.yandex.realty2.extdataloader.loaders.templates

import com.google.protobuf.Descriptors.FieldDescriptor
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.wizard.Palma.{TemplateExperiment, TemplateSetting, WizardTemplateSettings}
import ru.yandex.realty.SpecBase
import ru.yandex.realty2.extdataloader.loaders.templates.WizardTemplatesFetcherValidator.WrongPlaceHolderEntryString

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class WizardTemplatesFetcherValidatorSpec extends SpecBase {

  private val DuplicateRearrs = TemplateSetting
    .newBuilder()
    .setTitleTemplate("title")
    .setDescriptionTemplate("desc")
    .setGreenUrlSecondPartTemplate("green")
    .addTitleExperiments(
      TemplateExperiment
        .newBuilder()
        .setTemplate("title exp1")
        .setExperiment("exp1")
    )
    .addTitleExperiments(
      TemplateExperiment
        .newBuilder()
        .setTemplate("title exp2")
        .setExperiment("exp1")
    )
    .build()

  private val EmptyRearrTemplate =
    TemplateSetting
      .newBuilder()
      .setTitleTemplate("title")
      .setDescriptionTemplate("desc")
      .setGreenUrlSecondPartTemplate("green")
      .addTitleExperiments(
        TemplateExperiment
          .newBuilder()
          .setTemplate("title exp1")
          .setExperiment("exp1")
      )
      .addTitleExperiments(
        TemplateExperiment
          .newBuilder()
          .setTemplate("")
          .setExperiment("exp2")
      )
      .build()

  private val CorrectSettings =
    TemplateSetting
      .newBuilder()
      .setTitleTemplate("title")
      .setDescriptionTemplate("desc")
      .setGreenUrlSecondPartTemplate("green")
      .addTitleExperiments(
        TemplateExperiment
          .newBuilder()
          .setTemplate("title exp1")
          .setExperiment("exp1")
      )
      .addTitleExperiments(
        TemplateExperiment
          .newBuilder()
          .setTemplate("t2")
          .setExperiment("exp2")
      )
      .build()

  private def collectSettingFields =
    WizardTemplateSettings.getDescriptor.getFields.asScala
      .filter { d =>
        d.getContainingType != null &&
        d.getContainingType.getFullName == "realty.wizard.WizardTemplateSettings" &&
        d.getType == FieldDescriptor.Type.MESSAGE
      }

  private def rejectTest(settings: TemplateSetting) = {
    an[RuntimeException] should be thrownBy validator(
      WizardTemplateSettings
        .newBuilder()
        .setIpotekaWizard(settings)
        .build()
    )
  }

  private val validator = WizardTemplatesFetcherValidator

  "WizardTemplatesFetcherValidator" should {
    "reject duplicate rearrs" in rejectTest(DuplicateRearrs)

    "reject empty rearr template" in rejectTest(EmptyRearrTemplate)

    "reject on empty templates" in {
      rejectTest(CorrectSettings.toBuilder.setTitleTemplate("").build())
      rejectTest(CorrectSettings.toBuilder.setDescriptionTemplate("").build())
      rejectTest(CorrectSettings.toBuilder.setGreenUrlSecondPartTemplate("").build())
    }

    "reject if some filed is empty" in {
      val fields = collectSettingFields

      for {
        wrongField <- fields
        fieldData <- Seq(
          DuplicateRearrs,
          EmptyRearrTemplate,
          CorrectSettings.toBuilder.setTitleTemplate("").build(),
          CorrectSettings.toBuilder.setDescriptionTemplate("").build(),
          CorrectSettings.toBuilder.setGreenUrlSecondPartTemplate("").build()
        )
      } {
        val settings = WizardTemplateSettings.newBuilder()

        fields.foreach {
          settings.setField(_, CorrectSettings)
        }
        settings.setField(wrongField, fieldData)

        an[RuntimeException] should be thrownBy validator(settings.build())
      }
    }

    "pass valid settings" in {
      val settings = WizardTemplateSettings.newBuilder()

      collectSettingFields.foreach(settings.setField(_, CorrectSettings))

      validator(settings.build()) shouldBe (())
    }

    "reject placeholder" when {

      def rejectPlaceHolderTest[A: ClassTag](s: String) =
        an[A] should be thrownBy validator.validateTemplateString(s)

      "comes empty place holder" in rejectPlaceHolderTest[WrongPlaceHolderEntryString](
        "Рассчитать ипотеку на ${} - в мск"
      )

      "comes place holder with whitespace in form" in {
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${REALTY_TYPE: acc-sin} - в мск")
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${REALTY_TYPE:acc-sin } - в мск")
      }

      "comes place holder with whitespace in var name" in {
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${REALTY_TYPE :acc-sin} - в мск")
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${ REALTY_TYPE:acc-sin } - в мск")
      }

      "comes placeholder with double `:`" in rejectPlaceHolderTest[WrongPlaceHolderEntryString](
        "Рассчитать ипотеку на ${REALTY_TYPE:acc-sin:acc-sin0} - в мск"
      )

      "comes placeholder with unexpected name symbol" in rejectPlaceHolderTest[WrongPlaceHolderEntryString](
        "Рассчитать ипотеку на ${REALTY_TYPE]:acc-sin} - в мск"
      )

      "comes placeholder with unexpected form name symbol" in {
        // real case
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${REALTY_TYPE:]acc-sin} - в мск")
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${REALTY_TYPE:acc-sin]} - в мск")
      }

      "comes wrong bracket sequence" in {
        // other cases returns WrongPlaceHolderEntryString
        // F.e. `Рассчитать ипотеку на ${A ${B}- в мск`, place holder entry is `A ${B`
        rejectPlaceHolderTest[WrongPlaceHolderEntryString]("Рассчитать ипотеку на ${A - в мск")
      }
    }

    "pass place holder" when {

      def placeHolderPassTest(s: String) =
        validator.validateTemplateString(s) shouldBe (())

      "comes simple place holder" in placeHolderPassTest("Рассчитать ипотеку на ${A} - в мск")

      "comes simple place holder at the end" in placeHolderPassTest("Рассчитать ипотеку - в ${A}")

      "comes simple place holder with form" in placeHolderPassTest("Рассчитать ипотеку на ${A:form-1} - в мск")

      "comes simple place holder with form at the end" in placeHolderPassTest("Рассчитать ипотеку - в ${A:form-1}")

      "comes some strange template with brackets #1" in placeHolderPassTest("Word 1 {${A}}")

      "comes some strange template with brackets #2" in placeHolderPassTest("Word 1 {${A}")

      "comes some strange template with brackets #3" in placeHolderPassTest("Word 1 ${A}}")

      "comes some strange template with `$` #1" in placeHolderPassTest("Word 1 $${A}")

      "comes some strange template with `$` #2" in placeHolderPassTest("Word 1 $ ${A}")
    }
  }
}
