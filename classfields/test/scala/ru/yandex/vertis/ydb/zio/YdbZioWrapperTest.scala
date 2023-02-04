package ru.yandex.vertis.ydb.zio

import com.dimafeng.testcontainers.ForAllTestContainer
import com.yandex.ydb.core.UnexpectedResultException
import com.yandex.ydb.table.YdbTable.ExecuteScanQueryRequest
import com.yandex.ydb.table.description.TableDescription
import com.yandex.ydb.table.query.Params
import com.yandex.ydb.table.settings.{ExecuteScanQuerySettings, ReadTableSettings}
import com.yandex.ydb.table.values.PrimitiveType
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.ydb.zio.TxError.Died
import ru.yandex.vertis.ydb.{QueryOptions, YdbContainer, YdbQuerySyntaxVersion, YdbResult}
import zio.clock.Clock
import zio.{FiberFailure, Has, Promise, Ref, Runtime, Schedule, ZIO, ZLayer}

import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-08
  */
class YdbZioWrapperTest extends AnyWordSpec with Matchers with ForAllTestContainer {

  override val container: YdbContainer = YdbContainer.stable

  val r = Runtime.default

  lazy val ydb: YdbZioWrapper =
    LoggedYdbZioWrapper(YdbZioWrapper.make(container.tableClient, "/local", 3.seconds))

  private val count = new AtomicInteger

  def withTestTable[T](io: String => ZIO[Clock, Any, T]): T = {
    r.unsafeRun {
      val tableName = "test_table_" + count.getAndIncrement
      ydb
        .executeSchema(s"create table $tableName (field Utf8, PRIMARY KEY (field))")
        .bracket(_ => ydb.executeSchema(s"drop table $tableName").ignore) { _ =>
          io(tableName)
        }
    }
  }

  def withTestTableWithIndex[T](io: (String, String, String) => ZIO[Clock, Any, T]): T = {
    r.unsafeRun {
      val tableName = "test_table_" + count.getAndIncrement
      val indexedFieldName = "indexed"
      val indexName = "indexed_idx"
      ydb
        .executeSchema(
          s"create table $tableName (field Utf8, $indexedFieldName Utf8, " +
            s"PRIMARY KEY (field), INDEX $indexName GLOBAL ON ($indexedFieldName))",
          QueryOptions(syntaxVersion = YdbQuerySyntaxVersion.V1)
        )
        .bracket(_ => ydb.executeSchema(s"drop table $tableName").ignore) { _ =>
          io(tableName, indexedFieldName, indexName)
        }
    }
  }

  def withTestTable[T](description: TableDescription)(io: String => ZIO[Clock, Any, T]): T = {
    r.unsafeRun {
      val tableName = "test_table_" + count.getAndIncrement
      ydb
        .createTable(tableName, description)
        .bracket(_ => ydb.executeSchema(s"drop table $tableName").ignore) { _ =>
          io(tableName)
        }
    }
  }

  "YdbZioWrapper" should {
    "single select" in {
      val query = ydb.execute("select 1")
      val res = r.unsafeRun(ydb.runTx(query))

      (res.resultSets should have).length(1)
      res.resultSets.head.getRowCount shouldBe 1
    }
    "create and drop table" in {
      val res = withTestTable { tableName =>
        ydb.runTx(ydb.execute(s"select * from $tableName"))
      }
      res.resultSet.getRowCount shouldBe 0
    }
    "do update with automatic commit" in {
      val res = withTestTable { tableName =>
        for {
          _ <- ydb.runTx(ydb.execute(s"replace into $tableName(field) values ('test message')"))
          res <- ydb.runTx(ydb.execute(s"select * from $tableName"))
        } yield res
      }
      res.resultSet.getRowCount shouldBe 1
      res.resultSet.next() shouldBe true
      res.resultSet.getColumn("field").getUtf8 shouldBe "test message"
    }

    "select many rows" in {
      import ydb.ops._
      val res = withTestTable { tableName =>
        for {
          _ <- ydb.runTx(ydb.execute(s"replace into $tableName(field) values ('test1'), ('test2')"))
          res <- ydb.runTx(ydb.execute(s"select * from $tableName"))
        } yield res
      }
      res.resultSet.getRowCount shouldBe 2
      res.resultSet.toSeq(_.getColumn("field").getUtf8()) shouldBe Seq("test1", "test2")
    }

    "do updates with retries (with auto commit)" in {
      import ydb.ops._
      val res = withTestTable { tableName =>
        for {
          counter <- Ref.make(0)
          barrier <- Promise.make[Nothing, Unit]
          barrier2 <- Promise.make[Nothing, Unit]
          _ <- ydb.runTx(
            ydb.execute(
              s"replace into $tableName (field) values ('test message')"
            )
          )

          first <- ydb.runTx {
            for {
              _ <- counter.update(_ + 1)
              _ <- ydb.execute(
                s"select * from $tableName where field = 'test message'"
              )
              _ <- barrier2.succeed(())
              _ <- barrier.await
              _ <-
                ydb
                  .execute(s"delete from $tableName where field = 'test message'")
                  .withAutoCommit
              count <- counter.get
            } yield count
          }.fork

          second <- {
            for {
              _ <- barrier2.await
              _ <- ydb.runTx(
                ydb
                  .execute(
                    s"delete from $tableName where field = 'test message'"
                  )
                  .withAutoCommit
              )
              _ <- barrier.succeed(())
            } yield ()
          }.fork

          cnt <- first.join
          _ <- second.join
        } yield cnt
      }

      res shouldBe 2
    }

    "do updates with retries (without auto commit)" in {
      val res = withTestTable { tableName =>
        for {
          counter <- Ref.make(0)
          barrier <- Promise.make[Nothing, Unit]
          barrier2 <- Promise.make[Nothing, Unit]
          _ <- ydb.runTx(
            ydb.execute(
              s"replace into $tableName (field) values ('test message')"
            )
          )

          first <- ydb.runTx {
            for {
              _ <- counter.update(_ + 1)
              _ <- ydb.execute(
                s"select * from $tableName where field = 'test message'"
              )
              _ <- barrier2.succeed(())
              _ <- barrier.await
              _ <- ydb.execute(s"delete from $tableName where field = 'test message'")
              count <- counter.get
            } yield count
          }.fork

          second <- {
            for {
              _ <- barrier2.await
              _ <- ydb.runTx(
                ydb
                  .execute(
                    s"delete from $tableName where field = 'test message'"
                  )
              )
              _ <- barrier.succeed(())
            } yield ()
          }.fork

          cnt <- first.join
          _ <- second.join
        } yield cnt
      }

      res shouldBe 2
    }

    "readTable" in {
      val settings = ReadTableSettings
        .newBuilder()
        .timeout(10, TimeUnit.SECONDS)
        .build()
      val res = withTestTable { tableName =>
        for {
          _ <- ydb.runTx(
            ydb.execute(
              s"insert into $tableName (field) " +
                "values ('test1'), ('test2'), ('test3')"
            )
          )
          res <- ydb.readTable(tableName, _.getColumn("field").getUtf8(), settings).runCollect
        } yield res
      }
      res shouldBe List("test1", "test2", "test3")
    }

    "partial readTable" in {
      val settings = ReadTableSettings
        .newBuilder()
        .timeout(10, TimeUnit.SECONDS)
        .build()
      val res = withTestTable { tableName =>
        for {
          _ <- ydb.runTx(
            ydb.execute(
              s"insert into $tableName (field) " +
                "values ('test1'), ('test2'), ('test3')"
            )
          )
          res <- ydb.readTable(tableName, _.getColumn("field").getUtf8(), settings).take(2).runCollect
        } yield res
      }
      res shouldBe List("test1", "test2")
    }

    "create table with index" in {
      val indexList = util.Arrays.asList("index")
      val description =
        TableDescription
          .newBuilder()
          .addNullableColumn("id", PrimitiveType.utf8())
          .addNullableColumn("index", PrimitiveType.utf8())
          .setPrimaryKey("id")
          .addGlobalIndex("index_idx", indexList)
          .build()

      val actualDescription = withTestTable(description) { tableName =>
        ydb.describeTable(tableName)
      }

      actualDescription.getIndexes.size should be(1)
      actualDescription.getIndexes.get(0).getColumns should be(util.Arrays.asList("index"))
      actualDescription.getIndexes.get(0).getName should be("index_idx")
    }

    "select from index" in {
      val res = withTestTableWithIndex { (tableName, indexedFieldName, indexName) =>
        ydb.runTx(
          ydb.execute(
            s"select * from $tableName view $indexName WHERE $indexedFieldName LIKE '%any%'",
            QueryOptions(syntaxVersion = YdbQuerySyntaxVersion.V1)
          )
        )
      }
      res.resultSet.getRowCount shouldBe 0
    }

    "work with env" in {
      type TestEnv = Has[String]
      val res = withTestTable { tableName =>
        (for {
          _ <- ydb.runTx {
            for {
              _ <- ydb.execute("select 1")
              env <- TxEnv.env[TestEnv]
              _ <- ydb.execute(s"replace into $tableName(field) values ('${env.get[String]}')")
            } yield ()
          }
          res <- ydb.runTx(ydb.execute(s"select * from $tableName"))
        } yield res)
          .provideLayer(Clock.any ++ ZLayer.succeed("simple env"))
      }
      res.resultSet.getRowCount shouldBe 1
      res.resultSet.next() shouldBe true
      res.resultSet.getColumn("field").getUtf8 shouldBe "simple env"
    }
    "work with env inside tx" in {
      import ydb.ops._
      trait TestEnv {
        def value: String
      }
      val res = withTestTable { tableName =>
        for {
          _ <- ydb.runTx {
            val action: Tx[TestEnv, Nothing, Unit] = for {
              env <- TxEnv.env[TestEnv]
              _ <- ydb.execute(s"replace into $tableName(field) values ('${env.value}')")
            } yield ()

            (ydb.execute("select 1") *> action).provideTxEnv(new TestEnv {
              override val value: String = "simple env"
            })
          }
          res <- ydb.runTx(ydb.execute(s"select * from $tableName"))
        } yield res
      }
      res.resultSet.getRowCount shouldBe 1
      res.resultSet.next() shouldBe true
      res.resultSet.getColumn("field").getUtf8 shouldBe "simple env"
    }

    "drop table" in {
      withTestTable { tableName =>
        ydb.describeTable(tableName) *>
          ydb.dropTable(tableName) *>
          ydb.describeTable(tableName).flip
      }
    }

    "propagate error if it occurs in the middle of transaction" in {
      import ydb.ops._

      val exc = intercept[FiberFailure] {
        withTestTable { tableName =>
          def op(fieldName: String) =
            ydb.execute(s"select $fieldName FROM $tableName")

          ydb.runTx(
            for {
              _ <- op("field")
              _ <- op("fiel").withAutoCommit
            } yield {}
          )
        }
      }

      exc.cause.failures shouldNot be(empty)
      exc.cause.failures.head should be(a[Died])
      val died = exc.cause.failures.head.asInstanceOf[Died]
      val cause = died.cause
      cause should be(a[UnexpectedResultException])
      cause.getMessage should include("Member not found: fiel")
    }
    "propagate defect if it occurs in the middle of transaction" in {
      import ydb.ops._

      val exc = intercept[FiberFailure] {
        withTestTable { tableName =>
          def op(fieldName: String) =
            ydb.execute(s"select $fieldName FROM $tableName")

          ydb.runTx(
            for {
              _ <- op("field")
              _ <- (ZIO.dieMessage("Expected"): TxUIO[YdbResult]).withAutoCommit
            } yield {}
          )
        }
      }

      exc.cause.defects shouldNot be(empty)
      exc.cause.defects.head should be(a[RuntimeException])
      val cause = exc.cause.defects.head.asInstanceOf[RuntimeException]
      cause.getMessage should include("Expected")
    }
    "close transactions after failure" in {
      import ydb.ops._
      withTestTable { tableName =>
        ydb
          .runTx {
            for {
              _ <- ydb.execute(s"select 1 from $tableName")
              _ <- (ZIO.dieMessage("Expected"): TxUIO[YdbResult]).withAutoCommit
            } yield ()
          }
          .catchSomeDefect {
            case ex: RuntimeException if ex.getMessage.contains("Expected") => ZIO.unit
          }
          .repeat(
            Schedule.recurs(1000)
          ) // проверяем что транзакции не текут. ydb начнет жаловаться примерно на 200-ах активных транзакциях
      }
    }
    "abort transaction on failure" in {
      import ydb.ops._
      val result = withTestTable { tableName =>
        val insert = ydb
          .runTx {
            for {
              _ <- ydb.execute(s"replace into $tableName(field) values('1')")
              _ <- (ZIO.dieMessage("Expected"): TxUIO[YdbResult]).withAutoCommit
            } yield ()
          }
          .catchSomeDefect {
            case ex: RuntimeException if ex.getMessage.contains("Expected") => ZIO.unit
          }

        val select = ydb.runTx {
          for {
            res <- ydb.execute(s"select * from $tableName")
          } yield res
        }

        insert *> select
      }
      result.resultSet.getRowCount shouldBe 0
    }
    "scanQuery reads table" in {
      val settings = ExecuteScanQuerySettings
        .newBuilder()
        .timeout(10, TimeUnit.SECONDS)
        .mode(ExecuteScanQueryRequest.Mode.MODE_EXEC)
        .build()
      val res = withTestTable { tableName =>
        for {
          _ <- ydb.runTx(
            ydb.execute(
              s"insert into $tableName (field) " +
                "values ('test1'), ('test2'), ('test3')"
            )
          )
          res <-
            ydb
              .scanQuery(
                s"select * from $tableName",
                Params.empty(),
                _.getColumn("field").getUtf8(),
                settings
              )
              .runCollect
        } yield res
      }
      res shouldBe List("test1", "test2", "test3")
    }
    "scanQuery reads table without limit of 1000 rows" in {
      val settings = ExecuteScanQuerySettings
        .newBuilder()
        .timeout(10, TimeUnit.SECONDS)
        .mode(ExecuteScanQueryRequest.Mode.MODE_EXEC)
        .build()
      val res = withTestTable { tableName =>
        for {
          _ <- ZIO.foreachParN_(20)(1 until 1100) { i =>
            ydb.runTx(
              ydb.execute(s"insert into $tableName (field) values ('$i')")
            )
          }
          res <-
            ydb
              .scanQuery(
                s"select * from $tableName where cast(field as UInt32) < 1050",
                Params.empty(),
                _.getColumn("field").getUtf8(),
                settings
              )
              .runCollect
        } yield res
      }
      res shouldBe (1 to 1049).map(_.toString).toList.sorted
    }
  }
}
