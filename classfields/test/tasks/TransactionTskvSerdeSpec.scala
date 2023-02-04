package ru.yandex.vertis.billing.tasks

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.BillingEvent
import ru.yandex.vertis.billing.model_core.gens.{OrderTransactionGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.protobuf.kv.Converter

/**
  * @author ruslansd
  */
class TransactionTskvSerdeSpec extends AnyWordSpec with Matchers {

  "TransactionLogTask" should {

    "correctly SerDe" in {
      val trs = OrderTransactionGen.next(100)
      val proto = trs.map(Conversions.toMessage(_).get)
      val tskv = proto.map { p =>
        (Converter.toKeyValue(p, Some("billing.transaction")).get, p)
      }
      tskv.foreach { case (t, p) =>
        val b = BillingEvent.Transaction.newBuilder()
        (Converter.fromKeyValue(t, b, Some("billing.transaction")) should be).a(Symbol("Success"))

        b.build() shouldBe p
      }
    }
  }
}
