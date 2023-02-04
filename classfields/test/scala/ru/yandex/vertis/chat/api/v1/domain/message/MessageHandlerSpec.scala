package ru.yandex.vertis.chat.api.v1.domain.message

import akka.http.scaladsl.model.{StatusCodes, Uri}
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.api.v1.domain.room.RoomHandler
import ru.yandex.vertis.chat.api.v1.proto.ApiProtoFormats._
import ru.yandex.vertis.chat.components.dao.authority.JvmAuthorityService
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.TestData._
import ru.yandex.vertis.chat.model.{Message, Room, Window}
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.security.TestSecuredChatService
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.util.DateUtils
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
import org.joda.time.{DateTime, DateTimeUtils}

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

/**
  * Specs on [[MessageHandler]].
  *
  * @author dimas
  */
class MessageHandlerSpec extends HandlerSpecBase with RequestContextAware with ScalaFutures {

  private val timeService: TimeService = new DefaultTimeServiceImpl
  private val state = JvmChatState.empty()
  private val authority = new JvmAuthorityService(state, timeService)

  private val service =
    TestSecuredChatService.wrap(
      new JvmChatService(state),
      authority,
      timeService
    )(Implicits.global)
  private val route = seal(new MessageHandler(service).route)
  private val roomRoute = seal(new RoomHandler(service).route)

  s"POST $root" should {
    "allow to send message" in {
      val parameters = sendMessageParameters.next
      val room = withUserContext(parameters.author) { rc =>
        val p = createRoomParameters.next
        service.createRoom(p.withId(parameters.room))(rc).futureValue
      }
      val author = anyParticipant(room).next
      Post(root, parameters.copy(author = author))
        .withUser(author)
        .withSomePassportUser ~> route ~> check {
        status should be(StatusCodes.OK)
        val message = responseAs[Message]
        message.payload should be(parameters.payload)
      }
    }

    "fail when attempting to send message from not a member" in {
      val parameters = sendMessageParameters.next
      withUserContext(parameters.author) { rc =>
        val p = createRoomParameters.next.withId(parameters.room)
        service.createRoom(p)(rc).futureValue
      }
      Post(root, parameters).withSomeUser.withSomePassportUser ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
  }

  s"GET $root" should {
    "report 404 if no chat specified" in {
      Get(root).withSomePassportUser ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "provide messages with default window" in {
      val room = createRoom()
      val parameters = sendMessageParameters(room).next(10)
      parameters.foreach(service.sendMessage)
      val user = parameters.head.author

      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?room_id=${room.id}")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val messages = responseAs[Seq[Message]]
          messages.size should be(parameters.size)
        }
      }
    }

    "provide messages with specified window" in {
      val room = createRoom()
      val parameters = sendMessageParameters(room).next(10)
      parameters.foreach(service.sendMessage)
      val user = parameters.head.author

      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?room_id=${room.id}&count=1&from=")
          .accepting(mediaType)
          .withUser(user)
          .withSomePassportUser ~> route ~> check {
          status should be(StatusCodes.OK)
          val messages = responseAs[Seq[Message]]
          messages.size should be(1)
        }
      }
    }

    "provide spam messages" in {
      val room = createRoom()
      val spamer = anyParticipant(room).next
      val parameters = sendMessageParameters(room)
        .next(10)
        .map(_.copy(isSpam = true, author = spamer))
      parameters.foreach(service.sendMessage)
      val user = anyParticipant(room.participants.withoutUserId(spamer)).next
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?room_id=${room.id}&count=10&include_spam=true")
          .accepting(mediaType)
          .withUser(user)
          .withSomePassportUser ~> route ~> check {
          status should be(StatusCodes.OK)
          val messages = responseAs[Seq[Message]]
          messages.size should be(10)
          messages.foreach(m => m.isSpam shouldBe true)
        }
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?room_id=${room.id}&count=10")
          .accepting(mediaType)
          .withUser(user)
          .withSomePassportUser ~> route ~> check {
          status should be(StatusCodes.OK)
          val messages = responseAs[Seq[Message]]
          messages.size should be(0)
        }
      }
    }

    "provide messages with specified dateFrom and dateTo" in {
      val fixedCurrent = DateTime.now()
      DateTimeUtils.setCurrentMillisFixed(fixedCurrent.getMillis)

      val room = createRoom()
      val parameters = sendMessageParameters(room).next(5)
      Future.sequence(parameters.map(service.sendMessage)).futureValue
      val user = parameters.head.author

      val dateFrom = DateUtils.date2str(fixedCurrent.plusDays(1))

      DateTimeUtils.setCurrentMillisFixed(fixedCurrent.plusDays(2).getMillis)
      val parameters2 = sendMessageParameters(room).next(10)
      Future.sequence(parameters2.map(service.sendMessage)).futureValue
      DateTimeUtils.setCurrentMillisFixed(fixedCurrent.plusDays(3).getMillis)
      val dateTo = DateUtils.date2str(DateTime.now)
      DateTimeUtils.setCurrentMillisFixed(fixedCurrent.plusDays(4).getMillis)
      val parameters3 = sendMessageParameters(room).next(5)
      Future.sequence(parameters3.map(service.sendMessage)).futureValue

      supportedMediaTypes.foreach { mediaType =>
        val params = Map(
          "room_id" -> room.id,
          "from_date" -> dateFrom,
          "to_date" -> dateTo
        )

        Get(Uri(root).withQuery(Uri.Query(params)))
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val messages = responseAs[Seq[Message]]
          messages.size should be(parameters2.size)
        }
      }
      DateTimeUtils.setCurrentMillisSystem()
    }
  }

  s"GET ${root}unread" should {
    "provide unread messages count" in {
      val room = createRoom()
      val parameters = sendMessageParameters(room).next(10)
      parameters.foreach(service.sendMessage)

      supportedMediaTypes.foreach { mediaType =>
        Get(s"${root}unread?user_id=$Alice")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(Alice.id) ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(false)
        }
      }
    }
  }

  s"PUT ${root}spam" should {
    "mark and unmark message as spam" in {
      val room = createRoom()
      val roomId = room.id
      val author = anyParticipant(room).next
      val recipient = (room.users - author).head
      val parameters = sendMessageParameters(roomId, author).next
      val messageId = service.sendMessage(parameters).futureValue.message.id
      val window = Window(None, 10, asc = true)
      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}spam?message_id=$messageId&value=true")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(Alice.id) ~> route ~> check {
          status should be(StatusCodes.OK)
          withUserContext(recipient) { rc =>
            service.getMessages(roomId, window)(rc).futureValue shouldBe empty
          }
          withUserContext(author) { rc =>
            val message =
              service.getMessages(roomId, window)(rc).futureValue.head
            message.isSpam shouldBe true
          }
        }
      }
      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}spam?message_id=$messageId&value=false")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(Alice.id) ~> route ~> check {
          status should be(StatusCodes.OK)
          withUserContext(recipient) { rc =>
            val message =
              service.getMessages(roomId, window)(rc).futureValue.head
            message.isSpam shouldBe false
          }
        }
      }
    }
  }

  s"PUT ${root}user-spam" should {
    "mark message as spam with author and user" in {
      val room = createRoom()
      val roomId = room.id
      val author = anyParticipant(room).next
      val recipient = (room.users - author).head
      val parameters = sendMessageParameters(roomId, author).next
      val messageId = service.sendMessage(parameters).futureValue.message.id
      val newParameters = sendMessageParameters(roomId, author).next
      val newMessageId = service.sendMessage(newParameters).futureValue.message.id
      val window = Window(None, 10, asc = true)
      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}user-spam?message_id=$messageId&user_id=$recipient&comment=${"test"}")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(Alice.id) ~> route ~> check {
          status should be(StatusCodes.OK)
          withUserContext(author) { rc =>
            val message =
              service.getMessages(roomId, window)(rc).futureValue.head
            message.isSpam shouldBe true
          }
        }
      }

      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}user-spam?message_id=$newMessageId&user_id=$recipient&comment=${"test2"}")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(Alice.id) ~> route ~> check {
          status should be(StatusCodes.OK)
          withUserContext(recipient) { rc =>
            val message =
              service.getMessages(roomId, window, includeSpam = true)(rc).futureValue.find(_.id == newMessageId).get
            message.isSpam shouldBe true
            val prevMessage =
              service.getMessages(roomId, window, includeSpam = true)(rc).futureValue.find(_.id == messageId).get
            prevMessage.isSpam shouldBe false
          }
        }
      }
    }
    "same user exception" in {
      val room = createRoom()
      val roomId = room.id
      val author = anyParticipant(room).next
      val parameters = sendMessageParameters(roomId, author).next
      val messageId = service.sendMessage(parameters).futureValue.message.id
      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}user-spam?message_id=$messageId&user_id=$author&comment=${"test"}&author_id=$author")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(Alice.id) ~> route ~> check {
          status should be(StatusCodes.BadRequest)
        }
      }
    }
  }

  private def createRoom(): Room = {
    val parameters = createRoomParameters.next
    val user = parameters.participants.userIds.headOption.getOrElse(userId.next)
    Post(root, parameters).withSomePassportUser
      .withUser(user) ~> roomRoute ~> check {
      status should be(StatusCodes.OK)
      responseAs[Room]
    }
  }
}
