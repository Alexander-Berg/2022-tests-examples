package ru.yandex.vertis.telepony.yql

import org.testcontainers.containers.{GenericContainer, Network}
import ru.yandex.vertis.telepony.logging.{SimpleLogging, SuperClassLogging}
import ru.yandex.vertis.telepony.util.yql.{YqlClient, YqlConfig}
import TestYqlServer.{apiPort, yqlContainer}
import akka.actor.Scheduler
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Minutes, Seconds, Span}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.util.TestPrometheusComponent
import ru.yandex.vertis.telepony.yt.YtTest
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.util.future.RetryUtils

import scala.concurrent.duration._
import java.time.Duration
import scala.concurrent.ExecutionContext

/**
  * @author tolmach
  */
trait TestYqlServer extends BeforeAndAfterAll with TestPrometheusComponent with SimpleLogging { this: SpecBase =>

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Minutes), interval = Span(1, Seconds))

  implicit def ec: ExecutionContext
  implicit def scheduler: Scheduler

  protected lazy val yqlConfig: YqlConfig =
    YqlConfig(
      cluster = "local_yt",
      user = "me",
      token = "test",
      hostPort = s"${yqlContainer.getContainerIpAddress}:${yqlContainer.getMappedPort(apiPort)}"
    )

  protected lazy val yqlClient = YqlClient.make(yqlConfig)(prometheusRegistry)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val query = sql"SELECT 123"

    log.info("Going to ping yql")
    RetryUtils
      .backoffRetry(5.second, times = 60, factor = 1) {
        yqlClient.executeSelectHead[Int]("test", query)
      }
      .futureValue

    log.info("Yql is ready")
  }

  protected def dropTableQuery(tableName: String) = {
    sql"DROP TABLE `#$tableName`"
  }

}

object TestYqlServer extends SimpleLogging {

  private val apiPort = 32390

  private lazy val yqlContainer: GenericContainer[_] = {
    log.info("Starting yql container...")
    val container: GenericContainer[_] =
      new GenericContainer("registry.yandex.net/vertis/etc/yql:dev")

    container.withExposedPorts(apiPort)
    container.withStartupTimeout(Duration.ofMinutes(1))

    //too noisy, enable for debug issues only
    //it is easier to connect to container directly
    //    container.withLogConsumer(toLogConsumer(logger))

    //to share with yql container
    container.withNetwork(Network.SHARED)

    //that's actually doesn't do much because it should be already started in val devinition
    container.dependsOn(YtTest.ytContainer)
    container.start()
    container
  }

}
