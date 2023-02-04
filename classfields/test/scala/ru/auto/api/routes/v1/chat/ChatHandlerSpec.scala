package ru.auto.api.routes.v1.chat

import akka.http.scaladsl.model.headers.{Accept, RawHeader}
import akka.http.scaladsl.model.{HttpHeader, MediaTypes, StatusCodes, Uri}
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel._
import ru.auto.api.managers.chat.{ChatManager, ChatUserRef}
import ru.auto.api.managers.chat.ChatManager._
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.ChatApiGenerators._
import ru.auto.api.routes.v1.chat.ChatHandlerSpec._
import ru.auto.api.services.MockedClients
import ru.auto.api.util.ManagerUtils
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Specs on [[ChatHandler]].
  *
  * @author dimas
  */
class ChatHandlerSpec extends ApiSpec with MockedClients with BeforeAndAfter {

  override lazy val passportManager: PassportManager = mock[PassportManager]
  //for prevent resolving registered user

  before {
    when(passportManager.getClientId(?)(?)).thenReturn(None)
  }

  override lazy val chatManager: ChatManager = mock[ChatManager]

  after {
    verifyNoMoreInteractions(chatManager)
    reset(chatManager)
  }

  "/chat/message" should {
    val base = "/1.0/chat/message"

    "send message by POST" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val request = sendMessageRequest.next
      val message = chatMessage.next

      when(chatManager.sendMessage(eq(user), eq(request))(?))
        .thenReturn(toMessageResponse(message))

      SupportedAccepts.foreach { accept =>
        Post(base, request)
          .withHeaders(accept, testAuthorizationHeader, userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[MessageResponse].getMessage shouldBe message
          }
      }

      verify(chatManager, times(SupportedAccepts.length)).sendMessage(eq(user), eq(request))(?)
    }

    "send message by dealer" in {
      val chatUser = ChatUserRef.from(PrivateUserRefGen.next)
      val dealer = DealerUserRefGen.next
      val chatDealer = ChatUserRef.from(dealer)
      val request = sendMessageRequest.next
      val message = chatMessage.next

      when(passportManager.getClientId(?)(?)).thenReturn(Some(dealer))

      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)

      when(chatManager.sendMessage(eq(chatDealer), eq(request))(?))
        .thenReturn(toMessageResponse(message))

      SupportedAccepts.foreach { accept =>
        Post(base, request)
          .withHeaders(accept, testAuthorizationHeader, userHeader(chatUser)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[MessageResponse].getMessage shouldBe message
          }
      }

      verify(chatManager, times(SupportedAccepts.length)).sendMessage(eq(chatDealer), eq(request))(?)
    }

    "retrieve messages by GET" in {
      val room = roomId.next
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val slice = window(messageId).next
      val messages = chatMessage.list.take(10)

      when(chatManager.getMessages(eq(room), eq(user), eq(slice))(?))
        .thenReturn(toMessageListingResponse(messages))

      SupportedAccepts.foreach { accept =>
        val query = Uri.Query(
          "room_id" -> room.value,
          "from" -> slice.from.value,
          "count" -> slice.count.toString,
          "asc" -> slice.asc.toString
        )
        Get(Uri(base).withQuery(query)) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[MessageListingResponse].getMessagesList.asScala shouldBe messages
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).getMessages(eq(room), eq(user), eq(slice))(?)
    }
  }

  "/chat/event" should {
    val base = "/1.0/chat/event"

    "send event by POST" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val event = eventRequest.next

      when(chatManager.handleChatEvent(eq(event))(?))
        .thenReturn(ManagerUtils.SuccessResponse)

      SupportedAccepts.foreach { accept =>
        Post(base, event).withHeaders(accept, testAuthorizationHeader, userHeader(user)) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse
        }
      }
      verify(chatManager, times(SupportedAccepts.length)).handleChatEvent(eq(event))(?)
    }
  }

  "/chat/tech-support" should {
    val base = "/1.0/chat/tech-support"

    "send tech support poll" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val hash = "hash"
      val rating = 1
      val response = TechSupportPollResponse.newBuilder().setRatingSaved(true).setStatus(ResponseStatus.SUCCESS).build()
      when(chatManager.techSupportPoll(?, ?, ?)(?)).thenReturn(response)
      SupportedAccepts.foreach { accept =>
        Put(Uri(base + s"/poll/$hash?rating=1")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[TechSupportPollResponse] shouldBe response
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).techSupportPoll(eq(user), eq(hash), eq(rating))(?)
    }
  }

  "/chat/bot" should {
    val base = "/1.0/chat/bot"

    "start new chatbot checkup" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val response = RoomResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
      val offerLink = "https://auto.ru/cars/used/sale/skoda/octavia/1085376352-0face745/"
      when(chatManager.startNewChatBotCheckUp(?)(?)).thenReturn(response)
      SupportedAccepts.foreach { accept =>
        Get(Uri(base + "/vibiralshik/start-checkup?offer-link=" + offerLink)) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RoomResponse] shouldBe response
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).startNewChatBotCheckUp(eq(offerLink))(any())
    }
  }

  "/chat/room" should {
    val base = "/1.0/chat/room"

    "mute chat for user" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val room = roomId.next
      when(chatManager.setRoomMuted(eq(room), eq(user), eq(true))(any())).thenReturn(ManagerUtils.SuccessResponse)
      SupportedAccepts.foreach { accept =>
        Put(Uri(base + "/" + room.value + "/mute")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).setRoomMuted(eq(room), eq(user), eq(true))(any())
    }

    "unmute chat for user" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val room = roomId.next
      when(chatManager.setRoomMuted(eq(room), eq(user), eq(false))(any())).thenReturn(ManagerUtils.SuccessResponse)
      SupportedAccepts.foreach { accept =>
        Put(Uri(base + "/" + room.value + "/unmute")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).setRoomMuted(eq(room), eq(user), eq(false))(any())
    }

    "block chat by user" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val room = roomId.next
      when(chatManager.setRoomBlocked(eq(room), eq(user), eq(true))(any())).thenReturn(ManagerUtils.SuccessResponse)
      SupportedAccepts.foreach { accept =>
        Put(Uri(base + "/" + room.value + "/block")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).setRoomBlocked(eq(room), eq(user), eq(true))(any())
    }

    "unblock chat by user" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val room = roomId.next
      when(chatManager.setRoomBlocked(eq(room), eq(user), eq(false))(any())).thenReturn(ManagerUtils.SuccessResponse)
      SupportedAccepts.foreach { accept =>
        Put(Uri(base + "/" + room.value + "/unblock")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).setRoomBlocked(eq(room), eq(user), eq(false))(any())
    }

    "check if room with given params exists" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val request = requestRoomLocator.next
      val response = RoomExistsResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).setRoomExists(true).build()
      when(chatManager.checkRoomExists(?)(?)).thenReturn(response)
      SupportedAccepts.foreach { accept =>
        Put(Uri(base + "/check-exists"), request) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RoomExistsResponse] shouldBe response
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).checkRoomExists(eq(request))(any())
    }

    "return tech support room" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val response = RoomResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
      when(chatManager.getTechSupportRoom(?)).thenReturn(response)
      SupportedAccepts.foreach { accept =>
        Get(Uri(base + "/tech-support")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RoomResponse] shouldBe response
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).getTechSupportRoom(any())
    }

    "return list of light rooms" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val response = RoomListingResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
      when(chatManager.getLightRooms(?)(?)).thenReturn(response)
      SupportedAccepts.foreach { accept =>
        Get(Uri(base + "/light")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RoomListingResponse] shouldBe response
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).getLightRooms(eq(user))(any())
    }

    "return list of enriched rooms by ids" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val r = roomId.next
      val response = RoomListingResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
      when(chatManager.getRoomsByIds(?, ?)(?)).thenReturn(response)
      SupportedAccepts.foreach { accept =>
        Get(Uri(base + s"/by-id?id=${r.value}")) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RoomListingResponse] shouldBe response
          }
      }
      verify(chatManager, times(SupportedAccepts.length)).getRoomsByIds(eq(user), eq(Iterable(r)))(any())
    }
  }

}

object ChatHandlerSpec {
  implicit def asFuture[A](value: A): Future[A] = Future.successful(value)

  val AcceptJson = Accept(MediaTypes.`application/json`)
  val AcceptProtobuf = Accept(Protobuf.mediaType)

  val SupportedAccepts = Seq(AcceptJson, AcceptProtobuf)

  def userHeader(user: ChatUserRef): HttpHeader =
    RawHeader("x-uid", user.toRaw)

}
