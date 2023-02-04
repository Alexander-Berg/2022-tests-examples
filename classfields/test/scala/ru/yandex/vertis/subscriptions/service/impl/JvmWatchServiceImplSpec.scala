package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.vertis.subscriptions.TestExecutionContext

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao
import ru.yandex.vertis.subscriptions.storage.memory.{SubscriptionsDao => JvmSubscriptionDao}

/**
  * Runnable specs on [[WatchServiceImpl]] over Jvm [[SubscriptionsDao]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class JvmWatchServiceImplSpec extends WatchServiceImplSpecBase with BeforeAndAfterAll with TestExecutionContext {

  lazy val subscriptionsDao: SubscriptionsDao = new JvmSubscriptionDao

}
