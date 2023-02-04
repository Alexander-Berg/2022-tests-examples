package ru.yandex.vertis.subscriptions.model

import com.google.protobuf.Message
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.protobuf.ProtoFormat

/**
  * Base spec for [[ProtoFormat]] implementations.
  *
  * @author dimas
  */
trait ProtoFormatSpecBase extends FunSuite with Matchers with PropertyChecks {

  def testFormat[T, M <: Message](format: ProtoFormat[T, M], values: Gen[T]): Unit = {
    test(format.getClass.getSimpleName) {
      forAll(values) { value =>
        format.read(format.write(value)) should be(value)
      }
    }
  }

}
