package billing.finstat.storage.test

import billing.finstat.model.domain._
import billing.finstat.storage.FinstatStorage
import billing.finstat.storage.schema_service.SchemaWorkspaceMap
import billing.finstat.storage.test.FinstatClickhouseJdbcSpec.testM
import common.zio.clients.clickhouse.http.codec.StdDecoders
import zio.ZIO
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test.{TestLogger, _}

import java.time.LocalDate

object AggregationTests extends ClickhouseTestStatics with StdDecoders {

  val simpleAdd = testM("add single value") {
    for {
      b4 <- query[Long](countRows)
      _ <- query_(
        s"INSERT INTO $table VALUES " ++
          "('2021-01-01 00:00:00', 'transactionId1', 1, 10, 'boost', 'clientId1', 'offerId1', 'CARS', 'NEW', '44488', '998877')"
      )
      after <- query[Long](countRows)
    } yield assert(after - b4)(equalTo(1L))
  }

  val simpleAddWithCaseClass = testM("add single value made of case class") {
    for {
      b4 <- query[Long](countRows)
      sqlVal = s"INSERT INTO $table VALUES " ++ event1.toSqlValue
      _ <- TestLogger.logLine(sqlVal)
      _ <- query_(sqlVal)
      res <- query[Long](countRows)
    } yield assert(res - b4)(equalTo(1L))
  }

  val mainAggregateTest = testM("correctly parse aggregated results from JSON") {
    for {
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ event2.toSqlValue)
      stor <- ZIO.service[FinstatStorage]
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      req = AggregatedRequest(
        Workspace.AutoDealers,
        Nil,
        List("product"),
        List(
          AggregateBy("spent_kopecks", AggregationKind.Sum),
          AggregateBy("spent_kopecks", AggregationKind.Mean),
          AggregateBy("spent_kopecks", AggregationKind.Count)
        ),
        GroupingWindow.Month,
        DateRange(
          from = LocalDate.parse("2020-10-01"),
          to = LocalDate.parse("2021-12-01")
        )
      )
      res <- stor.selectAggregatedEvents(req)
      head = res.head.valuesByFields("spent_kopecks")

    } yield assert(res.length)(equalTo(1)) &&
      assert(head)(hasSameElements(List(Sum(110L), Count(2L), Mean(55L))))
  }

  val versioningTest = testM("Only aggregated data from the last version of transaction") {
    for {
      _ <- query_(s"INSERT INTO $table VALUES " ++ event1.toSqlValue)
      _ <- query_(s"INSERT INTO $table VALUES " ++ incrementVersion(event1).toSqlValue)
      stor <- ZIO.service[FinstatStorage]
      _ <- SchemaWorkspaceMap.update(Workspace.AutoDealers, AutoDealersWorkspaceSchema)
      req = AggregatedRequest(
        Workspace.AutoDealers,
        List(FinstatFilter("client_id", Equals, StringValue("clientId1"))),
        Nil,
        List(AggregateBy("spent_kopecks", AggregationKind.Sum)),
        GroupingWindow.Day,
        DateRange(
          from = LocalDate.parse("2021-10-01"),
          to = LocalDate.parse("2021-12-01")
        )
      )
      res <- stor.selectAggregatedEvents(req)
      head = res.head.valuesByFields("spent_kopecks")

    } yield assert(res.length)(equalTo(1)) && assert(head)(hasSameElements(List(Sum(10))))
  }
}
