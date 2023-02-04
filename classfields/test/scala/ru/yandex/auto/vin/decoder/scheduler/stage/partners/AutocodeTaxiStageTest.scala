package ru.yandex.auto.vin.decoder.scheduler.stage.partners

import auto.carfax.common.utils.tracing.Traced
import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito.{doNothing, never, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.manager.vin.VinDataManager
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.{LicensePlateGen, VinCodeGen}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.state.StateUtils
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.service.licenseplate.LicensePlateUpdateService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AutocodeTaxiStageTest
  extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with CompoundStageSupport[VinCode, AutocodeTaxiStage] {

  implicit val t: Traced = Traced.empty
  val vinDataManager = mock[VinDataManager]
  val lpUpdateService = mock[LicensePlateUpdateService]
  val autocodeTaxiStage = createProcessingStage()

  override def createProcessingStage(): AutocodeTaxiStage = {
    new AutocodeTaxiStage(vinDataManager, lpUpdateService)
  }

  before {
    reset(vinDataManager)
    reset(lpUpdateService)
  }

  "AutocodeTaxiStage" should {
    "update taxi when lp ready" in {
      val vin = VinCodeGen.sample.get
      val lp = LicensePlateGen.sample.get
      val compoundStateBuilder = CompoundState.newBuilder()
      compoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.DummyTaxi.id)
        .setShouldProcess(true)
        .addStateUpdateHistoryBuilder()
        .setTimestamp(1)
      val processingState = createDefaultProcessingState(compoundStateBuilder.build())

      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(lp.some))
      when(lpUpdateService.getState(?, ?)(?))
        .thenReturn(WatchingStateHolder(lp, StateUtils.getNewState, 1).some)
      doNothing().when(lpUpdateService).upsertUpdate(eq(lp))(?)(?)

      autocodeTaxiStage.shouldProcess(processingState) shouldBe true
      autocodeTaxiStage.processWithAsync(vin, processingState).delay.isDefault shouldBe false

      verify(lpUpdateService).upsertUpdate(eq(lp))(?)(?)
    }

    "set no_info when lp doesn't exists" in {
      val vin = VinCodeGen.sample.get
      val lp = LicensePlateGen.sample.get
      val compoundStateBuilder = CompoundState.newBuilder()
      compoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.DummyTaxi.id)
        .setShouldProcess(true)
        .addStateUpdateHistoryBuilder()
        .setTimestamp(1)
      val processingState = createDefaultProcessingState(compoundStateBuilder.build())

      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(None))

      autocodeTaxiStage.shouldProcess(processingState) shouldBe true
      val watchingStateUpdate = autocodeTaxiStage.processWithAsync(vin, processingState)

      watchingStateUpdate.delay.isDefault shouldBe true
      watchingStateUpdate.state.getAutocodeState.getAutocodeReports(0).getNoInfo shouldBe true

      verify(lpUpdateService, never()).upsertUpdate(eq(lp))(?)(?)
    }

    "retry when lp is in progress" in {
      val vin = VinCodeGen.sample.get
      val compoundStateBuilder = CompoundState.newBuilder()
      compoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.DummyTaxi.id)
        .setShouldProcess(true)
        .addStateUpdateHistoryBuilder()
        .setTimestamp(1)
      compoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Identifiers.id)
        .setRequestSent(1)
      val processingState = createDefaultProcessingState(compoundStateBuilder.build())

      autocodeTaxiStage.shouldProcess(processingState) shouldBe true
      autocodeTaxiStage.processWithAsync(vin, processingState).delay.isDefault shouldBe false
    }

    "retry when taxi is in progress" in {
      val vin = VinCodeGen.sample.get
      val lp = LicensePlateGen.sample.get
      val vinCompoundStateBuilder = CompoundState.newBuilder()
      vinCompoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.DummyTaxi.id)
        .setShouldProcess(true)
        .addStateUpdateHistoryBuilder()
        .setTimestamp(1)
      val processingState = createDefaultProcessingState(vinCompoundStateBuilder.build())

      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(lp.some))

      val lpCompoundStateBuilder = CompoundState.newBuilder()
      lpCompoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Taxi.id)
        .setRequestSent(1)

      when(lpUpdateService.getState(?, ?)(?))
        .thenReturn(WatchingStateHolder(lp, lpCompoundStateBuilder.build(), 1).some)

      autocodeTaxiStage.shouldProcess(processingState) shouldBe true
      autocodeTaxiStage.processWithAsync(vin, processingState).delay.isDefault shouldBe false
    }

    "do nothing when taxi is ready" in {
      val vin = VinCodeGen.sample.get
      val lp = LicensePlateGen.sample.get
      val vinCompoundStateBuilder = CompoundState.newBuilder()
      vinCompoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.DummyTaxi.id)
        .setShouldProcess(true)
        .addStateUpdateHistoryBuilder()
        .setTimestamp(1)
      val processingState = createDefaultProcessingState(vinCompoundStateBuilder.build())

      when(vinDataManager.getCurrentLp(?, ?)(?)).thenReturn(Future.successful(lp.some))

      val lpCompoundStateBuilder = CompoundState.newBuilder()
      lpCompoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Taxi.id)
        .setReportArrived(2)

      when(lpUpdateService.getState(?, ?)(?))
        .thenReturn(WatchingStateHolder(lp, lpCompoundStateBuilder.build(), 1).some)

      autocodeTaxiStage.shouldProcess(processingState) shouldBe true
      val watchingStateUpdate = autocodeTaxiStage.processWithAsync(vin, processingState)
      watchingStateUpdate.delay.isDefault shouldBe true
      watchingStateUpdate.state.getAutocodeState.getAutocodeReports(0).getShouldProcess shouldBe false
    }
  }
}
