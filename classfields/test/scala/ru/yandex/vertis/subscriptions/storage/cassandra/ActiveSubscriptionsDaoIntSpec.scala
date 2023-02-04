package ru.yandex.vertis.subscriptions.storage.cassandra

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.storage.{ActiveSubscription, ActiveSubscriptionsDao, ActiveSubscriptionsDaoImpl, KeyValueTemplateAsync}

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
  * Specs on [[ActiveSubscriptionsDao]] with cassandra template
  */
@RunWith(classOf[JUnitRunner])
class ActiveSubscriptionsDaoIntSpec
  extends ru.yandex.vertis.subscriptions.storage.ActiveSubscriptionsDaoSpec
  with BeforeAndAfterAll
  with TestCassandra {

  private val session = testSession

  // for prevent writes and reads from same table at concurrent test runs
  val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  implicit val ec = ExecutionContext.global

  implicit val format = ActiveSubscription.Format

  val template = KeyValueTemplateAsync.cassandraInstance[ActiveSubscription](
    session,
    s"active_subscriptions",
    compositeKeySize = 2,
    autoInitSchema = true,
    ActiveSubscriptionsDao.TTL,
    ActiveSubscriptionsDao.REMOVED_TTL,
    instanceId,
    fetchSize = Some(500)
  )

  val dao = new ActiveSubscriptionsDaoImpl(new IntTokens(1024), template)

  override def cleanData() {
    session.execute(s"TRUNCATE ${dao.table}")
  }
}
