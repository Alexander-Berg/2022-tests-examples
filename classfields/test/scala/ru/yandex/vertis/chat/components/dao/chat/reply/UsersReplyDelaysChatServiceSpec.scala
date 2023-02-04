package ru.yandex.vertis.chat.components.dao.chat.reply

import org.scalatest.OptionValues
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.chat.components.clients.s3.{FileStorage, NopFileStorage}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.workersfactory.SimpleWorkersFactorySupport
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.jdbc.JdbcSpec
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceSpecBase, TestOperationalSupport}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * TODO
  *
  * @author aborunov
  */
class UsersReplyDelaysChatServiceSpec
  extends ChatServiceSpecBase
  with MockitoSupport
  with OptionValues
  with JdbcSpec
  with Eventually {
  private val state = JvmChatState.empty()

  val service: ChatService =
    new JvmChatService(state)
      with SimpleWorkersFactorySupport
      with TestOperationalSupport
      with UsersReplyDelaysChatService
      with DomainAutoruSupport {
      override def s3Client: FileStorage = NopFileStorage
    }

  "UsersReplyDelaysChatService" should {
    "add reply on message send" in {
      val user1 = userId.next
      val user2 = userId.next
      val user3 = userId.next
      val room = createAndCheckRoom(_.withUserIds(Set(user1, user2)))
      val room2 = createAndCheckRoom(_.withUserIds(Set(user1, user3)))

      // TODO
    }
  }
}
