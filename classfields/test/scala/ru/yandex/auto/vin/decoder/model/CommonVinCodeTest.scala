package ru.yandex.auto.vin.decoder.model

import org.scalatest.funsuite.AnyFunSuite

class CommonVinCodeTest extends AnyFunSuite {

  test("checksum") {
    assert(CommonVinCode("JS1GN7DA362110088").checksum())
    assert(CommonVinCode("jS1GN7DA362110088").checksum())
    assert(!CommonVinCode("JS1GN7DA362110089").checksum())
    assert(CommonVinCode("XW7BF4FKX0S127312").checksum())
    assert(!CommonVinCode("XW7BF4FKY0S127312").checksum())
  }
}
