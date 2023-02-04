package ru.yandex.realty.clients.chat

import org.junit.runner.RunWith
import org.scalamock.matchers.ArgCapture.CaptureAll
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OneInstancePerTest
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.chat.model.{ChatUserRef, MessageId, RoomId, Window}
import ru.yandex.realty.model.request.Page
import ru.yandex.realty.request.{ImplicitRequest, Request}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.model.api.ApiModel.{Message, Room, RoomsPage}
import ru.yandex.vertis.paging.Paging

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FilteredChatClientSpec extends AsyncSpecBase with OneInstancePerTest with ImplicitRequest {

  class MyClientBase extends ChatClient {
    val windowArg = CaptureAll[Window[MessageId]]()

    override def getMessages(
      roomId: RoomId,
      window: Window[MessageId],
      includeSpam: Option[Boolean]
    )(
      implicit context: ChatRequestContext
    ): Future[Seq[Message]] = {
      windowArg.value = window
      val start = window.from.get.toInt
      val ids: Seq[Int] = if (window.asc) {
        val end = (start + window.count - 1) min 10 max 1
        (start to end).toSeq
      } else {
        val end = (start - window.count + 1) min 10 max 1
        (end to start)
      }
      Future.successful(ids.map(id => Message.newBuilder().setId(id.toString).build()))
    }

    override def getRoom(roomId: RoomId)(implicit context: ChatRequestContext): Future[Room] =
      Future.successful(Room.newBuilder().setLastMessage(Message.newBuilder().setId("6")).build())

    override def getRooms(
      user: ChatUserRef,
      slice: Page,
      sortBy: String
    )(implicit context: ChatRequestContext): Future[RoomsPage] =
      Future.successful(
        RoomsPage
          .newBuilder()
          .addRooms(Room.newBuilder().setLastMessage(Message.newBuilder().setId("6")))
          .setPaging(Paging.newBuilder().setTotal(1))
          .build()
      )

    override def getRoomsAll(user: ChatUserRef)(implicit context: ChatRequestContext): Future[Seq[Room]] =
      Future.successful(
        Seq(
          Room.newBuilder().setLastMessage(Message.newBuilder().setId("6")).build()
        )
      )

    override def hasUnreadMessages(user: ChatUserRef)(implicit t: Traced, r: Request): Future[Boolean] = ???
    override def sendMessage(message: ApiModel.CreateMessageParameters)(
      implicit context: ChatRequestContext
    ): Future[Message] = ???
    override def markMessagesRead(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def getUnreadRoomsCount(user: ChatUserRef)(implicit t: Traced, r: Request): Future[Long] = ???
    override def createRoom(
      room: ApiModel.CreateRoomParameters
    )(implicit t: Traced, r: Request): Future[Room] = ???
    override def updateRoom(roomId: RoomId, room: ApiModel.UpdateRoomParameters)(
      implicit context: ChatRequestContext
    ): Future[Room] = ???
    override def checkRoomExists(roomLocator: ApiModel.RoomLocator)(implicit t: Traced, r: Request): Future[Boolean] =
      ???
    override def removeRoom(roomId: RoomId)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def openRoom(roomId: RoomId, user: ChatUserRef)(implicit context: ChatRequestContext): Future[Unit] = ???
    override def setRoomActive(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def setRoomInactive(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def setRoomMuted(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def setRoomUnmuted(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def setRoomBlocked(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def setRoomUnblocked(roomId: RoomId, user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def getTechSupportRoom(implicit context: ChatRequestContext): Future[Room] = ???
    override def techSupportPoll(hash: String, rating: Int)(implicit t: Traced, r: Request): Future[Boolean] = ???
    override def isUserBanned(user: ChatUserRef)(implicit t: Traced, r: Request): Future[Boolean] = ???
    override def banUser(user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
    override def unbanUser(user: ChatUserRef)(implicit t: Traced, r: Request): Future[Unit] = ???
  }

  private val roomId: RoomId = RoomId("1")

  implicit private val crc: ChatRequestContext = null

  private def mkClient(isCommandMessageId: String => Boolean): MyClientBase with FilteredChatClient =
    new MyClientBase with FilteredChatClient with ExecutionContextProviderFromContext {
      override def isCommandMessage(msg: Message): Boolean = isCommandMessageId(msg.getId)
    }

  "ChatClient mock" should {

    val chatClient = new MyClientBase

    "ascend correctly" in {
      chatClient
        .getMessages(roomId, Window(Some("1"), count = 2, asc = true))
        .futureValue
        .map(_.getId) shouldBe Seq("1", "2")
    }

    "descend correctly" in {
      chatClient
        .getMessages(roomId, Window(Some("2"), count = 2, asc = false))
        .futureValue
        .map(_.getId) shouldBe Seq("1", "2")
    }

  }

  "FilteredChatClient.getMessages" should {

    "load one page asc" in {
      val client = mkClient((_: String) => false)
      client.getMessages(roomId, Window(Some("1"), 3, asc = true), None).futureValue.map(_.getId) shouldBe
        Seq("1", "2", "3")
      client.windowArg.values.size shouldBe 1
    }

    "load one page desc" in {
      val client = mkClient((_: String) => false)
      client.getMessages(roomId, Window(Some("3"), 3, asc = false), None).futureValue.map(_.getId) shouldBe
        Seq("1", "2", "3")
      client.windowArg.values.size shouldBe 1
    }

    "load two pages asc" in {
      val client = mkClient((_: String) == "2")
      client.getMessages(roomId, Window(Some("1"), 3, asc = true), None).futureValue.map(_.getId) shouldBe
        Seq("1", "3", "4")
      client.windowArg.values.map(_.from) shouldBe Seq(Some("1"), Some("3"))
      client.windowArg.values.map(_.count) shouldBe Seq(3, 2)
    }

    "load two pages desc" in {
      val client = mkClient((_: String) == "2")
      client.getMessages(roomId, Window(Some("4"), 3, asc = false), None).futureValue.map(_.getId) shouldBe
        Seq("1", "3", "4")
      client.windowArg.values.map(_.from) shouldBe Seq(Some("4"), Some("2"))
      client.windowArg.values.map(_.count) shouldBe Seq(3, 2)
    }

    "load many pages asc" in {
      val client = mkClient((_: String).toInt > 2)
      client.getMessages(roomId, Window(Some("1"), 5, asc = true), None).futureValue.map(_.getId) shouldBe
        Seq("1", "2")
      client.windowArg.values.map(_.from) shouldBe Seq(Some("1"), Some("5"), Some("8"), Some("10"))
      client.windowArg.values.map(_.count) shouldBe Seq(5, 4, 4, 4)
    }

    "load many pages desc" in {
      val client = mkClient((_: String).toInt > 2)
      client.getMessages(roomId, Window(Some("10"), 5, asc = false), None).futureValue.map(_.getId) shouldBe
        Seq("1", "2")
      client.windowArg.values.map(_.from) shouldBe Seq(Some("10"), Some("6"), Some("1"))
      client.windowArg.values.map(_.count) shouldBe Seq(5, 6, 4)
    }

  }

  "FilteredChatClient.getRoom" should {

    "leave the last message if it is not a command" in {
      val client = mkClient((_: String) => false)
      client.getRoom(roomId).futureValue.getLastMessage.getId shouldBe "6"
      client.windowArg.values.size shouldBe 0
    }

    "update the last message if it is filtered" in {
      val client = mkClient((_: String) == "6")
      client.getRoom(roomId).futureValue.getLastMessage.getId shouldBe "5"
      client.windowArg.values shouldBe Seq(
        Window(from = Some("6"), count = 2, asc = false)
      )
    }

    "be persistent when looking for unfiltered messages" in {
      val client = mkClient((_: String).toInt > 2)
      client.getRoom(roomId).futureValue.getLastMessage.getId shouldBe "2"
      client.windowArg.values shouldBe Seq(
        Window(from = Some("6"), count = 2, asc = false),
        Window(from = Some("5"), count = 2, asc = false),
        Window(from = Some("4"), count = 2, asc = false),
        Window(from = Some("3"), count = 2, asc = false)
      )
    }

    "fall back on what it got initially when there are no good messages" in {
      val client = mkClient((_: String) => true)
      client.getRoom(roomId).futureValue.getLastMessage.getId shouldBe "6"
      client.windowArg.values shouldBe Seq(
        Window(from = Some("6"), count = 2, asc = false),
        Window(from = Some("5"), count = 2, asc = false),
        Window(from = Some("4"), count = 2, asc = false),
        Window(from = Some("3"), count = 2, asc = false),
        Window(from = Some("2"), count = 2, asc = false),
        Window(from = Some("1"), count = 2, asc = false)
      )
    }

  }

  "FilteredChatClient.getRooms" should {

    "update filtered messages" in {
      val client = mkClient((_: String).toInt > 5)
      val roomsPage = client.getRooms(null, null, null).futureValue
      roomsPage.getRooms(0).getLastMessage.getId shouldBe "5"
      client.windowArg.values shouldBe Seq(
        Window(from = Some("6"), count = 2, asc = false)
      )
      // check that other fields in the response are preserved
      roomsPage.getPaging.getTotal shouldBe 1
    }

  }

  "FilteredChatClient.getRoomsAll" should {

    "update filtered messages" in {
      val client = mkClient((_: String).toInt > 5)
      val room = client.getRoomsAll(null).futureValue.head.getLastMessage.getId shouldBe "5"
      client.windowArg.values shouldBe Seq(
        Window(from = Some("6"), count = 2, asc = false)
      )
    }

  }

}
