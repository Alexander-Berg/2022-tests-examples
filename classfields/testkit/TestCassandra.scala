package ru.yandex.hydra.profile.cassandra.testkit

import com.datastax.driver.core.Session
import org.testcontainers.containers.CassandraContainer
import org.testcontainers.utility.DockerImageName
import ru.yandex.common.monitoring.error.AlwaysWarningErrorPercentileTimeWindowReservoir
import ru.yandex.util.cassandra.{Credentials, StaticSessionFactory}
import zio._

import scala.concurrent.duration.DurationInt

object TestCassandra {
  private val IMAGE_NAME = "cassandra"
  private val TAG = "3.11.2"

  val keyspace: String = "test"

  private def start: Task[Session] = Task {
    val container = new CassandraContainer(DockerImageName.parse(IMAGE_NAME).withTag(TAG))
    container.start()
    container.getCluster
      .newSession()
      .execute(
        s"""
        CREATE KEYSPACE $keyspace
          WITH REPLICATION = {
         'class' : 'SimpleStrategy',
         'replication_factor' : 1
        }
      """
      )

    val host = container.getHost
    val port = container.getMappedPort(CassandraContainer.CQL_PORT)
    val dc = container.getCluster.getMetadata.getAllHosts.iterator().next().getDatacenter
    val credentials = Credentials(container.getUsername, container.getPassword)
    new StaticSessionFactory(
      nodes = host,
      localDataCenter = dc,
      credentials = Some(credentials),
      keyspace = Some(keyspace),
      port = Some(port)
    )(new AlwaysWarningErrorPercentileTimeWindowReservoir(5, 1.hour)).createSession()
  }

  private def stop(session: Session) = UIO(session.close)

  private val managedSession: UManaged[Session] = ZManaged.make(start)(stop).orDie

  val live: ULayer[Has[Session]] = managedSession.toLayer
}
