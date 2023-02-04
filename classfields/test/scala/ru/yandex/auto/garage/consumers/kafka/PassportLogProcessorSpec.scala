package ru.yandex.auto.garage.consumers.kafka

import auto.carfax.common.clients.recalls.{RecallsClient, RecallsUserCards}
import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.recalls.RecallsApiModel.Card
import ru.yandex.auto.garage.consumers.components.GarageApiManager
import ru.yandex.auto.garage.consumers.components.GarageApiManager.NavigatorRecallsCardId
import ru.yandex.auto.vin.decoder.model.{AutoruUser, VinCode, YandexUser}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.passport.model.api.ApiModel.SocialUserSource
import ru.yandex.vertis.SocialProvider
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.model.proto.{EmailSent, Event, EventPayload, SocialUserLinked}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PassportLogProcessorSpec extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  private val recallsClient = mock[RecallsClient]
  private val garageApiManager = mock[GarageApiManager]
  private val processor = new PassportLogProcessor(recallsClient, garageApiManager)
  implicit val t: Traced = Traced.empty
  private val vin = VinCode("Z0NZWE00054341234")
  private val autoruUser = AutoruUser(123)
  private val yandexUser = YandexUser(321)
  private val recallsCardId = 1234

  private val createEvent = (provider: SocialProvider) =>
    Event
      .newBuilder()
      .setPayload(
        EventPayload
          .newBuilder()
          .setUserId(autoruUser.uid.toString)
          .setSocialUserLinked(
            SocialUserLinked
              .newBuilder()
              .setSource(
                SocialUserSource
                  .newBuilder()
                  .setId(yandexUser.uid.toString)
              )
              .setProvider(provider)
          )
      )
      .build()

  after {
    reset(recallsClient, garageApiManager)
  }

  "PassportLogProcessor" should {
    "process social link event" in {
      val event = createEvent(SocialProvider.YANDEX)
      val recallCard = Card
        .newBuilder()
        .setCardId(recallsCardId)
        .setVinCode(vin.toString)
        .build()
      val recallsResult = RecallsUserCards(List(recallCard))
      when(recallsClient.getUserCards(eq(yandexUser), any())(any())).thenReturn(Future.successful(recallsResult))
      when(
        garageApiManager.createCard(eq(vin.toString), eq(NavigatorRecallsCardId(recallsCardId)), eq(autoruUser))(any())
      )
        .thenReturn(Future.successful(()))
      processor.processMessage(event.toByteArray).await
    }

    "ignore social link event provider except YANDEX" in {
      val event = createEvent(SocialProvider.APPLE)
      processor.processMessage(event.toByteArray).await
      verify(recallsClient, never).getUserCards(?, ?)(?)
      verify(garageApiManager, never).createCard(?, ?, ?)(?)
    }

    "ignore event except social link event" in {
      val event = Event
        .newBuilder()
        .setPayload(
          EventPayload
            .newBuilder()
            .setUserId(autoruUser.uid.toString)
            .setEmailSent(EmailSent.getDefaultInstance)
        )
        .build()
      processor.processMessage(event.toByteArray).await
      verify(recallsClient, never).getUserCards(?, ?)(?)
      verify(garageApiManager, never).createCard(?, ?, ?)(?)
    }
  }
}
