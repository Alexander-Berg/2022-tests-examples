package ru.yandex.auto.vin.decoder.scheduler.stage.orders

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.ResponseModel.{RawEssentialsReportResponse, RawGibddReportResponse}
import ru.auto.api.vin.VinReportModel
import ru.auto.api.vin.VinReportModel.RawVinReport
import ru.auto.api.vin.orders.OrdersApiModel.{OrderIdentifierType, ReportType}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}
import ru.yandex.auto.vin.decoder.report.ReportDefinition
import ru.yandex.auto.vin.decoder.report.ReportDefinition.{AdvancedReportType, EnumReportType, UnknownReportType}
import ru.yandex.auto.vin.decoder.report.processors.report.{OrderReportManager, ReportDefinitionManager, ReportManager}
import ru.yandex.auto.vin.decoder.scheduler.MockedFeatures
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, ExactDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.OrdersStageSupport
import auto.carfax.common.utils.misc.DateTimeUtils.nowProto
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class OrderReadyStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with MockedFeatures
  with OrdersStageSupport[OrderReadyStage] {

  private val reportManager = mock[ReportManager]
  private val reportDefinitionManager = mock[ReportDefinitionManager]
  private val orderReportManager = mock[OrderReportManager]
  private val stage = createProcessingStage()

  private val TestVin = VinCode("SALVA2BG8CH610042")

  private def orderTemplate(
      reportType: ReportType = ReportType.GIBDD_REPORT,
      status: OrderStatus = OrderStatus.UPDATING): Order = {
    val builder = Order
      .newBuilder()
      .setReportType(reportType)
      .setOrderId(UUID.randomUUID().toString)
      .setCreated(nowProto)
      .setIdentifierType(OrderIdentifierType.VIN)
      .setIdentifier(TestVin.toString)
      .setVin(TestVin.toString)
      .setUserId("dealer:123")
      .setStatus(status)

    builder.getSourcesBuilder.setStartUpdating(nowProto)
    builder.build()
  }

  def gibddReport(ready: Boolean): RawGibddReportResponse = {
    val report = {
      val builder = VinReportModel.RawGibddReport.newBuilder()
      builder.getHeaderBuilder.setIsUpdating(!ready)
      builder.build()
    }
    RawGibddReportResponse.newBuilder().setReport(report).build()
  }

  def fullReport(ready: Boolean): RawEssentialsReportResponse = {
    val report = {
      val builder = VinReportModel.RawVinEssentialsReport.newBuilder()
      builder.getSourcesBuilder.getHeaderBuilder.setIsUpdating(!ready)
      builder.build()
    }
    RawEssentialsReportResponse.newBuilder().setReport(report).build()
  }

  before {
    reset(reportManager)
  }

  "stage.should_process" should {
    "return false" when {
      for {
        status <- OrderStatus.values().filter(s => s != OrderStatus.UPDATING && s != OrderStatus.UNRECOGNIZED)
        reportType <- ReportType.values().filter(r => r != ReportType.UNRECOGNIZED)
      } s"status = $status, report_type = $reportType" in {
        val order = orderTemplate(reportType, status)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
    }
    "return true" when {
      "status = UPDATING" in {
        val order = orderTemplate()
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe true
      }
    }
  }

  "stage.process" should {
    "update status and timestamp" when {
      "gibdd report is ready" in {
        when(reportManager.getGibddReport(?)(?)).thenReturn(Future.successful(gibddReport(ready = true)))

        val order = orderTemplate()
        val state = createProcessingState(order)
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.SUCCESS
        result.state.getSources.hasStartUpdating shouldBe true
        result.state.getSources.hasFinishUpdating shouldBe true

        verify(reportManager, times(1)).getGibddReport(eq(TestVin))(?)
      }
      "full report is ready" in {
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.successful(fullReport(ready = true)))

        val order = orderTemplate(reportType = ReportType.FULL_REPORT)
        val state = createProcessingState(order)
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.SUCCESS
        result.state.getSources.hasStartUpdating shouldBe true
        result.state.getSources.hasFinishUpdating shouldBe true

        verify(reportManager, times(1)).getEssentialsReport(eq(TestVin), ?)(?)
      }
      "custom report is ready" in {
        val readyReportBuilder = RawVinReport.newBuilder().build()
        when(orderReportManager.getEssentialsReport(?, ?, ?)(?)).thenReturn(Future.successful(readyReportBuilder))

        val order = orderTemplate().toBuilder.setReportId("any_id").build()
        val state = createProcessingState(order)

        val reportDefinition = mock[ReportDefinition]
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(AdvancedReportType(reportDefinition)))

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.SUCCESS
        result.state.getSources.hasStartUpdating shouldBe true
        result.state.getSources.hasFinishUpdating shouldBe true
      }
    }
    "reschedule" when {
      "gibdd report is updating" in {
        when(reportManager.getGibddReport(?)(?)).thenReturn(Future.successful(gibddReport(ready = false)))

        val order = orderTemplate()
        val state = createProcessingState(order)
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.UPDATING
        result.state.getSources.hasStartUpdating shouldBe true
        result.state.getSources.hasFinishUpdating shouldBe false
        result.delay.isInstanceOf[ExactDelay] shouldBe true
        result.delay.toDuration.toMinutes < 5 shouldBe true

        verify(reportManager, times(1)).getGibddReport(eq(TestVin))(?)
      }
      "full report is updating" in {
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(Future.successful(fullReport(ready = false)))

        val order = orderTemplate(reportType = ReportType.FULL_REPORT)
        val state = createProcessingState(order)
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.UPDATING
        result.state.getSources.hasStartUpdating shouldBe true
        result.state.getSources.hasFinishUpdating shouldBe false
        result.delay.isInstanceOf[ExactDelay] shouldBe true
        result.delay.toDuration.toMinutes < 5 shouldBe true

        verify(reportManager, times(1)).getEssentialsReport(eq(TestVin), ?)(?)
      }
      "custom report is updating" in {
        val readyReportBuilder = RawVinReport.newBuilder()
        readyReportBuilder.getSourcesBuilder.getHeaderBuilder.setIsUpdating(true)
        when(orderReportManager.getEssentialsReport(?, ?, ?)(?))
          .thenReturn(Future.successful(readyReportBuilder.build()))

        val order = orderTemplate().toBuilder.setReportId("any_id").build()
        val state = createProcessingState(order)
        val reportDefinition = mock[ReportDefinition]
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(AdvancedReportType(reportDefinition)))

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.UPDATING
        result.state.getSources.hasStartUpdating shouldBe true
        result.state.getSources.hasFinishUpdating shouldBe false
        result.delay.isInstanceOf[ExactDelay] shouldBe true
        result.delay.toDuration.toMinutes < 5 shouldBe true
      }
    }
    "throw an error" when {
      "report id is not valid" in {
        val order = orderTemplate().toBuilder.setReportId("any_id").build()
        when(reportDefinitionManager.extractReportType(order)).thenReturn(Future.successful(UnknownReportType))
        val state = createProcessingState(order)
        val error = the[RuntimeException] thrownBy stage.processWithAsync(order.getOrderId, state)
        (error.getCause should have).message(s"Unexpected report type for order ${order.getOrderId}")
      }
    }
  }

  private def createProcessingState(order: Order): ProcessingState[Order] = {
    ProcessingState(WatchingStateUpdate(order, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): OrderReadyStage = {
    new OrderReadyStage(reportManager, reportDefinitionManager, orderReportManager)
  }

}
