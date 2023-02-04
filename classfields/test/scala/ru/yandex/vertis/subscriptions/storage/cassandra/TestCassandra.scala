package ru.yandex.vertis.subscriptions.storage.cassandra

import ru.yandex.util.cassandra.{Credentials, StaticSessionFactory}
import ru.yandex.vertis.subscriptions.ops.NopErrorReservoir
import ru.yandex.vertis.subscriptions.util.Logging

import com.datastax.driver.core.Session
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.CassandraContainer

/**
  *
  * @author zvez
  */
trait TestCassandra extends BeforeAndAfterAll { this: Suite =>
  import TestCassandra._

  protected lazy val cassandraHost: String = container.getContainerIpAddress
  protected lazy val cassandraPort: Int = container.getMappedPort(CassandraContainer.CQL_PORT)
  protected lazy val cassandraDc: String = container.getCluster.getMetadata.getAllHosts.iterator().next().getDatacenter
  protected lazy val cassandraCredentials: Credentials = Credentials(container.getUsername, container.getPassword)

  protected def cassandraKeyspace: String = TestCassandra.keyspace

  protected lazy val testSession: Session =
    new StaticSessionFactory(
      nodes = cassandraHost,
      localDataCenter = cassandraDc,
      keyspace = Some(cassandraKeyspace),
      port = Some(cassandraPort)
    )(NopErrorReservoir).createSession()

  override protected def afterAll(): Unit = {
    testSession.close()
    super.afterAll()
  }
}

object TestCassandra extends Logging {

  val keyspace: String = "test"

  class TestCassandraContainer extends CassandraContainer[TestCassandraContainer]

  private lazy val container = {
    log.info("Starting cassandra container")
    val c = new TestCassandraContainer()
    sys.addShutdownHook {
      log.info("Stopping cassandra container")
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
