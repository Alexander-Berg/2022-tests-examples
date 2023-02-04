package ru.auto.api.managers.chat

import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.chat.ChatModel.ChatUser
import ru.auto.api.managers.TestRequestWithId
import ru.yandex.passport.model.api.ApiModel.{ImageUrl, UserProfileLight}
import ru.yandex.vertis.mockito.MockitoSupport
import scala.jdk.CollectionConverters._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-19.
  */
class ChatBotRoomSpec extends AnyFunSuite with MockitoSupport with ScalaFutures with TestRequestWithId with Matchers {

  test("create ChatBotRoom") {
    val chatUser = ChatUser
      .newBuilder()
      .setId("user:12345")
      .build()

    val imageUrl = ImageUrl
      .newBuilder()
      .putSizes("image", "image")
      .build()

    val chatBotProfile = UserProfileLight
      .newBuilder()
      .setAlias("bot")
      .setUserpic(imageUrl)
      .build()

    val chatBot = ChatUser
      .newBuilder()
      .setId("chatbot:testbot")
      .setDescription("test")
      .setProfile(chatBotProfile)
      .build()

    val chatUsers = CleanChatUsers(Set(chatBot, chatUser), None)
    val chatRoom = ChatBotRoom("test", chatUsers)

    val room = chatRoom.toRoom

    room.getSubject.getImageMap.get("image") shouldBe "image"
    room.getUsersList.asScala.exists(_.getProfile.getAlias == "bot") shouldBe true
    room.getSubject.getTitle shouldBe "test"
    room.getSubject.getTitleV2 shouldBe "test"
  }

}
