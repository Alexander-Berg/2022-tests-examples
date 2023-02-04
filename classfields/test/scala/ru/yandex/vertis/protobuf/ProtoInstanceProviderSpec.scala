package ru.yandex.vertis.protobuf

import org.scalatest.FunSuite
import ru.yandex.vertis.protobuf.test.Foo

/**
  * Spec on protobuf defaults extractor
  *
  * @author abulychev
  */
class ProtoInstanceProviderSpec extends FunSuite {

  import ProtoInstanceProvider._

  test("extract default of Foo message") {
    val instance = protoInstance[Foo]
    assert(instance.hasNum === false)
    assert(instance.hasStr === false)
    assert(instance.hasBool === false)
    assert(instance.hasEnum === false)
  }
}
