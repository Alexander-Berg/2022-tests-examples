package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.TestExecutionContext
import ru.yandex.vertis.subscriptions.storage.cassandra._
import ru.yandex.vertis.subscriptions.util.RandomIdGenerator

/**
  * Runnable specs on [[WatchServiceImpl]] over Cassandra [[SubscriptionsDao]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class CassandraWatchServiceImplIntSpec
  extends WatchServiceImplSpecBase
  with BeforeAndAfterAll
  with TestCassandra
  with TestExecutionContext {

  private def nextQualifier = Some(RandomIdGenerator.next)

  lazy val subscriptionsDao = new SubscriptionsDao(
    testSession,
    new IntTokens(1024),
    autoInitSchema = true,
    instanceId = nextQualifier,
    blockingEc = ec
  )

}
