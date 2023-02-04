package ru.yandex.vertis.billing.service.cached

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.{AutoRuUid, CustomerId, OrderTransactions, Uid}
import ru.yandex.vertis.billing.service.OrderService.{TransactionFilter, TransactionsOutcomeQuery}
import ru.yandex.vertis.billing.util.{AutomatedContext, DateTimeInterval, OperatorContext, Page}

/**
  * Specs on [[CachedOrderService]]
  *
  * @author alesavin
  */
class CachedOrderServiceSpec extends AnyWordSpec with Matchers {

  "CachedOrderService" should {
    "provide listTransactionsKey" in {

      CachedOrderService.listTransactionsKey(
        CustomerId(1, Some(2)),
        1001L,
        DateTimeInterval(
          DateTime.parse("2016-09-19T01:00:16.740+03:00"),
          DateTime.parse("2016-09-19T22:00:16.740+03:00")
        ),
        Page(1, 20),
        None,
        AutomatedContext("test")
      ) should be("c-1_2-o-1001-in-1474236016_1474311616-sl-20_40--")

      CachedOrderService.listTransactionsKey(
        CustomerId(3, None),
        1002L,
        DateTimeInterval(
          DateTime.parse("2016-09-19T01:00:16.740+03:00"),
          DateTime.parse("2016-09-20T22:00:16.740+03:00")
        ),
        Page(0, 10),
        Some(TransactionFilter(Set(OrderTransactions.Incoming, OrderTransactions.Withdraw))),
        OperatorContext("test", Uid(123L))
      ) should be("c-3-o-1002-in-1474236016_1474398016-sl-0_10-0,2-Uid(123)")

      CachedOrderService.listTransactionsKey(
        CustomerId(3, None),
        1002L,
        DateTimeInterval(
          DateTime.parse("2016-09-19T01:00:16.740+03:00"),
          DateTime.parse("2016-09-20T22:00:16.740+03:00")
        ),
        Page(0, 10),
        Some(TransactionFilter(Set(OrderTransactions.Withdraw, OrderTransactions.Incoming))),
        OperatorContext("test", Uid(123L))
      ) should be("c-3-o-1002-in-1474236016_1474398016-sl-0_10-0,2-Uid(123)")

      CachedOrderService.listTransactionsKey(
        CustomerId(3, Some(4)),
        1002L,
        DateTimeInterval(
          DateTime.parse("2016-09-19T01:00:16.740+03:00"),
          DateTime.parse("2016-09-20T22:00:16.740+03:00")
        ),
        Page(0, 10),
        Some(TransactionFilter(Set(OrderTransactions.Withdraw))),
        OperatorContext("test", AutoRuUid("salesman"))
      ) should be("c-3_4-o-1002-in-1474236016_1474398016-sl-0_10-2-AutoRuUid(salesman)")

    }
    "provide getTransactionOutcomeKey" in {

      CachedOrderService.getTransactionOutcomeKey(
        CustomerId(3, Some(4)),
        TransactionsOutcomeQuery(1003L)
      ) should be("c-3_4-o-1003-in--wd-false")

      CachedOrderService.getTransactionOutcomeKey(
        CustomerId(5, None),
        TransactionsOutcomeQuery(
          1003L,
          Some(
            DateTimeInterval(
              DateTime.parse("2016-09-19T01:00:16.740+03:00"),
              DateTime.parse("2016-09-20T22:00:16.740+03:00")
            )
          )
        )
      ) should be("c-5-o-1003-in-1474236016_1474398016-wd-false")

    }
  }
}
