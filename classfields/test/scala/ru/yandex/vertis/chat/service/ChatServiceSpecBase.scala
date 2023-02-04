package ru.yandex.vertis.chat.service

import org.joda.time.DateTime
import org.scalatest.OptionValues
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.app.Application
import ru.yandex.vertis.chat.components.dao.chat.locator.RemoteHttpChatService
import ru.yandex.vertis.chat.components.dao.chat.techsupport.TechSupportChatService
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.TestData.{Users => TestUsers, _}
import ru.yandex.vertis.chat.model._
import ru.yandex.vertis.chat.model.api.ApiModel.MessageProperties
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.util.test.RequestContextAware

import scala.util.Random

/**
  * Base specs on [[ChatService]].
  *
  * @author dimas
  * @author 747mmhg
  */
trait ChatServiceSpecBase
  extends SpecBase
  with ChatServiceTestKit
  with RequestContextAware
  with Eventually
  with OptionValues {

  import ChatServiceSpecBase._

  "ChatService" should {

    "add only request originator to participants when re-creating existing room" in {
      val room = createAndCheckRoom()
      val originator = userId.next
      val added = userId.next
      val removed = anyParticipant(room).next
      val newParticipants = room.participants.withUserId(originator).withUserId(added).withoutUserId(removed)
      val parameters = createRoomParameters(Some(room.id)).next
        .copy(participants = newParticipants)
      val expectedUserIds = room.participants.userIds + originator
      withUserContext(originator) { rc =>
        service.createRoom(parameters)(rc).futureValue.users should be(expectedUserIds)
      }
    }

    "add last not spam message to room for all participants" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      service.sendMessage(parameters).futureValue
      room.users.foreach { participant =>
        checkLastMessage(participant, room.id, Some(parameters))
      }
    }

    "add last spam message to room for author only" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next.copy(isSpam = true)
      service.sendMessage(parameters).futureValue
      checkLastMessage(parameters.author, room.id, Some(parameters))
      val recipients = room.users - parameters.author
      recipients.foreach { participant =>
        checkLastMessage(participant, room.id, None)
      }
    }

    "add last message (visible for)" in {
      val room = createAndCheckRoom()
      val user = anyParticipant(room).next
      val parameters = sendMessageParameters(room).next
      val visibleFor =
        if (user == parameters.author) Seq.empty
        else Seq(user, parameters.author)
      val parametersWithVisibleFor = parameters.copy(visibleFor = visibleFor)
      service.sendMessage(parametersWithVisibleFor).futureValue
      room.users.foreach { participant =>
        val recipients =
          if (visibleFor.nonEmpty) visibleFor
          else room.users.toSeq
        val isRecipient = recipients.contains(participant)
        val parametersOpt = Some(parameters).filter(_ => isRecipient)
        checkLastMessage(participant, room.id, parametersOpt)
      }
    }

    "send message with properties" in {
      val room = createAndCheckRoom()
      val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()
      val parameters = sendMessageParameters(room).next.withProperties(_.setUserAppVersion("ios"))
      val result = service.sendMessage(parameters).futureValue
      result.message.properties shouldBe props

      room.users.foreach(participant => {
        checkLastMessage(participant, room.id, Some(parameters))
        withUserContext(participant) { rc =>
          val message = service.getMessages(room.id, Window(None, 1, asc = false))(rc).futureValue.head
          message.properties shouldBe props
        }
      })
    }

    "send silent message without unread update" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next.copy(isSilent = true)

      val result = service.sendMessage(parameters).futureValue
      result.message.isSilent shouldBe true

      val expected = room.users.map(u => (u, false)).toMap
      checkUnread(room.id, expected)
      room.users.foreach(participant => {
        checkLastMessage(participant, room.id, Some(parameters))
      })

      val isRemote = service.isInstanceOf[RemoteHttpChatService]

      if (!isRemote) {
        val result2 = service.sendMessage(parameters).futureValue
        result2.message.isSilent shouldBe true
        result2.previousMessage.value.isSilent shouldBe true
      }
    }

    "round trip message attachments" in {
      val room = createAndCheckRoom()
      val attachments = attachment.next(3).toSeq
      val parameters = sendMessageParameters(room).next
        .copy(attachments = attachments)
      val sent = service.sendMessage(parameters).futureValue
      sent.attachments should be(attachments)
      val messages = withUserContext(parameters.author) { rc =>
        service.getMessages(room.id, Window(None, 100, asc = true))(rc).futureValue
      }
      messages.find(_.id == sent.id).get.attachments should be(attachments)
    }

    "correctly provide unread flag" in {
      val users = userId.next(5).toSet
      val room = createAndCheckRoom(_.withUserIds(users))

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          service.hasUnread(user).futureValue should be(false)
        }
      }

      val writer = users.head
      withUserContext(writer) { implicit rc =>
        service.sendMessage(sendMessageParameters(room.id, writer).next).futureValue
      }

      (users - writer).foreach { user =>
        withUserContext(user) { implicit rc =>
          service.hasUnread(user).futureValue should be(true)
        }
      }

    }

    "not take into account ureadeness of inactive room" in {
      val users = userId.next(5).toSet
      val room = createAndCheckRoom(_.withUserIds(users))

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          service.hasUnread(user).futureValue should be(false)
        }
      }

      val writer = users.head
      withUserContext(writer) { implicit rc =>
        service.sendMessage(sendMessageParameters(room.id, writer).next).futureValue
      }

      val reader = (users - writer).head

      withUserContext(reader) { implicit rc =>
        service.hasUnread(reader).futureValue should be(true)
      }

      withUserContext(reader) { implicit rc =>
        service.setActive(room.id, reader, active = false)
      }

      withUserContext(reader) { implicit rc =>
        val readRooms = service.getRooms(reader).futureValue
        withClue(readRooms) {
          service.hasUnread(reader).futureValue should be(false)
        }
      }

    }

    // TODO сделать тест про фичу CHAT_BOT, когда выпилим список юзеров и останется только фича

    "correctly provide rooms" in {
      val withTechSupport = service.isInstanceOf[TechSupportChatService] ||
        service.isInstanceOf[RemoteHttpChatService]
      val users = passportPrivateUserId.next(5).toSet

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          if (withTechSupport) {
            val techSupportRoom = coarse(TechSupportUtils.techSupportRoom(user))
            service.getRooms(user).futureValue.map(coarse) should be(Seq(techSupportRoom))
          } else service.getRooms(user).futureValue should be(Seq.empty[Room])
        }
      }

      val room = createAndCheckRoom(_.withUserIds(users))

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          if (withTechSupport) {
            val techSupportRoom = coarse(TechSupportUtils.techSupportRoom(user))
            service.getRooms(user).futureValue.map(coarse) should be(Seq(techSupportRoom, coarse(room)))
          } else service.getRooms(user).futureValue.map(coarse) should be(Seq(coarse(room)))
        }
      }

    }

    "correctly provide rooms for non passport users" in {
      val withTechSupport = service.isInstanceOf[TechSupportChatService] ||
        service.isInstanceOf[RemoteHttpChatService]
      val users = nonPassportUserId.next(5).toSet

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          service.getRooms(user).futureValue should be(Seq.empty[Room])
        }
      }

      val room = createAndCheckRoom(_.withUserIds(users))

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          service.getRooms(user).futureValue.map(coarse) should be(Seq(coarse(room)))
        }
      }

    }

    "get messages" in {
      val room = createAndCheckRoom()
      val roomId = room.id
      val messageParameters = sendMessageParameters(room)
        .next(10)
        .toArray

      val ids = messageParameters.map { parameters =>
        val user0 = parameters.author
        withUserContext(user0) { rc =>
          Thread.sleep(1)
          val id = service.sendMessage(parameters)(rc).futureValue.id
          id
        }
      }

      val user = anyParticipant(room).next
      val messages = withUserContext(user) { rc =>
        val window = Window(Some(ids(0)), 10, asc = true)
        service.getMessages(roomId, window)(rc).futureValue.toIndexedSeq
      }

      messages.size should be(10)

      val cases: Seq[(Window[MessageId], Seq[CreateMessageParameters])] = Seq(
        Window(None, 1, asc = true) -> messageParameters.take(1), // 0
        Window(None, 1, asc = false) -> messageParameters.takeRight(1), // 1
        Window(None, 2, asc = true) -> messageParameters.take(2), // 2
        Window(None, 2, asc = false) -> messageParameters.takeRight(2), // 3
        Window(Some(messages(0).id), 1, asc = true) -> messageParameters.take(1), // 4
        Window(Some(messages(0).id), 1, asc = false) -> messageParameters.take(1), // 5
        Window(Some(messages(0).id), 10, asc = true) -> messageParameters, // 6
        Window(Some(messages(9).id), 10, asc = false) -> messageParameters, // 7
        Window(Some(messages(1).id), 1, asc = true) -> messageParameters.slice(1, 2), // 8
        Window(Some(messages(1).id), 1, asc = false) -> messageParameters.slice(1, 2) // 9
      )

      cases.zipWithIndex.foreach {
        case ((window, expected), idx) =>
          val exp = withUserContext(user) { rc =>
            coarse(service.getMessages(roomId, window)(rc).futureValue)
          }
          withClue("case " + idx + " (" + window + ")") {
            exp should be(expected)
          }
      }
    }

    "get messages including spam" in {
      val room = createAndCheckRoom()
      val roomId = room.id
      val user1 = anyParticipant(room).next
      val messageParameters = sendMessageParameters(room)
        .next(10)
        .zipWithIndex
        .map {
          case (m, idx) =>
            if (idx < 4) m.copy(isSpam = true, author = user1)
            else m.copy(isSpam = false, author = user1)
        }
        .toIndexedSeq

      val ids = messageParameters.map { parameters =>
        service.sendMessage(parameters).futureValue.id
      }

      val messagesForUser1WithoutSpam = withUserContext(user1) { rc =>
        val window = Window(Some(ids(0)), 10, asc = true)
        service.getMessages(roomId, window)(rc).futureValue.toIndexedSeq
      }

      messagesForUser1WithoutSpam.size should be(10)

      val user2 = anyParticipant(room.participants.withoutUserId(user1)).next

      val messagesForUser2WithoutSpam = withUserContext(user2) { rc =>
        val window = Window(Some(ids(0)), 10, asc = true)
        service.getMessages(roomId, window)(rc).futureValue.toIndexedSeq
      }

      messagesForUser2WithoutSpam.size should be(6)

      val messagesForUser2WithSpam = withUserContext(user2) { rc =>
        val window = Window(Some(ids(0)), 10, asc = true)
        service.getMessages(roomId, window, includeSpam = true)(rc).futureValue.toIndexedSeq
      }

      messagesForUser2WithSpam.size should be(10)
    }

    "get only spam messages" in {
      val room = createAndCheckRoom()
      val roomId = room.id
      val user1 = anyParticipant(room).next
      val messageParameters = sendMessageParameters(room)
        .next(10)
        .zipWithIndex
        .map {
          case (m, idx) =>
            if (idx < 4) m.copy(isSpam = true, author = user1)
            else m.copy(isSpam = false, author = user1)
        }
        .toIndexedSeq

      val ids = messageParameters.map { parameters =>
        service.sendMessage(parameters).futureValue.id
      }

      val user2 = anyParticipant(room.participants.withoutUserId(user1)).next

      val messagesForUser2OnlySpam = withUserContext(user2) { rc =>
        val window = Window(Some(ids(0)), 10, asc = true)
        service.getMessages(roomId, window, onlySpam = true)(rc).futureValue.toIndexedSeq
      }

      messagesForUser2OnlySpam.size should be(4)
    }

    "get messages with visible for" in {
      val room = createAndCheckRoom()
      val roomId = room.id
      val messageParameters = sendMessageParameters(room)
        .next(10)
        .map { parameters =>
          val user = anyParticipant(room).next
          val visibleFor =
            if (user == parameters.author) Seq.empty
            else Seq(user, parameters.author)
          parameters.copy(visibleFor = visibleFor)
        }
        .toArray

      val ids = messageParameters.map { parameters =>
        service.sendMessage(parameters).futureValue.id
      }

      room.users.foreach { user =>
        val visibleMessages = withUserContext(user) { rc =>
          val window = Window(Some(ids(0)), 10, asc = true)
          service.getMessages(roomId, window)(rc).futureValue
        }
        val visibleMessagesCount = messageParameters.count { parameters =>
          parameters.visibleFor.isEmpty || parameters.visibleFor.contains(user)
        }

        visibleMessages.size should be(visibleMessagesCount)
      }
    }

    "search user messages" in {
      val room = createAndCheckRoom()
      val authorId = Random.shuffle(room.users).head
      val messageParameters = sendMessageParameters(room)
        .next(2)
        .map(_.copy(author = authorId))
        .toArray

      val ids = messageParameters.map { parameters =>
        service.sendMessage(parameters).futureValue.id
      }

      withUserContext(room.users.head) { implicit rc =>
        val res = service
          .searchMessages(
            messageParameters(0).payload.value,
            authorId,
            false,
            Window(None, 10, asc = true),
            DateTime.now.minusDays(1),
            DateTime.now.plusDays(1)
          )
          .futureValue
          .map(_.id)

        res should contain(ids(0))
        res shouldNot contain(ids(1))
      }
    }

    "search messages in invalid time range" in {
      val room = createAndCheckRoom()
      val authorId = Random.shuffle(room.users).head
      val messageParameters = sendMessageParameters(room)
        .next(2)
        .map(_.copy(author = authorId))
        .toArray

      withUserContext(room.users.head) { implicit rc =>
        val res = service
          .searchMessages(
            messageParameters(0).payload.value,
            authorId,
            false,
            Window(None, 10, asc = true),
            DateTime.now.minusDays(10),
            DateTime.now.minusDays(9)
          )
          .futureValue
          .map(_.id)

        res shouldBe empty
      }
    }

    "search messages within support chats" in {
      val room = createAndCheckRoom()
      val authorId = Random.shuffle(room.users).head
      val messageParameters = sendMessageParameters(room)
        .next(2)
        .map(_.copy(author = authorId))
        .toArray

      withUserContext(room.users.head) { implicit rc =>
        val res = service
          .searchMessages(
            messageParameters(0).payload.value,
            authorId,
            true,
            Window(None, 10, asc = true),
            DateTime.now.minusDays(1),
            DateTime.now.plusDays(1)
          )
          .futureValue
          .map(_.id)

        res shouldBe empty
      }
    }

    "create non-empty chat room" in {
      createAndCheckRoom()
    }

    "provide created chat room in listing" in {
      val withTechSupport = service.isInstanceOf[TechSupportChatService] ||
        service.isInstanceOf[RemoteHttpChatService]
      val room = createAndCheckRoom()
      room.users.foreach(participant => {
        withUserContext(participant) { rc =>
          val rooms = service.getRooms(participant)(rc).futureValue
          rooms.foreach(println)
          if (withTechSupport) rooms.size shouldBe 2
          else rooms.size shouldBe 1
          rooms.map(_.id) should contain(room.id)
        }
      })
    }

    "create empty chat room" in {
      createAndCheckRoom { parameters =>
        parameters.copy(participants = Participants.empty)
      }
    }

    "update chat room" in {
      val room = createAndCheckRoom { parameters =>
        parameters.copy(participants = Participants(TestUsers))
      }
      val usersPatch = Patch
        .empty[User]
        .alsoRemove(Alice)
        .alsoAdd(Peter) //It's a men's chat!

      val guys = TestUsers - Alice + Peter

      val updated = withUserContext(room) { rc =>
        val update = UpdateRoomParameters(usersPatch)
        service.updateRoom(room.id, update)(rc).futureValue
      }
      updated.users should be(guys.map(_.id))
      withUserContext(updated) { rc =>
        service.getRoom(room.id)(rc).futureValue.users should be(guys.map(_.id))
      }
    }

    "active room after sending not spam message" in {
      val room = createAndCheckRoom()
      val user = anyParticipant(room).next
      withUserContext(user) { rc =>
        service.setActive(room.id, user, active = false)(rc).futureValue
        service.getRooms(user)(rc).futureValue.exists(_.id == room.id) shouldBe false
      }

      val parameters = sendMessageParameters(room.withoutUser(user)).next
      withUserContext(parameters.author) { rc =>
        service.sendMessage(parameters)(rc).futureValue
      }

      withUserContext(user) { rc =>
        service.getRooms(user)(rc).futureValue.exists(_.id == room.id) shouldBe true
      }
    }

    "not activate room after sending spam message" in {
      val room = createAndCheckRoom()
      val user = anyParticipant(room).next
      withUserContext(user) { rc =>
        service.setActive(room.id, user, active = false)(rc).futureValue
      }
      service.getRooms(user).futureValue.exists(_.id == room.id) shouldBe false

      val updated = room.copy(users = room.users - user)
      val parameters = sendMessageParameters(updated).next.copy(isSpam = true)
      service.sendMessage(parameters).futureValue
      service.getRooms(user).futureValue.exists(_.id == room.id) shouldBe false
    }

    "activate and deactivate chat room" in {
      val room = createAndCheckRoom()
      val user = anyParticipant(room).next
      withUserContext(user) { rc =>
        service.setActive(room.id, user, active = false)(rc).futureValue
      }
      service.getRooms(user).futureValue.exists(_.id == room.id) shouldBe false
      withUserContext(user) { rc =>
        service.setActive(room.id, user, active = true)(rc).futureValue
        service.getRooms(user)(rc).futureValue.exists(_.id == room.id) shouldBe true
      }
    }

    "getRooms including non active" in {
      val room = createAndCheckRoom()
      val user = anyParticipant(room).next
      withUserContext(user) { rc =>
        service.setActive(room.id, user, active = false)(rc).futureValue
      }
      service.getRooms(user).futureValue.exists(_.id == room.id) shouldBe false
      service.getRooms(user, includeHidden = true).futureValue.exists(_.id == room.id) shouldBe true
    }

    "get only unread rooms" in {
      val withTechSupport = service.isInstanceOf[TechSupportChatService] ||
        service.isInstanceOf[RemoteHttpChatService]
      val user1 = passportPrivateUserId.next
      val user2 = passportPrivateUserId.next
      val payloads = createRoomParameters.next(10).toList.map(_.withoutAllUsers.withUserId(user1).withUserId(user2))
      val rooms = withUserContext(user1) { rc =>
        payloads.map(service.createRoom(_)(rc).futureValue)
      }
      rooms.foreach(room => {
        val message = sendMessageParameters(room.id, user2).next
        withUserContext(user2) { implicit rc =>
          service.sendMessage(message)(rc).futureValue
        }
      })
      rooms
        .take(5)
        .foreach(room => {
          withUserContext(user1) { implicit rc =>
            service.markRead(room.id, user1)(rc).futureValue
          }
        })
      withUserContext(user1) { rc =>
        service.getRooms(user1, onlyUnread = true)(rc).futureValue.foreach(println)
        service.getRooms(user1, onlyUnread = true)(rc).futureValue.length shouldBe (if (withTechSupport) 6 else 5)
        service.getRooms(user1, onlyUnread = false)(rc).futureValue.foreach(println)
        service.getRooms(user1, onlyUnread = false)(rc).futureValue.length shouldBe (if (withTechSupport) 11 else 10)
      }
    }

    "get only nonempty rooms" in {
      val withTechSupport = service.isInstanceOf[TechSupportChatService] ||
        service.isInstanceOf[RemoteHttpChatService]
      val user1 = passportPrivateUserId.next
      val user2 = passportPrivateUserId.next
      val payloads = createRoomParameters.next(10).toList.map(_.withoutAllUsers.withUserId(user1).withUserId(user2))
      val rooms = withUserContext(user2) { rc =>
        payloads.map(service.createRoom(_)(rc).futureValue)
      }
      rooms
        .take(5)
        .foreach(room => {
          val message = sendMessageParameters(room.id, user2).next
          withUserContext(user2) { implicit rc =>
            service.sendMessage(message)(rc).futureValue
          }
        })
      withUserContext(user1) { rc =>
        service.getRooms(user1, onlyNonEmpty = true)(rc).futureValue.foreach(println)
        service.getRooms(user1, onlyNonEmpty = true)(rc).futureValue.length shouldBe (if (withTechSupport) 6 else 5)
        println("---------------------")
        service.getRooms(user1, onlyNonEmpty = false)(rc).futureValue.foreach(println)
        service.getRooms(user1, onlyNonEmpty = false)(rc).futureValue.length shouldBe (if (withTechSupport) 11 else 10)
      }
    }

    "save room read timestamp" in {
      val user1 = passportPrivateUserId.next
      val user2 = passportPrivateUserId.next
      val createRoomRequest = createRoomParameters.next.withoutAllUsers.withUserId(user1).withUserId(user2)
      val room = withUserContext(user1) { rc =>
        service.createRoom(createRoomRequest)(rc).futureValue
      }
      val messageRequests = sendMessageParameters(room.id, user2).next(3)
      messageRequests.foreach { message =>
        withUserContext(user2) { implicit rc =>
          service.sendMessage(message)(rc).futureValue
        }
      }
      val room0 = withUserContext(user2) { implicit rc =>
        service.getRoom(room.id).futureValue
      }
      room0.participants.find(user1).value.roomLastReadMoment shouldBe empty
      withUserContext(user1) { implicit rc =>
        service.markRead(room.id, user1)(rc).futureValue
      }
      val room1 = withUserContext(user2) { implicit rc =>
        service.getRoom(room.id).futureValue
      }
      room1.participants.find(user1).value.roomLastReadMoment shouldBe defined
      Thread.sleep(100)
      withUserContext(user2) { implicit rc =>
        service.sendMessage(sendMessageParameters(room.id, user2).next)(rc).futureValue
      }
      val room2 = withUserContext(user2) { implicit rc =>
        service.getRoom(room.id).futureValue
      }
      room2.participants.find(user1).value.roomLastReadMoment shouldBe
        room1.participants.find(user1).value.roomLastReadMoment

      room2.participants.find(user2).value.roomLastReadMoment shouldBe empty
    }

    "room read timestamp for single user chats" in {
      val user1 = passportPrivateUserId.next
      val createRoomRequest = createRoomParameters.next.withoutAllUsers.withUserId(user1)
      val room = withUserContext(user1) { rc =>
        service.createRoom(createRoomRequest)(rc).futureValue
      }
      val room0 = withUserContext(user1) { implicit rc =>
        service.getRoom(room.id).futureValue
      }
      room0.participants.find(user1).value.roomLastReadMoment shouldBe empty
      withUserContext(user1) { implicit rc =>
        service.markRead(room.id, user1)(rc).futureValue
      }
      val room1 = withUserContext(user1) { implicit rc =>
        service.getRoom(room.id).futureValue
      }
      room1.participants.find(user1).value.roomLastReadMoment shouldBe defined
    }

    "get sorted rooms" in {
      val withTechSupport = service.isInstanceOf[TechSupportChatService] ||
        service.isInstanceOf[RemoteHttpChatService]
      val users: Seq[UserId] = passportPrivateUserId.next(5).toList
      val firstUser = users.head
      val payloads =
        (1 to 10).map(i => createRoomParameters(Some(i + "num" + roomId.next)).next.withoutAllUsers.withUserIds(users))
      val roomIds = payloads.map(_.id.get).toList
      withUserContext(firstUser) { implicit rc =>
        payloads.foreach(p => {
          Thread.sleep(50)
          service.createRoom(p).futureValue
        })
      }
      roomIds.reverse.zip(users.flatMap(u => Seq(u, u))).foreach {
        case (roomId, user) =>
          withUserContext(user) { implicit rc =>
            Thread.sleep(50)
            val message = sendMessageParameters(roomId, user).next
            service.sendMessage(message).futureValue
          }
      }
      checkSortedRooms(roomIds.reverse, firstUser, Some(RoomsSortBy.UpdatedAsc), withTechSupport)
      checkSortedRooms(roomIds, firstUser, Some(RoomsSortBy.UpdatedDesc), withTechSupport)
      checkSortedRooms(roomIds, firstUser, None, withTechSupport)
      checkSortedRooms(roomIds, firstUser, Some(RoomsSortBy.CreatedAsc), withTechSupport)
      checkSortedRooms(roomIds.reverse, firstUser, Some(RoomsSortBy.CreatedDesc), withTechSupport)
    }

    "remove chat room" in {
      val room = createAndCheckRoom()
      service.removeRoom(room.id).futureValue should be(())
      eventually {
        intercept[NoSuchElementException] {
          withUserContext(room) { rc =>
            cause(service.getRoom(room.id)(rc).futureValue)
          }
        }
      }
    }

    "mark room as unread after sending not spam message" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      service.sendMessage(parameters).futureValue
      val expected = room.users.map(u => (u, u != parameters.author)).toMap
      checkUnread(room.id, expected)
    }

    "not mark room as unread after sending spam message" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next.copy(isSpam = true)
      service.sendMessage(parameters).futureValue
      val expected = room.users.map(u => (u, false)).toMap
      checkUnread(room.id, expected)
    }

    "ignore spam message when reading by recipients" in {
      val room = createAndCheckRoom()
      val spam =
        service.sendMessage(sendMessageParameters(room).next.copy(isSpam = true)).futureValue
      val recipients = room.users - spam.author
      val window = Window[MessageId](None, 10, asc = true)
      recipients.foreach(recipient => {
        withUserContext(recipient) { rc =>
          val ids = service.getMessages(room.id, window)(rc).futureValue.map(_.id)
          ids shouldNot contain(spam.id)
        }
      })
      withUserContext(spam.author) { rc =>
        val ids = service.getMessages(room.id, window)(rc).futureValue.map(_.id)
        ids should contain(spam.id)
      }
    }

    "send message using roomLocator.direct" in {
      val room = createAndCheckRoom()
      val roomLocator = RoomLocator.Direct(room.id)
      val author = anyParticipant(room).next
      val parameters = sendMessageParameters(roomLocator, author).next
      service.sendMessage(parameters).futureValue
      val expected = room.users.map(u => (u, u != parameters.author)).toMap
      checkUnread(room.id, expected)
    }

    "send message using roomLocator.source with new room creation" in {
      val id = roomId.next

      intercept[NoSuchElementException] {
        cause(service.getRoom(id).futureValue)
      }

      val newRoomParameters = createRoomParameters(Some(id)).next
      val roomLocator = RoomLocator.Source(newRoomParameters)
      val author = anyParticipant(newRoomParameters.participants).next
      val parameters = sendMessageParameters(roomLocator, author).next

      val sendResult = service.sendMessage(parameters).futureValue
      sendResult.message.roomId shouldBe id

      val room = service.getRoom(id).futureValue

      val expected = room.users.map(u => (u, u != parameters.author)).toMap
      checkUnread(room.id, expected)
    }

    "send message using roomLocator.source without new room creation" in {
      val id = roomId.next
      val newRoomParameters = createRoomParameters(Some(id)).next
      val room = createAndCheckRoom(_ => newRoomParameters)
      val roomLocator = RoomLocator.Source(newRoomParameters)
      val author = anyParticipant(room).next
      val parameters = sendMessageParameters(roomLocator, author).next
      val sendResult = service.sendMessage(parameters).futureValue
      sendResult.message.roomId shouldBe id
      val expected = room.users.map(u => (u, u != parameters.author)).toMap
      checkUnread(room.id, expected)
    }

    "check if room exists using roomLocator.direct" in {
      val id = roomId.next
      val roomLocator = RoomLocator.Direct(id)
      service.checkRoomExists(roomLocator).futureValue shouldBe false
      val newRoomParameters = createRoomParameters(Some(id)).next

      createAndCheckRoom(_ => newRoomParameters)
      service.checkRoomExists(roomLocator).futureValue shouldBe true
    }

    "check if room exists using roomLocator.source" in {
      val id = roomId.next
      val newRoomParameters = createRoomParameters(Some(id)).next
      val roomLocator = RoomLocator.Source(newRoomParameters)
      service.checkRoomExists(roomLocator).futureValue shouldBe false

      createAndCheckRoom(_ => newRoomParameters)
      service.checkRoomExists(roomLocator).futureValue shouldBe true
      service.checkRoomExists(RoomLocator.Source(newRoomParameters.copy(id = None))).futureValue shouldBe false

    }

    "mute and unmute room for user" in {
      val create = createRoomParameters.next
      val room = service.createRoom(create).futureValue
      val roomId = room.id
      val user = anyParticipant(room).next
      service.mute(roomId, user, mute = true).futureValue
      service.getRoom(roomId).futureValue.participants.find(user).value.muted shouldBe true

      service.mute(roomId, user, mute = false).futureValue
      service.getRoom(roomId).futureValue.participants.find(user).value.muted shouldBe false
    }

    "mark as spam and not spam message" in {
      val create = createRoomParameters.next
      val room = service.createRoom(create).futureValue
      val roomId = room.id
      val author = anyParticipant(room).next
      val recipient = (room.users - author).head
      val parameters = sendMessageParameters(roomId, author).next
      val messageId = service.sendMessage(parameters).futureValue.message.id
      val window = Window(None, 10, asc = true)
      withUserContext(recipient) { rc =>
        val message = service.getMessages(roomId, window)(rc).futureValue.head
        message.isSpam shouldBe false
      }
      service.markSpam(messageId, value = true).futureValue
      withUserContext(recipient) { rc =>
        service.getMessages(roomId, window)(rc).futureValue shouldBe empty
      }
      withUserContext(author) { rc =>
        val message = service.getMessages(roomId, window)(rc).futureValue.head
        withClue(message) {
          message.isSpam shouldBe true
        }
      }
      withUserContext(recipient) { rc =>
        val message = service.getMessages(roomId, window, includeSpam = true)(rc).futureValue.head
        message.isSpam shouldBe true
      }
      service.markSpam(messageId, value = false).futureValue
      withUserContext(recipient) { rc =>
        val message = service.getMessages(roomId, window)(rc).futureValue.head
        message.isSpam shouldBe false
      }
    }

    "mark spam unknown message" in {
      val mId = messageId.next
      intercept[NoSuchElementException] {
        cause(service.markSpam(mId, value = true).futureValue)
      }
    }

    "block and unblock room by user" in {
      val create = createRoomParameters.next
      val room = service.createRoom(create).futureValue
      val roomId = room.id
      val user = anyParticipant(room).next

      service.blockRoom(roomId, user, value = true).futureValue

      val roomResult1 = service.getRoom(roomId).futureValue
      roomResult1.participants.find(user).value.blocked shouldBe true
      roomResult1.blocked shouldBe true

      service.blockRoom(roomId, user, value = false).futureValue

      val roomResult2 = service.getRoom(roomId).futureValue
      roomResult2.participants.find(user).value.blocked shouldBe false
      roomResult2.blocked shouldBe false
    }
  }

  private def checkSortedRooms(expectedRoomIds: Seq[RoomId],
                               user: UserId,
                               optSortBy: Option[RoomsSortBy],
                               withTechSupport: Boolean): Unit = {
    val rooms = service.getRooms(user, optSortBy = optSortBy).futureValue
    val techSupportIds = if (withTechSupport) Seq(TechSupportUtils.roomId(user)) else Seq.empty
    val expectedRoomIdsWithTechSupport = techSupportIds ++ expectedRoomIds
    expectedRoomIdsWithTechSupport shouldBe rooms.map(_.id)
  }

  private def checkUnread(roomId: RoomId, expected: Map[UserId, Boolean]): Unit = {
    expected.foreach {
      case (user, hasUnread) =>
        withUserContext(user) { rc =>
          service.getRoom(roomId)(rc).futureValue.hasUnread should be(hasUnread)
          service.unreadRoomsCount(user)(rc).futureValue > 0 should be(hasUnread)
          service.getRooms(user)(rc).futureValue.find(_.id == roomId).get.hasUnread should be(hasUnread)
          service.hasUnread(user).futureValue should be(hasUnread)
        }
    }
  }

  private def checkLastMessage(user: UserId, room: RoomId, parameters: Option[CreateMessageParameters]): Unit = {
    withUserContext(user) { rc =>
      val updated = service.getRoom(room)(rc).futureValue
      updated.lastMessage.map(coarse) shouldBe parameters.map(_.prepared)
    }
  }

  protected def coarse(room: Room): Room = room.copy(created = DateTime.parse("0"), updated = DateTime.parse("0"))

  private def coarse(messages: Seq[Message]): Seq[CreateMessageParameters] =
    messages.map(coarse)

  private def coarse(message: Message): CreateMessageParameters =
    CreateMessageParameters(
      message.roomId,
      message.author,
      message.payload,
      message.attachments,
      message.providedId,
      properties = message.properties
    )
}

object ChatServiceSpecBase {

  implicit private class RichCreateMessageParameters(val value: CreateMessageParameters) extends AnyVal {

    def prepared: CreateMessageParameters =
      value.copy(
        isSpam = false,
        isSilent = false,
        roomLocator = None,
        visibleFor = Seq.empty
      )
  }
}
