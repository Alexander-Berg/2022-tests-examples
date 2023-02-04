package ru.yandex.vertis.protobuf

import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.generators.BasicGenerators.list
import ru.yandex.vertis.protobuf.test.Foo
import ru.yandex.vertis.protobuf.test3.Foo3

/**
  * Specs on protobuf serialization/deserialization.
  *
  * @author dimas
  */
class ProtobufUtilsSpec
  extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks {

  import ProtobufUtils._

  test("toJson/fromJson") {
    forAll(TestMessageGenerators.foo) {
      foo =>
        roundTrip[Foo, String](foo)(
          message => toJson(message, compact = true),
          json => fromJson(Foo.getDefaultInstance, json))
    }
  }

  test("toJson with preservingProtoFieldNames / fromJson") {
    forAll(TestMessageGenerators.foo) {
      foo =>
        roundTrip[Foo, String](foo)(
          message => toJson(message, compact = true, preservingProtoFieldNames = true),
          json => fromJson(Foo.getDefaultInstance, json))
    }
  }

  test("toJson with includingDefaultValueFields / fromJson") {
    forAll(TestMessageGenerators.foo3) {
      foo =>
        val json = toJson(foo, includingDefaultValueFields = true)
        val actual = fromJson(Foo3.getDefaultInstance, json)
        foo shouldBe actual
    }
  }



  test("toJsonArray/seqFromJson") {
    forAll(list(0, 10, TestMessageGenerators.foo)) {
      values =>
        roundTrip[Seq[Foo], String](values)(
          messages => toJsonArray(messages, compact = true),
          json => seqFromJson(Foo.getDefaultInstance, json))
    }
  }

  test("writeDelimited/readDelimited") {
    forAll(list(0, 10, TestMessageGenerators.foo)) {
      values =>
        roundTrip[Seq[Foo], Array[Byte]](values)(
          messages => writeDelimited(messages),
          bytes => parseDelimited(Foo.getDefaultInstance, bytes))
    }
  }

  private def roundTrip[A, B](value: A)
                             (write: A => B, read: B => A) = {
    read(write(value)) should be(value)
  }
}
