package ru.yandex.util.cassandra

import com.typesafe.scalalogging.StrictLogging
import org.testcontainers.containers.CassandraContainer

/**
  *
  * @author zvez
  */
trait TestCassandra {
  import TestCassandra._

  lazy val cassandraHost: String = container.getContainerIpAddress
  lazy val cassandraPort: Int = container.getMappedPort(CassandraContainer.CQL_PORT)
  lazy val cassandraDc: String = container.getCluster.getMetadata.getAllHosts.iterator().next().getDatacenter
  lazy val cassandraCredentials: Credentials = Credentials(container.getUsername, container.getPassword)

  val cassandraKeyspace: String = "test"

}

object TestCassandra extends StrictLogging {

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
      """
        CREATE KEYSPACE test
          WITH REPLICATION = {
         'class' : 'SimpleStrategy',
         'replication_factor' : 1
        }
      """
    )
    c
  }
}
