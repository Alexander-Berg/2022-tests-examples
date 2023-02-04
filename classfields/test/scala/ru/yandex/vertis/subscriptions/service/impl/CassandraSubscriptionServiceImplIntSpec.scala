package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.TestExecutionContext
import ru.yandex.vertis.subscriptions.storage.cassandra.{SubscriptionsDao, TestCassandra}
import ru.yandex.vertis.subscriptions.util.RandomIdGenerator

/**
  * Runnable specs for [[SubscriptionServiceImpl]] over C* DAO.
  *
  * @author dimas
  */
//@RunWith(classOf[JUnitRunner])
// fixme flappy
class CassandraSubscriptionServiceImplIntSpec
  extends SubscriptionServiceImplSpecBase
  with BeforeAndAfterAll
  with TestCassandra
  with TestExecutionContext {

  private def nextQualifier = Some(RandomIdGenerator.next)

  lazy val dao: SubscriptionsDao =
    new ru.yandex.vertis.subscriptions.storage.cassandra.SubscriptionsDao(
      testSession,
      new IntTokens(1024),
      blockingEc = ec,
      autoInitSchema = true,
      instanceId = nextQualifier
    )

}
