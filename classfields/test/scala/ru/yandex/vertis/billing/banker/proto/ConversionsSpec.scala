package ru.yandex.vertis.billing.banker.proto

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.model.Payload
import ru.yandex.vertis.billing.banker.model.gens.{
  ContextGen,
  FieldGen,
  PayloadGen,
  PaymentRequestFormGen,
  PaymentRequestOptionsGen,
  RawPaymentGen
}
import ru.yandex.vertis.billing.banker.proto.Conversions.{
  contextFromMessage,
  payloadFromMessage,
  paymentRequestFormFromMessage,
  paymentRequestOptionsFromMessage,
  rawFromMessage,
  stringTuplesContainerFromMessage,
  toMessage
}
import spray.json.{JsNumber, JsObject, JsString}

import scala.util.Success

/**
  * Spec on [[Conversions]]
  *
  * @author ruslansd
  */
class ConversionsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Conversions" should {

    "correctly serde payloads" in {
      forAll(PayloadGen) { p =>
        val msg = toMessage(p)
        Success(p) shouldBe payloadFromMessage(msg)
      }
      val json = JsObject(
        "11111" -> JsString("2222"),
        "22222" -> JsString("2222"),
        "3" -> JsNumber(2222),
        "4" -> JsString("2222")
      )
      val p = Payload.Json(json)
      Success(p) shouldBe payloadFromMessage(toMessage(p))
    }

    "correctly serde payment options" in {
      forAll(PaymentRequestOptionsGen) { o =>
        val msg = toMessage(o)
        Success(o) shouldBe paymentRequestOptionsFromMessage(msg)
      }
    }
    "correctly serde payment form" in {
      forAll(PaymentRequestFormGen) { o =>
        val msg = toMessage(o)
        Success(o) shouldBe paymentRequestFormFromMessage(msg)
      }
    }
    "correctly serde raw payment" in {
      forAll(RawPaymentGen) { o =>
        val msg = toMessage(o)
        o shouldBe rawFromMessage(msg)
      }
    }
    "correctly serde string tuples" in {
      forAll(ConversionsSpec.StringTuplesGen) { o =>
        val msg = toMessage(o)
        o should contain theSameElementsAs stringTuplesContainerFromMessage(msg)
      }
    }
    "correctly serde payment request context" in {
      forAll(ContextGen) { o =>
        val msg = toMessage(o)
        Success(o) shouldBe contextFromMessage(msg)
      }
    }
  }

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000, workers = 5)

}

object ConversionsSpec {

  val StringTuplesGen: Gen[Iterable[(String, String)]] = for {
    n <- Gen.chooseNum(0, 50)
    tuples <- Gen.listOfN(n, FieldGen)
  } yield tuples
}
