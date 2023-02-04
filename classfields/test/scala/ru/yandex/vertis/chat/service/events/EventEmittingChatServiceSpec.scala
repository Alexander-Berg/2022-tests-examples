package ru.yandex.vertis.chat.service.events

import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, OptionValues}
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.Platform
import ru.yandex.vertis.chat.components.clients.events.{EventEmittingChatService, EventService}
import ru.yandex.vertis.chat.components.clients.journal.JournalEventService
import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.events.unread.UnreadEventsService
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.sandbox.NoSandboxSupport
import ru.yandex.vertis.chat.components.time.TimeSupport
import ru.yandex.vertis.chat.components.tracing.TraceCreator
import ru.yandex.vertis.chat.model.events.EventsModel
import ru.yandex.vertis.chat.model.events.EventsModel.JournalEvent
import ru.yandex.vertis.chat.service.ServiceGenerators.{sendMessageParameters, _}
import ru.yandex.vertis.chat.service.ServiceProtoFormats.RequestContextFormat
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState, JvmScheduledActions}
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceSpecBase, CreateMessageParameters}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.{CacheControl, Client, RequestContext, UserRequestContext}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.protobuf.asProto
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced}
import ru.yandex.vertis.util.Threads

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Test for EventEmittingChatService
  *
  * @author aborunov
  */
class EventEmittingChatServiceSpec
  extends ChatServiceSpecBase
  with MockitoSupport
  with OptionValues
  with Eventually
  with BeforeAndAfter {
  private val mockedEventService = mock[EventService]
  var messageSentEvent: EventsModel.Event = _
  stub(mockedEventService.send(_: EventsModel.Event)(_: Traced)) {
    case (event, _) =>
      messageSentEvent = event
      Future.unit
  }

  private val mockedJournalEventService = mock[JournalEventService]
  when(mockedJournalEventService.send(any())(any())).thenReturn(Future.unit)

  private val state = JvmChatState.empty()
  private val effectiveChatService = new JvmChatService(state)

  private val scheduledActions = new JvmScheduledActions

  private val tracingSupport = LocalTracingSupport(EndpointConfig.Empty)

  private val unreadEventsSupport = new UnreadEventsService(Seq(0.millis))
    with DomainAutoruSupport
    with NoSandboxSupport {

    override val traceCreator: TraceCreator =
      TraceCreator.fromVertisTracing(tracingSupport)

    implicit override def ec: ExecutionContext = Threads.SameThreadEc

    override val eventService: DMap[EventService] =
      DMap.forAllDomains(mockedEventService)

    override val chatService: DMap[ChatService] =
      DMap.forAllDomains(effectiveChatService)

    override val scheduledActions: DMap[ScheduledActions] =
      DMap.forAllDomains(EventEmittingChatServiceSpec.this.scheduledActions)
  }

  private val mockedUnreadEventsSupport = mock[UnreadEventsService]

  val service: ChatService = new ChatServiceWrapper(effectiveChatService)
    with EventEmittingChatService
    with SameThreadExecutionContextSupport
    with DomainAutoruSupport
    with TimeSupport {

    override val eventService: DMap[EventService] =
      DMap.forAllDomains(mockedEventService)

    override val unreadEventsService: DMap[UnreadEventsService] =
      DMap.forAllDomains(unreadEventsSupport)

    override val journalService: DMap[JournalEventService] =
      DMap.forAllDomains(mockedJournalEventService)

  }

  private val serviceWithMockedUnreadSupport: ChatService =
    new ChatServiceWrapper(effectiveChatService)
      with EventEmittingChatService
      with SameThreadExecutionContextSupport
      with DomainAutoruSupport
      with TimeSupport {

      override val eventService: DMap[EventService] =
        DMap.forAllDomains(mockedEventService)

      override val unreadEventsService: DMap[UnreadEventsService] =
        DMap.forAllDomains(mockedUnreadEventsSupport)

      override val journalService: DMap[JournalEventService] =
        DMap.forAllDomains(mockedJournalEventService)

    }

  before {
    state.clear()
    scheduledActions.forgetAll()
  }

  "EventEmittingchatService" should {
    "send event on message sent with correct mute param in it" in {
      val create = createRoomParameters.next
      val room = service.createRoom(create).futureValue
      val roomId = room.id
      val muteUser = room.users.head

      service.mute(roomId, muteUser, mute = true).futureValue
      val message1: CreateMessageParameters = sendMessageParameters(room).next
      service.sendMessage(message1).futureValue
      val userFromEvent1 =
        messageSentEvent.getMessageSent.getRoom.getUsersList.asScala
          .find(_.getId == muteUser)
          .value
      userFromEvent1.getMutedNotifications shouldBe true

      service.mute(roomId, muteUser, mute = false).futureValue
      val message2 = sendMessageParameters(room).next
      service.sendMessage(message2).futureValue
      val userFromEvent2 =
        messageSentEvent.getMessageSent.getRoom.getUsersList.asScala
          .find(_.getId == muteUser)
          .value
      userFromEvent2.getMutedNotifications shouldBe false
    }

    "not schedule unread events on spam or silent message send" in {
      val create = createRoomParameters.next
      val room = service.createRoom(create).futureValue
      val message1: CreateMessageParameters =
        sendMessageParameters(room).next.copy(isSpam = true)
      serviceWithMockedUnreadSupport.sendMessage(message1).futureValue
      val message2: CreateMessageParameters =
        sendMessageParameters(room).next.copy(isSilent = true)
      serviceWithMockedUnreadSupport.sendMessage(message2).futureValue
      verifyZeroInteractions(mockedUnreadEventsSupport)
    }

    "generateEvents should not send unreadRooms event for muted user" in {
      val author = user.next.id
      val muteUser = user.next.id
      val unmutedUser = user.next.id
      val create = createRoomParameters.next.withoutAllUsers.withUserIds(Seq(author, muteUser, unmutedUser))
      val room = service.createRoom(create).futureValue
      val roomId = room.id

      val unreadRoomsUsers = ArrayBuffer[String]()
      stub(mockedEventService.send(_: EventsModel.Event)(_: Traced)) {
        case (event, _) =>
          if (event.hasUnreadRooms) {
            unreadRoomsUsers += event.getUnreadRooms.getUser
          }
          Future.unit
      }

      service.mute(roomId, muteUser, mute = true).futureValue
      val message1: CreateMessageParameters =
        sendMessageParameters(room.id, author).next
      service.sendMessage(message1).futureValue

      eventually {
        unreadEventsSupport.generateEvents().futureValue
        unreadRoomsUsers should not contain muteUser
        unreadRoomsUsers should contain(unmutedUser)
      }

      unreadRoomsUsers.clear()
      println(unreadRoomsUsers)
      service.mute(roomId, muteUser, mute = false).futureValue
      val message2: CreateMessageParameters =
        sendMessageParameters(room.id, author).next
      service.sendMessage(message2).futureValue
      eventually {
        unreadEventsSupport
          .generateEvents()
          .futureValue
        unreadRoomsUsers should contain(muteUser)
        unreadRoomsUsers should contain(unmutedUser)
      }
    }

    "createJournalEvent should handle event from telepony" in {
      val application = "telepony"
      val ctx: RequestContext = UserRequestContext(
        id = "39c967c164c76de7f724383c650b9d22",
        requester = Client(
          ip = Some("85.174.27.157"),
          agent = None,
          deviceUid = Some(
            "g59833ee85q398fs1vjsmb7vln7alfe2.287726cb17063fd6577b5d85b8cd6358"
          ),
          platform = Some(application),
          application = Some(application)
        ),
        user = "user:32367311",
        passportId = 32367311,
        cacheControl = CacheControl.Default,
        isInternal = false,
        trace = Traced.empty,
        idempotencyKey = None
      )
      val journalEvent = JournalEvent.newBuilder()
      journalEvent.setRequest(ctx)
      val result = journalEvent.build()
      result.getRequest.getPlatform shouldBe Platform.PLATFORM_UNKNOWN
      result.getRequest.getApplication shouldBe application
    }
  }
}
