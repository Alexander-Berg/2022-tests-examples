package ru.yandex.vertis.subscriptions.storage.cassandra

import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.{storage, TestExecutionContext}

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner

import scala.concurrent.duration.DurationDouble
import scala.util.Random

/** Test on Cassandra [[ru.yandex.vertis.subscriptions.storage.cassandra.SubscriptionsDao]] behavior.
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionsDaoIntSpec
  extends storage.SubscriptionsDaoSpecBase
  with BeforeAndAfterAll
  with TestCassandra
  with TestExecutionContext {

  private val session = testSession

  // for prevent writes and reads from same table at concurrent test runs
  private val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  protected val dao = new SubscriptionsDao(session, new IntTokens(16), true, ec, instanceId, 1.millis)

}
