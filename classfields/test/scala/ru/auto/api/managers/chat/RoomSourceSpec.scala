package ru.auto.api.managers.chat

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.{Category, Offer}
import ru.auto.api.GeneratorUtils
import ru.auto.api.RequestModel.CreateRoomRequest
import ru.auto.api.chat.ChatModel.{OfferSubjectSource, SubjectSource}
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.testkit.TestData
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-19.
  */
class RoomSourceSpec
  extends AnyFunSuite
  with MockitoSupport
  with ScalaFutures
  with TestRequestWithId
  with Matchers
  with GeneratorUtils {

  private val offerLoader = mock[EnrichedOfferLoader]
  private val passportClient = mock[PassportClient]
  private val botInfo = TestData.chatBotInfo

  implicit private val resolvingContext: ResolvingContextImpl =
    new ResolvingContextImpl(offerLoader, passportClient, botInfo)

  test("create ChatBotRoomSource") {
    val userEssentials = UserEssentials.newBuilder().build()
    when(passportClient.getUserEssentials(?, ?)(?)).thenReturn(Future.successful(userEssentials))

    val request = CreateRoomRequest
      .newBuilder()
      .addUsers("chatbot:vibiralshik")
      .addUsers("user:12345")
      .build()

    val roomSource = RoomSource.fromCreateRequest(request).futureValue

    roomSource shouldBe a[ChatBotRoomSource]
  }

  test("create SimpleRoomSource") {
    val userEssentials = UserEssentials.newBuilder().build()
    when(passportClient.getUserEssentials(?, ?)(?)).thenReturn(Future.successful(userEssentials))

    val request = CreateRoomRequest
      .newBuilder()
      .addUsers("user:54321")
      .addUsers("user:12345")
      .build()

    val roomSource = RoomSource.fromCreateRequest(request).futureValue

    roomSource shouldBe a[SimpleRoomSource]
  }

  test("create OfferRoomSource") {
    val userEssentials = UserEssentials.newBuilder().build()
    val offer = Offer
      .newBuilder()
      .setCategory(Category.CARS)
      .setId("1234")
      .setUserRef("user:12345")
      .build()
    when(passportClient.getUserEssentials(?, ?)(?)).thenReturn(Future.successful(userEssentials))
    when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
    val offerSubjectSource = OfferSubjectSource.newBuilder().setCategory("CARS").setId("1234").build()
    val subjectSource = SubjectSource.newBuilder().setOffer(offerSubjectSource).build()
    val request = CreateRoomRequest
      .newBuilder()
      .addUsers("user:54321")
      .setSubject(subjectSource)
      .build()

    val roomSource = RoomSource.fromCreateRequest(request).futureValue

    roomSource shouldBe a[OfferRoomSource]
  }
}
