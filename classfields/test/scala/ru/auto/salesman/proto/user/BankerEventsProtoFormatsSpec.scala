package ru.auto.salesman.proto.user

import java.util.UUID

import com.github.nscala_time.time.Imports.DateTime
import ru.auto.salesman.model.PaymentActions
import ru.auto.salesman.model.user.{PaymentPayloadRaw, PaymentRequestRaw}
import ru.auto.salesman.proto.user.BankerEventsProtoFormats.PaymentRequestReader
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.proto.user.PaymentRequestWriter

class BankerEventsProtoFormatsSpec extends BaseSpec {

  "PaymentRequestReader" should {
    "parse correctly" in {
      def rndStr = UUID.randomUUID().toString

      val original = PaymentRequestRaw(
        bankerTransactionId = rndStr,
        payload = PaymentPayloadRaw(rndStr, "autoru"),
        time = DateTime.now,
        action = PaymentActions.Activate
      )

      val parsed =
        PaymentRequestReader.read(PaymentRequestWriter.write(original))

      parsed shouldEqual original
    }
  }

}
