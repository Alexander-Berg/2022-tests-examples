package ru.yandex.vos2.api.directives.api

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RealtyUserDirectiveSpec extends FlatSpec with Matchers {

  behavior of "hashPhone()"

  it should "return 24-char hashes" in {
    RealtyUserDirective.hashPhone("7111000001")._1 should have length 24
  }

}
