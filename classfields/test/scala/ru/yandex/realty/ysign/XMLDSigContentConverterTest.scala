package ru.yandex.realty.ysign

import org.scalatest.FunSuite

class XMLDSigContentConverterTest extends FunSuite {
  test("toBase64String") {
    val result = XMLDSigContentConverter.toBase64String("cert", "text")
    assert(result.nonEmpty)
  }
}
