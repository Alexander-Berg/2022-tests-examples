package ru.yandex.vertis.billing.banker.model.util

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.model.PaymentMethod.CardProperties.Brands
import ru.yandex.vertis.billing.banker.model.gens._

/**
  * Runnable specs on [[CardBrand]]
  */
class CardBrandSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with ShrinkLowPriority {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10000, workers = 50)

  def testAll(cardGen: Gen[String])(f: String => Assertion): Assertion =
    forAll(cardGen) { mask =>
      whenever(mask.length == 11 && mask.forall(c => c.isDigit || c == '|')) {
        f(mask)
      }
    }

  "CardBrand" should {
    "determine Visa cards" in {
      testAll(VisaGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.Visa)
      }
    }
    "determine Mir cards" in {
      testAll(MirGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.Mir)
      }
    }
    "determine Mastercard cards" in {
      testAll(MastercardGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.Mastercard)
      }
    }
    "determine Maestro cards" in {
      testAll(MaestroGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.Maestro)
      }
    }
    "determine JCB cards" in {
      testAll(JCBGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.JCB)
      }
    }
    "determine AmericanExpress cards" in {
      testAll(AmericanExpressGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.AmericanExpress)
      }
    }
    "determine DinersClub cards" in {
      testAll(DinersClubGen) { mask =>
        CardBrand(mask) shouldBe Some(Brands.DinersClub)
      }
    }
    "skip other cards" in {
      testAll(OtherCardGen) { mask =>
        CardBrand(mask) shouldBe None
      }

    }
  }
}
