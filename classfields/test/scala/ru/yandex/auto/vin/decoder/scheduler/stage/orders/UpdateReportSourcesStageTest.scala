package ru.yandex.auto.vin.decoder.scheduler.stage.orders

import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.orders.OrdersApiModel.{OrderIdentifierType, ReportType}
import ru.yandex.auto.vin.decoder.manager.vin.VinUpdateManager
import ru.yandex.auto.vin.decoder.model.{AutoruDealer, ContextForRequestTrigger, UserRef, VinCode}
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.Billing.BillingStatus
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}
import ru.yandex.auto.vin.decoder.report.ReportDefinition
import ru.yandex.auto.vin.decoder.report.ReportDefinition.{AdvancedReportType, EnumReportType, UnknownReportType}
import ru.yandex.auto.vin.decoder.report.processors.report.ReportDefinitionManager
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, ExactDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.OrdersStageSupport
import auto.carfax.common.utils.misc.DateTimeUtils.nowProto
import ru.yandex.auto.vin.decoder.utils.enumerations.VinUpdatePart
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class UpdateReportSourcesStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with OrdersStageSupport[UpdateReportSourcesStage] {

  private val vinUpdateManager = mock[VinUpdateManager]
  private val reportDefinitionManager = mock[ReportDefinitionManager]
  private val stage = createProcessingStage()

  before {
    reset(vinUpdateManager)
  }

  private val TestVin = VinCode("SALVA2BG8CH610042")

  private def makeContext(order: Order) =
    ContextForRequestTrigger(
      None,
      UserRef.parse("dealer:123").map(_.toPlain),
      Some(AutoruDealer(123L)),
      None,
      Some(order.getOrderId)
    )

  private def orderTemplate(
      billingStatus: BillingStatus,
      reportType: ReportType = ReportType.GIBDD_REPORT): Order = {
    val builder = Order
      .newBuilder()
      .setReportType(reportType)
      .setOrderId(UUID.randomUUID().toString)
      .setCreated(nowProto)
      .setIdentifierType(OrderIdentifierType.VIN)
      .setIdentifier(TestVin.toString)
      .setVin(TestVin.toString)
      .setUserId("dealer:123")

    builder.getBillingBuilder.setStatus(billingStatus)
    builder.build()
  }

  "stage.should_process" should {
    "return false" when {
      "billing is not ready" in {
        val order = orderTemplate(billingStatus = BillingStatus.WAITING)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      "billing is not started" in {
        val order = orderTemplate(billingStatus = BillingStatus.UNKNOWN_BILLING_STATUS)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      "billing failed" in {
        val order = orderTemplate(billingStatus = BillingStatus.FAILED)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      "sources already triggered" in {
        val order = {
          val builder = orderTemplate(billingStatus = BillingStatus.SUCCESS).toBuilder
          builder.getSourcesBuilder.setStartUpdating(nowProto)
          builder.build()
        }

        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
    }
    "return true" when {
      "billing status = success and not update sources yet" in {
        val order = orderTemplate(billingStatus = BillingStatus.SUCCESS)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe true
      }
    }
  }

  "stage.process" should {
    "processed" when {
      "sources for gibdd report successfully triggered" in {
        when(vinUpdateManager.scheduleVinForUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val order = orderTemplate(billingStatus = BillingStatus.SUCCESS, reportType = ReportType.GIBDD_REPORT)
        when(reportDefinitionManager.extractReportType(any[Order]()))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))
        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.UPDATING
        result.state.getSources.hasStartUpdating shouldBe true
        result.delay shouldBe ExactDelay(1.minute)

        verify(vinUpdateManager).scheduleVinForUpdate(
          eq(TestVin),
          eq(VinUpdatePart.REPORT_GIBDD),
          eq(makeContext(order))
        )(?)
      }
      "sources for full report successfully triggered" in {
        when(vinUpdateManager.scheduleVinForUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val order = orderTemplate(billingStatus = BillingStatus.SUCCESS, reportType = ReportType.FULL_REPORT)
        when(reportDefinitionManager.extractReportType(any[Order]()))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))
        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.UPDATING
        result.state.getSources.hasStartUpdating shouldBe true
        result.delay shouldBe ExactDelay(1.minute)

        verify(vinUpdateManager).scheduleVinForUpdate(
          eq(TestVin),
          eq(VinUpdatePart.REPORT),
          eq(makeContext(order))
        )(?)
      }
    }
    "throw an error on unexpected report type" in {
      val order = orderTemplate(billingStatus = BillingStatus.SUCCESS).toBuilder.setReportId("any_id").build()
      when(reportDefinitionManager.extractReportType(any[Order]())).thenReturn(Future.successful(UnknownReportType))

      val state = createProcessingState(order)
      val error = the[RuntimeException] thrownBy stage.processWithAsync(order.getOrderId, state)
      (error.getCause should have).message(s"Unexpected report type in ${order.getOrderId}")
    }
    "trigger updates for existing report definition" in {
      val user = UserRef.dealer(123L)

      val order = orderTemplate(billingStatus = BillingStatus.SUCCESS, reportType = ReportType.FULL_REPORT).toBuilder
        .setReportId("any_id")
        .build()
      val state = createProcessingState(order)

      when(vinUpdateManager.updateByDefinition(?, ?, ?)(?)).thenReturn(Future.unit)
      val reportDefinition = mock[ReportDefinition]
      when(reportDefinition.toString).thenReturn("")
      when(reportDefinitionManager.extractReportType(order))
        .thenReturn(Future.successful(AdvancedReportType(reportDefinition)))

      stage.processWithAsync(order.getOrderId, state)

      verify(vinUpdateManager).updateByDefinition(
        eq(TestVin),
        eq(ContextForRequestTrigger(None, user.some.map(_.toPlain), user.some, None, order.getOrderId.some)),
        eq(reportDefinition)
      )(?)
    }
  }

  private def createProcessingState(order: Order): ProcessingState[Order] = {
    ProcessingState(WatchingStateUpdate(order, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): UpdateReportSourcesStage = {
    new UpdateReportSourcesStage(vinUpdateManager, reportDefinitionManager)
  }
}
