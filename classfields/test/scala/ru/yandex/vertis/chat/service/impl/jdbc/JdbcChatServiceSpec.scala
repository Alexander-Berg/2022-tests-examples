package ru.yandex.vertis.chat.service.impl.jdbc

import org.joda.time.DateTime
import org.scalatest.OptionValues
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.{RequestContext, SlagGenerators}
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.model.ModelGenerators.{anyParticipant, roomId, userId}
import ru.yandex.vertis.chat.model.{MessagePayload, ModelGenerators, Participants, RoomId, User, UserId, Window}
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.TestDomainAware
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceSpecBase, CreateRoomParameters}
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}
import ru.yandex.vertis.generators.BasicGenerators.set
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Runnable specs on [[JdbcChatService]].
  *
  * @author 747mmhg
  */
class JdbcChatServiceSpec
  extends ChatServiceSpecBase
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

  val mockedTimeService: TimeService = mock[TimeService]

  val serviceForRollbackTest: ChatService = new JdbcChatService(
    database,
    roomUuidGenerator = RandomIdGenerator,
    messageUuidGenerator = new TimeIdGenerator("localhost"),
    timeService = mockedTimeService
  ) with SameThreadExecutionContextSupport

  "JdbcChatService" should {
    "reject chat with too long properties" in {
      val tooLong = Iterator.continually("a").take(150).mkString
      val pair = tooLong -> tooLong
      val tooLongProperties = List.fill(110)(pair).zipWithIndex.map { case ((k, v), i) => s"$k$i" -> v }.toMap
      val parameters = createRoomParameters.next
        .withProperties(tooLongProperties)
      a[IllegalArgumentException] should be thrownBy {
        cause(service.createRoom(parameters).futureValue)
      }
    }

    "reject empty message" in {
      val room = createAndCheckRoom()
      val message1 = sendMessageParameters(room).next
        .copy(payload = MessagePayload(MimeType.TEXT_PLAIN, "   "), attachments = Seq.empty)
      a[IllegalArgumentException] should be thrownBy {
        cause(service.sendMessage(message1).futureValue)
      }
    }

    "correctly support rooms limit on attempt to get rooms by user" in {
      pending // too slow
      val user = userId.next
      implicit val ctx: RequestContext = requestContext.withUser(user)
      val participantsMap: Map[RoomId, Set[UserId]] = (1 to 600)
        .map(_ => {
          val id = roomId.next
          val props = ModelGenerators.properties.next
          val participants = set(2, 9, userId).next + user
          val create = CreateRoomParameters(Some(id), Participants(participants.map(User(_))), props)
          service.createRoom(create)(ctx).futureValue
          (id, participants)
        })
        .toMap
      val rooms = service.getRooms(user)(ctx).futureValue
      assert(rooms.length == 500)
      rooms.foreach(room => {
        room.participants.userIds == participantsMap(room.id)
      })
    }

    "correctly support rooms limit on attempt to get rooms by rooms ids" in {
      val user = userId.next
      implicit val ctx: RequestContext = requestContext.withUser(user)
      val participantsMap: Map[RoomId, Set[UserId]] = (1 to 150)
        .map(_ => {
          val id = roomId.next
          val props = ModelGenerators.properties.next
          val participants = set(2, 9, userId).next + user
          val create = CreateRoomParameters(Some(id), Participants(participants.map(User(_))), props)
          service.createRoom(create)(ctx).futureValue
          (id, participants)
        })
        .toMap
      val rooms = service.getRoomsByIds(participantsMap.keys)(ctx).futureValue
      assert(rooms.length == 100)
      rooms.foreach(room => {
        room.participants.userIds == participantsMap(room.id)
      })
    }

    "should return empty list on attempt to get rooms with empty list of ids" in {
      val user = userId.next
      implicit val ctx: RequestContext = requestContext.withUser(user)
      val rooms = service.getRoomsByIds(Seq())(ctx).futureValue
      assert(rooms.isEmpty)
    }

    "rollback if previous message is newer" in {
      val room = createAndCheckRoom()
      val roomId = room.id

      val message1 = sendMessageParameters(room).next
      when(mockedTimeService.getNow).thenReturn(DateTime.now().withMillisOfDay(0).plusHours(1))
      serviceForRollbackTest.sendMessage(message1).futureValue

      val message2 = sendMessageParameters(room).next
      when(mockedTimeService.getNow).thenReturn(DateTime.now().withMillisOfDay(0))
      a[RuntimeException] should be thrownBy {
        cause(serviceForRollbackTest.sendMessage(message2).futureValue)
      }

      val user = anyParticipant(room).next
      val messages = withUserContext(user) { rc =>
        val window = Window(None, 10, asc = true)
        serviceForRollbackTest.getMessages(roomId, window)(rc).futureValue.toIndexedSeq
      }

      messages.size should be(1)
    }

    "skip event generation if idempotency key already exists" in {

      val room = createAndCheckRoom()
      val robotId = ModelGenerators.robotName.next
      val idempotencyKey = Some(ModelGenerators.idempotencyKey.next)

      val ctx: RequestContext = SlagGenerators.robotRequestContext.next
        .withIdempotencyKey(idempotencyKey)
        .withName(robotId)

      val message1 = sendMessageParameters(room).next
      val message2 = sendMessageParameters(room).next.withAuthor(message1.author)
      val message3 = sendMessageParameters(room).next.withAuthor(message1.author)

      when(mockedTimeService.getNow).thenReturn(DateTime.now().withMillisOfDay(0).plusHours(1))
      val messageSaveResult1 = serviceForRollbackTest.sendMessage(message1).futureValue

      messageSaveResult1.isDuplicate shouldBe false

      when(mockedTimeService.getNow).thenReturn(DateTime.now().withMillisOfDay(0).plusHours(2))
      val messageSaveResult2 = serviceForRollbackTest.sendMessage(message2)(ctx).futureValue

      messageSaveResult2.isDuplicate shouldBe false
      messageSaveResult2.previousMessage shouldBe Some(messageSaveResult1.message)

      when(mockedTimeService.getNow).thenReturn(DateTime.now().withMillisOfDay(0).plusHours(3))
      val messageSaveResult3 = serviceForRollbackTest.sendMessage(message3)(ctx).futureValue

      messageSaveResult3.isDuplicate shouldBe true
      messageSaveResult3.previousMessage shouldBe Some(messageSaveResult1.message)
      messageSaveResult3.message shouldBe messageSaveResult2.message

      val savedMessagesInRoom = serviceForRollbackTest
        .getMessages(room.id, Window(None, 10, asc = true))
        .futureValue
        .toSet

      savedMessagesInRoom shouldBe Set(messageSaveResult1.message, messageSaveResult2.message)
    }

    "get messages filtered with date from, date to" in {

      val room = createAndCheckRoom()
      val robotId = ModelGenerators.robotName.next
      val idempotencyKey = Some(ModelGenerators.idempotencyKey.next)

      val ctx: RequestContext = SlagGenerators.robotRequestContext.next
        .withIdempotencyKey(idempotencyKey)
        .withName(robotId)

      val message1 = sendMessageParameters(room).next
      val message2 = sendMessageParameters(room).next.withAuthor(message1.author)
      val message3 = sendMessageParameters(room).next.withAuthor(message1.author)
      val message4 = sendMessageParameters(room).next.withAuthor(message1.author)

      when(mockedTimeService.getNow).thenAnswer(_ => DateTime.now())
      val messageSaveResult1 = serviceForRollbackTest.sendMessage(message1).futureValue
      val dateFrom = DateTime.now()
      val messageSaveResult2 = serviceForRollbackTest.sendMessage(message2).futureValue
      val messageSaveResult3 = serviceForRollbackTest.sendMessage(message3).futureValue
      val dateTo = DateTime.now()
      val messageSaveResult4 = serviceForRollbackTest.sendMessage(message4).futureValue

      val savedMessagesInRoom = serviceForRollbackTest
        .getMessages(room.id, Window(None, 10, asc = true), fromDate = Some(dateFrom), toDate = Some(dateTo))
        .futureValue
        .toSet

      savedMessagesInRoom shouldBe Set(messageSaveResult2.message, messageSaveResult3.message)
    }
  }
}
