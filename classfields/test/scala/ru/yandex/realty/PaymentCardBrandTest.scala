package ru.yandex.realty

import org.scalatest.FlatSpec
import ru.yandex.realty.PaymentCardBrand.parseCardBrandByPan
import ru.yandex.realty.proto.{PaymentCardBrand => Brand}

class PaymentCardBrandTest extends FlatSpec {

  private val VISA = "401288******1881"
  private val MASTERCARD = "532853******0409"
  private val AMERICAN_EXPRESS = "37144******8431"
  private val DINERS_CLUB = "3852******3237"
  private val DISCOVER = "601184******1908"
  private val JCB = "356600******0505"
  private val UNION_PAY = "622315******8888"
  private val MAESTRO = "589374******1609"
  private val MIR = "220220******0494"
  private val UNKNOWN = "102858******0505"

  it should "have brand VISA" in {
    assertResult(Brand.VISA)(parseCardBrandByPan(VISA))
  }

  it should "have brand MASTERCARD" in {
    assertResult(Brand.MASTERCARD)(parseCardBrandByPan(MASTERCARD))
  }

  it should "have brand AMERICAN EXPRESS" in {
    assertResult(Brand.AMERICAN_EXPRESS)(parseCardBrandByPan(AMERICAN_EXPRESS))
  }

  it should "have brand DINERS CLUB" in {
    assertResult(Brand.DINERS_CLUB)(parseCardBrandByPan(DINERS_CLUB))
  }

  it should "have brand DISCOVER" in {
    assertResult(Brand.DISCOVER)(parseCardBrandByPan(DISCOVER))
  }

  it should "have brand JCB" in {
    assertResult(Brand.JCB)(parseCardBrandByPan(JCB))
  }

  it should "have brand UNION PAY" in {
    assertResult(Brand.UNION_PAY)(parseCardBrandByPan(UNION_PAY))
  }

  it should "have brand MAESTRO" in {
    assertResult(Brand.MAESTRO)(parseCardBrandByPan(MAESTRO))
  }

  it should "have brand MIR" in {
    assertResult(Brand.MIR)(parseCardBrandByPan(MIR))
  }

  it should "have brand UNKNOWN" in {
    assertResult(Brand.UNKNOWN_BRAND)(parseCardBrandByPan(UNKNOWN))
  }
}
