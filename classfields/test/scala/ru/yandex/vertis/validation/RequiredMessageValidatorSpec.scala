package ru.yandex.vertis.validation

import com.google.protobuf.{BoolValue, Timestamp}
import ru.yandex.vertis.protobuf.test.{ComplexMessage, OptFields, SimpleMessage, TestEnum, WithBool, WithEnum, WithFieldsOpt, WithIgnoreInnerFields, WithNested, WithNestedOneOf, WithOneOf, WithOptionalOneOf, WithRepeated}
import ru.yandex.vertis.validation.model.{Invalid, MatchOnlyRegexp, RegexpExclusion}
import ru.yandex.vertis.validation.validators.RequiredMessageValidator

import scala.collection.JavaConverters.asJavaCollectionConverter

/**
  * Spec on [[RequiredMessageValidator]].
  *
  * @author ruslansd
  */
class RequiredMessageValidatorSpec
  extends ValidatorSpecBase {

  protected val validator: MessageValidator = new RequiredMessageValidator()

  "RequiredMessageValidatorSpec with primitive type message" should {
    "pass fields without is_optional option" in {
      val message =
        Timestamp.newBuilder()
          .setSeconds(10000)
          .setNanos(1000)
          .build()
      checkValid(message)
    }

    "pass invalid message from exclusion" in {
      val message =
        Timestamp.newBuilder()
          .build()
      checkValid(message)
    }

    "pass fields with is_optional option" in {
      val withDefinedOptional = SimpleMessage.newBuilder()
        .setId("test")
        .setName("test")
        .setValue("test")
        .build()
      checkValid(withDefinedOptional)
    }

    "pass with undefined optional field" in {
      val valid = SimpleMessage.newBuilder()
        .setId("test")
        .setValue("test")
        .build()
      checkValid(valid)
    }

    "not pass message with missed required primitive field" in {
      val message = SimpleMessage.newBuilder()
        .setId("test")
        .setName("test")
        .build()
      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "not pass message with default value defined field" in {
      val message = SimpleMessage.newBuilder()
        .setId("test")
        .setName("test")
        .setValue("")
        .build()
      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "pass primitive bool fields with any value" in {
      val message =
        WithBool.newBuilder()
          .setSome(false)
          .setSomeWrapper(BoolValue.newBuilder().setValue(false))
          .build()
      checkValid(message)
    }

    "not pass wrapped bool fields with no value" in {
      val message = WithBool.newBuilder().setSome(true).build()
      checkInvalid(message, missingFields("WithBool.some_wrapper"))
    }

  }

  "RequiredMessageValidatorSpec with nested message" should {
    val nested = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()

    "pass with undefined optional field" in {
      val valid = nested
        .toBuilder
        .clearName()

      val message = WithNested.newBuilder()
        .setId("test")
        .setName("test")
        .setSimple(valid)
        .build()

      checkValid(message)
    }

    "do not pass message with missed required in nested message" in {
      val invalid = nested
        .toBuilder
        .clearValue()

      val message = WithNested.newBuilder()
        .setId("test")
        .setSimple(invalid)
        .build()

      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "do not pass message with missed required field" in {

      val message = WithNested.newBuilder()
        .setName("test")
        .setSimple(nested)
        .build()

      checkInvalid(message, missingFields("WithNested.id"))
    }

    "do not pass message with missed required nested message field" in {

      val message = WithNested.newBuilder()
        .setId("test")
        .build()

      checkInvalid(message, missingFields("WithNested.simple"))
    }

  }

  "RequiredMessageValidatorSpec with repeated message" should {
    val nested = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()

    "pass with non empty repeated" in {
      val message = WithRepeated.newBuilder()
        .setId("test")
        .addAllMsgs(Iterable(nested).asJavaCollection)
        .build()

      checkValid(message)
    }

    "do not pass with undefined" in {
      val invalid = WithRepeated.newBuilder()
        .setId("test")
        .build()

      checkInvalid(invalid, missingFields("WithRepeated.msgs"))
    }

    "do not pass with empty repeated field" in {
      val invalid = WithRepeated.newBuilder()
        .setId("test")
        .addAllMsgs(Iterable.empty[SimpleMessage].asJavaCollection)
        .build()

      checkInvalid(invalid, missingFields("WithRepeated.msgs"))
    }

    "do not pass with invalid repeated nested message" in {
      val invalid = nested.toBuilder.clearValue().build()

      val message = WithRepeated.newBuilder()
        .setId("test")
        .addAllMsgs(Iterable(invalid).asJavaCollection)
        .build()

      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "do not pass with few invalid repeated nested message" in {
      val invalid = nested.toBuilder.clearValue().build()

      val message = WithRepeated.newBuilder()
        .setId("test")
        .addAllMsgs(Iterable(nested, invalid).asJavaCollection)
        .build()

      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "do not pass with invalid optional repeated nested message" in {
      val invalid = nested.toBuilder.clearValue().build()

      val message = WithRepeated.newBuilder()
        .setId("test")
        .addAllMsgs(Iterable(nested, nested).asJavaCollection)
        .addAllOptionMsgs(Iterable(nested, invalid).asJavaCollection)
        .build()

      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

  }

  "RequiredMessageValidatorSpec with enums" should {
    val nested = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()

    "pass with defined enum field" in {
      val withEnum = WithEnum.newBuilder()
        .setEnu(TestEnum.TEST_OK)
        .setSimple(nested)
        .build()

      checkValid(withEnum)
    }

    "do not pass with undefined" in {
      val withEnum = WithEnum.newBuilder()
        .setSimple(nested)
        .build()

      checkInvalid(withEnum, missingFields("WithEnum.enu"))
    }

    "do not pass with default" in {
      val withEnum = WithEnum.newBuilder()
        .setEnu(TestEnum.TEST_UNKNOWN)
        .setSimple(nested)
        .build()

      checkInvalid(withEnum, missingFields("WithEnum.enu"))
    }

    "do not pass with invalid nested" in {
      val invalid = nested.toBuilder.clearValue().build()
      val withEnum = WithEnum.newBuilder()
        .setEnu(TestEnum.TEST_OK)
        .setSimple(invalid)
        .build()

      checkInvalid(withEnum, missingFields("SimpleMessage.value"))
    }
  }

  "RequiredMessageValidatorSpec with one of" should {
    val nested = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()
    "pass defined one of" in {
      val message = WithOneOf.newBuilder()
        .setId("test")
        .setValue("test")
        .build()
      checkValid(message)
    }

    "do not pass one of defined with invalid message" in {
      val invalid = nested.toBuilder.clearValue()
      val message = WithOneOf.newBuilder()
        .setSimple(invalid)
        .setValue("test")
        .build()
      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "do not pass undefined one of" in {
      val message = WithOneOf.newBuilder()
        .setValue("test")
        .build()

      checkInvalid(message, missingOneof("WithOneOf.payload"))
    }
  }

  "RequiredMessageValidatorSpec with optional one of" should {
    val nested = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()
    "pass defined one of" in {
      val message = WithOptionalOneOf.newBuilder()
        .setId("test")
        .setValue("test")
        .build()
      checkValid(message)
    }

    "do not pass one of defined with invalid message" in {
      val invalid = nested.toBuilder.clearValue()
      val message = WithOptionalOneOf.newBuilder()
        .setSimple(invalid)
        .setValue("test")
        .build()
      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "pass undefined optional one of" in {
      val message = WithOptionalOneOf.newBuilder()
        .setValue("test")
        .build()

      checkValid(message)
    }
  }

  "RequiredMessageValidatorSpec with all field optional message" should {
    val optFields = OptFields.newBuilder()
      .setId("test")
      .setName("test")
      .build()
    "pass defined fields" in {
      val message = WithFieldsOpt.newBuilder()
        .setOpt(optFields)
        .build()
      checkValid(message)
    }

    "pass defined empty valid message" in {
      val emptyValid = optFields.toBuilder
        .clearName()
        .clearId()
      val message = WithFieldsOpt.newBuilder()
        .setOpt(emptyValid)
        .build()
      checkValid(message)
    }

    "do not pass undefined" in {
      val message = WithFieldsOpt.newBuilder()
        .build()
      checkInvalid(message, missingFields("WithFieldsOpt.opt"))
    }
  }


  "RequiredMessageValidatorSpec with nested oneof" should {

    val withOneOf = WithOneOf.newBuilder()
      .setValue("value").build()

    val withNestedOneOf = WithNestedOneOf.newBuilder()
      .setId("id")
      .setName("name")
      .setMessage(withOneOf).build()

    "check non-optional nested oneof" in {
      checkInvalid(withOneOf, missingOneof("WithOneOf.payload"))
      checkInvalid(withNestedOneOf, missingOneof("WithOneOf.payload"))
    }
  }

  "RequiredMessageValidatorSpec with RequiredAll message" should {
    val oneOf = WithOneOf.newBuilder().setId("1").build()
    val simple = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()
    val opt = OptFields.newBuilder().build()

    val valid = ComplexMessage.newBuilder()
      .setOneOfField(oneOf)
      .setSimple(simple)
      .setOpt(opt)
      .setEnu(TestEnum.TEST_OK)
      .setId("1")
      .setName("name")
      .setSimpleOpt(simple)
      .build()
    "pass if all fields defined" in {
      checkValid(valid)
    }

    "find all invalid field" in {
      val invalid = valid.toBuilder.clear().build()
      checkInvalid(invalid, missingFields(
        "ComplexMessage.id",
        "ComplexMessage.name",
        "ComplexMessage.opt",
        "ComplexMessage.oneOfField",
        "ComplexMessage.enu",
        "ComplexMessage.simple"
      ))
    }

    "find only missed fields" in {
      val invalid = valid.toBuilder.clearId().clearEnu().build()
      checkInvalid(invalid, missingFields(
        "ComplexMessage.id",
        "ComplexMessage.enu"
      ))
    }

    "find all invalid nested messages" in {
      val invalidSimple = simple.toBuilder.clearValue()
      val invalidOneOf = oneOf.toBuilder.clearPayload()

      val invalid = valid.toBuilder
        .setSimple(invalidSimple)
        .setOneOfField(invalidOneOf)
        .setSimpleOpt(invalidSimple)
        .build()

      val reasons = missingFields("SimpleMessage.value", "SimpleMessage.value").reasons ++
        missingOneof("WithOneOf.payload").reasons
      checkInvalid(invalid, Invalid(reasons))
    }
  }

  "RequiredMessageValidatorSpec with ignore_inner_fields" should {
    val valid = SimpleMessage.newBuilder()
      .setId("test")
      .setName("test")
      .setValue("test")
      .build()
    val invalid = valid.toBuilder.clearValue().build()

    "pass with valid fields" in {
      val message = WithIgnoreInnerFields.newBuilder()
        .setId("test")
        .setName("test")
        .setIgnoreInner(valid)
        .setNotIgnoreInner(valid)
        .build()
      checkValid(message)
    }

    "pass invalid messages with ignore flag set to true" in {
      val message = WithIgnoreInnerFields.newBuilder()
        .setId("test")
        .setName("test")
        .setIgnoreInner(invalid)
        .setNotIgnoreInner(valid)
        .build()

      checkValid(message)
    }

    "do not pass invalid fields" in {
      val message = WithIgnoreInnerFields.newBuilder()
        .setId("test")
        .setName("test")
        .setIgnoreInner(invalid)
        .setNotIgnoreInner(invalid)
        .build()

      checkInvalid(message, missingFields("SimpleMessage.value"))
    }

    "do not skip validation of the field" in {
      val message = WithIgnoreInnerFields.newBuilder()
        .setId("test")
        .setIgnoreInner(valid)
        .setNotIgnoreInner(valid)
        .build()

      checkInvalid(message, missingFields("WithIgnoreInnerFields.name"))
    }

  }

}
