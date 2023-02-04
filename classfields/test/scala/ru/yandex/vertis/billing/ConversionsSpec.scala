package ru.yandex.vertis.billing

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.Conversions
import ru.yandex.vertis.billing.model_core.Division.{Components, Projects}
import ru.yandex.vertis.billing.model_core.EventStat.RawEventDetails
import ru.yandex.vertis.billing.model_core.gens.{
  orderTransactionGen,
  rawEventDetailsGen,
  AsRandom,
  CustomerIdGen,
  GoodGen,
  OrderGen,
  OrderTransactionGenParams,
  Producer
}
import ru.yandex.vertis.billing.model_core.{
  proto,
  BilledEventInfo,
  EnrichedBilledEventInfo,
  OrderTransactions,
  Product,
  Withdraw2
}
import ru.yandex.vertis.billing.util.DateTimeUtils

class ConversionsSpec extends AnyWordSpec with Matchers with DefaultPropertyChecks {

  "ConversionsSpec" should {
    "convert RawEventDetails" in {
      forAll(rawEventDetailsGen(AsRandom)) { expected =>
        val proto = Conversions.toMessage(expected)
        val actual = Conversions.detailsFromMessage(proto).get
        (expected, actual) match {
          case (RawEventDetails(a), RawEventDetails(b)) =>
            a should contain theSameElementsAs b
          case d =>
            fail(s"Unexpected $d")
        }
      }
    }

    "correctly convert to TransactionBillingInfo with one good" in {
      checkTransactionBillingInfoConversion(1)
    }

    "correctly convert to TransactionBillingInfo with few good" in {
      checkTransactionBillingInfoConversion(3)
    }

  }

  private def checkTransactionBillingInfoConversion(goodCount: Int) = {
    val product = Product(GoodGen.next(goodCount).toSet)
    val customer = CustomerIdGen.next
    val withdraw = {
      val w = orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Withdraw)).next
        .asInstanceOf[Withdraw2]
      w.copy(snapshot = w.snapshot.copy(product = product))
    }
    val order = {
      val o = OrderGen.next
      o.copy(id = withdraw.orderId)
    }
    val transaction = proto.Conversions.toMessage(withdraw).get
    val info =
      BilledEventInfo(
        Some("123234"),
        None,
        None,
        withdraw.timestamp,
        withdraw.amount,
        withdraw.amount,
        transaction,
        None,
        Some(DateTimeUtils.now().getMillis)
      )

    val enriched = EnrichedBilledEventInfo(info, customer, Projects.AutoRu, Components.Indexing.toString, order, None)

    val transactionBillingInfo = proto.Conversions.toTransactionBillingInfo(enriched)

    transactionBillingInfo.getWithdraw.getAllProductsCount shouldBe product.goods.size
    transactionBillingInfo.getWithdraw.hasProduct shouldBe true
  }

}
