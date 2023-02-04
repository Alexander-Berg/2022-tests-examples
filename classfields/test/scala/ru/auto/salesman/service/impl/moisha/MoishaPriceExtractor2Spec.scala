package ru.auto.salesman.service.impl.moisha

import org.joda.time.DateTime
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.money.Money.Kopecks

class MoishaPriceExtractor2Spec extends BaseSpec {

  "MoishaPriceExtractor" should {
    "parse price from dealer response" in {
      val data = readResource("/moisha/dealer/response/classic.json")
      val ex = new MoishaPriceExtractor(data.getBytes)
      ex.price(
        ProductId.Placement,
        DateTime.parse("2016-08-23T22:05:05.000+03:00")
      ).success
        .value shouldBe 3500L
      ex.price(
        ProductId.Placement,
        DateTime.parse("2016-08-23T20:36:21.000+03:00")
      ).success
        .value shouldBe 3500L
      // 2016-08-23 20:36:21
    }

    "parse product info from user response properly" in {
      val data = readResource("/moisha/user/response/classic.json")
      val ex = new MoishaPriceExtractor(data.getBytes)
      val product = ProductId.Turbo
      val dateTime = DateTime.parse("2020-03-03T13:29+03:00")

      val result = ex.productInfo(product, dateTime).success.value

      result.price shouldBe Kopecks(149700)
      result.prolongPrice shouldBe None
      result.product shouldBe product
      result.policyId shouldBe Some("autoru-users-c7ddf91e67692d712f036625544afe76")
      result.appliedExperiment shouldBe None
    }

    "parse product info with prolong price from user response properly" in {
      val data = readResource("/moisha/user/response/prolong_price.json")
      val ex = new MoishaPriceExtractor(data.getBytes)
      val product = ProductId.Turbo
      val dateTime = DateTime.parse("2020-03-03T13:29+03:00")

      val result = ex.productInfo(product, dateTime).success.value

      result.price shouldBe Kopecks(149700)
      result.prolongPrice.value shouldBe 99700
      result.product shouldBe product
      result.policyId shouldBe None
      result.appliedExperiment shouldBe Some("test-exp")
    }
  }

}
