package ru.yandex.auto.vin.decoder.scheduler.stage.partners

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{doNothing, reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.manager.vin.{KnownIdentifiers, VinDataManager}
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.VinCodeGen
import ru.yandex.auto.vin.decoder.model.{IdentifierContainer, ResolutionData, Sts, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportOrderState.{ReportOrder, StsReportOrder}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, ReportOrderState}
import ru.yandex.auto.vin.decoder.scheduler.engine.{AsyncState, ProcessingState}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.service.vin.VinUpdateService
import ru.yandex.auto.vin.decoder.state.{Autocode, PartnerRequestTrigger}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.ListHasAsScala

class StsStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with CompoundStageSupport[VinCode, StsStage] {
  implicit private val ec = ExecutionContext.global
  implicit private val t = Traced.empty
  implicit private val partner = new Autocode
  implicit private val partnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val vin = VinCode("WBAUE71090E005594")
  private val emptyResolutionData = ResolutionData.empty(vin)

  private val vinDataManager = mock[VinDataManager]
  private val vinUpdateService = mock[VinUpdateService]

  private val ReportFreshness = 30.days

  before {
    reset(vinDataManager)
    reset(vinUpdateService)
  }

  "StsStage" should {

    def vin = VinCodeGen.sample.get
    def cState = CompoundState.newBuilder

    "wait for updating identifiers from autocode" in {
      val state = cState
      state.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Identifiers.id)
        .setShouldProcess(true)
      val stsStage = createProcessingStage()
      val result = RichStage(stsStage).processWithAsync(
        vin,
        ProcessingState(WatchingStateUpdate.defaultSync[CompoundState](state.build()), Nil)
      )
      assert(result.delay.toDuration >= 1.minute)
      assert(result.delay.toDuration <= 5.minutes)
    }

    "request autocode identifiers if they aren't found" in {
      val state = cState
      val stsStage = createProcessingStage()
      when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(emptyResolutionData))
      doNothing().when(vinUpdateService).upsertUpdate(?)(?)(?)
      val result = RichStage(stsStage).processWithAsync(
        vin,
        ProcessingState(WatchingStateUpdate.defaultSync[CompoundState](state.build()), Nil)
      )

      assert(result.delay.toDuration == 0.seconds)
    }

    "re-request sts if an sts isn't fresh" in {
      val state = cState
      state.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Identifiers.id)
        .setReportArrived(System.currentTimeMillis() - (ReportFreshness + 1.day).toMillis)
      val stsStage = createProcessingStage()
      val identifiers = KnownIdentifiers.empty.copy(sts = List(IdentifierContainer.apply(Sts("123"), None, 0)))
      when(vinDataManager.getLatestResolutionData(?, ?)(?))
        .thenReturn(Future.successful(emptyResolutionData.copy(identifiers = identifiers)))
      doNothing().when(vinUpdateService).upsertUpdate(?)(?)(?)
      val result = RichStage(stsStage).processWithAsync(
        vin,
        ProcessingState(WatchingStateUpdate.defaultSync[CompoundState](state.build()), Nil)
      )

      verify(vinUpdateService, times(1)).upsertUpdate(?)(?)(?)
      assert(result.delay.toDuration == 0.seconds)
    }

    "process sts if an sts is fresh" in {
      val state = cState
      state.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Identifiers.id)
        .setReportArrived(System.currentTimeMillis())
      val stsStage = createProcessingStage()
      val identifiers = KnownIdentifiers.empty.copy(sts = List(IdentifierContainer.apply(Sts("123"), None, 0)))
      when(vinDataManager.getLatestResolutionData(?, ?)(?))
        .thenReturn(Future.successful(emptyResolutionData.copy(identifiers = identifiers)))
      val result = RichStage(stsStage).processWithAsync(
        vin,
        ProcessingState(WatchingStateUpdate.defaultSync[CompoundState](state.build()), Nil)
      )

      assert(result.state.getCheckburoState.getStsOrdersList.asScala.nonEmpty)
    }

    "gite up with no sts at all although they were requested recently" in {
      val state = cState
      state.getAutocodeStateBuilder
        .addAutocodeReportsBuilder()
        .setReportType(AutocodeReportType.Identifiers.id)
        .setReportArrived(System.currentTimeMillis())
      val stsStage = createProcessingStage()
      val identifiers = KnownIdentifiers.empty
      when(vinDataManager.getLatestResolutionData(?, ?)(?))
        .thenReturn(Future.successful(emptyResolutionData.copy(identifiers = identifiers)))
      val result = RichStage(stsStage).processWithAsync(
        vin,
        ProcessingState(WatchingStateUpdate.defaultSync[CompoundState](state.build()), Nil)
      )

      assert(result.state.getCreated == -1)
    }

    "update sts if we haven't requested them from autocode and nothing is in the storage" in {
      val state = cState
      val stsStage = createProcessingStage()
      val identifiers = KnownIdentifiers.empty
      when(vinDataManager.getLatestResolutionData(?, ?)(?))
        .thenReturn(Future.successful(emptyResolutionData.copy(identifiers = identifiers)))
      doNothing().when(vinUpdateService).upsertUpdate(?)(?)(?)
      val result = RichStage(stsStage).processWithAsync(
        vin,
        ProcessingState(WatchingStateUpdate.defaultSync[CompoundState](state.build()), Nil)
      )
      verify(vinUpdateService, times(1)).upsertUpdate(?)(?)(?)
      assert(result.delay.toDuration == 0.seconds)
    }
  }

  override def createProcessingStage(): StsStage = new StsStage(vinDataManager, vinUpdateService) {
    override protected def stsFreshness: FiniteDuration = ReportFreshness

    override protected def processStses(
        vin: VinCode,
        resolutionData: ResolutionData,
        stses: List[Sts],
        state: AsyncState[CompoundState]
      )(implicit trigger: PartnerRequestTrigger): AsyncState[CompoundState] = state.withUpdate { st =>
      st.toBuilder
        .setCheckburoState(
          ReportOrderState
            .newBuilder()
            .addStsOrders(
              StsReportOrder
                .newBuilder()
                .setReport(
                  ReportOrder
                    .newBuilder()
                    .setReportType("STS")
                )
            )
            .build()
        )
        .build()
    }

    override protected def cancelProcessing(asyncState: AsyncState[CompoundState]): AsyncState[CompoundState] =
      asyncState.withUpdate { cs =>
        cs.toBuilder.setCreated(-1).build()
      }

    override protected def extractTrigger(compoundState: CompoundState): PartnerRequestTrigger =
      PartnerRequestTrigger.Unknown

    override def shouldProcess(processingState: ProcessingState[CompoundState]): Boolean = true
  }
}
