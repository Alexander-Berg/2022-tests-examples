package ru.yandex.vertis.billing.banker.proto

import com.google.protobuf.Message
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.State.{Incoming, Statuses, TotalIncoming}
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentGen,
  paymentMarkupRequest,
  paymentRequestGen,
  paymentRequestSourceGen,
  recurrentPaymentSourceGen,
  refundGen,
  refundRequestGen,
  refundRequestSourceGen,
  PayloadGen
}
import ru.yandex.vertis.billing.banker.model.{Raw, State}
import ru.yandex.vertis.billing.banker.proto.CommonProtoFormats.PayloadProtoFormat
import ru.yandex.vertis.billing.banker.proto.PaymentProtoFormats.{
  PaymentFormat,
  PaymentMarkupRequestFormat,
  PaymentRequestFormat,
  PaymentRequestSourceFormat,
  RecurrentPaymentSourceFormat,
  RefundFormat,
  RefundPaymentRequestFormat,
  RefundPaymentRequestSourceFormat
}
import ru.yandex.vertis.protobuf.ProtoFormat

class ProtoFormatsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  def test[T, R <: Message](expectedGen: Gen[T], format: ProtoFormat[T, R]): Unit = {
    forAll(expectedGen) { expected =>
      val proto = format.write(expected)
      val actual = format.read(proto)
      actual shouldBe expected
    }
    ()
  }

  def removeInsignificantFieldsFromPayment(state: State.Payment): State.Payment = state match {
    case i: Incoming =>
      i.copy(status = Statuses.Created, rawData = Raw.Empty)
    case t: TotalIncoming =>
      t.copy(status = Statuses.Created, rawData = Raw.Empty)
  }

  def removeInsignificantFieldsFromRefund(state: State.Refund): State.Refund =
    state.copy(status = Statuses.Created, rawData = Raw.Empty)

  "ProtoFormats" should {

    "convert payloads" in {
      test(PayloadGen, PayloadProtoFormat)
    }

    "convert payment source" in {
      test(paymentRequestSourceGen(), PaymentRequestSourceFormat)
    }

    "convert recurrent payment source" in {
      test(recurrentPaymentSourceGen(), RecurrentPaymentSourceFormat)
    }

    "convert payment" in {
      val CorrectPaymentGen = paymentGen().map(removeInsignificantFieldsFromPayment)
      test(CorrectPaymentGen, PaymentFormat)
    }

    "convert payment request" in {
      val CorrectPaymentRequestGen = paymentRequestGen().map { req =>
        val payment = req.state.map(removeInsignificantFieldsFromPayment)
        req.copy(state = payment)
      }
      test(CorrectPaymentRequestGen, PaymentRequestFormat)
    }

    "convert refund" in {
      val CorrectRefundGen = refundGen().map(removeInsignificantFieldsFromRefund)
      test(CorrectRefundGen, RefundFormat)
    }

    "convert refund request source" in {
      test(refundRequestSourceGen(), RefundPaymentRequestSourceFormat)
    }

    "convert refund request" in {
      val CorrectRefundRequestGen = refundRequestGen().map { req =>
        val payment = req.state.map(removeInsignificantFieldsFromRefund)
        req.copy(state = payment)
      }
      test(CorrectRefundRequestGen, RefundPaymentRequestFormat)
    }

    "convert markup request" in {
      test(paymentMarkupRequest(), PaymentMarkupRequestFormat)
    }
  }

}
