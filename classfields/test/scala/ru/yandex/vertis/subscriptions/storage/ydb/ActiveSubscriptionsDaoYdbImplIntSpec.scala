package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.SlowAsyncSpec
import ru.yandex.vertis.subscriptions.storage.ActiveSubscriptionsDaoSpec

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class ActiveSubscriptionsDaoYdbImplIntSpec extends ActiveSubscriptionsDaoSpec with TestYdb with SlowAsyncSpec {

  import ydbWrapper.ops._

  override def dao: ActiveSubscriptionsDaoYdbImpl =
    new ActiveSubscriptionsDaoYdbImpl(new IntTokens(16), ydbWrapper, zioRuntime, 10)

  override def afterStart(): Unit = {
    super.afterStart()
    zioRuntime.unsafeRun(dao.storage.initSchema())
  }

  override def cleanData(): Unit =
    zioRuntime.unsafeRun {
      ydbWrapper.runTx {
        ydbWrapper.execute(s"DELETE FROM active_subscription").ignoreResult
      }
    }
}
