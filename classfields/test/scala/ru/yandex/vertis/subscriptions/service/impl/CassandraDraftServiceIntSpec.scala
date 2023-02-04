package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.service.DraftServiceSpecBase
import ru.yandex.vertis.subscriptions.storage.cassandra
import ru.yandex.vertis.subscriptions.storage.cassandra.TestCassandra
import ru.yandex.vertis.subscriptions.util.RandomIdGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
  * Runnable specs on [[CassandraDraftService]].
  *
  * @author dimas
  */
//@RunWith(classOf[JUnitRunner])
class CassandraDraftServiceIntSpec extends DraftServiceSpecBase with BeforeAndAfterAll with TestCassandra {

  def nextQualifier = RandomIdGenerator.next

  val service: CassandraDraftService =
    new CassandraDraftService(testSession, 10.minutes, autoInitSchema = true, Some(nextQualifier))
}
