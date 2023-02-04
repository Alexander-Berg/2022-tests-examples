package ru.auto.salesman.service.impl.user.autoru.price.service.periodical_discount

import org.joda.time.DateTime
import ru.auto.salesman.model.user.product.AutoruProduct
import ru.auto.salesman.model.user.{PeriodicalDiscount, PeriodicalDiscountContext}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.{Boost, Placement}
import ru.auto.salesman.model.user.periodical_discount_exclusion._
import ProductPeriodicalDiscount._

class ProductPeriodicalDiscountSpec extends BaseSpec {

  val now = DateTime.now

  private def createPeriodicalDiscount(
      products: List[AutoruProduct]
  ): PeriodicalDiscount = {
    val periodicalDiscountContext = PeriodicalDiscountContext(
      Some(products.map(_.toString))
    )
    PeriodicalDiscount(
      discountId = "1",
      start = now,
      deadline = now,
      discount = 70,
      context = Some(periodicalDiscountContext)
    )
  }

  "ProductPeriodicalDiscount" should {
    "use same periodical discount for one product discount" in {
      val periodicalDiscount = createPeriodicalDiscount(List(Placement))
      forProduct(
        Placement,
        User.UserInPeriodicalDiscount(periodicalDiscount)
      ) shouldBe Product
        .InDiscount(periodicalDiscount)
    }

    "not use periodical discount for different product" in {
      val periodicalDiscount = createPeriodicalDiscount(List(Placement))
      forProduct(
        Boost,
        User.UserInPeriodicalDiscount(periodicalDiscount)
      ) shouldBe Product.NoActiveDiscount
    }

    "use one corresponding product in discount" in {
      val periodicalDiscount = createPeriodicalDiscount(List(Placement, Boost))

      //Когда нашли соответствующий продукт, мы не меняем контекст распродажи.
      //Там так же остаются записаны все продукты.
      val expectedDiscount = createPeriodicalDiscount(List(Placement, Boost))

      forProduct(
        Boost,
        User.UserInPeriodicalDiscount(periodicalDiscount)
      ) shouldBe Product
        .InDiscount(expectedDiscount)
    }

    "use exclusion object for same product" in {
      val periodicalDiscount = createPeriodicalDiscount(List(Placement))
      forProduct(
        Placement,
        User.UserExcludedFromDiscount(periodicalDiscount)
      ) shouldBe Product
        .UserExcludedFromDiscount(periodicalDiscount)
    }

    "not use exclusion object for different product" in {
      val periodicalDiscount = createPeriodicalDiscount(List(Placement))
      forProduct(
        Boost,
        User.UserExcludedFromDiscount(periodicalDiscount)
      ) shouldBe Product.NoActiveDiscount
    }

    "not give discount if there is no discount" in {
      forProduct(
        Placement,
        User.NoActiveDiscount
      ) shouldBe Product.NoActiveDiscount
    }

  }
}
