package vertis.yql.container

import doobie.implicits._
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.containers.{GenericContainer, Network}
import common.db.config.DbConfig
import common.zio.logging.SyncLogger
import vertis.yql.container.TestYqlServer.{apiPort, yqlContainer, TestResources}
import vertis.yt.YtTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.ServerEnv
import vertis.zio.test.ZioSpecBase
import vertis.zio.yql.YqlClient
import vertis.zio.yql.conf.YqlConfig
import zio._
import zio.duration._

import java.time.Duration

trait TestYqlServer extends YtTest with BeforeAndAfterAll { this: ZioSpecBase =>

  override protected val ioTestTimeout: Duration = 5.minutes

  protected lazy val yqlConfig: YqlConfig =
    YqlConfig(
      DbConfig(
        driver = "ru.yandex.yql.YqlDriver",
        user = "me",
        password = "test",
        url = s"jdbc:yql://${yqlContainer.getHost}:${yqlContainer.getMappedPort(apiPort)}/local_yt?syntaxVersion=1",
        database = None
      )
    )

  protected lazy val makeYqlClient: RManaged[ServerEnv, YqlClient] = YqlClient.make(yqlConfig)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val query = sql"SELECT 123"
      .query[Int]
      .unique
    ioTest {
      for {
        _ <- logger.info("Going to ping yql")
        _ <- makeYqlClient.use { client =>
          // in case yql is not ready yet
          val retrySchedule = Schedule.exponential(1.seconds) && Schedule.recurs(5)
          client
            .executeQuery(query)
            .retry(retrySchedule)
        }
        _ <- logger.info("Yql is ready")
      } yield ()
    }
  }

  def resources: RManaged[ServerEnv, TestResources] =
    for {
      yt <- ytZio
      yql <- makeYqlClient
    } yield TestResources(yt, yql)
}

object TestYqlServer {
  private val logger = SyncLogger[TestYqlServer]

  case class TestResources(yt: YtZio, yql: YqlClient)

  private val apiPort = 32390

  private lazy val yqlContainer: GenericContainer[_] = {
    logger.info("Starting yt container...")
    val container: GenericContainer[_] =
      new GenericContainer("registry.yandex.net/vertis/vertis-yql-local:r8224978")

    container.withExposedPorts(apiPort)
    container.withStartupTimeout(Duration.ofMinutes(1))

    // too noisy, enable for debug issues only
    // it is easier to connect to container directly
//    container.withLogConsumer(toLogConsumer(logger))

    // to share with yql container
    container.withNetwork(Network.SHARED)

    // that's actually doesn't do much because it should be already started in val devinition
    container.dependsOn(YtTest.ytContainer)
    container.start()
    container
  }
}
