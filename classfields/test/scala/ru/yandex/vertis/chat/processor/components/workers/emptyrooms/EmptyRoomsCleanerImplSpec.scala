package ru.yandex.vertis.chat.processor.components.workers.emptyrooms

import org.joda.time.DateTime
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import ru.yandex.vertis.chat.components.dao.chat.storage.ChatStorage
import ru.yandex.vertis.chat.components.time.SetTimeServiceImpl
import ru.yandex.vertis.chat.dao.jdbc.api._
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.model.{Room, RoomId, UserId, Window}
import ru.yandex.vertis.chat.service.ServiceGenerators.{createRoomParameters, sendMessageParameters}
import ru.yandex.vertis.chat.service.features.ChatFeatures
import ru.yandex.vertis.chat.service.impl.jdbc.{JdbcChatService, JdbcSpec}
import ru.yandex.vertis.chat.service.{CreateMessageParameters, RoomLocator, SendMessageResult}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.generators.{BasicGenerators, ProducerProvider}

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class EmptyRoomsCleanerImplSpec
  extends WordSpec
  with JdbcSpec
  with ProducerProvider
  with RequestContextAware
  with Matchers
  with BeforeAndAfter {

  private val registry = new InMemoryFeatureRegistry(BasicFeatureTypes)

  after {
    database.master.run(sqlu"""delete from users_rooms""").futureValue
    database.master.run(sqlu"""delete from room""").futureValue
    database.master.run(sqlu"""delete from message""").futureValue
    database.master.run(sqlu"""delete from message_idempotency""").futureValue
    database.master.run(sqlu"""delete from room_read_timestamps""").futureValue
    registry.updateFeature("empty_room_cleaner", "false")
  }

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds))

  abstract class Fixture {
    val instanceId = BasicGenerators.readableString.next
    val messageIdGenerator = new TimeIdGenerator(instanceId)
    val timeService = new SetTimeServiceImpl
    val now = DateTime.now().withMillisOfDay(0)
    setNow(now)
    val pool = Executors.newFixedThreadPool(3)
    val executionContext = ExecutionContext.fromExecutor(pool)

    val service = new JdbcChatService(database, RandomIdGenerator, messageIdGenerator, timeService) {
      implicit override def ec: ExecutionContext = executionContext
    }

    val cleaner: EmptyRoomsCleanerImpl = new EmptyRoomsCleanerImpl {
      implicit override def ec: ExecutionContext = executionContext

      override val shouldWork: Boolean = true

      override def start(): Unit = ???

      override def chatStorage: DMap[ChatStorage] = DMap.forAllDomains(database)

      override val features: DMap[ChatFeatures] =
        DMap.forAllDomains(new ChatFeatures {
          override def featureRegistry: FeatureRegistry = registry
        })
    }

    def setNow(d: DateTime): Unit = {
      timeService.setNow(d)
    }

    def createRoomInMoment(creator: UserId, user2: UserId, moment: DateTime): Room = {
      val createRoomRequest = createRoomParameters.next.withoutAllUsers.withUserId(creator).withUserId(user2)
      val room = withUserContext(creator) { rc =>
        service.createRoom(createRoomRequest)(rc).futureValue
      }
      val dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(moment.getMillis))
      database.master.run(sqlu"""update users_rooms set updated = $dateStr""").futureValue
      database.master.run(sqlu"""update room set created = $dateStr""").futureValue
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
      val dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(moment.getMillis))
      database.master.run(sqlu"""update message set created = $dateStr""").futureValue
      result
    }
  }

  "EmptyRoomsCleanerImpl.removeEmptyRooms" should {
    "remove one room and keep another" when {
      "rooms were created day ago, one without any message, another with message in it and feature=true" in new Fixture {
        registry.updateFeature("empty_room_cleaner", "true").futureValue
        val user1 = userId.next
        val user2 = userId.next
        val room1: Room = createRoomInMoment(user1, user2, now.minusDays(1).minusHours(1))
        val user3 = userId.next
        val user4 = userId.next
        val room2: Room = createRoomInMoment(user3, user4, now.minusDays(1).minusHours(1))
        sendMessageInMoment(room2.id, user3, now.minusHours(1))
        val result = cleaner.removeEmptyRoomsForTests(database, 0)
        result shouldBe 1
        service.checkRoomExists(RoomLocator.Direct(room1.id)).futureValue shouldBe false
        service.checkRoomExists(RoomLocator.Direct(room2.id)).futureValue shouldBe true
        service.getMessages(room2.id, Window(None, 100, asc = true)).futureValue.length shouldBe 1
      }
    }
    "remove room" when {
      "room was created day ago without any message in it and feature=true" in new Fixture() {
        val user1 = userId.next
        val user2 = userId.next
        registry.updateFeature("empty_room_cleaner", "true").futureValue
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1).minusHours(1))
        val result = cleaner.removeEmptyRoomsForTests(database, 0)
        result shouldBe 1
        service.checkRoomExists(RoomLocator.Direct(room.id)).futureValue shouldBe false
      }

      "room was created day ago with just spam message in it and feature=true" in new Fixture() {
        val user1 = userId.next
        val user2 = userId.next
        registry.updateFeature("empty_room_cleaner", "true").futureValue
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1).minusHours(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1).minusHours(1), params => params.copy(isSpam = true))
        val result = cleaner.removeEmptyRoomsForTests(database, 0)
        result shouldBe 1
        service.checkRoomExists(RoomLocator.Direct(room.id)).futureValue shouldBe false
      }
    }

    "not remove room" when {
      "room was created day ago without any message in it but feature=false" in new Fixture() {
        val user1 = userId.next
        val user2 = userId.next
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1).minusHours(1))
        val result = cleaner.removeEmptyRoomsForTests(database, 0)
        result shouldBe 0
        service.checkRoomExists(RoomLocator.Direct(room.id)).futureValue shouldBe true
      }

      "room was created less then day ago without any message in it and feature=true" in new Fixture() {
        val user1 = userId.next
        val user2 = userId.next
        registry.updateFeature("empty_room_cleaner", "true").futureValue
        val room: Room = createRoomInMoment(user1, user2, now.minusHours(5))
        val result = cleaner.removeEmptyRoomsForTests(database, 0)
        result shouldBe 0
        service.checkRoomExists(RoomLocator.Direct(room.id)).futureValue shouldBe true
      }

      "room was created day ago with message in it and feature=true" in new Fixture() {
        val user1 = userId.next
        val user2 = userId.next
        registry.updateFeature("empty_room_cleaner", "true").futureValue
        val room: Room = createRoomInMoment(user1, user2, now.minusDays(1).minusHours(1))
        sendMessageInMoment(room.id, user1, now.minusDays(1).minusHours(1))
        val result = cleaner.removeEmptyRoomsForTests(database, 0)
        result shouldBe 0
        service.checkRoomExists(RoomLocator.Direct(room.id)).futureValue shouldBe true
      }
    }
  }
}
