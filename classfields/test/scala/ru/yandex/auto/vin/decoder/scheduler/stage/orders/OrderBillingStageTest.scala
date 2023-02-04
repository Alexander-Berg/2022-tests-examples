package ru.yandex.auto.vin.decoder.scheduler.stage.orders

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.orders.OrdersApiModel.{FailReason, OrderIdentifierType, ReportType}
import ru.auto.salesman.products.ProductsOuterClass
import ru.auto.salesman.products.ProductsOuterClass.Product.ProductStatus
import ru.yandex.auto.vin.decoder.model.{UserRef, VinCode}
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.Billing.BillingStatus
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}
import ru.yandex.auto.vin.decoder.salesman.{ProductAlreadyExists, ProductCreated, SalesmanManager}
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.OrdersStageSupport
import auto.carfax.common.utils.misc.DateTimeUtils.nowProto
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class OrderBillingStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with OrdersStageSupport[OrderBillingStage] {

  implicit val t = Traced.empty

  private val DomainMap = Map(
    ReportType.GIBDD_REPORT -> SalesmanManager.GibddReportDomain,
    ReportType.FULL_REPORT -> SalesmanManager.FullReportDomain
  )

  private val ReportTypes = List(ReportType.GIBDD_REPORT, ReportType.FULL_REPORT)

  private val salesmanManager = mock[SalesmanManager]
  private val stage = createProcessingStage()

  private val TestVin = VinCode("SALVA2BG8CH610042")

  before {
    reset(salesmanManager)
  }

  private def orderTemplate(billingStatus: BillingStatus, reportType: ReportType = ReportType.GIBDD_REPORT): Order = {
    val builder = Order
      .newBuilder()
      .setReportType(reportType)
      .setOrderId(UUID.randomUUID().toString)
      .setCreated(nowProto)
      .setIdentifierType(OrderIdentifierType.VIN)
      .setIdentifier(TestVin.toString)
      .setVin(TestVin.toString)
      .setUserId("dealer:20101")
      .setStatus(OrderStatus.PREPARING)

    builder.getBillingBuilder.setStatus(billingStatus)
    builder.build()
  }

  private def productTemplate(productStatus: ProductStatus) = {
    ProductsOuterClass.Product.newBuilder().setStatus(productStatus).build()
  }

  "stage.should_process" should {
    "return false" when {
      "billing status = FAILED" in {
        val order = orderTemplate(BillingStatus.FAILED)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      "billing status = SUCCESS" in {
        val order = orderTemplate(BillingStatus.SUCCESS)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      "vin is empty" in {
        val order = {
          val builder = orderTemplate(BillingStatus.UNKNOWN_BILLING_STATUS).toBuilder
          builder.clearVin()
          builder.build()
        }
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      "order status is failed" in {
        val order = {
          val builder = orderTemplate(BillingStatus.UNKNOWN_BILLING_STATUS).toBuilder
          builder.setStatus(OrderStatus.FAILED)
          builder.build()
        }
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
    }
    "return true" when {
      "unknown billing status" in {
        val order = orderTemplate(BillingStatus.UNKNOWN_BILLING_STATUS)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe true
      }
      "billing status = WAITING" in {
        val order = orderTemplate(BillingStatus.WAITING)
        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe true
      }
    }
  }

  "stage.process" should {
    "create product and update status" when {
      for (reportType <- ReportTypes)
        s"report_type: $reportType - billing status unknown and manager return success" in {

          val order = orderTemplate(BillingStatus.UNKNOWN_BILLING_STATUS, reportType)
          when(salesmanManager.createReportProduct(?, ?, ?)(?)).thenReturn(Future.successful(ProductCreated))

          val state = createProcessingState(order)

          val result = stage.processWithAsync(order.getOrderId, state)

          result.state.getStatus shouldBe OrderStatus.PREPARING
          result.state.getBilling.getStatus shouldBe BillingStatus.WAITING
          result.state.getBilling.getUpdated.getSeconds > 0 shouldBe true
          result.delay.isDefault shouldBe false

          verify(salesmanManager, times(1)).createReportProduct(
            eq(UserRef.parseOrThrow(order.getUserId)),
            eq(DomainMap(reportType)),
            eq(order.getOrderId)
          )(?)
        }
      for (reportType <- ReportTypes)
        s"report_type: $reportType - billing status unknown and manager return already exists" in {

          val order = orderTemplate(BillingStatus.UNKNOWN_BILLING_STATUS, reportType)
          when(salesmanManager.createReportProduct(?, ?, ?)(?)).thenReturn(Future.successful(ProductAlreadyExists))

          val state = createProcessingState(order)

          val result = stage.processWithAsync(order.getOrderId, state)

          result.state.getStatus shouldBe OrderStatus.PREPARING
          result.state.getBilling.getStatus shouldBe BillingStatus.WAITING
          result.state.getBilling.getUpdated.getSeconds > 0 shouldBe true
          result.delay.isDefault shouldBe false

          verify(salesmanManager, times(1)).createReportProduct(
            eq(UserRef.parseOrThrow(order.getUserId)),
            eq(DomainMap(reportType)),
            eq(order.getOrderId)
          )(?)
        }
    }
    "get product result and update status" when {
      for (reportType <- ReportTypes)
        s"report_type: $reportType - billing status waiting and manager response with active product" in {

          val order = orderTemplate(BillingStatus.WAITING, reportType)
          when(salesmanManager.getReportProduct(?, ?, ?)(?))
            .thenReturn(Future.successful(Some(productTemplate(ProductStatus.ACTIVE))))

          val state = createProcessingState(order)

          val result = stage.processWithAsync(order.getOrderId, state)

          result.state.getStatus shouldBe OrderStatus.PREPARING
          result.state.getBilling.getStatus shouldBe BillingStatus.SUCCESS
          result.state.getBilling.getUpdated.getSeconds > 0 shouldBe true
          result.delay.isDefault shouldBe false

          verify(salesmanManager, times(1)).getReportProduct(
            eq(UserRef.parseOrThrow(order.getUserId)),
            eq(DomainMap(reportType)),
            eq(order.getOrderId)
          )(?)
        }
      for {
        status <- List(ProductStatus.FAILED, ProductStatus.INACTIVE)
        reportType <- List(ReportType.GIBDD_REPORT, ReportType.FULL_REPORT)
      } {
        s"report_type: $reportType - billing status waiting and manager response with $status product" in {

          val order = orderTemplate(BillingStatus.WAITING, reportType)
          when(salesmanManager.getReportProduct(?, ?, ?)(?))
            .thenReturn(Future.successful(Some(productTemplate(status))))

          val state = createProcessingState(order)

          val result = stage.processWithAsync(order.getOrderId, state)

          result.state.getStatus shouldBe OrderStatus.FAILED
          result.state.getError shouldBe FailReason.PAYMENT_FAILED
          result.state.getBilling.getStatus shouldBe BillingStatus.FAILED
          result.state.getBilling.getUpdated.getSeconds > 0 shouldBe true
          result.delay.isDefault shouldBe false

          verify(salesmanManager, times(1)).getReportProduct(
            eq(UserRef.parseOrThrow(order.getUserId)),
            eq(DomainMap(reportType)),
            eq(order.getOrderId)
          )(?)
        }
      }
    }
    "get product result and reschedule" when {
      for (reportType <- ReportTypes)
        s"report_type: $reportType - billing status waiting and manager response with need payment product" in {

          val order = {
            val builder = orderTemplate(BillingStatus.WAITING, reportType).toBuilder
            builder.getBillingBuilder.getUpdatedBuilder.setSeconds(123L)
            builder.build()
          }
          when(salesmanManager.getReportProduct(?, ?, ?)(?))
            .thenReturn(Future.successful(Some(productTemplate(ProductStatus.NEED_PAYMENT))))

          val state = createProcessingState(order)

          val result = stage.processWithAsync(order.getOrderId, state)

          result.state.getStatus shouldBe OrderStatus.PREPARING
          result.state.getBilling.getStatus shouldBe BillingStatus.WAITING
          result.state.getBilling.getUpdated.getSeconds shouldBe 123L
          result.delay.isDefault shouldBe false

          verify(salesmanManager, times(1)).getReportProduct(
            eq(UserRef.parseOrThrow(order.getUserId)),
            eq(DomainMap(reportType)),
            eq(order.getOrderId)
          )(?)
        }
      for (reportType <- ReportTypes)
        s"report_type: $reportType - billing status waiting and manager response with not found" in {
          val order = {
            val builder = orderTemplate(BillingStatus.WAITING, reportType).toBuilder
            builder.getBillingBuilder.getUpdatedBuilder.setSeconds(123L)
            builder.build()
          }
          when(salesmanManager.getReportProduct(?, ?, ?)(?)).thenReturn(Future.successful(None))

          val state = createProcessingState(order)

          val result = stage.processWithAsync(order.getOrderId, state)

          result.state.getStatus shouldBe OrderStatus.PREPARING
          result.state.getBilling.getStatus shouldBe BillingStatus.WAITING
          result.state.getBilling.getUpdated.getSeconds shouldBe 123L
          result.delay.isDefault shouldBe false

          verify(salesmanManager, times(1)).getReportProduct(
            eq(UserRef.parseOrThrow(order.getUserId)),
            eq(DomainMap(reportType)),
            eq(order.getOrderId)
          )(?)
        }
    }
    "skip payment process because non-empty report_id means no billing" in {
      val order = orderTemplate(billingStatus = BillingStatus.WAITING).toBuilder
        .setReportId("any_id")
        .setSkipBilling(true)
        .build()
      val state = createProcessingState(order)

      val result = stage.processWithAsync(order.getOrderId, state)

      result.state.getStatus shouldBe OrderStatus.PREPARING
      result.state.getBilling.getStatus shouldBe BillingStatus.SUCCESS
      result.state.getBilling.getUpdated.getSeconds > 0 shouldBe true
      result.delay.isDefault shouldBe false
    }
  }

  private def createProcessingState(order: Order): ProcessingState[Order] = {
    ProcessingState(WatchingStateUpdate(order, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): OrderBillingStage = {
    new OrderBillingStage(salesmanManager, Feature("", _ => 30))
  }

}
