package ru.yandex.auto.vin.decoder.scheduler.stage

import auto.carfax.common.clients.promocoder.PromocoderClient
import auto.carfax.common.clients.promocoder.model.{FeatureInstance, FeatureOrigin, FeaturePayload}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, BeforeAndAfter}
import ru.auto.api.vin.ResponseModel.RawEssentialsReportResponse
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.PromocodeState.UserPromocode
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import auto.carfax.common.clients.promocoder.promocoder._

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.Random

class UselessReportPromocodeStageSpec
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with CompoundStageSupport[VinCode, UselessReportPromocodeStage] {

  implicit val ops: TestOperationalSupport.type = TestOperationalSupport
  val promocoderClient: PromocoderClient = mock[PromocoderClient]
  val reportManager: ReportManager = mock[ReportManager]
  val stage: UselessReportPromocodeStage = createProcessingStage()
  val vin: VinCode = VinCode("X9FPXXEEDP9C44295")

  before {
    Mockito.reset(reportManager)
    Mockito.reset(promocoderClient)
  }

  "UselessReportPromocodeStage" should {

    "reschedule with random delay if usefulness status is UNKNOWN" in {
      val state = createDefaultProcessingState(testState())
      when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.successful(buildReport(Status.UNKNOWN)))
      checkShouldProcess(state)
      val update = stage.processWithAsync(vin, state)
      assert(update.delay.toDuration <= 5.minute)
      assert(update.delay.toDuration >= 1.minute)
      assert(
        state.compoundStateUpdate.state.getUselessReportPromocodeState.getPromocodesList.size() ==
          update.state.getUselessReportPromocodeState.getPromocodesList.size()
      )
    }

    "reschedule with exact delay if got promocoder error" in {
      val state = createDefaultProcessingState(testState())
      when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.successful(buildReport(Status.ERROR)))
      when(promocoderClient.releaseDiscount(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException))
      checkShouldProcess(state)
      val update = stage.processWithAsync(vin, state)
      update.delay.toDuration shouldBe 1.minute
    }

    "reschedule with exact delay if got report manager error" in {
      val state = createDefaultProcessingState(testState())
      when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.failed(new RuntimeException))
      checkShouldProcess(state)
      val update = stage.processWithAsync(vin, state)
      update.delay.toDuration shouldBe 1.minute
    }

    "update cancel_timestamp if usefulness == OK" in {
      val state = createDefaultProcessingState(testState(sentCount = 2, cancelledCount = 2, unsentCount = 2))
      when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.successful(buildReport(Status.OK)))
      checkShouldProcess(state)
      val update = stage.processWithAsync(vin, state)

      assert(update.delay.toDuration <= 10.minute)
      assert(update.delay.toDuration >= 5.minute)
      verify(promocoderClient, never()).releaseDiscount(?, ?, ?)(?)
      verifyState(update.state, 2, 4)
    }

    "update release_timestamp if usefulness == ERROR promocode released" in {
      val state = createDefaultProcessingState(testState(sentCount = 2, cancelledCount = 2, unsentCount = 2))
      when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.successful(buildReport(Status.ERROR)))
      when(promocoderClient.releaseDiscount(?, ?, ?)(?)).thenReturn(mockPromocoderResponse())
      checkShouldProcess(state)
      val update = stage.processWithAsync(vin, state)

      assert(update.delay.toDuration <= 10.minute)
      assert(update.delay.toDuration >= 5.minute)
      verify(promocoderClient, times(2)).releaseDiscount(?, ?, ?)(?)
      verifyState(update.state, 4, 2)
    }
  }

  override def createProcessingStage(): UselessReportPromocodeStage =
    new UselessReportPromocodeStage(reportManager, promocoderClient)

  private def testState(sentCount: Int = 2, unsentCount: Int = 2, cancelledCount: Int = 2) = {
    val stateBuilder = CompoundState.newBuilder()

    (0 until sentCount).foreach { _ =>
      stateBuilder.getUselessReportPromocodeStateBuilder.addPromocodes(
        UserPromocode
          .newBuilder()
          .setCreateTimestamp(System.currentTimeMillis())
          .setUserId(s"user:${Random.nextInt()}")
          .setReleaseTimestamp(System.currentTimeMillis())
      )
    }

    (0 until unsentCount).foreach { _ =>
      stateBuilder.getUselessReportPromocodeStateBuilder.addPromocodes(
        UserPromocode
          .newBuilder()
          .setCreateTimestamp(System.currentTimeMillis())
          .setUserId(s"user:${Random.nextInt()}")
      )
    }

    (0 until cancelledCount).foreach { _ =>
      stateBuilder.getUselessReportPromocodeStateBuilder.addPromocodes(
        UserPromocode
          .newBuilder()
          .setCreateTimestamp(System.currentTimeMillis())
          .setUserId(s"user:${Random.nextInt()}")
          .setCancelTimestamp(System.currentTimeMillis())
      )
    }

    stateBuilder.build()
  }

  private def mockPromocoderResponse(): Future[List[FeatureInstance]] = {
    val features = List(
      FeatureInstance(
        "id",
        FeatureOrigin("id"),
        FeatureTags.OneReport,
        "user:123",
        1,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        FeaturePayload()
      )
    )
    Future.successful(features)
  }

  private def verifyState(state: CompoundState, releasedCount: Int, cancelledCount: Int): Assertion = {
    val (released, others) =
      state.getUselessReportPromocodeState.getPromocodesList.asScala.toList.partition(_.getReleaseTimestamp != 0)
    val (canceled, unsent) = others.partition(_.getCancelTimestamp != 0)
    assert(released.size == releasedCount)
    assert(canceled.size == cancelledCount)
    assert(unsent.isEmpty)
  }

  private def buildReport(status: Status) = {
    val response = RawEssentialsReportResponse.newBuilder()
    response.getReportBuilder.setUsefulness(status)
    response.build()
  }
}
