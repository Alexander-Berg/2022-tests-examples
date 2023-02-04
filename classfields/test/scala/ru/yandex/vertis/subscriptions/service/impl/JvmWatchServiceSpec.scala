package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.service.{WatchService, WatchServiceSpecBase}

/**
  * Runnable spec on [[JvmWatchService]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class JvmWatchServiceSpec extends WatchServiceSpecBase {

  val watchService: WatchService = new JvmWatchService
}
