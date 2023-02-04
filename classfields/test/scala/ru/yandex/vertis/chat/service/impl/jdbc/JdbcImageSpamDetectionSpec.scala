package ru.yandex.vertis.chat.service.impl.jdbc

import org.scalatest.OptionValues
import ru.yandex.vertis.chat.{Domain, Domains}
import ru.yandex.vertis.chat.components.dao.chat.storage.ChatStorage
import ru.yandex.vertis.chat.components.dao.security.spam.ImageSpamDetectionService
import ru.yandex.vertis.chat.components.dao.security.spam.impl.ImageSpamDetectionServiceImpl
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.DefaultTimeServiceImpl
import ru.yandex.vertis.chat.service.{ChatService, ImageSpamDetectionServiceBaseSpec}
import ru.yandex.vertis.chat.service.impl.TestDomainAware
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}
import ru.yandex.vertis.mockito.MockitoSupport

class JdbcImageSpamDetectionSpec
  extends ImageSpamDetectionServiceBaseSpec
  with JdbcSpec
  with TestDomainAware
  with OptionValues
  with MockitoSupport {

  val service: ChatService = new JdbcChatService(
    database,
    roomUuidGenerator = RandomIdGenerator,
    messageUuidGenerator = new TimeIdGenerator("localhost"),
    timeService = new DefaultTimeServiceImpl
  ) with SameThreadExecutionContextSupport

  val imageSpamDetectionService: ImageSpamDetectionService = new ImageSpamDetectionServiceImpl {
    override def chatStorage: DMap[ChatStorage] = DMap.forAllDomains(database)

    implicit override def domain: Domain = Domains.Auto
  }

}
