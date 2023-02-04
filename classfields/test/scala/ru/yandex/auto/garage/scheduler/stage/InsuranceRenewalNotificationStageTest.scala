package ru.yandex.auto.garage.scheduler.stage

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.yandex.auto.garage.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Notification.NotificationType
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, Insurance, InsuranceInfo, Notification}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, State, WatchingStateUpdate}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class InsuranceRenewalNotificationStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with GarageCardStageSupport[InsuranceRenewalNotificationStage] {

  private val stage = createProcessingStage()

  "InsuranceRenewalStage" should {
    "process card with existing insurances ready for the first notification" in {
      val cardTemplateBuilder = cardBuilderTemplate()
      val card = cardTemplateBuilder
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addInsurances(buildPolicy(policyTo = DateTimeUtil.now().plusDays(14)))
        )
        .build()
      val result = stage.processWithAsync(0, createDefaultProcessingState(card))
      assert(!result.state.getNotificationsList.isEmpty)
      assert(
        result.state.getNotificationsList.get(0).getNotificationType == NotificationType.INSURANCE_RENEWAL_NOTIFICATION
      )
      assert(result.delay.toDuration == 0.seconds)
    }

    "process card with existing insurances ready for the second notification" in {
      val cardTemplateBuilder = cardBuilderTemplate()
      val card = cardTemplateBuilder
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addInsurances(buildPolicy(policyTo = DateTimeUtil.now().plusDays(7)))
        )
        .build()
      val result = stage.processWithAsync(0, createDefaultProcessingState(card))
      assert(!result.state.getNotificationsList.isEmpty)
      assert(
        result.state.getNotificationsList.get(0).getNotificationType == NotificationType.INSURANCE_RENEWAL_NOTIFICATION
      )
      assert(result.delay.toDuration == 0.seconds)
    }

    "not process card with insurances already sent" in {
      val cardTemplateBuilder = cardBuilderTemplate()
      val card = cardTemplateBuilder
        .addNotifications(
          Notification
            .newBuilder()
            .setNotificationType(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
            .setTimestampCreate(
              Timestamps.fromMillis(DateTimeUtil.now().minusHours(1).getMillis)
            )
        )
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addInsurances(buildPolicy(policyTo = DateTimeUtil.now().plusDays(14)))
        )
        .build()
      val result = stage.processWithAsync(0, createDelayedProcessingState(card))
      assert(result.state.getNotificationsList.size == 1)
      assert(result.delay.toDuration == 7.days)
    }

    "reschedule correctly card with insurances between processing days" in {
      val cardTemplateBuilder = cardBuilderTemplate()
      val card = cardTemplateBuilder
        .addNotifications(
          Notification
            .newBuilder()
            .setNotificationType(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
            .setTimestampCreate(
              Timestamps.fromMillis(DateTimeUtil.now().minusDays(4).getMillis)
            )
        )
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addInsurances(buildPolicy(policyTo = DateTimeUtil.now().plusDays(10)))
        )
        .build()
      val state = createDelayedProcessingState(card)
      val result = stage.processWithAsync(0, createDelayedProcessingState(card))
      assert(result.delay.toDuration == 3.days)
      assert(state.compoundStateUpdate.state == result.state)
    }

    "not process insurances with notifications already sent" in {
      val cardTemplateBuilder = cardBuilderTemplate()
      val card = cardTemplateBuilder
        .addNotifications(
          Notification
            .newBuilder()
            .setNotificationType(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
            .setTimestampCreate(
              Timestamps.fromMillis(DateTimeUtil.now().minusDays(12).getMillis)
            )
        )
        .addNotifications(
          Notification
            .newBuilder()
            .setNotificationType(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
            .setTimestampCreate(
              Timestamps.fromMillis(DateTimeUtil.now().minusDays(5).getMillis)
            )
        )
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addInsurances(buildPolicy(policyTo = DateTimeUtil.now().plusDays(2)))
        )
        .build()
      val state = createDelayedProcessingState(card)
      val result = stage.processWithAsync(0, state)
      assert(result.delay.toDuration == 100.days)
      assert(result.state == state.compoundStateUpdate.state)
    }

    "not process/delay state with outdated insurances" in {
      val cardTemplateBuilder = cardBuilderTemplate()
      val card = cardTemplateBuilder
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addInsurances(buildPolicy(policyTo = DateTimeUtil.now().minusDays(2)))
        )
        .build()
      val state = createDelayedProcessingState(card)
      val result = stage.processWithAsync(0, state)
      assert(result.delay.toDuration == 100.days)
      assert(result.state == state.compoundStateUpdate.state)
    }
  }

  override def createProcessingStage(): InsuranceRenewalNotificationStage =
    new InsuranceRenewalNotificationStage()

  private val TestVin = VinCode("SALGA2BE8LA405000")

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

  private def buildPolicy(
      insuranceType: InsuranceType = InsuranceType.OSAGO,
      isDeleted: Boolean = false,
      policyTo: DateTime = DateTimeUtil.now()): Insurance = {
    Insurance
      .newBuilder()
      .setInsuranceType(insuranceType)
      .setIsDeleted(isDeleted)
      .setTo(Timestamps.fromMillis(policyTo.getMillis))
      .build()
  }

  def createDelayedProcessingState[S: State](compoundState: S): ProcessingState[S] = {
    ProcessingState[S](WatchingStateUpdate(compoundState, DefaultDelay(100.days)))
  }
}
