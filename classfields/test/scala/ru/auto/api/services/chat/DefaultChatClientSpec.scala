package ru.auto.api.services.chat

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.BoolValue
import org.apache.http.message.BasicHeader
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.RequestModel.{AddChatAggregatorRequest, CreateChatAggregatorRequest}
import ru.auto.api.ResponseModel.ChatAggregatorResponse
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{ChatRoomNotFound, ChatSecurityException, TooManyRequestsException}
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.chat.AggregatorType
import ru.auto.api.model.chat.MessageId
import ru.auto.api.model.gen.BasicGenerators._
import ru.auto.api.model.gen.ChatApiGenerators.{attachments, banScope, roomLocator, _}
import ru.auto.api.model.{RequestParams, UserRef, Window}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.chat.model.api.ApiModel.{AggregatorInfo, AggregatorInstallRequest, MessageProperties}
import ru.yandex.vertis.tracing.Traced

/**
  * Specs on [[DefaultChatClient]].
  *
  * @author dimas
  */
class DefaultChatClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {

  val client = new DefaultChatClient(http)

  implicit private val clientFeatures: Set[ClientFeature] = Set(ClientFeature.CHAT_BOT, ClientFeature.SEARCH_SPECIALS)

  implicit override def request: Request = super.request

  import DefaultChatClient._

  "ChatClient" should {
    "create chat room" in {
      forAll(set(0, 10, readableString), readableString, properties, room) { (users, id, properties, result) =>
        http.expectUrl(POST, s"$UriBase/rooms")
        http.expectProto(createRoomParameters(id, users, properties))

        http.respondWith(StatusCodes.OK, result)

        val callResult = client.createRoom(id, users, properties).futureValue
        callResult shouldBe result
      }
    }

    "get chat rooms" in {
      forAll(readableString, list(0, 10, room)) { (user, result) =>
        http.expectUrl(GET, s"$UriBase/rooms?$UserIdParameter=$user&only_nonempty=true")

        http.respondWithMany(StatusCodes.OK, result)

        val callResult = client.getRooms(user).futureValue
        callResult shouldBe result
      }
    }

    "get chat rooms with chat bot support" in {
      forAll(readableString, list(0, 10, room)) { (user, result) =>
        http.expectUrl(GET, s"$UriBase/rooms?$UserIdParameter=$user&only_nonempty=true")
        http.expectHeader("X-Features", "chat_bot,search_specials")

        http.respondWithMany(StatusCodes.OK, result)

        val callResult = client.getRooms(user).futureValue
        callResult shouldBe result
      }
    }

    "get chat room" in {
      forAll(readableString, room) { (roomId, result) =>
        http.expectUrl(GET, s"$UriBase/rooms/$roomId")

        http.respondWith(StatusCodes.OK, result)

        val callResult = client.getRoom(roomId).futureValue
        callResult shouldBe result
      }
    }

    "get chat rooms by ids" in {
      forAll(list(1, 10, room)) { result =>
        http.expectUrl(GET, s"$UriBase/rooms/by-id?${result.map(_.getId).map("id=" + _).mkString("&")}")

        http.respondWithMany(StatusCodes.OK, result)

        val callResult = client.getRoomsByIds(result.map(_.getId)).futureValue
        callResult shouldBe result
      }
    }

    "get tech_support room" in {
      forAll(room) { result =>
        http.expectUrl(GET, s"$UriBase/techSupport")
        http.expectHeader("X-User-ID", request.user.ref.toPlain)

        http.respondWith(StatusCodes.OK, result)

        val callResult = client.getTechSupportRoom.futureValue
        callResult shouldBe result
      }
    }

    "get chat bot room" in {
      forAll(room) { result =>
        http.expectUrl(GET, s"$UriBase/rooms/service/chat_bot_vibiralshik")
        http.expectHeader("X-User-ID", request.user.ref.toPlain)

        http.respondWith(StatusCodes.OK, result)

        val callResult = client.getChatBotRoom.futureValue
        callResult shouldBe result
      }
    }

    "update chat room" in {
      forAll(readableString, usersPatch) { (id, users) =>
        http.expectUrl(PUT, s"$UriBase/rooms/$id")
        http.expectProto(updateRoomParameters(users))

        http.respondWithStatus(StatusCodes.OK)

        client.updateRoom(id, users).futureValue
      }
    }

    "remove chat room" in {
      forAll(readableString) { roomId =>
        http.expectUrl(DELETE, s"$UriBase/rooms/$roomId")

        http.respondWithStatus(StatusCodes.OK)

        client.removeRoom(roomId).futureValue
      }
    }

    "send message" in {
      forAll(readableString, messagePayload, attachments, roomLocator, Gen.option(readableString), chatMessage) {
        (user, payload, attachments, roomLocator, providedId, result) =>
          http.expectUrl(POST, s"$UriBase/messages")
          http.expectProto(
            createMessageParameters(user, roomLocator, payload, attachments, providedId.getOrElse(""), None)
          )

          http.respondWith(StatusCodes.OK, result)

          client.sendMessage(user, roomLocator, payload, attachments, providedId.getOrElse(""), None).futureValue
      }
    }

    "send message with non-empty provided-id" in {
      forAll(readableString, messagePayload, attachments, roomLocator, readableString, chatMessage) {
        (user, payload, attachments, roomLocator, providedId, result) =>
          http.expectHeader("X-Idempotency-Key", providedId)

          http.respondWith(StatusCodes.OK, result)

          client.sendMessage(user, roomLocator, payload, attachments, providedId, None).futureValue
      }
    }

    "send spam message" in {
      forAll(readableString, messagePayload, attachments, roomLocator, Gen.option(readableString), chatMessage) {
        (user, payload, attachments, roomLocator, providedId, result) =>
          http.expectUrl(POST, s"$UriBase/messages")
          http.expectProto(
            createMessageParameters(
              user,
              roomLocator,
              payload,
              attachments,
              providedId.getOrElse(""),
              None,
              isSpam = true
            )
          )

          http.respondWith(StatusCodes.OK, result)

          client
            .sendMessage(user, roomLocator, payload, attachments, providedId.getOrElse(""), None, isSpam = true)
            .futureValue
      }
    }

    "get messages" in {
      forAll(readableString, list(0, 100, chatMessage)) { (room, result) =>
        http.expectUrl(GET, s"$UriBase/messages?$RoomIdParameter=$room&from=&count=100&asc=true")

        http.respondWithMany(StatusCodes.OK, result)

        val callResult = client.getMessages(room, Window(MessageId.Default, 100, asc = true)).futureValue
        callResult shouldBe result
      }
    }

    "get messages as dealer" in {
      val dr = {
        val r = new RequestImpl
        r.setTrace(Traced.empty)
        r.setRequestParams(RequestParams.construct("1.1.1.1"))
        r.setApplication(Application.swagger)
        r.setUser(UserRef.user(17475737))
        r.setDealer(UserRef.dealer(22363))
        r
      }
      forAll(readableString, list(0, 100, chatMessage)) { (room, result) =>
        http.expectUrl(GET, s"$UriBase/messages?$RoomIdParameter=$room&from=&count=100&asc=true")
        http.expectHeader(new BasicHeader("X-User-ID", "dealer:22363"))
        http.expectHeader(new BasicHeader("X-Passport-User-ID", "17475737"))

        http.respondWithMany(StatusCodes.OK, result)

        val callResult = client.getMessages(room, Window(MessageId.Default, 100, asc = true))(dr, Set()).futureValue
        callResult shouldBe result
      }
    }

    "get unread info" in {
      forAll(readableString, bool) { (user, hasUnread) =>
        http.expectUrl(GET, s"$UriBase/messages/unread?$UserIdParameter=$user")

        http.respondWith(
          StatusCodes.OK,
          BoolValue
            .newBuilder()
            .setValue(hasUnread)
            .build
        )

        val callResult = client.hasUnreadMessages(user).futureValue
        callResult shouldBe hasUnread
      }
    }

    "mark chat room read" in {
      forAll(readableString, readableString) { (room, user) =>
        http.expectUrl(DELETE, s"$UriBase/messages/unread?$RoomIdParameter=$room&$UserIdParameter=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.markMessagesRead(room, user).futureValue
      }
    }

    "mark chat room as inactive" in {
      forAll(readableString, readableString) { (room, user) =>
        http.expectUrl(PUT, s"$UriBase/rooms/$room/active?$ValueParameter=false&$UserIdParameter=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.setRoomActive(room, user, value = false).futureValue
      }
    }

    "mute chat room for user" in {
      forAll(readableString, readableString) { (room, user) =>
        http.expectUrl(PUT, s"$UriBase/rooms/$room/mute?$ValueParameter=true&$UserIdParameter=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.setRoomMuted(room, user, value = true).futureValue
      }
    }

    "unmute chat room for user" in {
      forAll(readableString, readableString) { (room, user) =>
        http.expectUrl(PUT, s"$UriBase/rooms/$room/mute?$ValueParameter=false&$UserIdParameter=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.setRoomMuted(room, user, value = false).futureValue
      }
    }

    "block chat room by user" in {
      forAll(readableString, readableString) { (room, user) =>
        http.expectUrl(PUT, s"$UriBase/rooms/$room/block?$ValueParameter=true&$UserIdParameter=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.setRoomBlocked(room, user, value = true).futureValue
      }
    }

    "unblock chat room for user" in {
      forAll(readableString, readableString) { (room, user) =>
        http.expectUrl(PUT, s"$UriBase/rooms/$room/block?$ValueParameter=false&$UserIdParameter=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.setRoomBlocked(room, user, value = false).futureValue
      }
    }

    "get blocked users" in {
      forAll(list(0, 10, blockedUser)) { result =>
        http.expectUrl(GET, s"$UriBase/users/blocked?from=&count=10&asc=true")

        http.respondWithMany(StatusCodes.OK, result)

        val callResult = client.listBlocked(Window[String]("", 10, asc = true)).futureValue
        callResult shouldBe result
      }
    }

    "block user" in {
      forAll(readableString, blockContext) { (user, ctx) =>
        http.expectUrl(PUT, s"$UriBase/users/block")
        http.expectProto(blockUserParameters(user, ctx.toExternalContext))
        http.respondWithStatus(StatusCodes.OK)

        client.block(user, ctx.toExternalContext).futureValue
      }
    }

    "unblock user" in {
      forAll(readableString) { user =>
        http.expectUrl(DELETE, s"$UriBase/users/block?user_id=$user")
        http.respondWithStatus(StatusCodes.OK)

        client.unblock(user).futureValue
      }
    }

    "throw TooManyRequestException on 429 response" in {
      forAll(readableString, roomLocatorDirect, messagePayload, attachments, Gen.option(readableString)) {
        (user, roomLocator, payload, attachments, providedId) =>
          http.respondWith(StatusCodes.TooManyRequests, "")

          val failure =
            client
              .sendMessage(user, roomLocator, payload, attachments, providedId.getOrElse(""), None)
              .failed
              .futureValue
          failure shouldBe a[TooManyRequestsException]
      }
    }

    "throw ChatSecurityException on 403 response" in {
      forAll(readableString, roomLocatorDirect, messagePayload, attachments, Gen.option(readableString)) {
        (user, roomLocator, payload, attachments, providedId) =>
          http.respondWith(StatusCodes.Forbidden, "")

          val failure =
            client
              .sendMessage(user, roomLocator, payload, attachments, providedId.getOrElse(""), None)
              .failed
              .futureValue
          failure shouldBe a[ChatSecurityException]
      }
    }

    "throw ChatRoomnotFound on 404 response from sendMessage" in {
      forAll(readableString, roomLocatorDirect, messagePayload, attachments, Gen.option(readableString)) {
        (user, roomLocator, payload, attachments, providedId) =>
          http.respondWith(StatusCodes.NotFound, "")

          val failure =
            client
              .sendMessage(user, roomLocator, payload, attachments, providedId.getOrElse(""), None)
              .failed
              .futureValue
          failure shouldBe a[ChatRoomNotFound]
      }
    }

    "send tech support poll" in {
      forAll(readableString, readableString) { (user, hash) =>
        val rating = 1
        http.expectUrl(PUT, s"$UriBase/techSupport/poll/$hash?$RatingParameter=$rating")
        http.expectHeader("X-User-ID", request.user.ref.toPlain)
        http.expectHeader("X-Passport-User-ID", request.user.userRef.asPrivate.uid.toString)

        http.respondWith(
          StatusCodes.OK,
          BoolValue
            .newBuilder()
            .setValue(true)
            .build
        )

        val result = client.techSupportPoll(user, hash, rating).futureValue
        result shouldBe true
      }
    }

    "send message properties" in {
      forAll(readableString, messagePayload, attachments, roomLocator, Gen.option(readableString), chatMessage) {
        (user, payload, attachments, roomLocator, providedId, result) =>
          val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()
          http.expectUrl(POST, s"$UriBase/messages")
          http.expectProto(
            createMessageParameters(user, roomLocator, payload, attachments, providedId.getOrElse(""), Some(props))
          )

          http.respondWith(StatusCodes.OK, result)

          client.sendMessage(user, roomLocator, payload, attachments, providedId.getOrElse(""), Some(props)).futureValue
      }
    }

    "user typing to tech support" in {
      forAll(readableString) { user =>
        http.expectUrl(PUT, s"$UriBase/techSupport/userTyping?user_id=$user")

        http.respondWith(StatusCodes.OK, "")

        client.userTypingToTechSupport(user).futureValue
      }
    }

    "create aggregator" in {
      forAll(readableString, readableString) { (userId, channelName) =>
        val chatAggregatorRequest = AddChatAggregatorRequest.newBuilder
          .setChannelName(channelName)
          .build

        val aggregatorInfo = AggregatorInfo.newBuilder
          .setChannelName(channelName)
          .addUsers(userId)
          .build

        http.expectUrl(POST, s"$UriBase/aggregators/add")
        http.expectProto(aggregatorInfo)

        http.respondWithProto(StatusCodes.OK, aggregatorInfo)

        client.addAggregator(userId, chatAggregatorRequest).futureValue
      }
    }

    "create jivosite aggregator" in {
      val userId = readableString.next
      val channelName = readableString.next
      val hook = readableString.next
      val token = readableString.next
      val chatJivoSiteAggregatorRequest = CreateChatAggregatorRequest.newBuilder
        .setChannelName(channelName)
        .build

      val aggregatorInfo =
        AggregatorInfo.newBuilder().setChannelName(channelName).setHook(hook).setToken(token).build()

      val aggregatorInstallRequest = AggregatorInstallRequest.newBuilder
        .setChannelName(channelName)
        .addUsers(userId)
        .build

      val response = ChatAggregatorResponse.newBuilder
        .setChannelName(aggregatorInfo.getChannelName)
        .setToken(aggregatorInfo.getToken)
        .setHook(aggregatorInfo.getHook)
        .build()

      http.expectUrl(POST, s"$UriBase/aggregators/add/jivosite")
      http.expectProto(aggregatorInstallRequest)

      http.respondWithProto(StatusCodes.OK, aggregatorInfo)

      client.createAggregator(userId, AggregatorType.JivoSite, chatJivoSiteAggregatorRequest).futureValue shouldBe
        response
    }

    "get aggregator" in {
      forAll(readableString, readableString, readableString, readableString) { (userId, channelName, token, hook) =>
        val aggregatorInfo = {
          val b = AggregatorInfo.newBuilder
          b.setChannelName(channelName)
          b.setToken(token)
          b.setHook(hook)
          b.addUsers(userId)
          b.build
        }

        val chatAggregatorResponse = {
          val b = ChatAggregatorResponse.newBuilder
          b.setChannelName(channelName)
          b.setToken(token)
          b.setHook(hook)
          b.addUsers(userId)
          b.build
        }

        http.expectUrl(GET, s"$UriBase/aggregators?user=$userId")

        http.respondWithProto(StatusCodes.OK, aggregatorInfo)

        client.getAggregator(userId).futureValue shouldBe chatAggregatorResponse
      }
    }

    "delete aggregator" in {
      forAll(readableString) { user =>
        http.expectUrl(DELETE, s"$UriBase/aggregators?user=$user")

        http.respondWithStatus(StatusCodes.OK)

        client.deleteAggregator(user).futureValue
      }
    }

    "check user ban status" in {
      forAll(readableString, banScope, bool) { (user, scope, result) =>
        http.expectUrl(GET, s"$UriBase/users/$user/ban?scope=$scope")

        http.respondWithProto(StatusCodes.OK, BoolValue.of(result))

        client.getBan(user, scope).futureValue shouldBe result
      }
    }
  }
}
