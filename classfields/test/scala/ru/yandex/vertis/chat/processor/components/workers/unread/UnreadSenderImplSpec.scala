package ru.yandex.vertis.chat.processor.components.workers.unread

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import ru.yandex.vertis.chat.{Domain, Domains}
import ru.yandex.vertis.chat.components.time.{SetTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.dao.jdbc.api._
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.model.{Room, RoomId, UserId}
import ru.yandex.vertis.chat.processor.common.ServiceMessage
import ru.yandex.vertis.chat.processor.common.model.rooms.{Room => ProcessorRoom}
import ru.yandex.vertis.chat.processor.components.clients.telepony.Call
import ru.yandex.vertis.chat.processor.components.dao.rooms.{AutoruRoomsDao, AutoruRoomsDaoImpl}
import ru.yandex.vertis.chat.processor.components.dao.{ChatsQueryExecutor, ChatsQueryExecutorImpl}
import ru.yandex.vertis.chat.processor.components.mysql.MySqlConnection
import ru.yandex.vertis.chat.processor.components.query.{QueryExecutor, QueryExecutorImpl}
import ru.yandex.vertis.chat.processor.components.workers.unread.enricher.AutoruRoomsEnricher
import ru.yandex.vertis.chat.processor.components.workers.unread.sender.AutoruRoomsSender
import ru.yandex.vertis.chat.service.ServiceGenerators.{createOfferRoomParameters, sendMessageParameters}
import ru.yandex.vertis.chat.service.features.ChatFeatures
import ru.yandex.vertis.chat.service.impl.jdbc.{JdbcChatService, JdbcSpec}
import ru.yandex.vertis.chat.service.{CreateMessageParameters, SendMessageResult}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}
import ru.yandex.vertis.chat.util.workmoments.WorkMoments
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.generators.{BasicGenerators, ProducerProvider}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}

import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class UnreadSenderImplSpec
  extends WordSpec
  with JdbcSpec
  with ProducerProvider
  with RequestContextAware
  with Matchers
  with BeforeAndAfter
  with MockitoSupport {
  implicit private val domain: Domain = Domains.Auto

  private val registry = new InMemoryFeatureRegistry(BasicFeatureTypes)

  private def setNowInDb(d: DateTime): Unit = {
    val dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(d.getMillis))
    database.master.run(sqlu"""update users_rooms set updated = $dateStr""").futureValue
    database.master.run(sqlu"""update room set created = $dateStr""").futureValue
    database.master.run(sqlu"""update message set created = $dateStr""").futureValue
  }

  after {
    database.master.run(sqlu"""delete from users_rooms""").futureValue
    database.master.run(sqlu"""delete from room""").futureValue
    database.master.run(sqlu"""delete from message""").futureValue
    database.master.run(sqlu"""delete from message_idempotency""").futureValue
    database.master.run(sqlu"""delete from room_read_timestamps""").futureValue
    registry.updateFeature("unread_sender", "false")
  }

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds))

  trait Fixture {
    val instanceId = BasicGenerators.readableString.next
    val messageIdGenerator = new TimeIdGenerator(instanceId)
    val timeService = new SetTimeServiceImpl
    val now = DateTime.now().withMillisOfDay(0).plusMinutes(30)
    setNow(now)
    val pool = Executors.newFixedThreadPool(3)
    val executionContext = ExecutionContext.fromExecutor(pool)

    val service = new JdbcChatService(database, RandomIdGenerator, messageIdGenerator, timeService) {
      implicit override def ec: ExecutionContext = executionContext
    }

    val workMoments = WorkMoments.every(2.minutes)

    val queryExecutor: ChatsQueryExecutor = new ChatsQueryExecutorImpl {
      override val queryExecutor: QueryExecutor = new QueryExecutorImpl {}

      override def chatAutoruDb(queryName: String, timeout: Duration): Future[MySqlConnection] = {
        Future.successful {
          val con = database.master.source.createConnection()
          new MySqlConnection {
            override def conn: Connection = con

            override def close(): Unit = con.close()
          }
        }
      }
    }

    val dao: AutoruRoomsDao = new AutoruRoomsDaoImpl {
      override def chatsQueryExecutor: ChatsQueryExecutor = queryExecutor
    }

    val mockedSender: AutoruRoomsSender = mock[AutoruRoomsSender]

    val mockedEnricher: AutoruRoomsEnricher = mock[AutoruRoomsEnricher]

    val unreadSender = new UnreadSenderImpl(batchSize = 100) {
      override val autoruRoomsSender: AutoruRoomsSender = mockedSender

      override val shouldWork: Boolean = true

      override def start(): Unit = ???

      override def autoruRoomsDao: AutoruRoomsDao = dao

      override val features: DMap[ChatFeatures] = DMap.forAllDomains(new ChatFeatures {
        override def featureRegistry: FeatureRegistry = registry
      })

      override val autoruRoomsEnricher: AutoruRoomsEnricher = mockedEnricher

      override def timeService: TimeService = Fixture.this.timeService
    }

    def setNow(d: DateTime): Unit = {
      timeService.setNow(d)
    }

    def createRoomInMoment(creator: UserId, user2: UserId, moment: DateTime): Room = {
      val createRoomRequest = createOfferRoomParameters.next.withoutAllUsers.withUserId(creator).withUserId(user2)
      val room = withUserContext(creator) { rc =>
        service.createRoom(createRoomRequest)(rc).futureValue
      }
      setNowInDb(moment)
      room
    }

    def sendMessageInMoment(
        roomId: RoomId,
        author: UserId,
        moment: DateTime,
        correct: CreateMessageParameters => CreateMessageParameters = identity
    ): SendMessageResult = {
      val request = correct(sendMessageParameters(roomId, author).next)
      val result = withUserContext(author) { rc =>
        service.sendMessage(request)(rc).futureValue
      }
      setNowInDb(moment)
      result
    }
  }

  "UnreadSenderImpl.processRooms" should {
    "send message" when {
      "there is unanswered message a day ago" in new Fixture {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1))
        stub[Seq[ProcessorRoom], Seq[ProcessorRoom]](mockedEnricher.enrichRooms(_: Seq[ProcessorRoom])) {
          case rooms: Seq[ProcessorRoom] =>
            rooms
        }
        when(mockedSender.sendMessages(?, ?)).thenReturn(Seq())
        val result = unreadSender.processRooms
        verify(mockedEnricher).enrichRooms(?)
        verify(mockedSender).sendMessages(?, eeq(ServiceMessage.SuggestCall))
        result shouldBe 1
      }
    }

    "not send message" when {
      "no messages and rooms a day  ago" in new Fixture {
        val result = unreadSender.processRooms
        verifyZeroInteractions(mockedEnricher)
        result shouldBe 0
        verifyZeroInteractions(mockedSender)
      }

      "there is room created a day ago but no messages ever since" in new Fixture {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1))
        stub[Seq[ProcessorRoom], Seq[ProcessorRoom]](mockedEnricher.enrichRooms(_: Seq[ProcessorRoom])) {
          case rooms: Seq[ProcessorRoom] =>
            rooms
        }
        when(mockedSender.sendMessages(?, ?)).thenReturn(Seq())
        val result = unreadSender.processRooms
        verifyZeroInteractions(mockedEnricher)
        result shouldBe 0
        verifyZeroInteractions(mockedSender)
      }

      "there is unanswered message but in different hour" in new Fixture {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1).minusHours(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1).plusHours(1))
        stub[Seq[ProcessorRoom], Seq[ProcessorRoom]](mockedEnricher.enrichRooms(_: Seq[ProcessorRoom])) {
          case rooms: Seq[ProcessorRoom] =>
            rooms
        }
        when(mockedSender.sendMessages(?, ?)).thenReturn(Seq())
        val result = unreadSender.processRooms
        verifyZeroInteractions(mockedEnricher)
        result shouldBe 0
        verifyZeroInteractions(mockedSender)
      }

      "there is unanswered message a day ago but message is spam" in new Fixture {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1), params => params.copy(isSpam = true))
        stub[Seq[ProcessorRoom], Seq[ProcessorRoom]](mockedEnricher.enrichRooms(_: Seq[ProcessorRoom])) {
          case rooms: Seq[ProcessorRoom] =>
            rooms
        }
        val result = unreadSender.processRooms
        verifyZeroInteractions(mockedEnricher)
        result shouldBe 0
        verifyZeroInteractions(mockedSender)
      }

      "there is unanswered message a day ago but room is chat only" in new Fixture {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1))
        stub[Seq[ProcessorRoom], Seq[ProcessorRoom]](mockedEnricher.enrichRooms(_: Seq[ProcessorRoom])) {
          case rooms: Seq[ProcessorRoom] =>
            rooms.map(_.copy(chatOnly = true))
        }
        val result = unreadSender.processRooms
        verify(mockedEnricher).enrichRooms(?)
        result shouldBe 0
        verifyZeroInteractions(mockedSender)
      }

      "there is unanswered message a day ago but call history is non empty" in new Fixture {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1))
        stub[Seq[ProcessorRoom], Seq[ProcessorRoom]](mockedEnricher.enrichRooms(_: Seq[ProcessorRoom])) {
          case rooms: Seq[ProcessorRoom] =>
            rooms.map(_.copy(callHistory = Seq(Call("", "", "", "", 0, "", "", "", "", "", 0, ""))))
        }
        val result = unreadSender.processRooms
        verify(mockedEnricher).enrichRooms(?)
        result shouldBe 0
        verifyZeroInteractions(mockedSender)
      }
    }
  }
}
