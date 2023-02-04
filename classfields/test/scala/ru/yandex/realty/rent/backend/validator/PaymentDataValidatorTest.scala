package ru.yandex.realty.rent.backend.validator

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.backend.validator.PaymentDataValidator.checkOgrnipValid

@RunWith(classOf[JUnitRunner])
class PaymentDataValidatorTest extends FunSuite {
  test("testCheckOgrnipValid") {
    assert(checkOgrnipValid("313132804400022"))
    assert(!checkOgrnipValid("313132804400020"))
    assert(checkOgrnipValid("304500116000157"))
    assert(checkOgrnipValid("314507427600038"))
    assert(checkOgrnipValid("322997429670091"))
  }
}
