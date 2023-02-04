package ru.yandex.vertis.chat.service.impl.jvm

import org.scalatest.OptionValues
import ru.yandex.vertis.chat.{Domain, Domains}
import ru.yandex.vertis.chat.components.dao.chat.storage.{ChatStorage, JvmStorage}
import ru.yandex.vertis.chat.components.dao.security.spam.ImageSpamDetectionService
import ru.yandex.vertis.chat.components.dao.security.spam.impl.ImageSpamDetectionServiceImpl
import ru.yandex.vertis.chat.service.{ChatService, ImageSpamDetectionServiceBaseSpec}
import ru.yandex.vertis.chat.util.DMap

class JvmImageSpamDetectionSpec extends ImageSpamDetectionServiceBaseSpec {
  private val state = JvmChatState.empty()
  override val service: ChatService = new JvmChatService(state)

  val imageSpamDetectionService: ImageSpamDetectionService = new ImageSpamDetectionServiceImpl {
    override def chatStorage: DMap[ChatStorage] = DMap.forAllDomains(JvmStorage(state))

    implicit override def domain: Domain = Domains.Auto
  }
}
