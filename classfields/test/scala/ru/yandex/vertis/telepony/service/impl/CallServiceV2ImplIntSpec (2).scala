package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.service.CallServiceV2Spec

/**
  * @author evans
  */
class CallServiceV2ImplIntSpec extends CallServiceV2Spec with IntegrationSpecTemplate {
  override def clean(): Unit = clear()
}
