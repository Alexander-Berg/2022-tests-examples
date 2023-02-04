package billing.finstat.storage.test

import billing.finstat.model.domain.raw.{PagingRequest, RawSpendingsRequest}
import billing.finstat.model.domain.{DateRange, Equals, FinstatFilter, StringListValue, StringValue, Workspace}
import billing.finstat.storage.FinstatStorage
import billing.finstat.storage.schema_service.SchemaWorkspaceMap
import billing.finstat.storage.test.FinstatClickhouseJdbcSpec.testM
import common.zio.clients.clickhouse.http.codec.StdDecoders
import zio.ZIO
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test._

import java.time.{LocalDate, LocalDateTime}

object RawSpendingsTests extends ClickhouseTestStatics with StdDecoders {

  val mainParsingTest = testM("correctly parse raw results from JSON") {
    for {
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      storage <- ZIO.service[FinstatStorage]
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      req = RawSpendingsRequest(
        Workspace.AutoDealers,
        Nil,
        DateRange(
          from = LocalDate.parse("2020-10-01"),
          to = LocalDate.parse("2021-12-01")
        ),
        PagingRequest(1, 10),
        false
      )
      res <- storage.selectRawSpendings(req)
      _ <- TestLogger.logLine(res.mkString("Resulting list of raw events: [\n", "\n", "]"))
    } yield assert(res.head)(equalTo(event1))
  }

  val inclusiveDateRangeTest = testM("Return data inclusively with respect to date borders") {
    for {
      storage <- ZIO.service[FinstatStorage]
      ev1 = changeDate(event1, LocalDateTime.parse("2021-11-01T12:00:00"))
      ev2 = changeDate(event2, LocalDateTime.parse("2021-11-02T12:00:00"))
      _ <- query_(s"INSERT INTO $table VALUES " ++ ev1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ ev2.toSqlValue)
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      req = RawSpendingsRequest(
        Workspace.AutoDealers,
        Nil,
        DateRange(
          from = LocalDate.parse("2020-11-01"),
          to = LocalDate.parse("2021-11-02")
        ),
        PagingRequest(1, 10)
      )
      res <- storage.selectRawSpendings(req)
    } yield assert(res.length)(equalTo(2))
  }

  val getFirstPage = testM("get first page with right elements") {
    for {
      storage <- ZIO.service[FinstatStorage]
      event3 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T13:00:00")),
        transactionId = "tx3"
      )
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event2.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event3.toSqlValue)
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      req = RawSpendingsRequest(
        Workspace.AutoDealers,
        Nil,
        DateRange(
          from = LocalDate.parse("2020-11-01"),
          to = LocalDate.parse("2021-11-02")
        ),
        PagingRequest(pageNum = 1, pageSize = 2)
      )
      res <- storage.selectRawSpendings(req)
    } yield assert(res)(hasSameElements(List(event1, event2)))
  }

  val getMiddlePage = testM("get page in the middle with right elements") {
    for {
      storage <- ZIO.service[FinstatStorage]
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      event3 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T13:00:00")),
        transactionId = "tx3"
      )
      event4 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T14:00:00")),
        transactionId = "tx4"
      )
      event5 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T15:00:00")),
        transactionId = "tx5"
      )
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event2.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event3.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event4.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event5.toSqlValue)
      req = RawSpendingsRequest(
        Workspace.AutoDealers,
        Nil,
        DateRange(
          from = LocalDate.parse("2020-11-01"),
          to = LocalDate.parse("2021-11-02")
        ),
        PagingRequest(pageNum = 2, pageSize = 2)
      )
      res <- storage.selectRawSpendings(req)
    } yield assert(res)(hasSameElements(List(event3, event4)))
  }

  val getLastPage = testM("get last page with right elements") {
    for {
      storage <- ZIO.service[FinstatStorage]
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      event3 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T13:00:00")),
        transactionId = "tx3"
      )
      event4 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T14:00:00")),
        transactionId = "tx4"
      )
      event5 = changeTransactionId(
        changeDate(event1, LocalDateTime.parse("2021-11-02T15:00:00")),
        transactionId = "tx5"
      )
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event2.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event3.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event4.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event5.toSqlValue)
      req = RawSpendingsRequest(
        Workspace.AutoDealers,
        Nil,
        DateRange(
          from = LocalDate.parse("2020-11-01"),
          to = LocalDate.parse("2021-11-02")
        ),
        PagingRequest(pageNum = 3, pageSize = 2)
      )
      res <- storage.selectRawSpendings(req)
    } yield assert(res)(hasSameElements(List(event5)))
  }

  val getElementsWithStringListValueInFilters = testM("get elements with StringListValue in filters") {
    for {
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      storage <- ZIO.service[FinstatStorage]
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event2.toSqlValue)
      req = RawSpendingsRequest(
        Workspace.AutoDealers,
        List(FinstatFilter("offer_id", Equals, StringListValue(List("offerId1", "offerId2")))),
        DateRange(
          from = LocalDate.parse("2020-11-01"),
          to = LocalDate.parse("2021-11-02")
        ),
        PagingRequest(pageNum = 1, pageSize = 2)
      )
      res <- storage.selectRawSpendings(req)
    } yield assert(res)(hasSameElements(List(event1, event2)))
  }
}
