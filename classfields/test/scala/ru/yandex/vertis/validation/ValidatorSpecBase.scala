package ru.yandex.vertis.validation

import com.google.protobuf.{DynamicMessage, Message}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.validation.model.{Invalid, InvalidReason, MissingRequiredField, MissingRequiredOneOf, Valid, ValidationResult}

/**
  * Base spec trait for [[MessageValidator]]
  *
  * @author ruslansd
  */
trait ValidatorSpecBase
  extends Matchers
    with WordSpecLike {

  protected def validator: MessageValidator

  protected def packagedName(name: String): String =
    s"vertis.validation.test.$name"

  protected def missingFields(name: String*): Invalid =
    Invalid(name.map(packagedName).map(MissingRequiredField.apply))

  protected def missingOneof(name: String*): Invalid =
    Invalid(name.map(packagedName).map(MissingRequiredOneOf.apply))

  protected def checkValid(message: Message): Unit = {
    validator.validate(message) shouldBe Valid
    validator.validate(toDynamic(message)) shouldBe Valid
  }

  protected def toDynamic(message: Message): DynamicMessage = {
    val bytes = message.toByteArray
    DynamicMessage.parseFrom(message.getDescriptorForType, bytes)
  }

  protected def checkInvalid(message: Message, result: Invalid): Unit = {
    checkInvalid(validator.validate(message), result)
    checkInvalid(validator.validate(toDynamic(message)), result)
  }

  protected def checkInvalid(actual: ValidationResult, expected: Invalid): Unit = {
    actual.isValid shouldBe false
    actual.asInstanceOf[Invalid].reasons should contain theSameElementsAs expected.reasons
  }

  protected def getInvalid(reasons: InvalidReason*): Invalid =
    Invalid(reasons)
}
