package ru.yandex.auto.vin.decoder.scheduler.stage.notifications

import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.sender.SenderClient
import auto.carfax.common.clients.vos.VosClient
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import ru.auto.api.vin.ResponseModel.RawEssentialsReportResponse
import ru.auto.api.vin.VinReportModel.PtsBlock.{IntItem, StringItem}
import ru.auto.api.vin.VinReportModel.{PtsBlock, RawVinEssentialsReport}
import ru.yandex.auto.vin.decoder.model.{CommonVinCode, VinCode}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportNotificationState.NotificationInfo
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.utils.sources.PartnerProgressManager.PartnersProgress
import ru.yandex.auto.vin.decoder.utils.sources.ProgressCounter.IntCounter
import ru.yandex.auto.vin.decoder.utils.sources.{PartnerProgress, PartnerProgressManager, ProgressCounter}
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class ProgressReportNotificationStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfterAll
  with BeforeAndAfter
  with Matchers
  with CompoundStageSupport[VinCode, ProgressReportNotificationStage] {

  private val vinCode = CommonVinCode("WAUZZZF52HA005675")

  private val essentialReport: RawEssentialsReportResponse = {
    val pts =
      PtsBlock
        .newBuilder()
        .setMark(StringItem.newBuilder().setValueText("mark"))
        .setModel(StringItem.newBuilder().setValueText("model"))
        .setYear(IntItem.newBuilder().setValue(2020))

    val report =
      RawVinEssentialsReport
        .newBuilder()
        .setPtsInfo(pts)

    RawEssentialsReportResponse.newBuilder().setReport(report).build()
  }

  val partnerProgressManager: PartnerProgressManager = mock[PartnerProgressManager]
  val passportClient: PassportClient = mock[PassportClient]
  val vosClient: VosClient = mock[VosClient]
  val senderClient: SenderClient = mock[SenderClient]
  val reportManager: ReportManager = mock[ReportManager]

  val stage = new ProgressReportNotificationStage(
    partnerProgressManager,
    passportClient,
    senderClient,
    reportManager,
    vosClient
  )

  before {
    when(reportManager.getEssentialsReport(?, ?)(?))
      .thenReturn(Future.successful(essentialReport))

    when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.unit)

    when(passportClient.getEssentials(?)(?))
      .thenReturn(Future.successful(UserEssentials.newBuilder().setEmail("email@email").build()))
  }

  after {
    reset(reportManager)
    reset(senderClient)
    reset(passportClient)
    reset(partnerProgressManager)
  }

  private def getProgress(counter: ProgressCounter): PartnersProgress = {
    PartnersProgress(Seq(PartnerProgress("", counter, Seq.empty)))
  }

  "ProgressReportNotificationStage" should {
    "send notifications" when {
      "report not ready and there are new notifications" in {

        val enabledDealer = 1L
        val disabledDealer = 2L
        when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, eq(Some(enabledDealer))))
          .thenReturn(getProgress(0.of(1)))
        when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, eq(Some(disabledDealer))))
          .thenReturn(getProgress(0.of(0)))
        when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, eq(None)))
          .thenReturn(getProgress(0.of(1)))

        val info1 =
          NotificationInfo
            .newBuilder()
            .setUserId("test1")
            .setSentTimestamp(System.currentTimeMillis())
            .build()

        val info2 =
          NotificationInfo
            .newBuilder()
            .setUserId("test2")
            .setClientId(enabledDealer)
            .build()

        val info3 =
          NotificationInfo.newBuilder
            .setUserId("test3")
            .setClientId(disabledDealer)
            .build

        val info4 =
          NotificationInfo.newBuilder
            .setUserId("test4")
            .setCancelTimestamp(System.currentTimeMillis())
            .build

        val info5 =
          NotificationInfo
            .newBuilder()
            .setUserId("test5")
            .setCreateTimestamp(System.currentTimeMillis() - 1)
            .build()

        val stateBuilder = CompoundState.newBuilder()

        stateBuilder.getProgressReportNotificationStateBuilder
          .addInfo(info1)
          .addInfo(info2)
          .addInfo(info3)
          .addInfo(info4)
          .addInfo(info5)

        val ps = createDefaultProcessingState(stateBuilder.build())

        val res: WatchingStateUpdate[CompoundState] = stage.processWithAsync(vinCode, ps)

        verify(senderClient).sendLetterWithJsonParams(?, ?, ?)(?)
        verify(partnerProgressManager, times(2)).buildReportProgress(?, ?, ?, ?, ?)
        verify(passportClient).getEssentials(?)(?)

        val newState = res.state.getProgressReportNotificationState

        newState.getInfoCount shouldBe 5
        newState.getInfoList.asScala.count(_.getSentTimestamp != 0) shouldBe 2
        assert(newState.getInfoList.asScala.find(_.getUserId == "test2").get.getSentTimestamp > 0)
      }
    }
    "cancel notifications" when {
      "report is ready" in {
        when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, ?))
          .thenReturn(getProgress(1.of(1)))

        val info1 =
          NotificationInfo
            .newBuilder()
            .setUserId("test")
            .build()

        val info2 =
          NotificationInfo
            .newBuilder()
            .setUserId("test")
            .setSentTimestamp(System.currentTimeMillis())
            .build()

        val stateBuilder = CompoundState.newBuilder()

        stateBuilder.getProgressReportNotificationStateBuilder
          .addInfo(info1)
          .addInfo(info2)

        val ps = createDefaultProcessingState(stateBuilder.build())

        val res: WatchingStateUpdate[CompoundState] = stage.processWithAsync(vinCode, ps)

        verify(senderClient, never()).sendLetterWithJsonParams(?, ?, ?)(?)
        verify(passportClient, never()).getEssentials(?)(?)
        verify(partnerProgressManager).buildReportProgress(?, ?, ?, ?, ?)

        val newState = res.state.getProgressReportNotificationState

        newState.getInfoCount shouldBe 2
        newState.getInfoList.asScala.count(_.getSentTimestamp != 0) shouldBe 1
        newState.getInfoList.asScala.count(_.getCancelTimestamp != 0) shouldBe 1
      }
    }
    "not send notifications" when {
      "notification is too fresh" in {
        when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, ?))
          .thenReturn(getProgress(0.of(1)))

        val info =
          NotificationInfo
            .newBuilder()
            .setUserId("test")
            .setCreateTimestamp(System.currentTimeMillis())
            .build()

        val stateBuilder = CompoundState.newBuilder()

        stateBuilder.getProgressReportNotificationStateBuilder.addInfo(info)

        val ps = createDefaultProcessingState(stateBuilder.build())

        val res: WatchingStateUpdate[CompoundState] = stage.processWithAsync(vinCode, ps)

        verify(senderClient, never()).sendLetterWithJsonParams(?, ?, ?)(?)
        verify(passportClient, never()).getEssentials(?)(?)
        verify(partnerProgressManager, never()).buildReportProgress(?, ?, ?, ?, ?)

        val newState = res.state.getProgressReportNotificationState

        assert(res.delay.toDuration.toMillis <= ProgressReportNotificationStage.SendDelay.toMillis)
        newState.getInfoCount shouldBe 1
        newState.getInfoList.asScala.count(_.getSentTimestamp != 0) shouldBe 0
        newState.getInfoList.asScala.count(_.getCancelTimestamp != 0) shouldBe 0
      }
    }
  }

  override def createProcessingStage(): ProgressReportNotificationStage = ???
}
