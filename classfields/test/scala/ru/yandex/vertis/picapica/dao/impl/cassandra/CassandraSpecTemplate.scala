package ru.yandex.vertis.picapica.dao.impl.cassandra

import java.util.concurrent.Executors

import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.common.monitoring.error.ExpiringWarningErrorCounterReservoir
import ru.yandex.util.cassandra.RoutedSessionFactory
import ru.yandex.vertis.picapica.dao.impl.cassandra.CassandraSpecTemplate._

import scala.util.Random

/**
  * @author evans
  */
trait CassandraSpecTemplate extends BeforeAndAfterAll {
  this: Suite =>

  private val keySpace = KeySpaceBase + Random.nextInt(10000)

  val session =
    new RoutedSessionFactory(
      Host,
      s"sas->$Dc",
      "sas"
    )(new ExpiringWarningErrorCounterReservoir()).createSession()

  implicit val ec =
    scala.concurrent.ExecutionContext.fromExecutor(
      Executors.newCachedThreadPool())

  // Sleeping to initialize
  Thread.sleep(1000)
  session.execute(
    s"CREATE KEYSPACE $keySpace WITH REPLICATION={'class':'SimpleStrategy','replication_factor':1}")
  session.execute(s"USE $keySpace")

  override def afterAll() {
    session.execute(s"DROP KEYSPACE $keySpace")

    // Sleeping to deinitialize
    Thread.sleep(1000)

    session.getCluster.close()
    super.afterAll()
  }


}

object CassandraSpecTemplate {
  val Host = "unittest-01-sas.dev.vertis.yandex.net"
  val Dc = "SAS"
  val KeySpaceBase = "unit_test"
}
