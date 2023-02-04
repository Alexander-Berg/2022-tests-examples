package ru.auto.salesman.proto

import ru.auto.salesman.model.user.PaymentPayload
import ru.auto.salesman.proto.BankerBaseProtoFormats._
import ru.auto.salesman.test.BaseSpec

import scala.util.Random

class BankerBaseProtoFormatsSpec extends BaseSpec {

  "PayloadProtoFormat" should {
    "get the same result after reading written result" in {
      val original =
        PaymentPayload(Random.nextString(15))
      val parsed =
        PayloadRawProtoReader.read(PayloadProtoWriter.write(original))
      parsed.transactionId shouldBe original.transactionId
    }
  }

}
