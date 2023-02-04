package ru.yandex.vertis.subscriptions.backend.confirmation.impl

import ru.yandex.vertis.subscriptions.backend.confirmation.UserPropertiesServiceSpec
import ru.yandex.vertis.subscriptions.storage.cassandra.TestCassandra

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner

import scala.util.Random

/**
  */
@RunWith(classOf[JUnitRunner])
class CassandraUserPropertiesIntSpec extends UserPropertiesServiceSpec with BeforeAndAfterAll with TestCassandra {

  // for prevent writes and reads from same table at concurrent test runs
  private val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  private val session = testSession

  protected val service = new CassandraUserProperties(
    session,
    "user_properties",
    cassandraKeyspace,
    autoInitSchema = true,
    instanceId = instanceId
  )
}
