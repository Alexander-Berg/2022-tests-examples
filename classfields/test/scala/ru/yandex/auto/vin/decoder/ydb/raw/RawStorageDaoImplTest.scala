package ru.yandex.auto.vin.decoder.ydb.raw

import auto.carfax.common.utils.tracing.Traced
import auto.carfax.pro_auto.core.src.testkit.YdbContainerKit
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.ydb.raw.model.RowModel
import ru.yandex.auto.vin.decoder.ydb.raw.service.RawStorageDaoImpl
import ru.yandex.vertis.ydb.{QueryOptions, YdbContainer}
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.Runtime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RawStorageDaoImplTest
  extends AnyWordSpecLike
  with YdbContainerKit
  with ForAllTestContainer
  with BeforeAndAfterAll {

  // TODO refactoring raw storage dao tests

  implicit val t: Traced = Traced.empty

  lazy val zioRuntime: Runtime[zio.ZEnv] = Runtime.default

  lazy val ydb: YdbZioWrapper =
    YdbZioWrapper.make(container.tableClient, "/local", 3.seconds, QueryOptions.Default.withV1Syntax)

  lazy val table = new VinRawStorageTable(ydb, None, zioRuntime)
  lazy val lpTable = new LpRawStorageTable(ydb, None, zioRuntime)
  lazy val service = new RawStorageDaoImpl(table, lpTable, ydb, zioRuntime)

  private val TestData: Map[VinCode, Seq[RowModel[VinCode]]] = Map(
    VinCode.apply("WDD2221211A348904") -> Seq(
      RowModel(
        VinCode.apply("WDD2221211A348904"),
        RawStorageModel.RawData(
          "raw data response",
          "success"
        ),
        RawStorageModel.PreparedData(
          VinInfoHistory.newBuilder().setEventType(EventType.AUTOCODE_REGISTRATION).build()
        ),
        RawStorageModel.MetaData("g1", EventType.AUTOCODE_REGISTRATION, "raw data hash", 123, 456, 123)
      ),
      RowModel(
        VinCode.apply("WDD2221211A348904"),
        RawStorageModel.RawData(
          "raw data response 2",
          "404"
        ),
        RawStorageModel.PreparedData(
          VinInfoHistory.newBuilder().setEventType(EventType.AUTOCODE_ACCIDENT).build()
        ),
        RawStorageModel.MetaData("", EventType.AUTOCODE_ACCIDENT, "raw data hash 2", 789, 789, 0)
      )
    ),
    VinCode.apply("WDD2221211A348905") -> Seq(
      RowModel(
        VinCode.apply("WDD2221211A348905"),
        RawStorageModel.RawData(
          "raw data response",
          "success"
        ),
        RawStorageModel.PreparedData(
          VinInfoHistory.newBuilder().setEventType(EventType.AUTOCODE_REGISTRATION).build()
        ),
        RawStorageModel.MetaData("g2", EventType.AUTOCODE_REGISTRATION, "raw data hash", 123, 456, 1000)
      )
    )
  )

  private val TestVins: Seq[VinCode] = TestData.keys.toList

  import ydb.ops._

  val UpsertTestVin = TestVins.head
  val UpsertTestData: RowModel[VinCode] = TestData.head._2.head

  override def afterStart(): Unit = {
    table.init()
  }

  "RawStorageService" when {
    "bulk operations" should {
      "return empty response" in {
        val results = Await.result(Future.sequence(TestVins.map(service.getAllByVin)), 2.seconds)

        assert(results.size == TestVins.size)
        assert(results.forall(_.isEmpty))
      }
      "bulk insert" in {
        Await.result(service.bulkAppendVins(TestData.values.flatten.toList), 2.seconds)
      }
      "return non empty response" in {
        val res = TestVins
          .map(vin =>
            vin ->
              Await.result(service.getAllByVin(vin), 2.seconds)
          )
          .toMap

        assert(res.size == 2)

        TestVins.foreach(vin => {
          assert(TestData(vin).size == res(vin).size)
          assert(TestData(vin).forall(a => {
            res(vin).contains(a)
          }))
        })
      }
      "clear table" in {
        zioRuntime.unsafeRun(ydb.runTx(ydb.execute(s"delete from `${table.tablePath}`;").ignoreResult.withAutoCommit))
      }
    }
    "not bulk operations" should {
      "return empty response" in {
        val results = Await.result(Future.sequence(TestVins.map(service.getAllByVin)), 2.seconds)

        assert(results.size == TestVins.size)
        assert(results.forall(_.isEmpty))
      }
      "append data" in {
        val res = Future.sequence(TestData.flatMap { case (vin, records) =>
          records.map(data => {
            service.appendVin(data)
          })
        })

        Await.result(res, 2.seconds)
      }
      "append don't duplicate data" in {
        val vin = TestVins.head
        val data = TestData(vin).head

        Await.result(service.appendVin(data), 2.seconds)
      }
      "return non empty response" in {
        val res = TestVins
          .map(vin =>
            vin ->
              Await.result(service.getAllByVin(vin), 2.seconds)
          )
          .toMap

        assert(res.size == 2)

        TestVins.foreach(vin => {
          assert(TestData(vin).size == res(vin).size)
          assert(TestData(vin).forall(res(vin).contains))
        })
      }
      "return prepared" in {
        val res = TestVins
          .map(vin =>
            vin ->
              Await.result(service.getAllPreparedByVin(vin), 2.seconds)
          )
          .toMap

        assert(res.size == 2)

        TestVins.foreach(vin => {
          assert(TestData(vin).size == res(vin).size)
          assert(TestData(vin).forall(testData => res(vin).map(_.prepared).contains(testData.prepared)))
        })
      }
      "return batch prepared" in {
        val res = Await.result(service.getBatchAllPreparedByVin(TestVins), 2.seconds).groupBy(_.identifier)

        assert(res.size == 2)

        TestVins.foreach(vin => {
          assert(TestData(vin).size == res(vin).size)
          assert(TestData(vin).forall(testData => res(vin).map(_.prepared).contains(testData.prepared)))
        })
      }
      "clear table" in {
        zioRuntime.unsafeRun(ydb.runTx(ydb.execute(s"delete from `${table.tablePath}`;").ignoreResult.withAutoCommit))
      }
    }
    "upsert" should {
      "not duplicate row" in {
        assert(Await.result(service.getAllByVin(UpsertTestVin), 1.second).size === 0)
        Await.result(service.upsertVin(UpsertTestData), 1.second)
        assert(Await.result(service.getAllByVin(UpsertTestVin), 1.second).size === 1)
        Await.result(service.upsertVin(UpsertTestData), 1.second)
        assert(Await.result(service.getAllByVin(UpsertTestVin), 1.second).size === 1)
        Await.result(service.upsertVin(TestData.head._2.last), 1.second)
        assert(Await.result(service.getAllByVin(UpsertTestVin), 1.second).size === 2)
      }
      "clear table" in {
        zioRuntime.unsafeRun(ydb.runTx(ydb.execute(s"delete from `${table.tablePath}`;").ignoreResult.withAutoCommit))
      }
    }
  }
}
