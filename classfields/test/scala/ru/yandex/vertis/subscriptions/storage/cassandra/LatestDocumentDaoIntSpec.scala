package ru.yandex.vertis.subscriptions.storage.cassandra

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.storage.LatestDocumentDaoSpec

import scala.concurrent.duration._
import scala.util.Random

/**
  * Specs on [[LatestDocumentDao]]
  */
@RunWith(classOf[JUnitRunner])
class LatestDocumentDaoIntSpec extends LatestDocumentDaoSpec with BeforeAndAfterAll with TestCassandra {

  private val session = testSession

  // for prevent writes and reads from same table at concurrent test runs
  val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  val dao = new LatestDocumentDao(session, new IntTokens(1024), 10.minutes, true, instanceId)

  override def cleanData() {
    session.execute(s"TRUNCATE ${dao.table}")
  }

}
