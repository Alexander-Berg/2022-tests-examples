package ru.yandex.vertis.chat.api.v1.domain.room

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.CacheDirectives
import org.joda.time.DateTime
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.CommonModel.ClientFeature
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.api.v1.proto.ApiProtoFormats._
import ru.yandex.vertis.chat.components.time.SetTimeServiceImpl
import ru.yandex.vertis.chat.components.tracing.TracedUtils
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.{Room, RoomsPage, UserId}
import ru.yandex.vertis.chat.service.RoomLocator
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.util.uuid.RandomIdGenerator
import ru.yandex.vertis.chat.{CacheControl, Client, UserRequestContext}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._

/**
  * Runnable spec on [[RoomHandler]].
  *
  * @author dimas
  */
class RoomHandlerSpec extends HandlerSpecBase with OptionValues with MockitoSupport {

  private val timeService = new SetTimeServiceImpl

  private val realService = new JvmChatService(JvmChatState.empty(timeService))
  private val service = Mockito.spy(realService)
  private val route = seal(new RoomHandler(service).route)

  s"POST $root" should {

    "create room" in {
      val parameters = createRoomParameters.next
      Post(root, parameters).withSomePassportUser.withSomeUser ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "successfully re-create existing room" in {
      val id = roomId.next
      val parameters = createRoomParameters.next.copy(id = Some(id))
      val user = Gen.oneOf(parameters.participants.userIds.toSeq).next
      var existingRoom: Option[Room] = None
      Post(root, parameters).withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        existingRoom = Some(responseAs[Room])
      }
      Post(root, parameters).withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[Room] should be(existingRoom.get)
      }
    }
  }

  s"GET $root" should {
    "report 404 if no user specified" in {
      Get(root) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "provide rooms for user" in {
      val user = passportPrivateUserId.next
      val payloads = createRoomParameters
        .next(10)
        .map(_.withUserId(user))
      withUserContext(user) { rc =>
        payloads.foreach(service.createRoom(_)(rc))
      }

      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user")
          .accepting(mediaType)
          .withPassportUser(user.filter(_.isDigit).toLong)
          .withFeatures(Set(ClientFeature.CHAT_BOT_CHECKUP_BUTTON))
          .withRequestId("requestId")
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(payloads.size)
          rooms.foreach(r => r.participants.find(user).value.activeRoom shouldBe true)
        }
      }
      verify(service, times(2)).getRooms(
        eq(user),
        eq(false),
        eq(false),
        eq(false),
        eq(None)
      )(
        eq(
          UserRequestContext(
            "requestId",
            Client(features = Set(ClientFeature.CHAT_BOT_CHECKUP_BUTTON)),
            user,
            user.filter(_.isDigit).toLong,
            CacheControl.Default,
            isInternal = false,
            TracedUtils.empty,
            idempotencyKey = None
          )
        )
      )
    }

    "provide rooms for user without caching" in {
      val user = passportPrivateUserId.next
      val payloads = createRoomParameters
        .next(10)
        .map(_.withUserId(user))
      withUserContext(user) { rc =>
        payloads.foreach(service.createRoom(_)(rc))
      }

      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user")
          .accepting(mediaType)
          .withPassportUser(user.filter(_.isDigit).toLong)
          .withFeatures(Set(ClientFeature.CHAT_BOT_CHECKUP_BUTTON))
          .withRequestId("requestId")
          .withCacheControlHeader(CacheDirectives.`no-cache`)
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(payloads.size)
          rooms.foreach(r => r.participants.find(user).value.activeRoom shouldBe true)
        }
      }
      verify(service, times(2)).getRooms(
        eq(user),
        eq(false),
        eq(false),
        eq(false),
        eq(None)
      )(
        eq(
          UserRequestContext(
            "requestId",
            Client(features = Set(ClientFeature.CHAT_BOT_CHECKUP_BUTTON)),
            user,
            user.filter(_.isDigit).toLong,
            CacheControl.Disallow,
            isInternal = false,
            TracedUtils.empty,
            idempotencyKey = None
          )
        )
      )
    }

    "provide sorted rooms for user" in {
      val now = new DateTime(2019, 2, 22, 0, 0, 0, 0)
      timeService.setNow(now)
      val user = userId.next
      val payloads = (1 to 10).map(_ => createRoomParameters(Some(roomId.next)).next.withUserId(user))
      val roomIds = payloads.map(_.id.get).toList
      withUserContext(user) { implicit rc =>
        payloads.foreach(p => {
          timeService.setNow(timeService.getNow.plusMinutes(1))
          service.createRoom(p).futureValue
        })
      }
      withUserContext(user) { implicit rc =>
        roomIds.reverse.foreach(roomId => {
          timeService.setNow(timeService.getNow.plusMinutes(1))
          val message = sendMessageParameters(roomId, user).next
          service.sendMessage(message).futureValue
        })
      }
      checkRoomsListing(
        user,
        roomIds.reverse,
        s"$root?user_id=$user&sort_by=updated-asc"
      )
      checkRoomsListing(
        user,
        roomIds,
        s"$root?user_id=$user&sort_by=updated-desc"
      )
      checkRoomsListing(user, roomIds, s"$root?user_id=$user")
      checkRoomsListing(
        user,
        roomIds,
        s"$root?user_id=$user&sort_by=created-asc"
      )
      checkRoomsListing(
        user,
        roomIds.reverse,
        s"$root?user_id=$user&sort_by=created-desc"
      )
    }

    "provide only unread rooms for user" in {
      val user = userId.next
      val user2 = userId.next
      val payloads = createRoomParameters
        .next(10)
        .map(_.withoutAllUsers.withUserId(user).withUserId(user2))
      val rooms: Iterable[Room] = withUserContext(user) { rc =>
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
          withUserContext(user) { implicit rc =>
            service.markRead(room.id, user)(rc).futureValue
          }
        })

      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(10)
        }
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user&only_unread=true")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(5)
        }
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user&page_size=2&only_unread=true")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val roomsPage = responseAs[RoomsPage]
          val rooms = roomsPage.rooms
          rooms.size should be(2)
        }
      }
    }

    "provide hidden rooms for user" in {
      val user = userId.next
      val payloads = createRoomParameters.next(10).map(_.withUserId(user))
      val rooms: Iterable[Room] = withUserContext(user) { rc =>
        payloads.map(service.createRoom(_)(rc).futureValue)
      }
      withUserContext(user) { rc =>
        rooms.foreach(r => service.setActive(r.id, user, active = false)(rc).futureValue)
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(0)
        }
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user&include_hidden=true")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(payloads.size)
          rooms.foreach(r => r.participants.find(user).value.activeRoom shouldBe false)
        }
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root?user_id=$user&page_size=5&include_hidden=true")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val roomsPage = responseAs[RoomsPage]
          val rooms = roomsPage.rooms
          rooms.size should be(5)
          rooms.foreach(r => r.participants.find(user).value.activeRoom shouldBe false)
        }
      }
    }
  }

  private def checkRoomsListing(user: UserId, expectRoomIds: Seq[String], request: String): Unit = {
    supportedMediaTypes.foreach { mediaType =>
      Get(request)
        .accepting(mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val rooms = responseAs[Seq[Room]].toList
        expectRoomIds.zip(rooms).foreach {
          case (roomId, room) => room.id should be(roomId)
        }
      }
    }
  }

  s"GET ${root}by-id" should {
    "provide listed rooms" in {
      val user = userId.next
      val payloads = createRoomParameters.next(10).map(_.withUserId(user))
      val rooms: Iterable[Room] = withUserContext(user) { rc =>
        payloads.map(service.createRoom(_)(rc).futureValue)
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"${root}by-id?${rooms.map(_.id).map("id=" + _).mkString("&")}")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val rooms = responseAs[Seq[Room]]
          rooms.size should be(payloads.size)
        }
      }
    }
    "still return 200, when no rooms were found" in {
      val user = userId.next
      supportedMediaTypes.foreach { mediaType =>
        Get(s"${root}by-id?id=${RandomIdGenerator.generate()}")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Seq[Room]].size should be(0)
        }
      }
    }
    "return 400, when no parameters were passed" in {
      val user = userId.next
      supportedMediaTypes.foreach { mediaType =>
        Get(s"${root}by-id")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.BadRequest)
        }
      }
    }
  }

  s"GET ${root}unread" should {
    "provide unread rooms count for user" in {
      val user = userId.next
      supportedMediaTypes.foreach { mediaType =>
        Get(s"${root}unread?user_id=$user")
          .accepting(mediaType)
          .withSomePassportUser
          .withUser(user) ~> route ~> check {
          status should be(StatusCodes.OK)
          val count = responseAs[Int]
          count should be(0)
        }
      }
    }
  }

  s"PUT $root<id>" should {
    "report 404 if no room specified" in {
      Put(root) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "update room" in {
      val create = createRoomParameters.next
      val user = create.participants.userIds.head
      val roomId = withUserContext(user) { rc =>
        service.createRoom(create)(rc).futureValue.id
      }
      val update = updateRoomParameters.next
      Put(s"$root$roomId", update)
        .accepting(Protobuf.mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val room = withUserContext(user) { rc =>
          service.getRoom(roomId)(rc).futureValue
        }
        room.participants.users should contain allElementsOf update.users.add
        if (update.users.remove.nonEmpty) {
          room.participants.users should not contain allElementsOf(
            update.users.remove
          )
        }
      }
    }
  }

  s"PUT $root<id>/active" should {
    "change status of room from user's point of view" in {
      val create = createRoomParameters.next
      val room = withUserContext(create.participants.userIds.head) { rc =>
        service.createRoom(create)(rc).futureValue
      }
      val roomId = room.id
      val user = anyParticipant(room).next
      Put(s"$root$roomId/active?user_id=$user&value=false")
        .accepting(Protobuf.mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val rooms = withUserContext(user) { rc =>
          service.getRooms(user)(rc).futureValue
        }
        rooms.forall(_.id != roomId) shouldBe true
      }
    }
  }

  s"PUT $root<id>/mute" should {
    "mute and unmute this chat room for the given user" in {
      val create = createRoomParameters.next
      val room = withUserContext(create.participants.userIds.head) { rc =>
        service.createRoom(create)(rc).futureValue
      }
      val roomId = room.id
      val user = anyParticipant(room).next
      Put(s"$root$roomId/mute?user_id=$user&value=true")
        .accepting(Protobuf.mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val room = withUserContext(user) { rc =>
          service.getRoom(roomId)(rc).futureValue
        }
        room.participants.find(user).value.muted shouldBe true
      }
      Put(s"$root$roomId/mute?user_id=$user&value=false")
        .accepting(Protobuf.mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val room = withUserContext(user) { rc =>
          service.getRoom(roomId)(rc).futureValue
        }
        room.participants.find(user).value.muted shouldBe false
      }
    }
  }

  s"PUT $root<id>/block" should {
    "block and unblock this chat room by given user" in {
      val create = createRoomParameters.next
      val room = withUserContext(create.participants.userIds.head) { rc =>
        service.createRoom(create)(rc).futureValue
      }
      val roomId = room.id
      val user = anyParticipant(room).next
      Put(s"$root$roomId/block?user_id=$user&value=true")
        .accepting(Protobuf.mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val room = withUserContext(user) { rc =>
          service.getRoom(roomId)(rc).futureValue
        }
        room.participants.find(user).value.blocked shouldBe true
      }
      Put(s"$root$roomId/block?user_id=$user&value=false")
        .accepting(Protobuf.mediaType)
        .withSomePassportUser
        .withUser(user) ~> route ~> check {
        status should be(StatusCodes.OK)
        val room = withUserContext(user) { rc =>
          service.getRoom(roomId)(rc).futureValue
        }
        room.participants.find(user).value.blocked shouldBe false
      }
    }
  }

  s"DELETE $root<id>" should {
    "respond 200 OK" in {
      Delete(s"$root${roomId.next}").withSomeUser.withSomePassportUser ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "remove room in" in {
      val room = roomId.next
      val payload = createRoomParameters(Some(room)).next
      withUserContext(payload.participants.userIds.head) { rc =>
        service.createRoom(payload)(rc).futureValue
      }

      Delete(s"$root$room").withSomePassportUser.withSomeUser ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      Get(s"$root$room").withSomePassportUser.withSomeUser ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

  s"PUT ${root}exists" should {
    "return room not exists for RoomLocator.Direct" in {
      val room = roomId.next
      val payload = createRoomParameters(Some(room)).next
      withUserContext(payload.participants.userIds.head) { rc =>
        service.createRoom(payload)(rc).futureValue
      }

      val roomLocator: RoomLocator = RoomLocator.Direct(room)

      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}exists", roomLocator)
          .accepting(mediaType)
          .withSomePassportUser
          .withSomeUser ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(true)
        }
      }
    }

    "return room exists for RoomLocator.Direct" in {
      val room = roomId.next
      val roomLocator: RoomLocator = RoomLocator.Direct(room)

      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}exists", roomLocator)
          .accepting(mediaType)
          .withSomePassportUser
          .withSomeUser ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(false)
        }
      }
    }

    "return room not exists for RoomLocator.Source without id" in {
      val room = roomId.next
      val source = createRoomParameters(Some(room)).next
      withUserContext(source.participants.userIds.head) { rc =>
        service.createRoom(source)(rc).futureValue
      }

      val roomLocator: RoomLocator = RoomLocator.Source(source.copy(id = None))

      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}exists", roomLocator)
          .accepting(mediaType)
          .withSomePassportUser
          .withSomeUser ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(false)
        }
      }
    }

    "return room not exists for RoomLocator.Source with id" in {
      val room = roomId.next
      val source = createRoomParameters(Some(room)).next

      val roomLocator: RoomLocator = RoomLocator.Source(source)

      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}exists", roomLocator)
          .accepting(mediaType)
          .withSomePassportUser
          .withSomeUser ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(false)
        }
      }
    }

    "return room exists for RoomLocator.Source with id" in {
      val room = roomId.next
      val source = createRoomParameters(Some(room)).next
      withUserContext(source.participants.userIds.head) { rc =>
        service.createRoom(source)(rc).futureValue
      }

      val roomLocator: RoomLocator = RoomLocator.Source(source)

      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}exists", roomLocator)
          .accepting(mediaType)
          .withSomePassportUser
          .withSomeUser ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(true)
        }
      }
    }
  }
}
