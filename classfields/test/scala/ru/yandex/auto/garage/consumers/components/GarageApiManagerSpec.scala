package ru.yandex.auto.garage.consumers.components

import auto.carfax.common.clients.garage.GarageClient
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.recalls.RecallsApiModel
import ru.auto.api.vin.garage.GarageApiModel.Card
import ru.auto.api.vin.garage.ResponseModel.{CreateCardResponse, ErrorCode}
import ru.yandex.auto.garage.consumers.components.GarageApiManager.NavigatorRecallsCardId
import auto.carfax.common.clients.garage.GarageClient.{GarageError, GarageSuccess}
import ru.yandex.auto.vin.decoder.model.exception.CarfaxExceptions.VinInProgressException
import ru.yandex.auto.vin.decoder.model.{AutoruUser, VinCode}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.utils.EmptyRequestInfo
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GarageApiManagerSpec extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  private val garageClient = mock[GarageClient]
  private val manager = new GarageApiManager(garageClient)
  implicit private val ri = EmptyRequestInfo
  private val vin = VinCode("Z0NZWE00054341234")
  private val userRef = AutoruUser(1)
  private val recallsCardId = 1234

  private val recallsCard = RecallsApiModel.Card
    .newBuilder()
    .setCardId(recallsCardId)
    .build()

  private val createCardResponse = CreateCardResponse
    .newBuilder()
    .setCard(
      Card
        .newBuilder()
        .setId("1")
    )
    .build()

  after {
    reset(garageClient)
  }

  "GarageApiManager" should {
    "send create request" in {
      when(garageClient.buildCardFromVin(eq(userRef), eq(vin.toString), ?)(any()))
        .thenReturn(Future.successful(GarageSuccess(createCardResponse)))
      when(garageClient.createCard(eq(userRef), any(), any())(any()))
        .thenReturn(Future.successful(GarageSuccess(createCardResponse)))

      manager.createCard(vin.toString, NavigatorRecallsCardId(recallsCardId), userRef).await
    }

    "throw exception if can't build card from vin" in {
      when(garageClient.buildCardFromVin(eq(userRef), eq(vin.toString), ?)(any()))
        .thenReturn(Future.failed(new RuntimeException))

      intercept[RuntimeException] {
        manager.createCard(vin.toString, NavigatorRecallsCardId(recallsCardId), userRef).await
      }
    }

    "throw exception if can't send create request" in {
      when(garageClient.buildCardFromVin(eq(userRef), eq(vin.toString), ?)(any()))
        .thenReturn(Future.successful(GarageSuccess(createCardResponse)))
      when(garageClient.createCard(eq(userRef), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException))

      intercept[RuntimeException] {
        manager.createCard(vin.toString, NavigatorRecallsCardId(recallsCardId), userRef).await
      }
    }

    "not throw exception if got validation error: buildCardFromVin" in {
      val cardFromVinResponse = GarageError[CreateCardResponse](ErrorCode.VALIDATION_ERROR, "", Seq.empty)
      when(garageClient.buildCardFromVin(eq(userRef), eq(vin.toString), ?)(any()))
        .thenReturn(Future.successful(cardFromVinResponse))
      manager.createCard(vin.toString, NavigatorRecallsCardId(recallsCardId), userRef).await
      verify(garageClient, never).createCard(?, ?, ?)(?)
    }

    "not throw exception if got validation error: createCardByVin" in {
      when(garageClient.buildCardFromVin(eq(userRef), eq(vin.toString), ?)(any()))
        .thenReturn(Future.successful(GarageSuccess(createCardResponse)))
      when(garageClient.createCard(eq(userRef), any(), any())(any()))
        .thenReturn(Future.successful(GarageError(ErrorCode.VALIDATION_ERROR, "", Seq.empty)))
      manager.createCard(vin.toString, NavigatorRecallsCardId(recallsCardId), userRef).await
    }

    "throw exception if got in_progress error " in {
      when(garageClient.buildCardFromVin(eq(userRef), eq(vin.toString), ?)(any()))
        .thenReturn(Future.successful(GarageError(ErrorCode.IN_PROGRESS, "", Seq.empty)))
      intercept[VinInProgressException] {
        manager.createCard(vin.toString, NavigatorRecallsCardId(recallsCardId), userRef).await
      }
    }
  }
}
