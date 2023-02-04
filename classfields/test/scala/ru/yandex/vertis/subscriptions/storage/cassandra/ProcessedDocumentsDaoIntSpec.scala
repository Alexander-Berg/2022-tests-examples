package ru.yandex.vertis.subscriptions.storage.cassandra

import ru.yandex.vertis.subscriptions.storage

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

/**
  * Tests Cassandra [[ru.yandex.vertis.subscriptions.storage.ProcessedDocumentsDao]] implementation.
  */
@RunWith(classOf[JUnitRunner])
class ProcessedDocumentsDaoIntSpec extends storage.ProcessedDocumentsDaoSpec with BeforeAndAfterAll with TestCassandra {

  // for prevent writes and reads from same table at concurrent test runs
  private val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  private val session = testSession

  protected val dao = new ProcessedDocumentsDao(session, 1.day, true, instanceId)(ExecutionContext.Implicits.global)

}
