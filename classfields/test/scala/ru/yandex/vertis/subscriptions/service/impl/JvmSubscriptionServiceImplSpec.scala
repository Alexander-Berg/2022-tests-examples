package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.vertis.subscriptions.TestExecutionContext
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao

/**
  * Runnable specs for [[SubscriptionServiceImpl]] over in-memory DAO.
  *
  * @author dimas
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class JvmSubscriptionServiceImplSpec extends SubscriptionServiceImplSpecBase with TestExecutionContext {
  def dao: SubscriptionsDao = new ru.yandex.vertis.subscriptions.storage.memory.SubscriptionsDao
}
