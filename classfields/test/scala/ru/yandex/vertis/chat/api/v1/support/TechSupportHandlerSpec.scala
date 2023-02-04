package ru.yandex.vertis.chat.api.v1.support

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, OptionValues}
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupport.{ChatUser, UserMessage}
import ru.yandex.vertis.chat.common.techsupport.{TechSupport, TechSupportUtils}
import ru.yandex.vertis.chat.components.clients.jivosite.TechSupportClient
import ru.yandex.vertis.chat.components.clients.passport.PassportClient
import ru.yandex.vertis.chat.components.dao.techsupport.polls.{PollData, TechSupportPollService}
import ru.yandex.vertis.chat.components.dao.techsupport.polls.messages.{PollMessages, PollPreset}
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.model.{ModelGenerators, RoomId}
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.service.support.polls.TestPollMessagesService
import ru.yandex.vertis.chat.service.{ChatService, SendMessageResult}
import ru.yandex.vertis.generators.BasicGenerators._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
class TechSupportHandlerSpec
  extends HandlerSpecBase
  with OptionValues
  with MockitoSupport
  with BeforeAndAfter
  with Eventually {

  private val service = mock[ChatService]
  private val techSupportPollService = mock[TechSupportPollService]
  private var pollMessages = PollMessages()
  private val pollMessagesService = new TestPollMessagesService(TechSupportHandlerSpec.this.pollMessages)
  private val techSupportClient = mock[TechSupportClient]
  private val passportClient = mock[PassportClient]

  private val handler =
    new TechSupportHandler(service, pollMessagesService, techSupportPollService, techSupportClient, passportClient)
      with SameThreadExecutionContextSupport
  private val route = seal(handler.route)

  before {
    reset(service)
    reset(techSupportPollService)
    reset(techSupportClient)
    reset(passportClient)
  }

  after {
    verifyNoMoreInteractions(service)
    verifyNoMoreInteractions(techSupportPollService)
    verifyNoMoreInteractions(techSupportClient)
    verifyNoMoreInteractions(passportClient)
  }

  "GET /api/1.x/{domain}/techSupport" should {
    "return tech support room" in {
      val passportUser = uid.next
      val user = "user:" + passportUser
      val roomId = TechSupportUtils.roomId(user)
      val r = room.next
      when(service.getRoom(?)(?)).thenReturn(Future.successful(r))
      Get("/")
        .withPassportUser(passportUser)
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      verify(service).getRoom(eq(roomId))(?)
    }
  }

  "GET /api/1.x/{domain}/techSupport/by-ids" should {
    "return tech support rooms" in {
      val passportUsers = uid.nextIterator(10).toSeq
      val users = passportUsers.map("user:" + _)
      val roomIds = users.map(TechSupportUtils.roomId)

      val r = room.nextIterator(10).toSeq
      when(service.getRoomsByIds(?)(?)).thenReturn(Future.successful(r))

      val userIdParams = users.map("user_id=" + _).mkString("&")
      Get(s"/by-id?$userIdParams")
        .withPassportUser(passportUsers.head)
        .withUser(users.head) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      verify(service).getRoomsByIds(argThat[Iterable[RoomId]](_.toSet == roomIds.toSet))(?)
    }
  }

  "PUT /api/1.x/{domain}/techsupport/poll" should {
    "return false and don't send any messages" in {
      val passportUserId = uid.next
      val user = "user:" + passportUserId
      val hash = readableString(32, 32).next
      val rating = 1
      val pollData = PollData(hash, user, Some(rating), "", DateTime.now(), None, None)

      when(techSupportPollService.setRating(?, ?)(?)).thenReturn(Future.successful((false, pollData)))
      Put(s"/poll/$hash?rating=$rating")
        .withPassportUser(passportUserId)
        .withUser(user) ~> route ~> check {
        responseAs[String] shouldBe "false"
        status should be(StatusCodes.OK)
      }
      verify(techSupportPollService).setRating(eq(hash), eq(rating))(any())
      verifyZeroInteractions(service)
    }

    "handle bad rating" in {
      val passportUserId = uid.next
      val user = "user:" + passportUserId
      val hash = readableString(32, 32).next
      val rating = 1
      val pollData = PollData(hash, user, Some(rating), "", DateTime.now(), None, None)

      when(techSupportPollService.setRating(?, ?)(?)).thenReturn(Future.successful((true, pollData)))
      when(techSupportPollService.setPresetMessageId(?, ?)(?)).thenReturn(Future.successful(true))
      when(techSupportPollService.getUser(?)(?)).thenReturn(Future.successful(Some(user)))
      val presetMessage = ModelGenerators.message.next
      when(service.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(presetMessage, None)))
      pollMessages = PollMessages(
        questionBad = "questionBad",
        badPresets = Seq(
          PollPreset(id = "id1", preset = "badPreset1", end = "badEnd1")
        )
      )
      Put(s"/poll/$hash?rating=$rating")
        .withPassportUser(passportUserId)
        .withUser(user) ~> route ~> check {
        responseAs[String] shouldBe "true"
        status should be(StatusCodes.OK)
      }
      verify(techSupportPollService).setRating(eq(hash), eq(rating))(any())
      verify(techSupportPollService).setPresetMessageId(eq(hash), eq(presetMessage.id))(any())
      verify(techSupportPollService).getUser(eq(hash))(any())
      verifyZeroInteractions(passportClient)
      verifyZeroInteractions(techSupportClient)
      verify(service).sendMessage(
        eq(TechSupportUtils.pollQuestion(user, hash, "questionBad", 7, Seq(PollPreset("id1", "badPreset1", "badEnd1"))))
      )(any())
    }

    "handle norm rating" in {
      val passportUserId = uid.next
      val user = "user:" + passportUserId
      val hash = readableString(32, 32).next
      val rating = 2
      val pollData = PollData(hash, user, Some(rating), "", DateTime.now(), None, None)

      when(techSupportPollService.setRating(?, ?)(?)).thenReturn(Future.successful((true, pollData)))
      when(techSupportPollService.setPresetMessageId(?, ?)(?)).thenReturn(Future.successful(true))
      when(techSupportPollService.getUser(?)(?)).thenReturn(Future.successful(Some(user)))
      val presetMessage = ModelGenerators.message.next
      when(service.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(presetMessage, None)))
      pollMessages = PollMessages(
        questionNorm = "questionNorm",
        normPresets = Seq(
          PollPreset(id = "id1", preset = "normPreset1", end = "normEnd1")
        )
      )
      Put(s"/poll/$hash?rating=$rating")
        .withPassportUser(passportUserId)
        .withUser(user) ~> route ~> check {
        responseAs[String] shouldBe "true"
        status should be(StatusCodes.OK)
      }
      verify(techSupportPollService).setRating(eq(hash), eq(rating))(any())
      verify(techSupportPollService).setPresetMessageId(eq(hash), eq(presetMessage.id))(any())
      verify(techSupportPollService).getUser(eq(hash))(any())
      verifyZeroInteractions(passportClient)
      verifyZeroInteractions(techSupportClient)
      verify(service).sendMessage(
        eq(
          TechSupportUtils
            .pollQuestion(user, hash, "questionNorm", 7, Seq(PollPreset("id1", "normPreset1", "normEnd1")))
        )
      )(any())
    }

    "handle good rating" in {
      val passportUserId = uid.next
      val user = "user:" + passportUserId
      val hash = readableString(32, 32).next
      val rating = 3
      val pollData = PollData(hash, user, Some(rating), "", DateTime.now(), None, None)

      when(techSupportPollService.setRating(?, ?)(?)).thenReturn(Future.successful((true, pollData)))
      when(techSupportPollService.setPresetMessageId(?, ?)(?)).thenReturn(Future.successful(true))
      when(techSupportPollService.getUser(?)(?)).thenReturn(Future.successful(Some(user)))
      val presetMessage = ModelGenerators.message.next
      when(service.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(presetMessage, None)))
      pollMessages = PollMessages(
        questionGood = "questionGood",
        goodPresets = Seq(
          PollPreset(id = "id1", preset = "goodPreset1", end = "goodEnd1")
        )
      )
      Put(s"/poll/$hash?rating=$rating")
        .withPassportUser(passportUserId)
        .withUser(user) ~> route ~> check {
        responseAs[String] shouldBe "true"
        status should be(StatusCodes.OK)
      }
      verify(techSupportPollService).setRating(eq(hash), eq(rating))(any())
      verify(techSupportPollService).setPresetMessageId(eq(hash), eq(presetMessage.id))(any())
      verify(techSupportPollService).getUser(eq(hash))(any())
      verifyZeroInteractions(passportClient)
      verifyZeroInteractions(techSupportClient)
      verify(service).sendMessage(
        eq(
          TechSupportUtils
            .pollQuestion(user, hash, "questionGood", 7, Seq(PollPreset("id1", "goodPreset1", "goodEnd1")))
        )
      )(any())
    }

    "send notification about user is typing a message" in {
      val passportUserId = uid.next
      val user = "user:" + passportUserId
      when(techSupportClient.send(?)(?)).thenReturn(Future.unit)
      Put(s"/userTyping?user_id=$user")
        .withPassportUser(passportUserId)
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      verify(techSupportClient).send(
        eq(
          TechSupport.Request(
            sender = Some(ChatUser(Some("user:" + passportUserId), None, None, None, None, None, None, None)),
            message = Some(UserMessage("typein", None, None, None)),
            recipient = None
          )
        )
      )(any())
    }
  }
}
