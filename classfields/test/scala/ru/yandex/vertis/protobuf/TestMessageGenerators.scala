package ru.yandex.vertis.protobuf

import org.scalacheck.Gen
import ru.yandex.vertis.generators.BasicGenerators.{bool, _}
import ru.yandex.vertis.generators.ProtobufGenerators
import ru.yandex.vertis.protobuf.test.{Foo, Enum}
import ru.yandex.vertis.protobuf.test3.{Foo3, Enum3}

/**
  * Generators for test protobuf messages.
  *
  * @author dimas
  */
trait TestMessageGenerators {

  val foo: Gen[Foo] = for {
    num <- Gen.choose(0, Int.MaxValue)
    str <- readableString
    bool <- bool
    enum <- ProtobufGenerators.protoEnum(Enum.values)
  } yield Foo.newBuilder()
    .setNum(num)
    .setStr(str)
    .setBool(bool)
    .setEnum(enum)
    .build()

  val foo3: Gen[Foo3] = for {
    num <- Gen.choose(0, Int.MaxValue)
    str <- readableString
    bool <- bool
    enum <- ProtobufGenerators.protoEnum(Enum3.values)
  } yield Foo3.newBuilder()
    .setNum(num)
    .setStr(str)
    .setBool(bool)
    .setEnum(enum)
    .build()
}

object TestMessageGenerators
  extends TestMessageGenerators
