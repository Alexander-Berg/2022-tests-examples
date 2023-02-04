package ru.yandex.util.cassandra

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.error.AlwaysWarningErrorPercentileTimeWindowReservoir
import scala.concurrent.duration.DurationInt

/**
 * Unit tests for [[RoutedSessionFactory]]
 *
 * @author incubos
 */
class RoutedSessionFactoryIntSpec extends WordSpec with Matchers with TestCassandra {

  private val reservoir = new AlwaysWarningErrorPercentileTimeWindowReservoir(5, 1.hour)

  "RoutedSessionFactory" should {
    "connect to existing DC" in {
      val sf =
        new RoutedSessionFactory(
          cassandraHost,
          s"sas->$cassandraDc",
          "sas",
          Some(cassandraCredentials),
          Some(cassandraKeyspace),
          port = Some(cassandraPort))(reservoir)
      try {
        sf.createSession()
      } finally {
        sf.close()
      }
    }

    "fail to connect to nonexisting DC" in {
      intercept[IllegalArgumentException](
        new RoutedSessionFactory(
          cassandraHost,
          s"sas->$cassandraDc",
          "fol",
          Some(cassandraCredentials),
          Some(cassandraKeyspace),
          port = Some(cassandraPort))(reservoir)
      )
    }
  }
}
