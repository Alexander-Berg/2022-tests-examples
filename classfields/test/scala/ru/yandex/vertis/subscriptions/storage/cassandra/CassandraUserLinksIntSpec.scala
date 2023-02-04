package ru.yandex.vertis.subscriptions.storage.cassandra

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage.UserLinksSpecBase

import scala.util.Random

/**
  * Runnable spec on [[CassandraUserLinks]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class CassandraUserLinksIntSpec extends UserLinksSpecBase with BeforeAndAfterAll with TestCassandra {

  // for prevent writes and reads from same table at concurrent test runs
  val table = CassandraUserLinks.Table + "_" +
    Math.abs(Random.nextLong()).toString

  val userLinks: CassandraUserLinks = new CassandraUserLinks(testSession, autoInitSchema = true, table = table)

  override def cleanTestData(): Unit = {
    testSession.execute(s"TRUNCATE ${userLinks.table}")
  }

}
