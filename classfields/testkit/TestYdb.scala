package common.zio.ydb.testkit

import com.yandex.ydb.core.grpc.GrpcTransport
import com.yandex.ydb.scheme.SchemeOperationProtos.Entry
import com.yandex.ydb.table.{SchemeClient, TableClient}
import com.yandex.ydb.table.transaction.TransactionMode
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.{HasTxRunner, Ydb, YdbAuthToken, YdbConfig, YdbHostPort}
import org.testcontainers.containers.Network
import ru.yandex.vertis.ydb.zio.{Tx, TxEnv, TxError, TxRunner, YdbZioWrapper}
import ru.yandex.vertis.ydb.{RetryOptions, YdbContainer, YdbTransaction}
import zio.clock.Clock
import zio.{Has, ULayer, URIO, ZIO, ZLayer, ZManaged}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.Random

object TestYdb {
  val YdbPort = 2135
  val ChOverYdbPort = 8123
  val TablePrefix = "/local"

  private val ydbContainer = YdbContainer.stable
  ydbContainer.container.start()

  sys.addShutdownHook {
    ydbContainer.stop()
  }

  val container: ULayer[Has[YdbContainer]] = ZLayer.succeed(ydbContainer)
  val transport: ULayer[Has[GrpcTransport]] = ZLayer.succeed(ydbContainer.transport)
  val tableClient: ULayer[Has[TableClient]] = ZLayer.succeed(ydbContainer.tableClient)
  val schemeClient: ZLayer[Has[GrpcTransport], Nothing, Has[SchemeClient]] = transport >>> Ydb.schemeClient

  val configWithTemporaryDirectory: ZLayer[Has[SchemeClient] with Has[TableClient], Nothing, Has[YdbConfig]] =
    ZLayer.fromServicesManaged[SchemeClient, TableClient, Any, Nothing, YdbConfig] { (s, t) =>
      for {
        path <- ZManaged.effectTotal(TablePrefix + "/" + System.currentTimeMillis() + "_" + Random.nextLong())
        makeDirectory = ZIO.fromCompletionStage(s.makeDirectory(path)).map(_.expect(s"Failed to make directory $path"))
        _ <- ZManaged
          .make(makeDirectory) { _ =>
            cleanDirectory(s, t, path).orDie
          }
          .orDie
      } yield YdbConfig(
        YdbHostPort(
          ydbContainer.container.getHost,
          ydbContainer.container.getMappedPort(YdbPort)
        ),
        path,
        auth = YdbAuthToken(""),
        minSessions = 1,
        maxSessions = 50,
        sessionAcquireTimeout = 5000.millis,
        metrics = false
      )
    }

  val ydb: ZLayer[Any, Nothing, Ydb] =
    transport >>> (tableClient ++ schemeClient >>> configWithTemporaryDirectory).passthrough >>> Ydb.wrapper

  def runTx[R <: Clock with HasTxRunner, E, A](action: Tx[R, E, A]): ZIO[R, E, A] = {
    for {
      runner <- ZIO.service[TxRunner]
      result <- runner.runTx(action).flatMapError(e => e.get.orDie)
    } yield result
  }

  def clean(tableName: String): URIO[Clock with HasTxRunner with Ydb, Unit] =
    ZIO.service[YdbZioWrapper].flatMap { ydb =>
      runTx(ydb.execute(s"DELETE FROM $tableName;")).unit
    }

  private def cleanDirectory(scheme: SchemeClient, table: TableClient, path: String): ZIO[Any, Throwable, Unit] = {
    def session =
      ZIO
        .fromCompletionStage(table.getOrCreateSession(java.time.Duration.ofSeconds(3)))
        .map(_.expect("Create session"))
        .toManaged(s => ZIO.effectTotal(s.release()))

    def dropTable(name: String) =
      session.use { s =>
        val fullName = path + "/" + name
        ZIO.fromCompletionStage(s.dropTable(fullName)).map(_.expect(s"Drop table $fullName"))
      }

    for {
      list <- ZIO.fromCompletionStage(scheme.listDirectory(path)).map(_.expect("List directory"))
      _ <- ZIO.foreach_(list.getChildren.asScala) { entry =>
        if (entry.getType == Entry.Type.DIRECTORY) {
          cleanDirectory(scheme, table, path + "/" + entry.getName) *>
            ZIO.fromCompletionStage(scheme.removeDirectory(path)).map(_.expect("removeDirectory"))
        } else {
          dropTable(entry.getName)
        }
      }
    } yield ()
  }
}
