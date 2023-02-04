package ru.yandex.vertis.chat.service.support

import java.util.NoSuchElementException
import org.mockito.Mockito._
import ru.yandex.passport.model.api.ApiModel.UserResult
import ru.yandex.vertis.chat.action.TechSupportPollNotification
import ru.yandex.vertis.chat.common.techsupport.TechSupport.{ChatUser, UserMessage}
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils.techSupportRoom
import ru.yandex.vertis.chat.common.techsupport.{TechSupport, TechSupportUtils}
import ru.yandex.vertis.chat.components.app.{Application, TestApplication}
import ru.yandex.vertis.chat.components.app.config.LocalAppConfig
import ru.yandex.vertis.chat.components.clients.events.{EventEmittingChatService, EventService}
import ru.yandex.vertis.chat.components.clients.jivosite.TechSupportClient
import ru.yandex.vertis.chat.components.clients.journal.JournalEventService
import ru.yandex.vertis.chat.components.clients.passport.PassportClient
import ru.yandex.vertis.chat.components.clients.pushnoy.PushnoyClient
import ru.yandex.vertis.chat.components.clients.techsupport.TechSupportDestinationDecider.Destination.{JivositeDealers, JivositePrivate}
import ru.yandex.vertis.chat.components.clients.techsupport._
import ru.yandex.vertis.chat.components.dao.chat.techsupport.TechSupportChatService
import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions
import ru.yandex.vertis.chat.components.dao.techsupport.polls.messages.{PollMessages, PollMessagesService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.events.unread.UnreadEventsService
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.TimeSupport
import ru.yandex.vertis.chat.config.StaticLocatorConfig
import ru.yandex.vertis.chat.model.{ModelGenerators, Room, RoomId}
import ru.yandex.vertis.chat.model.api.ApiModel.MessageProperties
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service._
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.impl.jvm.JvmChatService
import ru.yandex.vertis.chat.service.support.polls.TestPollMessagesService
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.{Domain, Domains, RequestContext}
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.mockito.MockitoSupport
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * TODO
  *
  * @author aborunov
  */
class TechSupportChatServiceSpec extends ChatServiceSpecBase with MockitoSupport {
  private val jivosite = mock[TechSupportClient]
  private val passport = mock[PassportClient]
  private val pushnoy = mock[PushnoyClient]
  private val mockedScheduledActions = mock[ScheduledActions]

  private val mockedTechSupportDestinationDecider =
    mock[TechSupportDestinationDecider]
  private val prefix: String = "//avatars.mdst.yandex.net"

  private val mockedEventService = mock[EventService]
  private val mockedJournalEventService = mock[JournalEventService]
  private val mockedUnreadEventsService = mock[UnreadEventsService]

  private val mockedTechSupportEventService = mock[VertisTechSupportService]

  private val staticLocatorConfig = new StaticLocatorConfig
  private val domain = Domains.Auto
  private val config = new LocalAppConfig(staticLocatorConfig, domain)
  private val application = new TestApplication(config)

  val service: ChatService =
    new JvmChatService()
      with TechSupportChatService
      with LoggingChatService
      with TestOperationalSupport
      with SameThreadExecutionContextSupport {

      override val vertisTechSupportService: DMap[VertisTechSupportService] =
        DMap.forAllDomains(mockedTechSupportEventService)

      override protected def loggerClass: Class[_] = classOf[ChatService]

      override val passportClient: DMap[PassportClient] = DMap.forAllDomains(passport)

      override val pushnoyClient: DMap[PushnoyClient] = DMap.forAllDomains(pushnoy)

      implicit override def mdsReadUrlPrefix: String = prefix

      override val pollMessagesService: DMap[PollMessagesService] =
        DMap.forAllDomains(new TestPollMessagesService(PollMessages.Default))

      override val techSupportClient: DMap[TechSupportClient] = DMap.forAllDomains(jivosite)

      implicit override val domain: Domain = Domains.Auto

      override val scheduledActions: DMap[ScheduledActions] = DMap.forAllDomains(mockedScheduledActions)

      override val techSupportDestinationDecider: DMap[TechSupportDestinationDecider] =
        DMap.forAllDomains(mockedTechSupportDestinationDecider)

      override def app: Application = application
    }

  "JivositeSupportChatService" should {
    val m = mock[ChatService]
    val techSupportAware = new ChatServiceWrapper(m)
      with TimeSupport
      with EventEmittingChatService
      with SameThreadExecutionContextSupport
      with DomainAutoruSupport
      with TechSupportChatService {
      override val eventService: DMap[EventService] = DMap.forAllDomains(mockedEventService)
      override val unreadEventsService: DMap[UnreadEventsService] = DMap.forAllDomains(mockedUnreadEventsService)
      override val journalService: DMap[JournalEventService] = DMap.forAllDomains(mockedJournalEventService)
      override val vertisTechSupportService: DMap[VertisTechSupportService] =
        DMap.forAllDomains(mockedTechSupportEventService)

      override val passportClient: DMap[PassportClient] = DMap.forAllDomains(passport)

      override val pushnoyClient: DMap[PushnoyClient] = DMap.forAllDomains(pushnoy)

      implicit override def mdsReadUrlPrefix: String = prefix

      override val pollMessagesService: DMap[PollMessagesService] =
        DMap.forAllDomains(new TestPollMessagesService(PollMessages.Default))

      override def techSupportClient: DMap[TechSupportClient] = DMap.forAllDomains(jivosite)

      override val scheduledActions: DMap[ScheduledActions] = DMap.forAllDomains(mockedScheduledActions)

      override val techSupportDestinationDecider: DMap[TechSupportDestinationDecider] =
        DMap.forAllDomains(mockedTechSupportDestinationDecider)

      override def getRoom(room: RoomId)(implicit rc: RequestContext): Future[Room] =
        Future.successful(techSupportRoom(rc.user))

      override def app: Application = application
    }

    "not return 404 on mute virtual tech support room" in {
      when(m.mute(?, ?, ?)(?)).thenReturn(Future.failed(new NoSuchElementException))
      val user = ModelGenerators.userId.next
      val room = TechSupportUtils.roomId(user)
      techSupportAware.mute(room, user, mute = true).futureValue
      verify(m).mute(?, ?, ?)(?)
    }

    "not save to db if failed to send to jivosite" in {
      when(jivosite.send(?, ?)(?)).thenReturn(Future.failed(new RuntimeException("Error!")))
      when(passport.getUser(?)(?)).thenReturn(Future.successful(UserResult.getDefaultInstance))
      when(mockedTechSupportDestinationDecider.destination(?)(?))
        .thenReturn(TechSupportDestinationDecider.Destination.JivositePrivate)

      val user = ModelGenerators.userId.next
      val room = TechSupportUtils.roomId(user)
      val message = sendMessageParameters(room, user).next.copy(attachments = Seq.empty)
      intercept[RuntimeException] {
        withUserContextFromPlatform(user, Some("ios")) { implicit rc =>
          cause(techSupportAware.sendMessage(message).futureValue)
        }
      }
      verify(jivosite).send(
        eq(
          TechSupport.Request(
            sender = Some(
              ChatUser(
                Some(user),
                Some(user),
                None,
                None,
                None,
                None,
                Some("https://moderation.vertis.yandex-team.ru/autoru?user_id="),
                None
              )
            ),
            message = Some(UserMessage("text", Some("[IOS] " + message.payload.value), None, None)),
            recipient = None
          )
        ),
        eq(JivositePrivate)
      )(?)
      verifyZeroInteractions(m)
    }

    "reschedule notification to jivosite on good tech support presets" in {
      reset(passport, jivosite, pushnoy, mockedScheduledActions, m)
      val user = ModelGenerators.userId.next
      val room = TechSupportUtils.roomId(user)
      val properties = MessageProperties.newBuilder()
      val goodPresetId = PollMessages.Default.goodPresets.head.id
      val pollHash = readableString.next
      properties.getTechSupportFeedbackBuilder.setHash(pollHash).setSelectedPreset(goodPresetId)
      val message = sendMessageParameters(room, user).next.copy(
        attachments = Seq.empty,
        properties = properties.build()
      )
      when(m.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      when(mockedScheduledActions.rescheduleIfExists(?, ?, ?)).thenReturn(Future.successful(true))
      when(mockedTechSupportEventService.submitMessage(?)(?)).thenReturn(Future.unit)
      techSupportAware.sendMessage(message).futureValue
      verify(mockedScheduledActions).rescheduleIfExists(eq(TechSupportPollNotification(pollHash)), eq(0.millis), eq(3))
      verify(m).sendMessage(eq(message))(?)
      verifyNoMoreInteractions(passport, jivosite, pushnoy, mockedScheduledActions, m)
    }

    "reschedule notification to jivosite on norm tech support presets" in {
      reset(passport, jivosite, pushnoy, mockedScheduledActions, m)
      val user = ModelGenerators.userId.next
      val room = TechSupportUtils.roomId(user)
      val properties = MessageProperties.newBuilder()
      val goodPresetId = "all_good"
      val pollHash = readableString.next
      properties.getTechSupportFeedbackBuilder.setHash(pollHash).setSelectedPreset(goodPresetId)
      val message = sendMessageParameters(room, user).next.copy(
        attachments = Seq.empty,
        properties = properties.build()
      )
      when(m.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      when(mockedScheduledActions.rescheduleIfExists(?, ?, ?)).thenReturn(Future.successful(true))
      when(mockedTechSupportEventService.submitMessage(?)(?)).thenReturn(Future.unit)
      techSupportAware.sendMessage(message).futureValue
      verify(mockedScheduledActions).rescheduleIfExists(eq(TechSupportPollNotification(pollHash)), eq(0.millis), eq(3))
      verify(m).sendMessage(eq(message))(?)
      verifyNoMoreInteractions(passport, jivosite, pushnoy, mockedScheduledActions, m)
    }

    "reschedule notification to jivosite on bad tech support presets" in {
      reset(passport, jivosite, pushnoy, mockedScheduledActions)
      val user = ModelGenerators.userId.next
      val room = TechSupportUtils.roomId(user)
      val properties = MessageProperties.newBuilder()
      val goodPresetId = PollMessages.Default.badPresets.head.id
      val pollHash = readableString.next
      properties.getTechSupportFeedbackBuilder.setHash(pollHash).setSelectedPreset(goodPresetId)
      val message = sendMessageParameters(room, user).next.copy(
        attachments = Seq.empty,
        properties = properties.build()
      )
      when(mockedScheduledActions.rescheduleIfExists(?, ?, ?)).thenReturn(Future.successful(true))
      when(m.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      when(mockedTechSupportEventService.submitMessage(?)(?)).thenReturn(Future.unit)
      techSupportAware.sendMessage(message).futureValue
      verify(mockedScheduledActions).rescheduleIfExists(eq(TechSupportPollNotification(pollHash)), eq(0.millis), eq(3))
      verify(m).sendMessage(eq(message))(?)
      verifyNoMoreInteractions(passport, jivosite, pushnoy, mockedScheduledActions, m)
    }

    "correctly provide rooms including tech support room for dealer" in {
      val dealerId = "dealer:1015"
      val userId = "user:1016"
      val users = Set(dealerId, userId)

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          val techSupportRoom = coarse(TechSupportUtils.techSupportRoom(user))
          service.getRooms(user).futureValue.map(coarse) should be(Seq(techSupportRoom))
        }
      }

      val room = createAndCheckRoom(_.withUserIds(users))

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          val techSupportRoom = coarse(TechSupportUtils.techSupportRoom(user))
          service.getRooms(user).futureValue.map(coarse) should be(Seq(techSupportRoom, coarse(room)))
        }
      }

    }

    "provide tech support room on getRoomsById request, if it was not provided by db" in pendingUntilFixed {
      val user = ModelGenerators.userId.next
      val techSupportRoom = TechSupportUtils.techSupportRoom(user)
      when(m.getRoomsByIds(?)(?)).thenReturn(Future.successful(Seq()))

      withUserContext(user) { implicit rc =>
        techSupportAware.getRoomsByIds(Iterable(techSupportRoom.id)).futureValue shouldBe Seq(techSupportRoom)
      }
    }

    "don't hide created tech support room for dealer" in {
      val dealerId = "dealer:1017"
      val userId = "user:1018"
      val users = Set(dealerId, userId)

      users.foreach { user =>
        withUserContext(user) { implicit rc =>
          val room = createAndCheckRoom(TechSupportUtils.roomLocator(user).asSource.get.parameters)(rc)
          service.getRooms(user).futureValue.map(coarse) should be(Seq(coarse(room)))
        }
      }
    }

    "send requests from dealers to second tech support channel" in {
      reset(m)
      when(jivosite.send(?, ?)(?)).thenReturn(Future.unit)
      when(passport.getUser(?)(?)).thenReturn(Future.successful(UserResult.getDefaultInstance))
      when(mockedTechSupportDestinationDecider.destination(?)(?))
        .thenReturn(TechSupportDestinationDecider.Destination.JivositeDealers)
      when(mockedTechSupportEventService.submitMessage(?)(?)).thenReturn(Future.unit)
      val dealer = ModelGenerators.dealerId.next
      val room = TechSupportUtils.roomId(dealer)
      val message = sendMessageParameters(room, dealer).next.copy(attachments = Seq.empty)
      when(m.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      withUserContextFromPlatform(dealer, Some("ios")) { implicit rc =>
        techSupportAware.sendMessage(message).futureValue
      }
      verify(jivosite).send(
        eq(
          TechSupport.Request(
            sender = Some(
              ChatUser(
                Some(dealer),
                Some(dealer),
                None,
                None,
                None,
                None,
                Some("https://moderation.vertis.yandex-team.ru/autoru?user_id="),
                None
              )
            ),
            message = Some(UserMessage("text", Some("[IOS] " + message.payload.value), None, None)),
            recipient = None
          )
        ),
        eq(JivositeDealers)
      )(?)
      verify(m).sendMessage(
        eq(
          message.copy(
            roomLocator = Some(TechSupportUtils.roomLocator(dealer)),
            properties = MessageProperties.newBuilder().setUserAppVersion("IOS").build()
          )
        )
      )(?)
      verifyNoMoreInteractions(mockedTechSupportEventService)
      verifyNoMoreInteractions(m)
    }

    "send requests from users to techsupport if decider spec it" in {
      when(jivosite.send(?, ?)(?)).thenReturn(Future.unit)
      when(passport.getUser(?)(?)).thenReturn(Future.successful(UserResult.getDefaultInstance))
      when(mockedTechSupportDestinationDecider.destination(?)(?))
        .thenReturn(TechSupportDestinationDecider.Destination.VertisTechsupport)
      when(mockedTechSupportEventService.submitMessage(?)(?)).thenReturn(Future.unit)
      val dealer = ModelGenerators.dealerId.next
      val room = TechSupportUtils.roomId(dealer)
      val message = sendMessageParameters(room, dealer).next.copy(attachments = Seq.empty)
      when(m.sendMessage(?)(?)).thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      withUserContextFromPlatform(dealer, Some("ios")) { implicit rc =>
        techSupportAware.sendMessage(message).futureValue
      }
      verify(m).sendMessage(
        eq(
          message.copy(
            roomLocator = Some(TechSupportUtils.roomLocator(dealer)),
            properties = MessageProperties.newBuilder().setUserAppVersion("IOS").build()
          )
        )
      )(?)

      verify(mockedTechSupportEventService).submitMessage(?)(?)
      verifyNoMoreInteractions(m)
    }
  }
}
