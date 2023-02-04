package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.service.{DraftService, DraftServiceSpecBase}

/**
  * Runnable specs on [[JvmDraftService]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class JvmDraftServiceSpec extends DraftServiceSpecBase {
  val service: DraftService = new JvmDraftService
}
