package ru.yandex.auto.vin.decoder.scheduler.stage.orders

import akka.actor.ActorSystem
import akka.testkit.TestKit
import auto.carfax.common.clients.vos.VosClient
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel.{Category, Offer, OfferStatus, Section}
import ru.auto.api.vin.orders.OrdersApiModel.{FailReason, OrderIdentifierType, ReportType}
import ru.yandex.auto.vin.decoder.api.exceptions.InvalidLicensePlateException
import ru.yandex.auto.vin.decoder.manager.RelationshipManager
import ru.yandex.auto.vin.decoder.manager.orders.OrderParamsValidator
import ru.yandex.auto.vin.decoder.model.{AutoruOfferId, LicensePlate, VinCode}
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}
import ru.yandex.auto.vin.decoder.report.ReportDefinition
import ru.yandex.auto.vin.decoder.report.ReportDefinition.{AdvancedReportType, EnumReportType, UnknownReportType}
import ru.yandex.auto.vin.decoder.report.processors.report.ReportDefinitionManager
import ru.yandex.auto.vin.decoder.scheduler.MockedFeatures
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.OrdersStageSupport
import auto.carfax.common.utils.misc.DateTimeUtils.nowProto
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class PrepareOrderStageTest
  extends TestKit(ActorSystem("test-system"))
  with AnyWordSpecLike
  with MockedFeatures
  with MockitoSupport
  with BeforeAndAfter
  with OrdersStageSupport[PrepareOrderStage] {

  private val vosClient = mock[VosClient]
  private val relationshipManager = mock[RelationshipManager]
  private val reportDefinitionManager = mock[ReportDefinitionManager]
  private val stage = createProcessingStage()

  private val TestVin = VinCode("SALVA2BG8CH610042")
  private val TestLp = LicensePlate("T700TT62")
  private val TestOfferrId = AutoruOfferId.check("123-abc").get

  private def orderTemplate(
      status: OrderStatus = OrderStatus.PREPARING,
      reportType: ReportType = ReportType.GIBDD_REPORT,
      identifierType: OrderIdentifierType = OrderIdentifierType.VIN,
      identifier: String = TestVin.toString): Order = {
    val builder = Order
      .newBuilder()
      .setReportType(reportType)
      .setOrderId(UUID.randomUUID().toString)
      .setCreated(nowProto)
      .setIdentifierType(identifierType)
      .setIdentifier(identifier)
      .setUserId("dealer:20101")
      .setStatus(status)

    builder.build()
  }

  private def buildOffer(
      status: OfferStatus,
      vin: Option[String],
      section: Section,
      category: Category): Offer = {
    val builder = Offer.newBuilder()
    vin.foreach(builder.getDocumentsBuilder.setVin)
    builder.setStatus(status).setCategory(category).setSection(section)

    builder.build()
  }

  "stage.should_process" should {
    "return false" when {
      "vin non empty" in {
        val order = {
          val builder = orderTemplate().toBuilder
          builder.setVin(TestVin.toString)
          builder.build()
        }

        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe false
      }
      for (
        status <-
          OrderStatus.values().filter(status => status != OrderStatus.PREPARING && status != OrderStatus.UNRECOGNIZED)
      )
        s"status = $status" in {
          val order = {
            val builder = orderTemplate().toBuilder
            builder.setStatus(status)
            builder.build()
          }

          val state = createProcessingState(order)

          stage.shouldProcess(state) shouldBe false
        }
    }
    "return true" when {
      "vin is empty and status is PREPARING" in {
        val order = orderTemplate()

        val state = createProcessingState(order)

        stage.shouldProcess(state) shouldBe true
      }
    }
  }

  "stage.process" should {
    "set INVALID_VIN error" when {
      "vin is not valid" in {
        val order = {
          val builder = orderTemplate().toBuilder
          builder.setIdentifier("a").setIdentifierType(OrderIdentifierType.VIN).build()
        }
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.INVALID_IDENTIFIER
        result.delay.isDefault shouldBe false
      }
    }
    "set vin" when {
      "IDENTIFIER_TYPE = vin and vin is valid" in {
        val order = orderTemplate()
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.PREPARING
        result.state.getError shouldBe FailReason.UNKNOWN_ERROR
        result.state.getVin shouldBe TestVin.toString
        result.delay.isDefault shouldBe false
      }
      "report id is valid" in {
        val reportDefinition = mock[ReportDefinition]
        when(reportDefinition.id).thenReturn("any_id")
        val order = orderTemplate().toBuilder.setReportId("any_id").build()

        when(features.EnableOrderByStringReportId).thenReturn(enabledFeature)
        when(reportDefinition.toString).thenReturn(order.getReportId)
        when(reportDefinition.supportedIdentifierTypes).thenReturn(Set(OrderIdentifierType.VIN))
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(AdvancedReportType(reportDefinition)))

        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.PREPARING
        result.state.getError shouldBe FailReason.UNKNOWN_ERROR
        result.state.getVin shouldBe TestVin.toString
        result.delay.isDefault shouldBe false
      }
    }
    "throw error on unexpected report type" in {
      val order = orderTemplate().toBuilder.setReportId("any_id").build()
      when(reportDefinitionManager.extractReportType(order)).thenReturn(Future.successful(UnknownReportType))

      val state = createProcessingState(order)

      val result = stage.processWithAsync(order.getOrderId, state)

      result.state.getStatus shouldBe OrderStatus.FAILED
      result.state.getError shouldBe FailReason.NOT_SUPPORTED_REPORT_TYPE
      result.delay.isDefault shouldBe false
    }
  }

  "prepared gibdd report" should {
    "set vin" when {
      "order valid" in {
        val order = orderTemplate(
          reportType = ReportType.GIBDD_REPORT,
          identifierType = OrderIdentifierType.VIN,
          identifier = TestVin.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.PREPARING
        result.state.getError shouldBe FailReason.UNKNOWN_ERROR
        result.state.getVin shouldBe TestVin.toString
        result.delay.isDefault shouldBe false
      }
    }
  }

  "prepared full report" should {
    "set vin" when {
      "identifier_type = vin and vin is valid" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.VIN,
          identifier = TestVin.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.PREPARING
        result.state.getError shouldBe FailReason.UNKNOWN_ERROR
        result.state.getVin shouldBe TestVin.toString
        result.delay.isDefault shouldBe false
      }
      "identifier_type = license_plate and relationship manager return vin" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.LICENSE_PLATE,
          identifier = TestLp.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(relationshipManager.resolveVin(?, ?)(?, ?, ?)).thenReturn(Future.successful(TestVin))
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.PREPARING
        result.state.getError shouldBe FailReason.UNKNOWN_ERROR
        result.state.getVin shouldBe TestVin.toString
        result.delay.isDefault shouldBe false
      }
      "identifier_type = offer_id and vos return active offer with valid vin" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.AUTORU_OFFER_ID,
          identifier = TestOfferrId.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(vosClient.getOffer(?)(?))
          .thenReturn(
            Future.successful(Some(buildOffer(OfferStatus.ACTIVE, Some(TestVin.toString), Section.USED, Category.CARS)))
          )
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.PREPARING
        result.state.getError shouldBe FailReason.UNKNOWN_ERROR
        result.state.getVin shouldBe TestVin.toString
        result.delay.isDefault shouldBe false
      }
    }
    "set error IDENTIFIER_NOT_FOUND" when {
      "identifier_type = lp and relationship manager throw UNKNOWN_LICENSE_PLATE" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.LICENSE_PLATE,
          identifier = TestLp.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(relationshipManager.resolveVin(?, ?)(?, ?, ?))
          .thenReturn(Future.failed(InvalidLicensePlateException(TestLp)))
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.IDENTIFIER_NOT_FOUND
        result.state.getVin shouldBe ""
        result.delay.isDefault shouldBe false
      }
      "identifier_type = offer_id and vos return None" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.AUTORU_OFFER_ID,
          identifier = TestOfferrId.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(vosClient.getOffer(?)(?))
          .thenReturn(Future.successful(None))
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.IDENTIFIER_NOT_FOUND
        result.state.getVin shouldBe ""
        result.delay.isDefault shouldBe false
      }
    }
    "set error REPORT_NOT_AVAILABLE" when {
      "identifier_type = offer_id and vos return inactive offer" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.AUTORU_OFFER_ID,
          identifier = TestOfferrId.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(vosClient.getOffer(?)(?))
          .thenReturn(
            Future.successful(
              Some(buildOffer(OfferStatus.EXPIRED, Some(TestVin.toString), Section.USED, Category.CARS))
            )
          )
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.REPORT_NOT_AVAILABLE
        result.state.getVin shouldBe ""
        result.delay.isDefault shouldBe false
      }
      "identifier_type = offer_id and vos return offer without vin" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.AUTORU_OFFER_ID,
          identifier = TestOfferrId.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(vosClient.getOffer(?)(?))
          .thenReturn(Future.successful(Some(buildOffer(OfferStatus.ACTIVE, None, Section.USED, Category.CARS))))
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.REPORT_NOT_AVAILABLE
        result.state.getVin shouldBe ""
        result.delay.isDefault shouldBe false
      }
      "identifier_type = offer_id and vos return new offer" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.AUTORU_OFFER_ID,
          identifier = TestOfferrId.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(vosClient.getOffer(?)(?))
          .thenReturn(
            Future.successful(Some(buildOffer(OfferStatus.ACTIVE, Some(TestVin.toString), Section.NEW, Category.CARS)))
          )
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.REPORT_NOT_AVAILABLE
        result.state.getVin shouldBe ""
        result.delay.isDefault shouldBe false
      }
      "identifier_type = offer_id and vos return not car offer" in {
        val order = orderTemplate(
          reportType = ReportType.FULL_REPORT,
          identifierType = OrderIdentifierType.AUTORU_OFFER_ID,
          identifier = TestOfferrId.toString
        )
        when(reportDefinitionManager.extractReportType(order))
          .thenReturn(Future.successful(EnumReportType(order.getReportType)))

        val state = createProcessingState(order)

        when(vosClient.getOffer(?)(?))
          .thenReturn(
            Future.successful(Some(buildOffer(OfferStatus.ACTIVE, Some(TestVin.toString), Section.USED, Category.MOTO)))
          )
        val result = stage.processWithAsync(order.getOrderId, state)

        result.state.getStatus shouldBe OrderStatus.FAILED
        result.state.getError shouldBe FailReason.REPORT_NOT_AVAILABLE
        result.state.getVin shouldBe ""
        result.delay.isDefault shouldBe false
      }
    }
  }

  private def createProcessingState(order: Order): ProcessingState[Order] = {
    ProcessingState(WatchingStateUpdate(order, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): PrepareOrderStage = {
    new PrepareOrderStage(new OrderParamsValidator(features), vosClient, relationshipManager, reportDefinitionManager)
  }

}
