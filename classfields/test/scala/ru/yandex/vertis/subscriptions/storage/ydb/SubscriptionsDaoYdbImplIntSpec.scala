package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.SlowAsyncSpec
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDaoSpecBase

import scala.concurrent.duration.DurationInt

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionsDaoYdbImplIntSpec extends SubscriptionsDaoSpecBase with TestYdb with SlowAsyncSpec {

  override lazy val dao =
    new SubscriptionsDaoYdbImpl(
      tokens = new IntTokens(16),
      ydb = ydbWrapper,
      zioRuntime = zioRuntime,
      touchEvery = 1.millis,
      touchChance = 1.0
//      listReadLimit = 10 //todo bring back limit
    )

  override def afterStart(): Unit = {
    super.afterStart()
    zioRuntime.unsafeRun(dao.initSchema())
  }

}
