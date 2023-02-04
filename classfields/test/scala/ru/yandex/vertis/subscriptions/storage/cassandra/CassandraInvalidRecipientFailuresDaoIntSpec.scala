package ru.yandex.vertis.subscriptions.storage.cassandra

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage.InvalidRecipientFailuresDaoSpec

import scala.util.Random

/**
  * Cassandra implementation of [[InvalidRecipientFailuresDaoSpec]]
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class CassandraInvalidRecipientFailuresDaoIntSpec
  extends InvalidRecipientFailuresDaoSpec
  with BeforeAndAfterAll
  with TestCassandra {

  val table = CassandraInvalidRecipientFailuresDao.Table + "_" + Math.abs(Random.nextLong()).toString

  val dao = new CassandraInvalidRecipientFailuresDao(testSession, autoInitSchema = true, table = table)

}
