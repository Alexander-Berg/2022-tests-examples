package ru.yandex.vertis.subscriptions.storage.memory

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage

import scala.concurrent.ExecutionContext

/** Test on in-memory [[ru.yandex.vertis.subscriptions.storage.memory.SubscriptionsDao]] behavior.
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionsDaoSpec extends storage.SubscriptionsDaoSpecBase with BeforeAndAfter {

  protected val dao = new SubscriptionsDao()(ExecutionContext.global)

  before {
    dao.clear()
  }
}
