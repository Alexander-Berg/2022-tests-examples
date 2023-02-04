package ru.yandex.vertis.billing.service.checking

import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.settings.RealtyComponents
import FundsConversions._
import org.scalatest.wordspec.AnyWordSpec

/**
  * Tests for [[RealtyInputChecker]]
  *
  * @author alesavin
  */
class RealtyInputCheckerSpec extends AnyWordSpec with Matchers {

  val inputChecker =
    new RealtyInputChecker(RealtyComponents.callsRevenueConstraints.get)

  "checkProduct for realty" should {
    "pass only allowed products" in {
      inputChecker.checkProduct(Product(Placement(CostPerCall(230000L)))).get
      inputChecker.checkProduct(Product(Placement(CostPerCall(1000000L)))).get
      inputChecker.checkProduct(Product(Placement(CostPerCall(60000L)))).get
      inputChecker.checkProduct(Product(Placement(CostPerCall(1000L * 1000 * 10)))).get
      inputChecker.checkProduct(Product(Placement(CostPerCall(1000L * 1000 * 100)))).get
      inputChecker.checkProduct(Product(Custom("superCall", CostPerCall(10.thousands)))).get
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Placement(CostPerCall(100L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Placement(CostPerCall(1000L * 1000 * 100 + 1)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(`Raise+Highlighting`(CostPerDay(5L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Highlighting(CostPerClick(500L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(`Raise+Highlighting`(CostPerClick(600L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Raising(CostPerMille(3L)))).get
      }
    }
  }
}
