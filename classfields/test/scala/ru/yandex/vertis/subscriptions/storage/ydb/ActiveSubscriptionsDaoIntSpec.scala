package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.SlowAsyncSpec
import ru.yandex.vertis.subscriptions.storage.{ActiveSubscription, ActiveSubscriptionsDao, ActiveSubscriptionsDaoImpl, ActiveSubscriptionsDaoSpec, KeyValueTemplateAsync}

import scala.util.Random

/**
  * Specs on [[ActiveSubscriptionsDao]] with ydb template
  */
@RunWith(classOf[JUnitRunner])
class ActiveSubscriptionsDaoIntSpec extends ActiveSubscriptionsDaoSpec with TestYdb with SlowAsyncSpec {

  // for prevent writes and reads from same table at concurrent test runs
  val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  implicit val format = ActiveSubscription.LightWeightFormat

  val template = KeyValueTemplateAsync.ydbInstance[ActiveSubscription](
    ydbWrapper,
    s"active_subscriptions",
    2,
    autoInitSchema = true,
    instanceId
  )

  val dao = new ActiveSubscriptionsDaoImpl(
    new IntTokens(16),
    template
  )

  override def cleanData(): Unit =
    zioRuntime.unsafeRun {
      ydbWrapper.runTx {
        ydbWrapper.execute(s"DELETE FROM ${dao.table}")
      }
    }
}
