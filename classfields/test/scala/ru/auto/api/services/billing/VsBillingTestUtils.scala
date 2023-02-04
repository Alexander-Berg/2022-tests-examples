package ru.auto.api.services.billing

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

import com.google.protobuf.Timestamp
import org.scalatest.matchers.{MatchResult, Matcher}
import ru.auto.api.model.billing.BalanceId
import ru.auto.api.model.billing.vsbilling.{Balance2, Order, OrderId, OrderTransaction}
import ru.auto.api.services.billing.VsBillingClient.OrderTransactionsParams

object VsBillingTestUtils {

  private val offset = ZoneOffset.of("+03:00")

  val from = LocalDate.of(2018, 6, 1)
  val to = LocalDate.of(2018, 6, 19)
  val alternativeFrom = LocalDate.of(2017, 5, 6)
  val alternativeTo = LocalDate.of(2019, 8, 29)
  val transactionType = "Incoming"

  val params = OrderTransactionsParams(
    from,
    Some(to),
    pageNum = None,
    pageSize = None,
    transactionType = Some(transactionType),
    nonNegative = Some(true)
  )

  val alternativeParams = OrderTransactionsParams(
    alternativeFrom,
    Some(alternativeTo),
    pageNum = None,
    pageSize = None,
    transactionType = Some(transactionType),
    nonNegative = Some(false)
  )

  // data for short responses (not with all fields) here
  val dealerBalanceId = BalanceId(8168241)
  val dealerOrderId = OrderId(30829)
  val dealerOrders = List(Order(dealerOrderId, Balance2(100000)))
  val dealerTransactionTimestampNanos = 377000000
  val dealerTransactionTimestampSeconds = 1528382639
  val dealerTransactionTimestamp = OffsetDateTime.of(2018, 6, 7, 17, 43, 59, dealerTransactionTimestampNanos, offset)
  val dealerTransactionAmount = 242343400
  val dealerTransactionAmountRub = 2423434

  val dealerOrderTransactions = List(
    OrderTransaction(dealerTransactionTimestamp, dealerTransactionAmount, transactionType)
  )

  val intTestShortDealerParams = OrderTransactionsParams(
    LocalDate.of(2018, 9, 17),
    Some(LocalDate.of(2018, 9, 22)),
    pageNum = None,
    pageSize = None,
    transactionType = Some(transactionType),
    nonNegative = Some(true)
  )

  val intTestDealerOrderTransactions = List(
    OrderTransaction(OffsetDateTime.of(2018, 9, 17, 17, 30, 55, 638000000, offset), 2300, transactionType),
    OrderTransaction(OffsetDateTime.of(2018, 9, 17, 17, 26, 37, 282000000, offset), 100000, transactionType),
    OrderTransaction(OffsetDateTime.of(2018, 9, 17, 17, 25, 29, 942000000, offset), 100000, transactionType)
  )

  val agencyBalanceId = BalanceId(7320375)
  val agencyDealerBalanceId = BalanceId(32348772)
  val agencyOrderId = OrderId(71286)
  val agencyOrders = List(Order(agencyOrderId, Balance2(100000)))
  val agencyTransactionTimestampNanos = 904000000
  val agencyTransactionTimestampSeconds = 1528994858
  val agencyTransactionTimestamp = OffsetDateTime.of(2018, 6, 14, 19, 47, 38, 904000000, offset)
  val agencyTransactionAmount = 12770000
  val agencyTransactionAmountRub = 127700

  val agencyOrderTransactions = List(
    OrderTransaction(agencyTransactionTimestamp, agencyTransactionAmount, transactionType)
  )

  val intTestShortAgencyParams = OrderTransactionsParams(
    LocalDate.of(2018, 8, 7),
    Some(LocalDate.of(2018, 9, 22)),
    pageNum = None,
    pageSize = None,
    transactionType = Some(transactionType),
    nonNegative = Some(true)
  )

  // data for full response (with all fields) here
  val fullPageSize = 3
  val fullPageNumber = 15
  val fullTransactionType = "Withdraw"

  val fullParams = OrderTransactionsParams(
    from,
    Some(to),
    pageNum = Some(fullPageNumber),
    pageSize = Some(fullPageSize),
    transactionType = Some(fullTransactionType),
    nonNegative = Some(true)
  )
  val fullBalanceId = BalanceId(36273879)
  val fullOrderId = OrderId(82132)
  val fullOrders = List(Order(fullOrderId, Balance2(100000)))
  val fullTotal = 222
  val fullTransaction0Amount = 600
  val fullTransaction0AmountRub = 6
  val fullTransaction0TimestampSeconds = 1528902000
  val fullTransaction0Timestamp = OffsetDateTime.of(2018, 6, 13, 18, 0, 0, 0, offset)
  val fullTransaction1Amount = 300
  val fullTransaction1AmountRub = 3
  val fullTransaction1TimestampSeconds = 1528898400
  val fullTransaction1Timestamp = OffsetDateTime.of(2018, 6, 13, 17, 0, 0, 0, offset)
  val fullTransaction2Amount = 100
  val fullTransaction2AmountRub = 1
  val fullTransaction2TimestampSeconds = 1528894800
  val fullTransaction2Timestamp = OffsetDateTime.of(2018, 6, 13, 16, 0, 0, 0, offset)

  val fullOrderTransactions = List(
    OrderTransaction(fullTransaction0Timestamp, fullTransaction0Amount, fullTransactionType),
    OrderTransaction(fullTransaction1Timestamp, fullTransaction1Amount, fullTransactionType),
    OrderTransaction(fullTransaction2Timestamp, fullTransaction2Amount, fullTransactionType)
  )

  val intTestFullBalanceId = BalanceId(46333491)
  val intTestFullOrderId = OrderId(90899)
  val intTestFullAgencyId = BalanceId(41468582)

  val intTestFullParams = OrderTransactionsParams(
    LocalDate.of(2018, 1, 1),
    Some(LocalDate.of(2018, 9, 19)),
    pageNum = Some(fullPageNumber),
    pageSize = Some(fullPageSize),
    transactionType = Some(fullTransactionType),
    nonNegative = Some(true)
  )

  val robotOperatorUid = 454716531

  def equalTo(right: OffsetDateTime): Matcher[Timestamp] = left => {
    MatchResult(
      left.getSeconds == right.toEpochSecond && left.getNanos == right.getNano,
      s"$left isn't equal to $right",
      s"$left equals to $right"
    )
  }
}
