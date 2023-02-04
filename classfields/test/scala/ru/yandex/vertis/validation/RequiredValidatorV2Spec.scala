package ru.yandex.vertis.validation

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.protobuf.test.ModelWithOptionalMark.{SomeOptionalMsg, SomeRequiredMsg}
import ru.yandex.vertis.protobuf.test.{AlwaysValid, Inner, Mark, ModelWithOptionalMark, RepeatedTestModel, TestProto2}
import ru.yandex.vertis.validation.model.MissingRequiredField
import ru.yandex.vertis.validation.validators.RequiredValidatorV2

import scala.collection.JavaConverters._

class RequiredValidatorV2Spec extends Matchers with WordSpecLike {


  private val fullMark = Mark
    .newBuilder()
    .setCode("bmw")
    .setRussianAlias("бмв")
    .setSomeRequiredBool(true)
    .setMarkType(Mark.MarkType.CAR)
    .build()

  private val fullOptionalMsg = SomeOptionalMsg
    .newBuilder()
    .setCode("some_code")
    .setTitle("some_title")
    .build()
  private val fullRequiredMsg = SomeRequiredMsg
    .newBuilder()
    .setCode("some_code")
    .setTitle("some_title")
    .build()
  private val fullModel = ModelWithOptionalMark
    .newBuilder()
    .setCode("m3")
    .setMark(fullMark)
    .setSomeMsg(fullOptionalMsg)
    .setRequiredMsg(fullRequiredMsg)
    .build()

  private val fullModelWithoutOptionalMsg = fullModel.toBuilder.clearSomeMsg().build()
  private val fullModelWithEmptyOptionalMsg = fullModel.toBuilder.setSomeMsg(SomeOptionalMsg.getDefaultInstance).build()
  private val fullModelWithoutRequiredMsg = fullModel.toBuilder.clearRequiredMsg().build()

  private val invalidOptionalMsg = SomeOptionalMsg.newBuilder().setTitle("some_title").build()
  private val invalidRequiredMsg = SomeRequiredMsg.newBuilder().setCode("some_code").build()

  private val multipleErrorModel =
    ModelWithOptionalMark
      .newBuilder()
      .setCode("666")
      .setMark(fullMark)
      .setSomeMsg(invalidOptionalMsg)
      .setRequiredMsg(invalidRequiredMsg)
      .build()

  "RequiredValidatorV2 for proto3 syntax" should {

    "fail on default enum" in {
      val msg = fullMark.toBuilder.setMarkType(Mark.MarkType.UNSET).build()
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.Mark.mark_type")
      )
    }

    "not fail on valid message with non-empty required repeated" in {
      val msg = RepeatedTestModel.newBuilder()
        .addAllPrimitives(Seq("x").asJava)
        .addAllMessages(Seq(fullOptionalMsg).asJava)
        .build()
      RequiredValidatorV2.validate(msg) shouldBe 'valid
    }

    "fail on empty required repeated fields" in {
      val msg = RepeatedTestModel.getDefaultInstance
      RequiredValidatorV2.validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.RepeatedTestModel.primitives"),
        MissingRequiredField("vertis.validation.test.RepeatedTestModel.messages")
      )
    }

    "fail without required message" in {
      RequiredValidatorV2.validate(fullModelWithoutRequiredMsg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.required_msg")
      )
    }

    "allow default primitive value in repeated" in {
      val msg = RepeatedTestModel.newBuilder()
        .addAllPrimitives(Seq("").asJava)
        .addAllMessages(Seq(fullOptionalMsg).asJava)
        .build()
      RequiredValidatorV2.validate(msg) shouldBe 'valid
    }

    "validate each item in required repeated field" in {
      val msg = RepeatedTestModel.newBuilder()
        .addAllPrimitives(Seq("x").asJava)
        .addAllMessages(Seq(fullOptionalMsg, invalidOptionalMsg, invalidOptionalMsg).asJava)
        .build()
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeOptionalMsg.code"),
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeOptionalMsg.code")
      )
    }

    "validate each item in optional repeated field" in {
      val msg = RepeatedTestModel.newBuilder()
        .addAllPrimitives(Seq("x").asJava)
        .addAllMessages(Seq(fullOptionalMsg).asJava)
        .addAllOptMessages(Seq(invalidOptionalMsg).asJava)
        .build()
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeOptionalMsg.code")
      )
    }

    "not fail on full valid message" in {
      RequiredValidatorV2.validate(fullModel) shouldBe 'valid
    }

    "not fail on message without optional field with nested required" in {
      RequiredValidatorV2.validate(fullModelWithoutOptionalMsg) shouldBe 'valid
    }

    "fail on explicitly empty optional field with nested required" in {
      RequiredValidatorV2
        .validate(fullModelWithEmptyOptionalMsg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeOptionalMsg.code")
      )
    }

    "fail on default primitive required field" in {
      RequiredValidatorV2
        .validate(SomeOptionalMsg.getDefaultInstance)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeOptionalMsg.code")
      )
    }

    "collect all missing fields" in {
      RequiredValidatorV2
        .validate(multipleErrorModel)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeOptionalMsg.code"),
        MissingRequiredField("vertis.validation.test.ModelWithOptionalMark.SomeRequiredMsg.title")
      )
    }

    "ignore required option on bool field" in {
      RequiredValidatorV2.validate(fullMark) shouldBe 'valid
      RequiredValidatorV2.validate(fullMark.toBuilder.setSomeRequiredBool(false).build()) shouldBe 'valid
    }

  }

  "RequiredValidatorV2 for proto2 syntax" should {

    val validInner = Inner.newBuilder()
      .setOptField("value")
      .setOptFieldWithOption("value2")
      .build()

    val invalidInner = validInner.toBuilder
      .clearOptFieldWithOption()
      .build()

    val valid = TestProto2.newBuilder()
      .setReqField("required value")
      .setOptField(1L)
      .setOptFieldWithOption(666L)
      .setOptBoolWithOption(true)
      .addAllRepeatedFieldWithOption(Seq(1L, 2L).map(java.lang.Long.valueOf).asJava)
      .setReqMsg(validInner)
      .setOptMsg(validInner)
      .setOptMsgWithOption(validInner)
      .addAllRepeatedMsg(Seq(validInner).asJava)
      .addAllRepeatedMsgWithOption(Seq(validInner).asJava)
      .addAllRepeatedValidMsg(Seq(AlwaysValid.newBuilder().setUnused("unused").build()).asJava)
      .build()

    "allow explicitly default primitives required by modifier" in {
      val msg = valid.toBuilder.setReqField("").build
      RequiredValidatorV2.validate(msg) shouldBe 'valid
    }

    "allow explicitly default primitives required by option" in {
      val msg = valid.toBuilder.setOptFieldWithOption(0L).build
      RequiredValidatorV2.validate(msg) shouldBe 'valid
    }

    "fail without required by option primitives" in {
      val msg = valid.toBuilder.clearOptBoolWithOption().build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.TestProto2.opt_bool_with_option")
      )
    }
    "fail without bool required by option" in {
      val msg = valid.toBuilder.clearOptBoolWithOption().build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.TestProto2.opt_bool_with_option")
      )
    }
    "fail on empty repeated primitives required by option" in {
      val msg = valid.toBuilder.clearRepeatedFieldWithOption().build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.TestProto2.repeated_field_with_option")
      )
    }
    "allow defaults in repeated primitives required by option" in {
      val msg = valid.toBuilder.clearRepeatedFieldWithOption().addRepeatedFieldWithOption(0L).build
      RequiredValidatorV2.validate(msg) shouldBe 'valid
    }

    "fail without required by option message" in {
      val msg = valid.toBuilder
        .clearOptMsgWithOption()
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.TestProto2.opt_msg_with_option")
      )
    }

    "validate inner optional message" in {
      val msg = valid.toBuilder
        .setOptMsg(invalidInner)
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.Inner.opt_field_with_option")
      )
    }
    "validate inner required message" in {
      val msg = valid.toBuilder
        .setReqMsg(invalidInner)
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.Inner.opt_field_with_option")
      )
    }
    "validate inner required by option message" in {
      val msg = valid.toBuilder
        .setOptMsgWithOption(invalidInner)
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.Inner.opt_field_with_option")
      )
    }

    "validate inner repeated message" in {
      val msg = valid.toBuilder
        .clearRepeatedMsg()
        .addRepeatedMsg(invalidInner)
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.Inner.opt_field_with_option")
      )
    }

    "validate inner required repeated message" in {
      val msg = valid.toBuilder
        .clearRepeatedMsgWithOption()
        .addRepeatedMsgWithOption(invalidInner)
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.Inner.opt_field_with_option")
      )
    }

    "fail on empty required repeated message" in {
      val msg = valid.toBuilder
        .clearRepeatedMsgWithOption()
        .build
      RequiredValidatorV2
        .validate(msg)
        .asReasons should contain theSameElementsAs Seq(
        MissingRequiredField("vertis.validation.test.TestProto2.repeated_msg_with_option")
      )
    }

    "allow defaults in required repeated message" in {
      val msg = valid.toBuilder
        .clearRepeatedValidMsg()
        .addRepeatedValidMsg(AlwaysValid.getDefaultInstance)
        .build
      RequiredValidatorV2.validate(msg) shouldBe 'valid
    }

  }
}
