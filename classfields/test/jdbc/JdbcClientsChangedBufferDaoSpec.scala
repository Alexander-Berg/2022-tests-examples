package auto.dealers.loyalty.storage.jdbc

import auto.dealers.loyalty.model.ClientChangedBufferRecord.{InputRecord, ResultRecord}
import auto.dealers.loyalty.model.{ClientChangedBufferRecord, ClientId}
import auto.dealers.loyalty.storage.ClientsChangedBufferDao.DataSourceFilter
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment
import zio.{Task, ZIO}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.interop.catz._
import zio.test.TestAspect._

object JdbcClientsChangedBufferDaoSpec extends DefaultRunnableSpec {

  private val initialItems = List(
    ResultRecord(1, 1, "data_source_1"),
    ResultRecord(2, 1, "data_source_2"),
    ResultRecord(3, 1, "data_source_3"),
    ResultRecord(4, 2, "data_source_4")
  )

  private val sqlInitialItems = Fragment.const(
    initialItems
      .map { case ResultRecord(id, clientId, dataSource) =>
        s"($id, $clientId, '$dataSource')"
      }
      .mkString(", ")
  )

  private def toComparableItem(record: ClientChangedBufferRecord): (ClientId, String) =
    record.clientId -> record.dataSource

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("ClientsBufferChangedDao")(
      testM("list items by clientId and delete item by id") {
        val dataSources = Set("data_source_2", "data_source_3", "data_source_4")
        val filteredInitialItems = initialItems.filter(item => dataSources.contains(item.dataSource))
        val filter = DataSourceFilter(dataSources)

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcClientsChangedBufferDao(xa)

          initialList <- client.get(filter)
          _ <- client.delete(2)
          listAfterDelete <- client.get(filter)
        } yield assert(initialList)(hasSameElements(filteredInitialItems)) &&
          assert(listAfterDelete)(hasSameElements(filteredInitialItems.filterNot(_.id == 2)))
      },
      testM("insert items") {
        val filter = DataSourceFilter(Set("data_source_5", "data_source_6", "data_source_7"))
        val additionalToInsert = List(
          InputRecord(4, "data_source_5"),
          InputRecord(4, "data_source_6"),
          InputRecord(6, "data_source_6")
        )

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcClientsChangedBufferDao(xa)

          _ <- client.insert(additionalToInsert)
          result <- client.get(filter)
        } yield assert(result.map(toComparableItem))(hasSameElements(additionalToInsert.map(toComparableItem)))
      },
      testM("get filtered item") {
        val filteredResult = List(InputRecord(1, "data_source_1"))

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcClientsChangedBufferDao(xa)

          res <- client.get(DataSourceFilter(Set("data_source_1")))
        } yield assert(res.map(toComparableItem))(hasSameElements(filteredResult.map(toComparableItem)))
      }
    ) @@
      beforeAll(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            for {
              _ <- InitSchema("/schema.sql", xa)
            } yield ()
          }
      ) @@
      before(
        ZIO.service[Transactor[Task]].flatMap { xa =>
          sql"""
            INSERT INTO `clients_changed_buffer` (`id`, `client_id`, `data_source`)
            VALUES $sqlInitialItems
            """.update.run.transact(xa)
        }
      ) @@
      after(
        ZIO.service[Transactor[Task]].flatMap(xa => sql"TRUNCATE TABLE clients_changed_buffer".update.run.transact(xa))
      )
      @@ sequential).provideCustomLayerShared(TestMySQL.managedTransactor)
  }
}
