package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2Spec

/**
  * @author evans
  */
class OperatorNumberServiceV2ImplIntSpec extends OperatorNumberServiceV2Spec with IntegrationSpecTemplate {
  override def clean(): Unit = clear()
}
