package billing.finstat.storage.test

import billing.finstat.model.domain.Workspace
import billing.finstat.storage.FinstatStorage
import billing.finstat.storage.schema_service.SchemaWorkspaceMap
import billing.finstat.storage.schema_service.{SchemaLoader, SchemaWorkspaceMap}
import billing.finstat.storage.schema_service.SchemaLoader.SchemaLoader
import common.zio.clients.clickhouse.http.ClickHouseClientLive._
import common.zio.clients.clickhouse.http.codec.StdDecoders
import common.zio.clients.clickhouse.http.{ClickHouseClient, ClickHouseClientLive}
import common.zio.clients.clickhouse.testkit.TestClickhouse
import common.zio.logging.Logging
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import org.testcontainers.containers.ClickHouseContainer
import zio.clock.Clock
import zio.console.Console
import zio.magic._
import zio.test.TestAspect.{before, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{Has, ZIO, ZLayer}

object FinstatClickhouseJdbcSpec extends DefaultRunnableSpec with ClickhouseTestStatics with StdDecoders {

  override def spec: ZSpec[TestEnvironment, Any] = {
    val s = suite("FinstatClickHouseHttpSpec")(
      AggregationTests.simpleAdd,
      AggregationTests.simpleAddWithCaseClass,
      AggregationTests.mainAggregateTest,
      AggregationTests.versioningTest,
      RawSpendingsTests.mainParsingTest,
      RawSpendingsTests.inclusiveDateRangeTest,
      RawSpendingsTests.getFirstPage,
      RawSpendingsTests.getMiddlePage,
      RawSpendingsTests.getLastPage,
      RawSpendingsTests.getElementsWithStringListValueInFilters,
      SchemaLoaderTests.loadSchemaForAutoDealersWorkspace
    ) @@ before(dbClear *> SchemaWorkspaceMap.clear()) @@ sequential
    val clickhouseClientLive = ZLayer.fromServices[Endpoint, Sttp.Service, Auth, ClickHouseClient.Service](
      new ClickHouseClientLive(_, _, _)
    )
    val workspaceToClickhouseLive =
      ZLayer.fromService[ClickHouseClient.Service, Map[Workspace, ClickHouseClient.Service]](clickhouseClient =>
        Workspace.AllWorkspaces.map(_ -> clickhouseClient).toMap
      )

    val total =
      ZLayer.fromSomeMagic[
        Console,
        TestLogger with ClickHouseClient.ClickHouseClient with Has[FinstatStorage] with SchemaLoader
      ](
        ZLayer.fromService[ClickHouseContainer, Endpoint](cont =>
          Endpoint(cont.getHost, port = cont.getMappedPort(8123))
        ),
        Sttp.live,
        TestClickhouse.live,
        ZLayer.fromService[ClickHouseContainer, Auth](cont => Auth(cont.getUsername, cont.getPassword)),
        clickhouseClientLive,
        workspaceToClickhouseLive,
        FinstatStorage.live,
        Clock.live,
        SchemaLoader.live,
        Logging.live,
        TestLogger.fromConsole
      )
    s.provideCustomLayerShared {
      total.orDie
    }

  }

  private val dbInit = {
    for {
      client <- ZIO.service[ClickHouseClient.Service]
      sqls <- loadSqlFromFile("/schema.sql")
      queries = sqls.split(";\n")
      _ <- ZIO.foreach_(queries)(client.execute_)
    } yield ()
  }

  private val dbClear =
    for {
      client <- ZIO.service[ClickHouseClient.Service]
      _ <- client.execute_(dropTable)
      _ <- dbInit
    } yield ()

}
