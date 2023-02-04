package ru.yandex.vertis.validation

import ru.yandex.vertis.protobuf.test.{VerbaCheck, VerbaCheckWithNested, VerbaMessageWithOptionalField, VerbaWithInvalidPlaceholder, VerbaWithPlaceHolder, VerbaWithPlaceHolderWithNested}
import ru.yandex.vertis.validation.VerbaMessageValidatorSpec.DummyVerbaChecker
import ru.yandex.vertis.validation.model.{InvalidPlaceholderField, MissingPlaceholder, VerbaInvalidValue}
import ru.yandex.vertis.validation.validators.{VerbaChecker, VerbaMessageValidator}

/**
  * @author ruslansd
  */
class VerbaMessageValidatorSpec
  extends ValidatorSpecBase {

  protected val validator: VerbaMessageValidator = new VerbaMessageValidator(DummyVerbaChecker)

  "VerbaMessageValidator" should {
    val message = VerbaCheck.newBuilder()
      .setColor("red")
      .setName("bmw")
      .setNumber(10)
      .setValue("test")
      .build()

    "pass message with correct values" in {
      checkValid(message)
    }

    "do not pass message with incorrect values" in {
      val invalid = message.toBuilder.setColor("brown").build()
      checkInvalid(invalid, getInvalid(VerbaInvalidValue(packagedName("VerbaCheck.color"), "auto/color-stock", "brown")))
    }

    "do not pass message with undefined values" in {
      val withMissedValue = message.toBuilder.clearColor().build()
      checkValid(withMissedValue)
    }
  }

  "VerbaMessageValidator with nested" should {
    val nested = VerbaCheck.newBuilder()
      .setColor("red")
      .setName("bmw")
      .setNumber(10)
      .setValue("test")
      .build()

    "pass message with correct values" in {
      val message = VerbaCheckWithNested.newBuilder()
        .setPower(1.5d)
        .setCheck(nested)
        .build()

      checkValid(message)
    }

    "do not pass message with incorrect values" in {
      val invalid = nested.toBuilder.setColor("brown").build()
      val message = VerbaCheckWithNested.newBuilder()
        .setPower(1.5d)
        .setCheck(invalid)
        .build()
      checkInvalid(message, getInvalid(VerbaInvalidValue(packagedName("VerbaCheck.color"), "auto/color-stock", "brown")))
    }

    "pass message with undefined values" in {
      val invalid = nested.toBuilder.clearColor().build()
      val message = VerbaCheckWithNested.newBuilder()
        .setPower(1.5d)
        .setCheck(invalid)
        .build()
      checkValid(message)
    }

    "pass message with undefined nested" in {
      val message = VerbaCheckWithNested.newBuilder()
        .setPower(1.5d)
        .build()
      checkValid(message)
    }

  }

  "VerbaMessageValidator with placeholders" should {

    "pass message with defined placeholders" in {
      val message = VerbaWithPlaceHolder.newBuilder()
        .setColor("red")
        .setName("bmw")
        .build()

      checkValid(message)
    }

    "pass with nested placeholders" in {
      val nested = VerbaWithPlaceHolder.newBuilder()
        .setColor("red")
        .setName("bmw")
        .build()

      val message = VerbaWithPlaceHolderWithNested.newBuilder()
        .setColor("red")
        .setName("bmw")
        .setValue("ok")
        .setMsg(nested)
        .build()

      checkValid(message)
    }

    "reject message with undefined placeholder" in {
      val message = VerbaWithPlaceHolder.newBuilder()
        .setName("bmw")
        .build()

      checkInvalid(message, getInvalid(MissingPlaceholder("color", "auto/${COLOR}")))
    }
  }

  "VerbaWithInvalidPlaceholder" should {
    val nested = VerbaWithPlaceHolder.newBuilder()
      .setColor("red")
      .setName("bmw")
      .build()
    "reject with InvalidPlaceholder" in {

      val message = VerbaWithInvalidPlaceholder.newBuilder()
        .setPlaceholder(nested)
        .setName("test")
        .build()

      checkInvalid(message, getInvalid(InvalidPlaceholderField(packagedName("VerbaWithInvalidPlaceholder.placeholder"))))

    }

    "get all reasons" in {
      val message = VerbaWithInvalidPlaceholder.newBuilder()
        .setPlaceholder(nested.toBuilder.clear())
        .setName("test")
        .build()

      checkInvalid(message, getInvalid(
        InvalidPlaceholderField(packagedName("VerbaWithInvalidPlaceholder.placeholder")),
        MissingPlaceholder("color", "auto/${COLOR}")))
    }
  }

  "VerbaMessageWithOptionalField" should {
    "pass undefined optional fields" in {
      val msg = VerbaMessageWithOptionalField.newBuilder()
        .build()

      checkValid(msg)
    }
  }

}

object VerbaMessageValidatorSpec {

  object DummyVerbaChecker extends VerbaChecker {
    private val stringValues = Map(
      "auto/color-stock" → Set("green", "red"),
      "auto/name" → Set("honda", "bmw"),
      "auto/red" → Set("bmw"),
      "auto/red/bmw" → Set("ok")
    )

    private val longValues = Map(
      "auto/number" → Set(5L, 10L, 20L)
    )

    private val doubleValues = Map(
      "auto/power" → Set(1.5d, 2.0d, 3.0d)
    )

    override def isDefined(path: String, value: AnyRef): Boolean = value match {
      case v: String ⇒ stringValues.get(path).exists(_.contains(v))
      case v: java.lang.Long ⇒ longValues.get(path).exists(_.contains(v))
      case v: java.lang.Double ⇒ doubleValues.get(path).exists(_.contains(v))
      case _ ⇒ false
    }
  }
}
