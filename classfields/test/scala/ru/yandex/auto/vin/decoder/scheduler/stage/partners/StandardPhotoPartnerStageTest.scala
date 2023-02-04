package ru.yandex.auto.vin.decoder.scheduler.stage.partners

import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.IdentifiersManagerHelpers
import ru.yandex.auto.vin.decoder.manager.vin.VinDataManager
import ru.yandex.auto.vin.decoder.model.{LicensePlate, VinCode}
import ru.yandex.auto.vin.decoder.partners.migalki.Migalki
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.service.licenseplate.LicensePlateUpdateService
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StandardPhotoPartnerStageTest
  extends AnyWordSpecLike
  with Matchers
  with MockitoSupport
  with CompoundStageSupport[VinCode, StandardPhotoPartnerStage] {

  private val vinDataManager = mock[VinDataManager]
  private val lpUpdateManager = mock[LicensePlateUpdateService]
  private val idManagerHelpers = mock[IdentifiersManagerHelpers]

  private val stage = createProcessingStage()
  private val vin = VinCode("X4X3D59430PS96744")
  private val lp = LicensePlate("B751PK702")

  def createProcessingStage() =
    new StandardPhotoPartnerStage(
      vinDataManager,
      lpUpdateManager,
      Migalki,
      EventType.MIGALKI_PHOTO,
      idManagerHelpers
    )

  "LpPhotoStandardPartnerStage.shouldProcess" should {
    "return false" when {
      "compoundStateUpdate getShouldProcess = false" in {
        val withUpdateProcessingState = createProcessingStateWithUpdate(false)
        stage.shouldProcess(withUpdateProcessingState) shouldBe false
      }
    }

    "return true" when {
      "compoundStateUpdate getShouldProcess = true" in {
        val withUpdateProcessingState = createProcessingStateWithUpdate(true)
        stage.shouldProcess(withUpdateProcessingState) shouldBe true
      }
    }

  }

  "LpPhotoStandardPartnerStage.process" should {
    "delay process" when {
      "vin state in progress" in {
        when(idManagerHelpers.isVinStateInProgress(?)).thenReturn(true)

        val stage = createProcessingStage()
        val processingState = createProcessingStateWithUpdate(shouldProcess = true)
        val update = stage.processWithAsync(vin, processingState)

        stage.shouldProcess(processingState) shouldBe true
        update.delay.isDefault shouldBe false
      }
    }

    "stop process" when {
      "license plate not found" in {
        when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(None))
        when(idManagerHelpers.isVinStateInProgress(?)).thenReturn(false)

        val stage = createProcessingStage()
        val processingState = createProcessingStateWithUpdate(shouldProcess = true)
        val update = stage.processWithAsync(vin, processingState)

        stage.shouldProcess(processingState) shouldBe true
        update.state.getMigalkiState.getNotFound shouldBe true
        update.state.getMigalkiState.getShouldProcess shouldBe false
        update.state.getMigalkiState.getLastCheck shouldNot be(0)
        update.state.getMigalkiState.getStateUpdateHistoryCount shouldBe 0
        update.delay.isDefault shouldBe true
      }
    }

    "delay process" when {
      "license plate state is updating" in {
        when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(Some(lp)))
        when(lpUpdateManager.getState(?, ?)(?)).thenReturn(
          Some(
            WatchingStateHolder(lp, createCompoundState(), 0)
          )
        )
        when(idManagerHelpers.isVinStateInProgress(?)).thenReturn(false)

        val stage = createProcessingStage()
        val processingState = createProcessingStateWithUpdate(shouldProcess = true)
        val update = stage.processWithAsync(vin, processingState)

        stage.shouldProcess(processingState) shouldBe true
        update.delay.isDefault shouldBe false
      }
    }

    "schedule task for lp and vin when lpState not in process and not finished" in {
      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(Some(lp)))
      when(lpUpdateManager.getState(?, ?)(?)).thenReturn(
        Some(
          WatchingStateHolder(lp, createCompoundState(shouldProcess = false), 0)
        )
      )
      doNothing().when(lpUpdateManager).upsertUpdate(?)(?)(?)
      when(idManagerHelpers.isVinStateInProgress(?)).thenReturn(false)

      val stage = createProcessingStage()
      val processingState = createProcessingStateWithUpdate(shouldProcess = true)
      val update = stage.processWithAsync(vin, processingState)

      stage.shouldProcess(processingState) shouldBe true
      update.delay.isDefault shouldBe false
      update.state.getMigalkiState.getShouldProcess shouldBe true
      verify(lpUpdateManager, times(1)).upsertUpdate(eq(lp))(?)(?)
    }

    "finish process when lp processing state is not found" in {
      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(Some(lp)))
      when(lpUpdateManager.getState(?, ?)(?)).thenReturn(
        Some(
          WatchingStateHolder(lp, createCompoundState(false, 1, None), 0)
        )
      )
      when(idManagerHelpers.isVinStateInProgress(?)).thenReturn(false)

      val stage = createProcessingStage()
      val processingState = createProcessingStateWithUpdate(shouldProcess = true)
      val update = stage.processWithAsync(vin, processingState)

      stage.shouldProcess(processingState) shouldBe true
      update.delay.isDefault shouldBe true
      update.state.getMigalkiState.getShouldProcess shouldBe false
      update.state.getMigalkiState.getLastCheck shouldNot be(0)
      update.state.getMigalkiState.getStateUpdateHistoryCount shouldBe 0
    }

    "finish process when lp processing state is done" in {
      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(Some(lp)))

      val preparedDataState = PreparedDataState
        .newBuilder()
        .setLastCheck(1)
        .setEventType(EventType.MIGALKI_PHOTO)
        .setShouldProcess(false)
        .build()

      when(lpUpdateManager.getState(?, ?)(?)).thenReturn(
        Some(
          WatchingStateHolder(lp, createCompoundState(false, 1, Some(preparedDataState)), 0)
        )
      )
      when(idManagerHelpers.isVinStateInProgress(?)).thenReturn(false)

      val stage = createProcessingStage()
      val processingState = createProcessingStateWithUpdate(shouldProcess = true)
      val update = stage.processWithAsync(vin, processingState)

      stage.shouldProcess(processingState) shouldBe true
      update.delay.isDefault shouldBe true
      update.state.getMigalkiState.getShouldProcess shouldBe false
      update.state.getMigalkiState.getLastCheck shouldNot be(0)
      update.state.getMigalkiState.getStateUpdateHistoryCount shouldBe 0
    }

  }

  private def createCompoundState(
      shouldProcess: Boolean = true,
      lastCheck: Long = 0,
      preparedData: Option[PreparedDataState] = None) = {
    val csBuilder = CompoundState
      .newBuilder()

    csBuilder.getMigalkiStateBuilder
      .setShouldProcess(shouldProcess)
      .setLastCheck(lastCheck)

    if (preparedData.isDefined) {
      csBuilder.addPreparedDataState(preparedData.get)
    }

    csBuilder
      .build()
  }

  private def createProcessingStateWithUpdate(shouldProcess: Boolean) = {
    val compoundStateBuilder = CompoundState.newBuilder()
    val processingState = createDefaultProcessingState(compoundStateBuilder.build())

    processingState.withUpdate { cs =>
      val builder = cs.toBuilder
      builder.getMigalkiStateBuilder.setShouldProcess(shouldProcess)
      builder.build()
    }
  }

}
