package ru.yandex.auto.vin.decoder.scheduler.stage.partners

import com.google.protobuf.BoolValue
import org.mockito.Mockito.{doNothing, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.manager.vin.{KnownIdentifiers, VinDataManager}
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.VinCodeGen
import ru.yandex.auto.vin.decoder.model.{IdentifierContainer, ResolutionData, Sts, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{AutocodeReport, CompoundState}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Fine, VinInfoHistory}
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.service.vin.VinUpdateService
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class AutocodeFinesStageTest
  extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with CompoundStageSupport[VinCode, AutocodeFinesStage] {

  private val vinDataManager = mock[VinDataManager]
  private val vinUpdateService = mock[VinUpdateService]
  private val autocodeFinesStage = createProcessingStage()
  private val vin = VinCode("WBAUE71090E005594")
  private val emptyResolutionData = ResolutionData.empty(vin)

  override def createProcessingStage(): AutocodeFinesStage = {
    new AutocodeFinesStage(vinDataManager, vinUpdateService)
  }

  before {
    reset(vinDataManager)
    reset(vinUpdateService)
  }

  "AutocodeFinesStage" should {
    "just reschedule if STS is updating now" in {
      val vin = VinCodeGen.sample.get
      val compoundState = createCompoundState(
        finesBuild = _.setShouldProcess(true)
          .addStateUpdateHistoryBuilder()
          .setTimestamp(1): Unit,
        identifiersBuild = _.setShouldProcess(true): Unit // sts is updating
      )
      val processingState = createDefaultProcessingState(compoundState)

      autocodeFinesStage.shouldProcess(processingState) shouldBe true
      autocodeFinesStage.processWithAsync(vin, processingState).state shouldEqual compoundState
    }

    "pull down shouldProcess for whole fines report if all known STSs are processed recently" in {
      val vin = VinCodeGen.sample.get
      val now = System.currentTimeMillis
      val sts = Sts("sts")
      val processingState = createProcessingState(
        finesBuild = _.setShouldProcess(true)
          .addStateUpdateHistoryBuilder()
          .setTimestamp(1): Unit,
        identifiersBuild = _.setReportArrived(now - 10): Unit, // recently
        finesByStsBuilds = Seq(
          sts -> (_.setRequestSent(now - 10).setReportArrived(now - 1): Unit) // recently processed
        )
      )

      when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(
        Future.successful(
          emptyResolutionData.copy(
            identifiers = KnownIdentifiers.empty.copy(
              sts = List(IdentifierContainer(sts, None, 0))
            )
          )
        )
      )

      autocodeFinesStage.shouldProcess(processingState) shouldBe true
      val finesState = autocodeFinesStage
        .processWithAsync(vin, processingState)
        .state
        .getAutocodeState
        .findReport(AutocodeReportType.Fines.id)
        .get
      finesState.getShouldProcess shouldBe false
    }

    "raise shouldProcess for (not processed) and (processed but unpaid) and (most fresh) STSs " +
      "if STSs are known and was updated recently" in {
        val vin = VinCodeGen.sample.get
        val allPaidSts = Sts("allPaidSts")
        val hasUnpaidSts = Sts("hasUnpaidSts")
        val currentlyProcessingSts = Sts("currentlyProcessingSts")
        val notProcessedSts = Sts("notProcessedSts")
        val mostFreshSts = Sts("mostFreshSts")

        val now = System.currentTimeMillis
        val processingState = createProcessingState(
          finesBuild = _.setShouldProcess(true)
            .setProcessRequested(now - 100)
            .addStateUpdateHistoryBuilder()
            .setTimestamp(1): Unit,
          identifiersBuild = _.setReportArrived(now - 10), // recently
          finesByStsBuilds = Seq(
            allPaidSts -> (_.setProcessRequested(now - 3).setRequestSent(now - 2).setReportArrived(now - 1): Unit),
            hasUnpaidSts -> (_.setProcessRequested(now - 3).setRequestSent(now - 2).setReportArrived(now - 1): Unit),
            currentlyProcessingSts -> (_.setProcessRequested(now - 3)
              .setReportId("123")
              .setRequestSent(now - 2)
              .setCounter(1): Unit)
          )
        )

        when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(
          Future.successful(
            emptyResolutionData.copy(
              identifiers = KnownIdentifiers.empty.copy(
                sts = List(
                  IdentifierContainer(allPaidSts, None, 0),
                  IdentifierContainer(hasUnpaidSts, None, 1),
                  IdentifierContainer(currentlyProcessingSts, None, 3),
                  IdentifierContainer(notProcessedSts, None, 4),
                  IdentifierContainer(mostFreshSts, None, now - 10)
                )
              ),
              fines = Map(
                allPaidSts -> createFinePrepared(_.setIsPaid(BoolValue.of(true)): Unit),
                hasUnpaidSts -> createFinePrepared(_.setIsPaid(BoolValue.of(false)): Unit)
              )
            )
          )
        )

        autocodeFinesStage.shouldProcess(processingState) shouldBe true
        val stsReports = autocodeFinesStage
          .processWithAsync(vin, processingState)
          .state
          .getAutocodeState
          .getStsAutocodeReportsList
          .asScala

        def getShouldProcess(sts: Sts): Boolean = {
          stsReports.find(_.getSts == sts.toString).get.getReport.getShouldProcess
        }
        getShouldProcess(allPaidSts) shouldBe false
        getShouldProcess(hasUnpaidSts) shouldBe false
        getShouldProcess(currentlyProcessingSts) shouldBe false
        getShouldProcess(notProcessedSts) shouldBe true
        getShouldProcess(mostFreshSts) shouldBe true
      }

    "raise shouldProcess for Autocode.Identifiers report " +
      "if STSs are known but was updated too long ago" in {
        val vin = VinCodeGen.sample.get
        val processingState = createProcessingState(
          finesBuild = _.setShouldProcess(true)
            .addStateUpdateHistoryBuilder()
            .setTimestamp(1): Unit,
          identifiersBuild = _.setReportArrived(0): Unit // too long ago
        )

        when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(
          Future.successful(
            emptyResolutionData.copy(
              identifiers = KnownIdentifiers.empty.copy(
                sts = List(
                  IdentifierContainer(Sts("sts1"), None, 0),
                  IdentifierContainer(Sts("sts2"), None, 1)
                )
              )
            )
          )
        )
        doNothing().when(vinUpdateService).upsertUpdate(eq(vin))(?)(?)

        autocodeFinesStage.shouldProcess(processingState) shouldBe true
        autocodeFinesStage.processWithAsync(vin, processingState)
        verify(vinUpdateService).upsertUpdate(eq(vin))(?)(?)
      }

    "just give up if we don't have STSs even though we tried to get fresh recently" in {
      val vin = VinCodeGen.sample.get
      val processingState = createProcessingState(
        finesBuild = _.setShouldProcess(true)
          .addStateUpdateHistoryBuilder()
          .setTimestamp(1),
        identifiersBuild = _.setReportArrived(System.currentTimeMillis - 10) // updated recently
      )

      when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(emptyResolutionData))

      autocodeFinesStage.shouldProcess(processingState) shouldBe true
      val finesState = autocodeFinesStage
        .processWithAsync(vin, processingState)
        .state
        .getAutocodeState
        .findReport(AutocodeReportType.Fines.id)
        .get
      finesState.getShouldProcess shouldBe false
      finesState.getStateUpdateHistoryList.size shouldBe 0
    }

    "raise shouldProcess for Autocode.Identifiers report if we don't have STSs " +
      "and didn't try to get them long ago" in {
        val vin = VinCodeGen.sample.get
        val processingState = createProcessingState(
          finesBuild = _.setShouldProcess(true)
            .addStateUpdateHistoryBuilder()
            .setTimestamp(1),
          identifiersBuild = _.setReportArrived(0) // too long ago
        )

        when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(emptyResolutionData))
        doNothing().when(vinUpdateService).upsertUpdate(eq(vin))(?)(?)

        autocodeFinesStage.shouldProcess(processingState) shouldBe true
        autocodeFinesStage.processWithAsync(vin, processingState)
        verify(vinUpdateService).upsertUpdate(eq(vin))(?)(?)
      }
  }

  private def createCompoundState(
      finesBuild: AutocodeReport.Builder => Any,
      identifiersBuild: AutocodeReport.Builder => Any,
      finesByStsBuilds: Seq[(Sts, AutocodeReport.Builder => Any)] = Seq.empty): CompoundState = {
    val compoundStateBuilder = CompoundState.newBuilder
    finesBuild(
      compoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Fines.id)
    )
    identifiersBuild(
      compoundStateBuilder.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Identifiers.id)
    )
    finesByStsBuilds.foreach { case (sts, fineBuilder) =>
      compoundStateBuilder.getAutocodeStateBuilder
        .addStsAutocodeReportsBuilder()
        .setSts(sts.toString)
        .setReport {
          val reportBuilder = AutocodeReport.newBuilder
            .setReportType(AutocodeReportType.Fines.id)
          fineBuilder(reportBuilder)
          reportBuilder
        }
    }
    compoundStateBuilder.build
  }

  private def createProcessingState(
      finesBuild: AutocodeReport.Builder => Any,
      identifiersBuild: AutocodeReport.Builder => Any,
      finesByStsBuilds: Seq[(Sts, AutocodeReport.Builder => Any)] = Seq.empty): ProcessingState[CompoundState] = {
    createDefaultProcessingState(createCompoundState(finesBuild, identifiersBuild, finesByStsBuilds))
  }

  private def createVinInfoHistoryWithFine(fineBuilder: Fine.Builder => Any): VinInfoHistory = {
    val builder = VinInfoHistory.newBuilder
    fineBuilder(builder.addFinesBuilder())
    builder.build
  }

  private def createFinePrepared(fineBuilder: Fine.Builder => Any): Prepared = {
    Prepared.simulate(createVinInfoHistoryWithFine(fineBuilder))
  }
}
