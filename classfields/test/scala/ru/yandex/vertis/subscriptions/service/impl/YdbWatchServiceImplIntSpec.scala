package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.TestExecutionContext
import ru.yandex.vertis.subscriptions.storage.ydb.{SubscriptionsDaoYdbImpl, TestYdb}

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner

/**
  * Runnable specs on [[WatchServiceImpl]] over YDB [[SubscriptionsDaoYdbImpl]].
  *
  */
//@RunWith(classOf[JUnitRunner])
class YdbWatchServiceImplIntSpec
  extends WatchServiceImplSpecBase
  with BeforeAndAfterAll
  with TestYdb
  with TestExecutionContext {

  lazy val subscriptionsDao =
    new SubscriptionsDaoYdbImpl(
      new IntTokens(16),
      ydbWrapper,
      zioRuntime
    )

  override def afterStart(): Unit = {
    super.afterStart()
    zioRuntime.unsafeRun(subscriptionsDao.initSchema())
  }

}
