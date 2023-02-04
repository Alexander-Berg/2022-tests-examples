package ru.yandex.vertis.billing.model_core

import org.joda.time.DateTime
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.model_core.gens.{orderTransactionGen, OrderTransactionGenParams, Producer}
import ru.yandex.vertis.billing.model_core.gens.{OfferIdGen, Producer}

class BilledEventInfoSpec extends AnyWordSpec with Matchers {

  private val offerId = OfferIdGen.next

  "BilledEventInfo" should {
    "evaluate id correctly" in {
      val transaction = orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Withdraw)).next
        .asInstanceOf[Withdraw2]

      val snapshotBeforeFix = transaction.snapshot.copy(time = new DateTime("2019-07-07T19:00:00+03:00"))
      val transactionBeforeFix = transaction.copy(snapshot = snapshotBeforeFix)
      val billedEventInfoBeforeFix = BilledEventInfo(
        Some("raw_event_id"),
        Some(offerId),
        Some("callfact_id"),
        transaction.amount,
        transaction.amount,
        transactionBeforeFix,
        None
      )

      val snapshotAfterFix = transaction.snapshot.copy(time = new DateTime("2019-09-09T19:00:00+03:00"))
      val transactionAfterFix = transaction.copy(snapshot = snapshotAfterFix)
      val billedEventInfoAfterFix = BilledEventInfo(
        Some("raw_event_id"),
        Some(offerId),
        Some("callfact_id"),
        transaction.amount,
        transaction.amount,
        transactionAfterFix,
        None
      )

      billedEventInfoBeforeFix.id shouldNot be(billedEventInfoAfterFix.id)
    }
  }

}
