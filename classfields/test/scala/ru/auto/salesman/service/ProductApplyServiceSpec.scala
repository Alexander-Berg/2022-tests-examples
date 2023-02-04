package ru.auto.salesman.service

import ru.auto.salesman.service.ProductApplyService.Response.PaymentError
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.banker.model.ApiModel.ApiError
import ru.yandex.vertis.banker.model.ApiModel.ApiError.DigitalWalletError.INSUFFICIENT_FUNDS

class ProductApplyServiceSpec extends BaseSpec {

  "PaymentError.toString" should {

    "convert russian protobuf message to string" in {
      val message = "Недостаточно средств на карте"
      val code = INSUFFICIENT_FUNDS
      val error = ApiError
        .newBuilder()
        .setMessage(message)
        .setDigitalWalletError(code)
        .build()
      val result = PaymentError(List(error)).toString
      result should startWith("PaymentError(")
      result.contains("INSUFFICIENT_FUNDS") shouldBe true
      result.contains(message) shouldBe true
    }
  }
}
