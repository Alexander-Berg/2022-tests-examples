package ru.yandex.realty.util.protobuf

import com.google.protobuf.{Message, ProtocolMessageEnum}
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.protobuf.ProtoFormat

/**
  * User: abulychev
  * Date: 14.06.2018
  */
trait ProtoFormatSpecBase extends FunSuite with Matchers with PropertyChecks {

  def testFormat[T, M <: Message](format: ProtoFormat[T, M], values: Gen[T]): Unit = {
    test(format.getClass.getSimpleName) {
      forAll(values) { value =>
        format.read(format.write(value)) should be(value)
      }
    }
  }

  def testEnumFormat[T, M <: ProtocolMessageEnum](format: EnumProtoFormat[T, M], values: Gen[T]): Unit = {
    test(format.getClass.getSimpleName) {
      forAll(values) { value =>
        format.read(format.write(value)) should be(value)
      }
    }
  }

}
