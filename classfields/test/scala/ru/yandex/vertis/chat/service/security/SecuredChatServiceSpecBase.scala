package ru.yandex.vertis.chat.service.security

import org.scalatest.OptionValues
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.chatbot.ChatBotUtils
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.dao.authority.AuthorityService
import ru.yandex.vertis.chat.components.dao.chat.SecuredChatService
import ru.yandex.vertis.chat.components.dao.security.BlockUserParameters
import ru.yandex.vertis.chat.components.time.TimeService
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model._
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service._
import ru.yandex.vertis.chat.service.exceptions.SecurityViolationException
import ru.yandex.vertis.chat.util.test.RequestContextAware

import scala.concurrent.ExecutionContext.Implicits.global
import ru.yandex.vertis.chat.components.dao.authority.BanScope

/**
  * Base spec for all possible secured chat service implementations
  */
trait SecuredChatServiceSpecBase extends SpecBase with ChatServiceTestKit with RequestContextAware with OptionValues {

  def effectiveService: ChatService

  def authorityService: AuthorityService

  def timeService: TimeService

  val service: SecuredChatService =
    TestSecuredChatService.wrap(effectiveService, authorityService, timeService)

  "SecuredChatService" should {

    "pass messages from participant" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      service.sendMessage(parameters).futureValue
    }

    "allow reading messages by participant" in {
      val room = createAndCheckRoom()
      loadSomeMessages(room.id, anyParticipant(room).next)
    }

    "check membership when sending message" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
        .copy(author = userId.next)
      a[SecurityViolationException] should be thrownBy {
        cause(service.sendMessage(parameters).futureValue)
      }
    }

    "check membership when reading messages" in {
      val room = createAndCheckRoom()
      a[SecurityViolationException] should be thrownBy {
        loadSomeMessages(room.id, userId.next)
      }
    }

    "check membership when updating chat" in {
      val room = createAndCheckRoom()
      a[SecurityViolationException] should be thrownBy {
        withUserContext(userId.next) { rc =>
          cause(
            service
              .updateRoom(room.id, updateRoomParameters.next)(rc)
              .futureValue
          )
        }
        loadSomeMessages(room.id, userId.next)
      }
    }

    "check membership when activating/deactivating chat" in {
      val room = createAndCheckRoom()
      a[SecurityViolationException] should be thrownBy {
        withUserContext(userId.next) { rc =>
          val participant = anyParticipant(room).next
          cause(
            service
              .setActive(room.id, participant, active = false)(rc)
              .futureValue
          )
        }
        loadSomeMessages(room.id, userId.next)
      }
    }

    "handle chat update" in {
      val user = userId.next
      val room = createAndCheckRoom()
      a[SecurityViolationException] should be thrownBy {
        loadSomeMessages(room.id, user)
      }
      val updated = updateRoom(room, anyParticipant(room).next, add = Set(user))
      loadSomeMessages(room.id, user)
      updateRoom(updated, user, remove = Set(user))
      a[SecurityViolationException] should be thrownBy {
        loadSomeMessages(room.id, user)
      }
    }

    "ignore all bans for user when creating tech support room" in {
      val user = userId.next
      val operator = userId.next
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      authorityService.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      withUserContext(user) { rc =>
        val parameters =
          TechSupportUtils.roomLocator(user).asSource.get.parameters
        createAndCheckRoom(parameters)(rc)
      }
    }

    "ignore all bans for user when creating chatbot room" in {
      val user = userId.next
      val operator = userId.next
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      authorityService.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      withUserContext(user) { rc =>
        val parameters = ChatBotUtils.roomLocator(user).asSource.get.parameters
        createAndCheckRoom(parameters)(rc)
      }
    }

    "respect AllUserChats ban for user when creating room" in {
      val user = userId.next
      val operator = userId.next
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      a[SecurityViolationException] should be thrownBy {
        withUserContext(user) { rc =>
          cause(createAndCheckRoom(rc))
        }
      }
      authorityService.clearBan(user, BanScope.AllUserChats, operator, None).futureValue
      withUserContext(user) { rc =>
        createAndCheckRoom(rc)
      }
    }

    "ignore AllUserChats ban for user when sending message to tech support" in {
      val user = userId.next
      val room = createAndCheckRoom(_ => TechSupportUtils.roomLocator(user).asSource.get.parameters)
      val operator = userId.next
      val parameters = sendMessageParameters(room).next
        .copy(author = user)
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      withUserContext(user) { rc =>
        service.sendMessage(parameters)(rc).futureValue
      }
    }

    "ignore all bans for user when sending message to chatbot" in {
      val user = userId.next
      val room = createAndCheckRoom(_ => ChatBotUtils.roomLocator(user).asSource.get.parameters)
      val operator = userId.next
      val parameters = sendMessageParameters(room).next
        .copy(author = user)
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      authorityService.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      withUserContext(user) { rc =>
        service.sendMessage(parameters)(rc).futureValue
      }
    }

    "respect AllUserChats ban for user when sending message" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      val parameters = sendMessageParameters(room).next
        .copy(author = user)
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      a[SecurityViolationException] should be thrownBy {
        withUserContext(user) { rc =>
          cause(service.sendMessage(parameters)(rc).futureValue)
        }
      }
      authorityService.clearBan(user, BanScope.AllUserChats, operator, None).futureValue
      withUserContext(user) { rc =>
        service.sendMessage(parameters)(rc).futureValue
      }
    }

    "ignore SupportChat ban for user when sending message" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      val parameters = sendMessageParameters(room).next
        .copy(author = user)
      authorityService.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      withUserContext(user) { rc =>
        service.sendMessage(parameters)(rc).futureValue
      }
    }

    "respect SupportChat ban for user when sending message to tech support" in {
      val user = userId.next
      val room = createAndCheckRoom(_ => TechSupportUtils.roomLocator(user).asSource.get.parameters)
      val operator = userId.next
      val parameters = sendMessageParameters(room).next
        .copy(author = user)
      authorityService.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      a[SecurityViolationException] should be thrownBy {
        withUserContext(user) { rc =>
          cause(service.sendMessage(parameters)(rc).futureValue)
        }
      }
      authorityService.clearBan(user, BanScope.SupportChat, operator, None).futureValue
      withUserContext(user) { rc =>
        service.sendMessage(parameters)(rc).futureValue
      }
    }

    "respect AllUserChats ban for user when updating room" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      a[SecurityViolationException] should be thrownBy {
        withUserContext(user) { rc =>
          cause(
            service
              .updateRoom(room.id, updateRoomParameters.next)(rc)
              .futureValue
          )
        }
      }
      authorityService.clearBan(user, BanScope.AllUserChats, operator, None).futureValue
      withUserContext(user) { rc =>
        service.updateRoom(room.id, updateRoomParameters.next)(rc).futureValue
      }
    }

    "respect block for user when sending message" in {
      val user = userId.next
      val blocked = userId.next
      val room = createAndCheckRoom(
        _.copy(participants = Participants.fromUserIds(user, blocked))
      )
      block(user, blocked)
      a[SecurityViolationException] should be thrownBy {
        withUserContext(blocked) { rc =>
          val parameters = sendMessageParameters(room).next
            .copy(author = blocked)
          cause(service.sendMessage(parameters)(rc).futureValue)
        }
      }
    }

    def block(from: UserId, to: UserId): Unit = {
      val ctx = userBlockingContext.next
      withUserContext(from) { rc =>
        authorityService.block(BlockUserParameters(to, ctx))(rc).futureValue
      }
    }

    "respect block for user when creating room" in {
      val user = userId.next
      val blocked = userId.next
      block(user, blocked)
      a[SecurityViolationException] should be thrownBy {
        withUserContext(blocked) { rc =>
          val parameters = createRoomParameters.next
            .copy(participants = Participants.fromUserIds(user, blocked))
          cause(createAndCheckRoom(parameters)(rc))
        }
      }
    }

    "ignore multi-user rooms" in {
      val user = userId.next
      val blocked = userId.next
      val other = userId.next
      block(user, blocked)
      withUserContext(blocked) { rc =>
        val roomParameters = createRoomParameters.next
          .copy(participants = Participants.fromUserIds(user, blocked, other))
        val room = createAndCheckRoom(roomParameters)(rc)
        val parameters = sendMessageParameters(room).next
          .copy(author = blocked)
        service.sendMessage(parameters)(rc).futureValue
      }
    }

    "allow to mute rooms for participants only" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      val nonParticipant = userId.next

      withUserContext(operator) { rc =>
        service.mute(room.id, user, mute = true)(rc).futureValue
      }

      a[SecurityViolationException] should be thrownBy {
        withUserContext(operator) { rc =>
          cause(
            service.mute(room.id, nonParticipant, mute = true)(rc).futureValue
          )
        }
      }
    }

    "allow to mute rooms for banned participants" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      withUserContext(user) { rc =>
        service.mute(room.id, user, mute = true)(rc).futureValue
      }
    }

    "allow to block rooms for participants only" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      val nonParticipant = userId.next

      withUserContext(operator) { rc =>
        service.blockRoom(room.id, user, value = true)(rc).futureValue
      }

      a[SecurityViolationException] should be thrownBy {
        withUserContext(operator) { rc =>
          cause(
            service
              .blockRoom(room.id, nonParticipant, value = true)(rc)
              .futureValue
          )
        }
      }
    }

    "not allow to block rooms by banned participants" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val operator = userId.next
      authorityService.ban(user, BanScope.AllUserChats, operator, None, None).futureValue

      a[SecurityViolationException] should be thrownBy {
        withUserContext(operator) { rc =>
          cause(service.blockRoom(room.id, user, value = true)(rc).futureValue)
        }
      }
    }

    "not allow to send message to blocked room" in {
      val room = createAndCheckRoom()
      val user = room.users.head
      val user2 = room.users.tail.head

      service.blockRoom(room.id, user, value = true).futureValue
      service.getRoom(room.id).futureValue.blocked shouldBe true
      val parameters1 = sendMessageParameters(room).next.copy(author = user2)
      a[SecurityViolationException] should be thrownBy {
        cause(service.sendMessage(parameters1).futureValue)
      }

      service.blockRoom(room.id, user, value = false).futureValue
      val parameters2 = sendMessageParameters(room).next.copy(author = user2)
      service.sendMessage(parameters2).futureValue
    }
  }

  private def updateRoom(room: Room,
                         user: UserId,
                         add: Set[UserId] = Set.empty,
                         remove: Set[UserId] = Set.empty): Room = {
    withUserContext(user) { rc =>
      val parameters =
        UpdateRoomParameters(Patch(add.map(User(_)), remove.map(User(_))))
      service.updateRoom(room.id, parameters)(rc).futureValue
    }
  }

  private def loadSomeMessages(room: RoomId, user: UserId): Unit = {
    withUserContext(user) { rc =>
      cause(
        service.getMessages(room, Window(None, 1, asc = true))(rc).futureValue
      )
    }
  }
  //TODO Timed ban tests
}
