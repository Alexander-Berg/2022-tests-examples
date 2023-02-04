package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.service.BlockedCallServiceSpec

/**
  * @author evans
  */
class BlockedCallServiceV2ImplIntSpec extends BlockedCallServiceSpec with IntegrationSpecTemplate {
  override def clean(): Unit = clear()
}
