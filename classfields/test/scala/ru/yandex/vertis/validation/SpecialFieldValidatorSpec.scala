package ru.yandex.vertis.validation

import ru.yandex.vertis.protobuf.test.TestOptions
import ru.yandex.vertis.protobuf.test.{MissingAll, MissingOneSpecial, WithSpecialField}
import ru.yandex.vertis.validation.model.MissingFieldWithOption
import ru.yandex.vertis.validation.validators.SpecialFieldValidator

/**
  * @author ruslansd
  */
class SpecialFieldValidatorSpec
  extends ValidatorSpecBase {

  protected val validator: MessageValidator =
    new SpecialFieldValidator(Seq(TestOptions.isClassified, TestOptions.isSource))

  "SpecialFieldValidator" should {
    "pass messages with defined special field" in {
      val message = WithSpecialField.newBuilder()
        .setClassified("test")
        .setSource("test")
        .build()

      checkValid(message)
    }

    "do not message with undefined special field" in {
      val message = WithSpecialField.newBuilder()
        .setSource("test")
        .build()

      checkInvalid(message, getInvalid(MissingFieldWithOption("vertis.validation.test.is_classified")))
    }

    "do not message with special field set as false" in {
      val message = MissingOneSpecial.newBuilder()
        .setSource("test")
        .setClassified("test")
        .build()

      checkInvalid(message, getInvalid(MissingFieldWithOption("vertis.validation.test.is_source")))
    }

    "do not message without special field" in {
      val message = MissingAll.newBuilder()
        .setId("test")
        .build()

      checkInvalid(message, getInvalid(MissingFieldWithOption("vertis.validation.test.is_classified"), MissingFieldWithOption("vertis.validation.test.is_source")))
    }
  }

}
