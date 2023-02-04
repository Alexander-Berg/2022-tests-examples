package ru.yandex.vertis.chat.api.v1.support.jivosite

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, OptionValues}
import play.api.libs.json.Json
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.RequestContext
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.clients.events.EventService
import ru.yandex.vertis.chat.components.clients.mds.MdsUploader
import ru.yandex.vertis.chat.components.dao.techsupport.polls.TechSupportPollService
import ru.yandex.vertis.chat.components.dao.techsupport.polls.messages.PollMessages
import ru.yandex.vertis.chat.components.dao.techsupport.subjects.TechSupportSubjectService
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.workersfactory.workers.TestWorkersFactory
import ru.yandex.vertis.chat.model.api.ApiModel.MessagePropertyType
import ru.yandex.vertis.chat.model.events.EventsModel
import ru.yandex.vertis.chat.model.{MessagePayload, ModelGenerators, Participants, User}
import ru.yandex.vertis.chat.service._
import ru.yandex.vertis.chat.service.features.ChatFeatures
import ru.yandex.vertis.chat.service.support.polls.TestPollMessagesService
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.versionchecker.{Platform, Version}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * TODO
  *
  * @author aborunov
  */
class JivositeHandlerSpec
  extends HandlerSpecBase
  with OptionValues
  with MockitoSupport
  with BeforeAndAfter
  with Eventually {
  private val chatService = mock[ChatService]
  private val mdsUploader = mock[MdsUploader]
  private val techSupportSubjectService = mock[TechSupportSubjectService]
  private val techSupportPollService = mock[TechSupportPollService]
  private val eventService = mock[EventService]
  private val mockedFeatures = mock[ChatFeatures]
  private val workersFactory = new TestWorkersFactory

  private val pollMessages = PollMessages(
    pollStartMessage = "Надеемся, мы вам помогли!\\nБудет здорово, если вы поставите нам оценку",
    ttlDays = 7
  )
  private val pollMessagesService = new TestPollMessagesService(JivositeHandlerSpec.this.pollMessages)

  private val handler = new JivositeHandler(
    DMap.forAllDomains(chatService),
    DMap.forAllDomains(techSupportSubjectService),
    DMap.forAllDomains(techSupportPollService),
    DMap.forAllDomains(mdsUploader),
    DMap.forAllDomains(JivositeHandlerSpec.this.eventService),
    DMap.forAllDomains(mockedFeatures),
    DMap.forAllDomains(pollMessagesService),
    workersFactory
  ) with SameThreadExecutionContextSupport

  private val route = seal(handler.route)

  "POST /api/1.x/jivosite/auto" should {
    "store operator email in message properties" in {
      reset(chatService)
      val user = "user:10245460"
      val json = Json.obj(
        "recipient" -> Json.obj("id" -> user),
        "sender" -> Json.obj("name" -> "Андрей", "email" -> "aborunov@yandex-team.ru"),
        "message" -> Json.obj("type" -> "text", "text" -> "тыц тыц!", "id" -> "1179266")
      )
      when(chatService.markRead(?, ?)(?)).thenReturn(Future.unit)
      when(chatService.sendMessage(?)(?))
        .thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      Post("/auto", HttpEntity(ContentTypes.`application/json`, Json.stringify(json))) ~> route ~> check {
        withClue(responseAs[String]) {
          status should be(StatusCodes.OK)
        }
      }
      val m = CreateMessageParameters.create(
        TechSupportUtils.roomId(user),
        TechSupportUtils.TechSupportUser,
        MessagePayload(MimeType.TEXT_HTML, "тыц тыц!"),
        Nil,
        None,
        isSpam = false,
        isSilent = false,
        Some(
          RoomLocator.Source(
            CreateRoomParameters(
              Some(TechSupportUtils.roomId(user)),
              Participants(
                Set(
                  User(user, muted = false, blocked = false, activeRoom = true, Duration.Inf),
                  User(
                    TechSupportUtils.TechSupportUser,
                    muted = false,
                    blocked = false,
                    activeRoom = true,
                    Duration.Inf
                  )
                )
              ),
              Map()
            )
          )
        ),
        propertiesFunc = _.setTechSupportOperatorId("aborunov@yandex-team.ru")
      )
      verify(chatService).markRead(eq(TechSupportUtils.roomId(user)), eq(TechSupportUtils.TechSupportUser))(any())
      verify(chatService).sendMessage(eq(m))(any())
      verifyNoMoreInteractions(chatService)
    }

    "save subjects" in {
      reset(chatService)
      val user = "user:10245461"
      val room = TechSupportUtils.roomId(user)
      val operator = "aborunov@yandex-team.ru"
      val subject1 = "Деньги:Возврат"
      val json = Json.obj(
        "recipient" -> Json.obj("id" -> user),
        "sender" -> Json.obj("name" -> "Андрей", "email" -> operator),
        "message" -> Json.obj("type" -> "text", "text" -> ("тыц тыц! ###" + subject1), "id" -> "1179266")
      )
      when(chatService.markRead(?, ?)(?)).thenReturn(Future.unit)
      when(techSupportSubjectService.addSubjects(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      Post("/auto", HttpEntity(ContentTypes.`application/json`, Json.stringify(json))) ~> route ~> check {
        withClue(responseAs[String]) {
          status should be(StatusCodes.OK)
        }
      }
      verify(techSupportSubjectService).addSubjects(eq(room), eq(user), eq(operator), eq(Seq(subject1)))(?)
      verify(chatService).markRead(eq(room), eq(TechSupportUtils.TechSupportUser))(any())
      verifyNoMoreInteractions(chatService)
      verifyNoMoreInteractions(techSupportSubjectService)
    }

    "send poll on chat_close subject" in {
      reset(chatService)
      val user = "user:10245462"
      val room = TechSupportUtils.roomId(user)
      val operator = "aborunov@yandex-team.ru"
      val subject1 = "Разное:Закрытие_чата"
      val json = Json.obj(
        "recipient" -> Json.obj("id" -> user),
        "sender" -> Json.obj("name" -> "Андрей", "email" -> operator),
        "message" -> Json.obj("type" -> "text", "text" -> ("тыц тыц! ###" + subject1), "id" -> "1179266")
      )
      when(chatService.markRead(?, ?)(?)).thenReturn(Future.unit)
      val pollMessage = ModelGenerators.message.next
      var hash = ""
      stub(chatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
        case (params, _) =>
          params.room shouldBe room
          params.author shouldBe TechSupportUtils.TechSupportUser
          params.payload shouldBe MessagePayload.fromText(pollMessages.pollStartMessage)
          params.attachments shouldBe Seq.empty
          params.providedId shouldBe None
          params.roomLocator.value shouldBe TechSupportUtils.roomLocator(user)
          params.properties.getType shouldBe MessagePropertyType.TECH_SUPPORT_POLL
          params.properties.getTechSupportPoll.getTtl shouldBe 7.days.toSeconds
          hash = params.properties.getTechSupportPoll.getHash
          Future.successful(SendMessageResult(pollMessage, None))
      }
      when(techSupportSubjectService.addSubjects(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      val platform: (Platform, Version) = Platform.Android -> Version(6, 5, 3, 0)
      when(techSupportPollService.getLastUserAppVersion(?)(?)).thenReturn(Future.successful(platform))
      when(techSupportPollService.acceptedVersions).thenReturn(Map(platform))
      when(techSupportPollService.addPoll(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      Post("/auto", HttpEntity(ContentTypes.`application/json`, Json.stringify(json))) ~> route ~> check {
        withClue(responseAs[String]) {
          status should be(StatusCodes.OK)
        }
      }
      verify(techSupportSubjectService).addSubjects(eq(room), eq(user), eq(operator), eq(Seq(subject1)))(?)
      verify(techSupportPollService).getLastUserAppVersion(eq(user))(?)
      verify(techSupportPollService, atLeastOnce).acceptedVersions
      verify(chatService).sendMessage(
        eq(TechSupportUtils.pollStartMessage(user, hash, pollMessages.pollStartMessage, pollMessages.ttlDays))
      )(?)
      verify(techSupportPollService).addPoll(eq(pollMessage.id), ?, eq(user), eq(operator))(?)
      verify(chatService).markRead(eq(room), eq(TechSupportUtils.TechSupportUser))(any())
      verifyNoMoreInteractions(chatService)
      verifyNoMoreInteractions(techSupportPollService)
      verifyNoMoreInteractions(techSupportSubjectService)
    }

    "don't send poll on chat_close subject if version is not accepted" in {
      reset(chatService)
      val user = "user:10245463"
      val room = TechSupportUtils.roomId(user)
      val operator = "aborunov@yandex-team.ru"
      val subject1 = "Разное:Закрытие_чата"
      val json = Json.obj(
        "recipient" -> Json.obj("id" -> user),
        "sender" -> Json.obj("name" -> "Андрей", "email" -> operator),
        "message" -> Json.obj("type" -> "text", "text" -> ("тыц тыц! ###" + subject1), "id" -> "1179266")
      )
      when(chatService.markRead(?, ?)(?)).thenReturn(Future.unit)
      val pollMessage = ModelGenerators.message.next
      var hash = ""
      stub(chatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
        case (params, _) =>
          params.room shouldBe room
          params.author shouldBe TechSupportUtils.TechSupportUser
          params.payload shouldBe MessagePayload.fromText(pollMessages.pollStartMessage)
          params.attachments shouldBe Seq.empty
          params.providedId shouldBe None
          params.roomLocator.value shouldBe TechSupportUtils.roomLocator(user)
          params.properties.getType shouldBe MessagePropertyType.TECH_SUPPORT_POLL
          params.properties.getTechSupportPoll.getTtl shouldBe 7.days.toSeconds
          hash = params.properties.getTechSupportPoll.getHash
          Future.successful(SendMessageResult(pollMessage, None))
      }
      when(techSupportSubjectService.addSubjects(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      val userPlatform: (Platform, Version) = Platform.Android -> Version(5, 5, 3, 0)
      val supportedPlatform: Map[Platform, Version] = Map(Platform.Android -> Version(5, 5, 4, 0))
      when(techSupportPollService.getLastUserAppVersion(?)(?)).thenReturn(Future.successful(userPlatform))
      when(techSupportPollService.acceptedVersions).thenReturn(supportedPlatform)
      Post("/auto", HttpEntity(ContentTypes.`application/json`, Json.stringify(json))) ~> route ~> check {
        withClue(responseAs[String]) {
          status should be(StatusCodes.OK)
        }
      }
      verify(techSupportSubjectService).addSubjects(eq(room), eq(user), eq(operator), eq(Seq(subject1)))(?)
      verify(techSupportPollService).getLastUserAppVersion(eq(user))(?)
      verify(techSupportPollService, atLeastOnce).acceptedVersions
      verify(chatService).markRead(eq(room), eq(TechSupportUtils.TechSupportUser))(any())
      verifyNoMoreInteractions(chatService)
      verifyNoMoreInteractions(techSupportPollService)
      verifyNoMoreInteractions(techSupportSubjectService)
    }

    "dont save empty subjects" in {
      reset(chatService)
      val user = "user:10245464"
      val room = TechSupportUtils.roomId(user)
      val operator = "aborunov@yandex-team.ru"
      val subject1 = "Деньги:Возврат"
      val json = Json.obj(
        "recipient" -> Json.obj("id" -> user),
        "sender" -> Json.obj("name" -> "Андрей", "email" -> operator),
        "message" -> Json.obj("type" -> "text", "text" -> ("тыц тыц! ### " + subject1), "id" -> "1179266")
      )
      when(chatService.markRead(?, ?)(?)).thenReturn(Future.unit)
      Post("/auto", HttpEntity(ContentTypes.`application/json`, Json.stringify(json))) ~> route ~> check {
        withClue(responseAs[String]) {
          status should be(StatusCodes.OK)
        }
      }
      verifyZeroInteractions(techSupportSubjectService)
      verify(chatService).markRead(eq(room), eq(TechSupportUtils.TechSupportUser))(any())
      verifyNoMoreInteractions(chatService)
    }

    "send user typing notification to jivosite" in {
      reset(chatService)
      val user = "user:10245465"
      val operator = "aborunov@yandex-team.ru"
      val room = TechSupportUtils.roomId(user)
      val json = Json.obj(
        "recipient" -> Json.obj("id" -> user),
        "sender" -> Json.obj("name" -> "Андрей", "email" -> operator),
        "message" -> Json.obj("type" -> "typein")
      )
      val event = EventsModel.Event.newBuilder()
      event.getTechSupportTypingBuilder.setRoomId(room).setUserId(user)
      when(chatService.markRead(?, ?)(?)).thenReturn(Future.unit)
      when(eventService.send(?)(?)).thenReturn(Future.unit)
      Post("/auto", HttpEntity(ContentTypes.`application/json`, Json.stringify(json))) ~> route ~> check {
        withClue(responseAs[String]) {
          status should be(StatusCodes.OK)
        }
      }
      verifyZeroInteractions(techSupportSubjectService)
      verify(chatService).markRead(eq(room), eq(TechSupportUtils.TechSupportUser))(any())
      verify(eventService).send(eq(event.build()))(any())
      verifyNoMoreInteractions(eventService)
      verifyNoMoreInteractions(chatService)
    }
  }
}
