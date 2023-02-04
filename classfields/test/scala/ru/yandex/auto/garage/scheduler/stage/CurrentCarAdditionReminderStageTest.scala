package ru.yandex.auto.garage.scheduler.stage

import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.garage.dao.cards.CardsTableRow
import ru.yandex.auto.garage.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo.ChangeEvent
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Notification.NotificationType
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, Meta}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, State, WatchingStateUpdate}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class CurrentCarAdditionReminderStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfterEach
  with GarageCardStageSupport[CurrentCarAdditionReminderStage] {

  private lazy val cardsService = mock[CardsService]
  implicit private val t: Traced = Traced.empty
  private val stage = createProcessingStage()

  override def beforeEach(): Unit = {
    reset(cardsService)
  }

  "CurrentCarAdditionReminderStage" should {
    "schedule notification for a card" when {
      "exists a state transition to EX and no CURRENT CARS present" in {
        when(cardsService.getUserCards(?, ?, ?, ?, ?, ?)(?)).thenReturn(
          Future.successful(
            List.empty
          )
        )
        val cardTemplateBuilder = cardBuilderTemplate(cardType = CardType.EX_CAR)
        val card = cardTemplateBuilder
          .setMeta(
            Meta
              .newBuilder()
              .setCardTypeInfo(
                CardTypeInfo
                  .newBuilder()
                  .addHistory(
                    ChangeEvent
                      .newBuilder()
                      .setDate(Timestamps.fromMillis(DateTimeUtil.now().minusDays(14).getMillis))
                      .setState(CardTypeInfo.State.newBuilder().setCardType(CardType.EX_CAR))
                  )
              )
          )
          .build()
        val result = stage.processWithAsync(0, createDefaultProcessingState(card))
        assert(!result.state.getNotificationsList.isEmpty)
        assert(
          result.state.getNotificationsList.get(0).getNotificationType == NotificationType.CURRENT_CAR_ADDITION_REMINDER
        )
        assert(result.delay.toDuration == 0.seconds)
      }

    }
    "reschedule card processing" when {
      "exists a state transition to EX and no CURRENT CARS present and it's still not the time" in {
        when(cardsService.getUserCards(?, ?, ?, ?, ?, ?)(?)).thenReturn(
          Future.successful(
            List.empty
          )
        )
        val cardTemplateBuilder = cardBuilderTemplate(cardType = CardType.EX_CAR)
        val card = cardTemplateBuilder
          .setMeta(
            Meta
              .newBuilder()
              .setCardTypeInfo(
                CardTypeInfo
                  .newBuilder()
                  .addHistory(
                    ChangeEvent
                      .newBuilder()
                      .setDate(Timestamps.fromMillis(DateTimeUtil.now().minusDays(5).getMillis))
                      .setState(CardTypeInfo.State.newBuilder().setCardType(CardType.EX_CAR))
                  )
              )
          )
          .build()
        val result = stage.processWithAsync(0, createDelayedProcessingState(card))
        assert(result.state.getNotificationsList.isEmpty)
        assert(result.delay.toDuration == 9.days)
      }

    }

    "do nothing" when {
      "there are CURRENT CARS present" in {
        when(cardsService.getUserCards(?, ?, ?, ?, ?, ?)(?)).thenReturn(
          Future.successful(
            List(
              row(DateTimeUtil.now(), CardType.CURRENT_CAR, None)
            )
          )
        )
        val cardTemplateBuilder = cardBuilderTemplate(cardType = CardType.EX_CAR)
        val card = cardTemplateBuilder
          .setMeta(
            Meta
              .newBuilder()
              .setCardTypeInfo(
                CardTypeInfo
                  .newBuilder()
                  .addHistory(
                    ChangeEvent
                      .newBuilder()
                      .setDate(Timestamps.fromMillis(DateTimeUtil.now().minusDays(5).getMillis))
                      .setState(CardTypeInfo.State.newBuilder().setCardType(CardType.EX_CAR))
                  )
              )
          )
          .build()
        val result = stage.processWithAsync(0, createDefaultProcessingState(card))
        assert(result.state.getNotificationsList.isEmpty)
        assert(result.delay.isDefault)
      }

    }
  }

  override def createProcessingStage(): CurrentCarAdditionReminderStage =
    new CurrentCarAdditionReminderStage(cardsService)

  private val TestVin = VinCode("SALGA2BE8LA405000")

  def createDelayedProcessingState[S: State](compoundState: S): ProcessingState[S] = {
    ProcessingState[S](WatchingStateUpdate(compoundState, DefaultDelay(100.days)))
  }

  private def cardBuilderTemplate(
      status: GarageCard.Status = GarageCard.Status.ACTIVE,
      cardType: CardType = CardType.CURRENT_CAR,
      optVin: Option[VinCode] = Some(TestVin),
      created: Long = System.currentTimeMillis()): GarageCard.Builder = {
    val builder = GarageCard.newBuilder()
    builder.getMetaBuilder.setStatus(status).setCreated(Timestamps.fromMillis(created))
    builder.getMetaBuilder.getCardTypeInfoBuilder.getCurrentStateBuilder.setCardType(cardType)
    optVin.foreach(vin => builder.getVehicleInfoBuilder.getDocumentsBuilder.setVin(vin.toString))
    builder
  }

  private def row(creationDate: DateTime, cardType: CardType, saleDate: Option[DateTime] = None) =
    CardsTableRow(
      123L,
      "user:123",
      None,
      None,
      Instant.ofEpochMilli(creationDate.getMillis),
      GarageCard.Status.ACTIVE,
      cardType, {
        val builder = GarageCard.newBuilder()
        builder.setMeta(
          Meta
            .newBuilder()
            .setCardTypeInfo {
              val ctsB = CardTypeInfo.newBuilder()
              val stateB = CardTypeInfo.State.newBuilder().setCardType(cardType)
              ctsB.setCurrentState(stateB)
            }
            .setCreated(Timestamps.fromMillis(creationDate.getMillis))
        )
        saleDate.foreach(dt =>
          builder.getVehicleInfoBuilder.getDocumentsBuilder.setSaleDate(Timestamps.fromMillis(dt.getMillis))
        )
        builder.build()
      },
      None,
      None,
      None,
      Instant.ofEpochMilli(System.currentTimeMillis())
    )

}
