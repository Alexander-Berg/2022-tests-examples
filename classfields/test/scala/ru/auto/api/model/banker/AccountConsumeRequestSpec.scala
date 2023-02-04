package ru.auto.api.model.banker

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.BankerModelGenerators.{receiptDataGen, PayloadGen}
import ru.yandex.vertis.banker.model.ApiModel.TransactionType

class AccountConsumeRequestSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "AccountConsumeRequest factory" should {

    "create request" in {
      forAll(ReadableStringGen, PayloadGen, ReadableStringGen, Gen.posNum[Long]) {
        (paymentId, payload, accountId, amount) =>
          val receipt = receiptDataGen(amount).next
          val request = AccountConsumeRequest(paymentId, payload, accountId, amount, receipt)
          request.getId shouldBe paymentId
          request.getType shouldBe TransactionType.WITHDRAW
          request.getAccount shouldBe accountId
          request.getPayload shouldBe payload
      }
    }
  }
}
