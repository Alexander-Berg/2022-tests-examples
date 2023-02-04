package ru.yandex.hydra.profile.dao.cassandra

import ru.yandex.util.cassandra.{Credentials, StaticSessionFactory}
import com.datastax.driver.core.Session
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.CassandraContainer
import ru.yandex.common.monitoring.error.AlwaysWarningErrorPercentileTimeWindowReservoir

import scala.annotation.nowarn
import scala.concurrent.duration._

/** @author zvez
  */
trait TestCassandra extends BeforeAndAfterAll {
  this: Suite =>

  import TestCassandra._

  protected lazy val cassandraHost: String = container.getHost
  protected lazy val cassandraPort: Int = container.getMappedPort(CassandraContainer.CQL_PORT)
  protected lazy val cassandraDc: String = container.getCluster.getMetadata.getAllHosts.iterator().next().getDatacenter
  protected lazy val cassandraCredentials: Credentials = Credentials(container.getUsername, container.getPassword)

  protected def cassandraKeyspace: String = TestCassandra.keyspace

  protected lazy val session: Session =
    new StaticSessionFactory(
      nodes = cassandraHost,
      localDataCenter = cassandraDc,
      keyspace = Some(cassandraKeyspace),
      port = Some(cassandraPort)
    )(new AlwaysWarningErrorPercentileTimeWindowReservoir(5, 1.hour)).createSession()

  override protected def afterAll(): Unit = {
    session.close()
    super.afterAll()
  }
}

@nowarn("cat=deprecation")
object TestCassandra extends StrictLogging {

  val keyspace: String = "test"

  class TestCassandraContainer extends CassandraContainer[TestCassandraContainer]

  private lazy val container = {
    logger.info("Starting cassandra container")
    val c = new TestCassandraContainer()
    sys.addShutdownHook {
      logger.info("Stopping cassandra container")
      c.stop()
    }
    c.start()
    val session = c.getCluster.newSession()
    session.execute(
      s"""
        CREATE KEYSPACE $keyspace
          WITH REPLICATION = {
         'class' : 'SimpleStrategy',
         'replication_factor' : 1
        }
      """
    )
    c
  }
}
