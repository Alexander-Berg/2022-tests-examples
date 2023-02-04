package ru.yandex.vertis.validation

import ru.yandex.vertis.protobuf.test.TestOptions
import ru.yandex.vertis.protobuf.test.CompositeMessage
import ru.yandex.vertis.validation.model.{Invalid, MissingFieldWithOption, MissingPlaceholder}
import ru.yandex.vertis.validation.validators.{CompositeMessageValidator, RequiredMessageValidator, SpecialFieldValidator, VerbaMessageValidator}

/**
  * @author ruslansd
  */
class CompositeMessageValidatorSpec
  extends ValidatorSpecBase {
  override protected def validator: MessageValidator = new CompositeMessageValidator(
    Seq(
      new RequiredMessageValidator,
      new VerbaMessageValidator(VerbaMessageValidatorSpec.DummyVerbaChecker),
      new SpecialFieldValidator(Seq(TestOptions.isClassified, TestOptions.isSource))
    )
  )

  "CompositeMessageValidator" should {
    val valid = CompositeMessage.newBuilder()
      .setName("test")
      .setClassified("test")
      .setSource("test")
      .setColor("red")
      .setValue("bmw")
      .build()
    "pass valid message" in {
      checkValid(valid)
    }

    "list all reasons" in {
      val reasons = missingFields(
        "CompositeMessage.name",
        "CompositeMessage.classified",
        "CompositeMessage.source",
        "CompositeMessage.color",
        "CompositeMessage.value").reasons.toList :+
        MissingPlaceholder("color", "auto/${COLOR}") :+
        MissingFieldWithOption("vertis.validation.test.is_classified") :+
        MissingFieldWithOption("vertis.validation.test.is_source")

      checkInvalid(valid.toBuilder.clear().build(), Invalid(reasons))
    }
  }
}
