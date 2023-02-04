package vertis.ydb.test

import com.yandex.ydb.table.query.ExplainDataQueryResult
import common.zio.logging.SyncLogger
import org.scalatest.{Assertion, BeforeAndAfterAll}
import org.testcontainers.containers.Network
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.ydb.zio.{LoggedYdbZioWrapper, YdbZioWrapper}
import ru.yandex.vertis.ydb.{QueryOptions, YdbContainer, YdbQuerySyntaxVersion}
import vertis.ydb.YEnv
import vertis.ydb.conf.YdbConfig
import vertis.ydb.dao.YdbStorageDao
import vertis.ydb.queue.storage.Query
import vertis.ydb.test.YdbTest.{ydbContainer, ydbPort}
import vertis.zio.ServerEnv
import vertis.zio.logging.TestContainersLogging.toLogConsumer
import vertis.zio.test.ZioSpecBase
import zio.{Has, Runtime, ULayer, ZIO, ZLayer}

import scala.concurrent.duration._

/** @author kusaeva
  */
trait YdbTest extends TestOperationalSupport with YdbStorageDao with BeforeAndAfterAll {
  this: org.scalatest.Suite with ZioSpecBase =>

  def container: YdbContainer = ydbContainer

  private val tablePrefix = "/local"

  protected def tablePrefixFromClassName: String =
    s"$tablePrefix/" + this.getClass.getSimpleName
      .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
      .toLowerCase()

  // feel free to overrride
  protected def syntaxVersion = YdbQuerySyntaxVersion.V1

  private lazy val tableClient = container.tableClient

  private lazy val endpoint: String = s"${container.containerIpAddress}:$ydbPort"

  protected lazy val ydbWrapper: YdbZioWrapper = {
    new LoggedYdbZioWrapper(
      YdbZioWrapper.make(
        tableClient,
        tablePrefixFromClassName,
        30.seconds,
        QueryOptions.Default.copy(syntaxVersion = syntaxVersion)
      )
    )
  }

  protected lazy val ydbConfig: YdbConfig =
    YdbConfig(
      endpoint = endpoint,
      database = "",
      tablePrefix = tablePrefixFromClassName,
      token = "YDB_TOKEN"
    )

  protected lazy val ydbLayer: ULayer[Has[YdbZioWrapper]] = ZLayer.succeed(ydbWrapper)

  implicit lazy val zioRuntime: Runtime.Managed[YEnv with ServerEnv] =
    Runtime.unsafeFromLayer(
      env +!+ ydbLayer
    )

  protected lazy val queryExplainer = new QueryExplainer(ydbWrapper)

  def explainAll(queries: Query*): Unit = ioTest(ZIO.foreach(queries)(queryExplainer.printExplain))

  def withExplainResult(queries: Query*)(assertion: ExplainDataQueryResult => Assertion): Unit =
    ioTest(ZIO.foreach(queries) { q =>
      for {
        _ <- queryExplainer.printExplain(q)
        res <- queryExplainer.explain(q).map(assertion)
      } yield res
    })

  def ydbTest[E, A](io: ZIO[ServerEnv with YEnv, E, A]): Unit =
    ioTest {
      io.provideSomeLayer[ServerEnv](ydbLayer)
    }
}

object YdbTest {
  private val syncLogger = SyncLogger[YdbTest.type]

  /* same port as in docker-compose.yml */
  val ydbPort = 2135

  lazy val ydbContainer: YdbContainer = {
    val c = YdbContainer.stable
    c.container.withExposedPorts(ydbPort)
    c.container.withLogConsumer(toLogConsumer(syncLogger))
    c.container.start()
    c
  }
}
