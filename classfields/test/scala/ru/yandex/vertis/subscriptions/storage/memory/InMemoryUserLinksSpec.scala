package ru.yandex.vertis.subscriptions.storage.memory

import ru.yandex.vertis.subscriptions.TestExecutionContext

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage.UserLinksSpecBase

/**
  * Runnable specs on [[InMemoryUserLinks]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class InMemoryUserLinksSpec extends UserLinksSpecBase with TestExecutionContext {
  val userLinks: InMemoryUserLinks = new InMemoryUserLinks

  override def cleanTestData(): Unit =
    userLinks.clean()

}
