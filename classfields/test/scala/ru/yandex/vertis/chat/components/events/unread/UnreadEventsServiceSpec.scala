package ru.yandex.vertis.chat.components.events.unread

import org.joda.time.DateTime
import org.mockito.Mockito._
import ru.yandex.vertis.chat.Domains
import ru.yandex.vertis.chat.action.{ScheduledAction, UserNotification}
import ru.yandex.vertis.chat.common.chatbot.ChatBotUtils
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.ComponentsSpecBase
import ru.yandex.vertis.chat.components.clients.events.EventService
import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions
import ru.yandex.vertis.chat.components.tracing.{TraceCreator, TracedUtils}
import ru.yandex.vertis.chat.model.ModelProtoFormats.RoomFormat
import ru.yandex.vertis.chat.model.events.EventsModel.{Event, UnreadRooms}
import ru.yandex.vertis.chat.model.{ModelGenerators, Room}
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.TestDomainAware
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.protobuf.asProto
import ru.yandex.vertis.util.Threads

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class UnreadEventsServiceSpec extends ComponentsSpecBase with MockitoSupport {

  private val scheduledActionsMock = mock[ScheduledActions]
  private val eventServiceMock = mock[EventService]
  private val traceCreatorMock = mock[TraceCreator]
  private val chatServiceMock = mock[ChatService]
  private val ecMock = Threads.SameThreadEc
  private val delays = Seq(1.hours, 2.hours, 3.hours)

  private val unreadEventsService = new UnreadEventsService(delays) with TestDomainAware {
    override def sandbox: Boolean = true

    override def scheduledActions: DMap[ScheduledActions] =
      DMap.forAllDomains(scheduledActionsMock)

    override def eventService: DMap[EventService] =
      DMap.forAllDomains(eventServiceMock)

    override def traceCreator: TraceCreator = traceCreatorMock

    override def chatService: DMap[ChatService] =
      DMap.forAllDomains(chatServiceMock)

    implicit override def ec: ExecutionContext = ecMock
  }

  "UnreadEventsServiceSpec" should {
    "schedule" in {
      val user = ModelGenerators.userId.next
      when(scheduledActionsMock.schedule(?, ?, ?)).thenReturn(Future.unit)
      unreadEventsService.schedule(user)
      verify(scheduledActionsMock).schedule(
        eq(UserNotification(user)),
        ?,
        eq(delays.size)
      )
    }

    "generateEvents: filtered all rooms = forget call" in {
      val user = ModelGenerators.userId.next
      val techSupportRoom = TechSupportUtils.techSupportRoom(user)
      val chatBotRoom = ChatBotUtils.chatBotRoom(user)

      when(scheduledActionsMock.forget(?)).thenReturn(Future.unit)
      when(scheduledActionsMock.getMatured(?)).thenReturn(
        Future.successful(
          Seq(ScheduledAction(UserNotification(user), DateTime.now(), 3))
        )
      )
      when(chatServiceMock.unreadRoomsCount(?)(?)).thenReturn(Future.successful(1))
      when(chatServiceMock.getRooms(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(Seq(techSupportRoom, chatBotRoom)))
      unreadEventsService.generateEvents().futureValue
      verify(scheduledActionsMock).forget(UserNotification(user))
    }

    "generateEvents 2 rooms(1 techsupport): filtered techSupport = sendmessage" in {
      implicit val domain: Domains.Value = Domains.Auto
      val userTech = ModelGenerators.userId.next
      val techSupportRoom = TechSupportUtils.techSupportRoom(userTech)
      val simpleRoomRoot = ModelGenerators.room.next
      val simpleRoomForTest = simpleRoomRoot.copy(
        hasUnread = true,
        properties = Map("_sandbox" -> "true"),
        participants = simpleRoomRoot.participants.map { u =>
          u.copy(muted = false)
        }
      )
      val simpleUser1 = simpleRoomForTest.creator
      val action =
        ScheduledAction(UserNotification(simpleUser1), DateTime.now(), 3)

      when(scheduledActionsMock.getMatured(?))
        .thenReturn(Future.successful(Seq(action)))
      when(chatServiceMock.getRooms(?, ?, ?, ?, ?)(?))
        .thenReturn(Future.successful(Seq(techSupportRoom, simpleRoomForTest)))
      when(scheduledActionsMock.reschedule(?, ?, ?)).thenReturn(Future.unit)
      when(eventServiceMock.send(?)(?)).thenReturn(Future.unit)
      when(traceCreatorMock.trace).thenReturn(TracedUtils.empty)

      unreadEventsService.generateEvents().futureValue

      verify(scheduledActionsMock).reschedule(
        eq(action.value),
        eq(delays(0)),
        eq(2)
      )

      val unreadRoomsEvent = UnreadRooms
        .newBuilder()
        .setUser(simpleUser1)
        .setCount(1)
        .addAllRooms(Seq(simpleRoomForTest).map(room => asProto(room)).asJava)

      val event = Event
        .newBuilder()
        .setUnreadRooms(unreadRoomsEvent)
        .build()
      verify(eventServiceMock).send(eq(event))(?)
    }

    "generateEvents 2 rooms of 1 user: filtering nothing" in {
      implicit val domain: Domains.Value = Domains.Auto
      val simpleRoomRoot = ModelGenerators.room.next
      val simpleRoomForTest = simpleRoomRoot.copy(
        hasUnread = true,
        properties = Map("_sandbox" -> "true"),
        participants = simpleRoomRoot.participants.map { u =>
          u.copy(muted = false, activeRoom = true)
        }
      )
      val simpleUser1 = simpleRoomForTest.creator
      val action =
        ScheduledAction(UserNotification(simpleUser1), DateTime.now(), 3)

      val simpleRoomRootAnother = ModelGenerators.room.next
      val simpleRoomForTestAnother = simpleRoomRootAnother.copy(
        hasUnread = true,
        properties = Map("_sandbox" -> "true"),
        participants = simpleRoomRootAnother.participants.map { u =>
          u.copy(id = simpleUser1, muted = false, activeRoom = true)
        },
        creator = simpleUser1
      )

      when(chatServiceMock.unreadRoomsCount(?)(?)).thenReturn(Future.successful(1))
      when(scheduledActionsMock.getMatured(?))
        .thenReturn(Future.successful(Seq(action)))
      when(chatServiceMock.getRooms(?, ?, ?, ?, ?)(?))
        .thenReturn(
          Future.successful(Seq(simpleRoomForTestAnother, simpleRoomForTest))
        )
      when(scheduledActionsMock.reschedule(?, ?, ?)).thenReturn(Future.unit)
      when(eventServiceMock.send(?)(?)).thenReturn(Future.unit)
      when(traceCreatorMock.trace).thenReturn(TracedUtils.empty)

      unreadEventsService.generateEvents().futureValue

      verify(scheduledActionsMock).reschedule(
        eq(action.value),
        eq(delays(0)),
        eq(2)
      )

      val unreadRooms = Seq(simpleRoomForTestAnother, simpleRoomForTest)
      val unreadRoomsEvent = UnreadRooms
        .newBuilder()
        .setUser(simpleUser1)
        .setCount(unreadRooms.size)
        .addAllRooms(unreadRooms.map(room => asProto(room)).asJava)

      val event = Event
        .newBuilder()
        .setUnreadRooms(unreadRoomsEvent)
        .build()
      verify(eventServiceMock).send(eq(event))(?)
    }
    // TODO: finish tests
//
//    "generateEvents: no rooms" in {
//      verify(scheduledActionsMock).reschedule(?,?,?)
//      when(eventServiceMock.send(?)(?))
//    }
//
//    "generateEvents: filtered sandbox only" in {
//      verify(scheduledActionsMock).reschedule(?,?,?)
//      when(eventServiceMock.send(?)(?))
//    }
//
//    "generateEvents: forget after remainattempts" in {
//      verify(scheduledActionsMock).forget(?)
//      when(eventServiceMock.send(?)(?))
//    }
  }

  // TODO: refactor duplicate code
  private def createRoomForThisTestsWithCreatorUser(): Room = {
    val simpleRoomRoot = ModelGenerators.room.next
    simpleRoomRoot.copy(
      hasUnread = true,
      properties = Map("_sandbox" -> "true"), // sometimes filters fail if not do this
      participants = simpleRoomRoot.participants.map { u =>
        u.copy(muted = false) // sometimes filters fail if not do this
      }
    )
  }

  private def mockDoableFunction(rooms: Seq[Room], actions: Seq[ScheduledAction]) = {
    when(scheduledActionsMock.forget(?)).thenReturn(Future.unit)
    when(chatServiceMock.unreadRoomsCount(?)(?)).thenReturn(Future.successful(1))
    when(scheduledActionsMock.getMatured(?))
      .thenReturn(Future.successful(actions))
    when(chatServiceMock.getRooms(?, ?, ?, ?)(?))
      .thenReturn(Future.successful(rooms))
    when(scheduledActionsMock.reschedule(?, ?, ?)).thenReturn(Future.unit)
    when(eventServiceMock.send(?)(?)).thenReturn(Future.unit)
    when(traceCreatorMock.trace).thenReturn(TracedUtils.empty)
  }
}
