package ru.yandex.vertis.vsquality.utils.ydb_utils

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.ForAllTestContainer
import com.yandex.ydb.table.query.Params
import com.yandex.ydb.table.values.{ListValue, PrimitiveValue, StructValue}
import common.ydb.YdbUtils
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.vsquality.utils.ydb_utils.model.Row
import ru.yandex.vertis.ydb.YdbContainer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-05-31
  */
class YdbWrapperImplSpec extends AnyWordSpec with Matchers with ForAllTestContainer {
  override val container: YdbContainer = YdbContainer.stable

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  lazy val ydb: DefaultYdbWrapper[IO] = YdbWrapper.make[IO](container.tableClient, "/local", 3.seconds)
  val rowReader: RowReader[IO, String] = (row: Row) => IO.pure(row.getColumn("field").getUtf8)

  def withTestTable[T](io: IO[T]): T = {
    ydb
      .executeSchema("create table test_table (field Utf8, PRIMARY KEY (field))")
      .bracket(_ => io)(_ => ydb.executeSchema("drop table test_table"))
      .unsafeRunSync()
  }

  "YdbWrapper" should {
    "single select" in {
      val query = ydb.execute("select 1")
      val res = ydb.runTx(query).unsafeRunSync()

      (res.resultSets should have).length(1)
      res.resultSets.head.getRowCount shouldBe 1
    }
    "create and drop table" in {
      val res =
        withTestTable {
          ydb.runTx(ydb.execute("select * from test_table"))
        }
      res.resultSet.getRowCount shouldBe 0
    }
    "do update with automatic commit" in {
      val res =
        withTestTable {
          for {
            _   <- ydb.runTx(ydb.execute("replace into test_table(field) values (\"test message\")"))
            res <- ydb.runTx(ydb.execute("select * from test_table"))
          } yield res
        }
      res.resultSet.getRowCount shouldBe 1
      res.resultSet.next() shouldBe true
      res.resultSet.getColumn("field").getUtf8 shouldBe "test message"
    }
    "select many rows" in {
      val res =
        withTestTable {
          for {
            _   <- ydb.runTx(ydb.execute("replace into test_table(field) values ('test1'), ('test2')"))
            res <- ydb.runTx(ydb.execute("select * from test_table"))
          } yield res
        }
      res.resultSet.getRowCount shouldBe 2
      res.resultSet.rowIterator.map(_.getColumn("field").getUtf8()).toSeq shouldBe Seq("test1", "test2")
    }
    "read full table" in {
      val valuesGen = Gen.listOfN(2000, Gen.alphaNumStr)
      val valuesSet: Set[String] =
        Iterator
          .continually(valuesGen.sample)
          .flatten
          .next()
          .toSet

      val values = valuesSet.map(s => StructValue.of("field", PrimitiveValue.utf8(s))).toList
      val res =
        withTestTable {
          for {
            _ <- ydb.runTx(
              ydb.execute(
                """DECLARE $values AS List<Struct<field: Utf8>>;
                  |UPSERT into test_table SELECT * FROM AS_TABLE($values)""".stripMargin,
                Params.of("$values", YdbUtils.toListType(NonEmptyList.fromListUnsafe(values)))
              )
            )
            res <- ydb.readTable("test_table", rowReader).compile.toList
          } yield res
        }
      res.size shouldBe valuesSet.size
      res should contain theSameElementsAs valuesSet
    }
  }
}
