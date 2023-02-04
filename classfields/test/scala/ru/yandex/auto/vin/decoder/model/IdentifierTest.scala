package ru.yandex.auto.vin.decoder.model

import org.scalatest.funsuite.AnyFunSuite

class IdentifierTest extends AnyFunSuite {

  test("identifiers") {
    assert(VinCode("XWFPF2DC1C0020829").asShard() == 0)
    assert(LicensePlate("M765MT152").asShard() == 1)
  }
}
