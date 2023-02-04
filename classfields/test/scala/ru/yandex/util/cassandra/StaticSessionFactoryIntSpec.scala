package ru.yandex.util.cassandra

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.error.AlwaysWarningErrorPercentileTimeWindowReservoir
import scala.concurrent.duration.DurationInt

/**
 * Unit tests for [[StaticSessionFactory]]
 *
 * @author incubos
 */
class StaticSessionFactoryIntSpec
  extends WordSpec
    with Matchers
    with TestCassandra {

  private val reservoir = new AlwaysWarningErrorPercentileTimeWindowReservoir(5, 1.hour)

  "StaticSessionFactory" should {
    "connect with password and keyspace" in {
      val sf =
        new StaticSessionFactory(
          cassandraHost,
          cassandraDc,
          Some(cassandraCredentials),
          Some(cassandraKeyspace),
          port = Some(cassandraPort))(reservoir)
      try {
        sf.createSession()
      } finally {
        sf.close()
      }
    }

    "connect without password" in {
      val sf =
        new StaticSessionFactory(
          cassandraHost,
          cassandraDc,
          port = Some(cassandraPort))(reservoir)
      try {
        sf.createSession()
      } finally {
        sf.close()
      }
    }
  }
}
