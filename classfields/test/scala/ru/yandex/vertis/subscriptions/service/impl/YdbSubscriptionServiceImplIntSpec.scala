package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.TestExecutionContext
import ru.yandex.vertis.subscriptions.storage.ydb.{SubscriptionsDaoYdbImpl, TestYdb}

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner

//@RunWith(classOf[JUnitRunner])
// fixme flappy
class YdbSubscriptionServiceImplIntSpec
  extends SubscriptionServiceImplSpecBase
  with BeforeAndAfterAll
  with TestYdb
  with TestExecutionContext {

  lazy val dao =
    new SubscriptionsDaoYdbImpl(
      new IntTokens(16),
      ydbWrapper,
      zioRuntime
    )

  override def afterStart(): Unit = {
    super.afterStart()
    zioRuntime.unsafeRun(dao.initSchema())
  }
}
