package ru.yandex.auto.garage.consumers.kafka

import auto.carfax.common.clients.passport.{PassportClient, SocialUserProvider}
import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.recalls.RecallsApiModel.{CardEvent, CardEventType, Source}
import ru.yandex.auto.garage.consumers.components.GarageApiManager
import ru.yandex.auto.garage.consumers.components.GarageApiManager.AutoruRecallsCardId
import auto.carfax.common.clients.passport.PassportClient.UserSearchFilter
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.model.{AutoruUser, VinCode, YandexUser}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RecallsCardEventProcessorSpec extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  private val passportClient = mock[PassportClient]
  implicit val m = TestOperationalSupport
  private val garageApiManager = mock[GarageApiManager]
  private val enableCreateAccount = mock[Feature[Boolean]]
  private val processor = new RecallsCardEventProcessor(passportClient, garageApiManager, enableCreateAccount)
  implicit val t: Traced = Traced.empty
  private val vin = VinCode("Z0NZWE00054341234")
  private val autoruUser = AutoruUser(123)
  private val yandexUser = YandexUser(321)
  private val recallsCardId = 1234

  after {
    reset(passportClient, garageApiManager)
  }

  "RecallsCardEventProcessor" should {

    "process create event" in {
      val expectedFilter = UserSearchFilter(
        socialUserProvider = SocialUserProvider.Yandex.some,
        socialUserId = yandexUser.toRaw.some
      )
      when(passportClient.userSearch(eq(expectedFilter))(any())).thenReturn(Future.successful(autoruUser.some))
      when(garageApiManager.createCard(eq(vin.toString), eq(AutoruRecallsCardId(recallsCardId)), eq(autoruUser))(any()))
        .thenReturn(Future.unit)
      val event = CardEvent
        .newBuilder()
        .setEventType(CardEventType.CREATED)
        .setSource(Source.AUTORU)
        .setVin(vin.toString)
        .setUserId(yandexUser.toPlain)
        .setCardId(recallsCardId)
        .build()
      processor.processMessage(event.toByteArray).await
      verify(garageApiManager, never).deleteCard(?, ?, ?)(?)
    }

    "process delete event" in {
      val expectedFilter = UserSearchFilter(
        socialUserProvider = SocialUserProvider.Yandex.some,
        socialUserId = yandexUser.toRaw.some
      )
      when(passportClient.userSearch(eq(expectedFilter))(any())).thenReturn(Future.successful(autoruUser.some))
      when(garageApiManager.deleteCard(eq(vin.toString), eq(AutoruRecallsCardId(recallsCardId)), eq(autoruUser))(any()))
        .thenReturn(Future.unit)
      val event = CardEvent
        .newBuilder()
        .setEventType(CardEventType.DELETED)
        .setSource(Source.AUTORU)
        .setVin(vin.toString)
        .setUserId(yandexUser.toPlain)
        .setCardId(recallsCardId)
        .build()
      processor.processMessage(event.toByteArray).await
      verify(garageApiManager, never).createCard(?, ?, ?)(?)
    }

    "not go to passport if event has autoru user" in {
      when(garageApiManager.deleteCard(eq(vin.toString), eq(AutoruRecallsCardId(recallsCardId)), eq(autoruUser))(any()))
        .thenReturn(Future.unit)
      val event = CardEvent
        .newBuilder()
        .setEventType(CardEventType.DELETED)
        .setSource(Source.AUTORU)
        .setVin(vin.toString)
        .setUserId(autoruUser.toPlain)
        .setCardId(recallsCardId)
        .build()
      processor.processMessage(event.toByteArray).await
      verify(garageApiManager, never).createCard(?, ?, ?)(?)
      verify(passportClient, never).userSearch(?)(?)
    }
  }
}
