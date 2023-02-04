package ru.auto.api.managers.chat

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.GeneratorUtils
import ru.auto.api.RequestModel.CreateRoomRequest
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.testkit.TestData
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.chat.model.api.{ApiModel => ChatApiModel}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-19.
  */
class ChatBotUserSourceSpec
  extends AnyFunSuite
  with MockitoSupport
  with ScalaFutures
  with TestRequestWithId
  with GeneratorUtils {

  private val offerLoader = mock[EnrichedOfferLoader]
  private val passportClient = mock[PassportClient]
  private val botInfo = TestData.chatBotInfo

  implicit private val resolvingContext = new ResolvingContextImpl(offerLoader, passportClient, botInfo)

  test("resolving") {
    val userEssentials = UserEssentials.newBuilder().build()
    when(passportClient.getUserEssentials(?, ?)(?)).thenReturn(Future.successful(userEssentials))
    val botUser = ChatApiModel.User
      .newBuilder()
      .setId("chatbot:vibiralshik")
      .build()
    val chatUser = ChatApiModel.User
      .newBuilder()
      .setId("user:12345")
      .build()
    val userSource = ChatBotUserSource(Set(botUser, chatUser), Some(chatUser))

    val users = userSource.resolve(resolvingContext).futureValue
    val bot = users.all.find(user => ChatUserRef.parse(user.getId).isChatBot).get
    val images = bot.getProfile.getUserpic.getSizesMap.asScala
    val title = bot.getDescription
    val alias = bot.getProfile.getAlias

    assert(images.nonEmpty)
    assert(title.nonEmpty)
    assert(alias.nonEmpty)
  }

  test("from room") {
    val userEssentials = UserEssentials.newBuilder().build()
    when(passportClient.getUserEssentials(?, ?)(?)).thenReturn(Future.successful(userEssentials))
    val botUser = ChatApiModel.User
      .newBuilder()
      .setId("chatbot:vibiralshik")
      .build()
    val chatUser = ChatApiModel.User
      .newBuilder()
      .setId("user:12345")
      .build()

    val room = ChatApiModel.Room
      .newBuilder()
      .addUsers(botUser)
      .addUsers(chatUser)
      .build()

    val userSource = ChatBotUserSource.fromRoom(room)

    val users = userSource.resolve(resolvingContext).futureValue
    val bot = users.all.find(user => ChatUserRef.parse(user.getId).isChatBot).get
    val images = bot.getProfile.getUserpic.getSizesMap.asScala
    val title = bot.getDescription
    val alias = bot.getProfile.getAlias

    assert(images.nonEmpty)
    assert(title.nonEmpty)
    assert(alias.nonEmpty)
  }

  test("from request") {
    val userEssentials = UserEssentials.newBuilder().build()
    val request = CreateRoomRequest
      .newBuilder()
      .addUsers("chatbot:vibiralshik")
      .addUsers("user:12345")
      .build()

    when(passportClient.getUserEssentials(?, ?)(?)).thenReturn(Future.successful(userEssentials))
    val userSource = ChatBotUserSource.fromCreateRequest(request)

    val users = userSource.resolve(resolvingContext).futureValue
    val bot = users.all.find(user => ChatUserRef.parse(user.getId).isChatBot).get
    val images = bot.getProfile.getUserpic.getSizesMap.asScala
    val title = bot.getDescription
    val alias = bot.getProfile.getAlias

    assert(images.nonEmpty)
    assert(title.nonEmpty)
    assert(alias.nonEmpty)
  }

}
