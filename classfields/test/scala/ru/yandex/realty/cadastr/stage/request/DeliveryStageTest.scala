package ru.yandex.realty.cadastr.stage.request

import org.mockito.Mockito._
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach}
import ru.yandex.realty.cadastr.backend.ReportConversion.ReportConversion
import ru.yandex.realty.cadastr.backend.{CadastrEventProducer, ReportConversion, ReportStatusManager}
import ru.yandex.realty.cadastr.dao.{OfferDao, ReportDao}
import ru.yandex.realty.cadastr.excerpt.service.ExcerptService
import ru.yandex.realty.cadastr.gen.CadastrModelsGen
import ru.yandex.realty.cadastr.model.enums.RequestStatus
import ru.yandex.realty.cadastr.model.{ExcerptUtils, Offer, Report}
import ru.yandex.realty.cadastr.proto.api.excerpt.ExcerptReportStatus
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class DeliveryStageTest extends AsyncWordSpec with BeforeAndAfterEach with MockitoSupport with CadastrModelsGen {
  val offerDao = mock[OfferDao]
  val reportDao = mock[ReportDao]
  val reportStatusManager = mock[ReportStatusManager]
  val cadastrEventProducer = mock[CadastrEventProducer]
  val excerptService = mock[ExcerptService]
  val deliveryStage = new DeliveryStage(offerDao, reportDao, reportStatusManager, cadastrEventProducer)
  implicit val traced: Traced = Traced.empty

  override protected def beforeEach(): Unit = {
    reset(offerDao)
    reset(reportDao)
    reset(reportStatusManager)
    reset(cadastrEventProducer)
    reset(excerptService)
  }

  "DeliveryStage" should {
    "process parsed request with paid report" in {
      val request = requestGen(statusOpt = Some(RequestStatus.Parsed)).next
      val excerptId = Some(ExcerptUtils.generateExcerptId())
      val processingState = ProcessingState(request).withWatcherContext(
        RequestWatcherContext(excerptService, excerptId = excerptId)
      )
      val report = reportGen().next

      when(reportDao.getById(eq(request.reportId.get))).thenReturn(Future.successful(report))
      when(
        cadastrEventProducer
          .produceExcerptRequestStatusChangedEvent(?, eq(excerptId))
      ).thenReturn(Future.unit)

      deliveryStage.process(processingState).map { state =>
        verify(cadastrEventProducer)
          .produceExcerptRequestStatusChangedEvent(?, eq(excerptId))
        assert(state.entry.status == RequestStatus.Delivered)
      }
    }

    "process parsed request with build report" in {
      val request = requestGen(statusOpt = Some(RequestStatus.Parsed)).next
      val excerptId = Some(ExcerptUtils.generateExcerptId())
      val report = reportGen().next
      val processingState = ProcessingState(request).withWatcherContext(
        RequestWatcherContext(excerptService, builtReport = Some(report), excerptId = excerptId)
      )
      val offer = offerGen.next
      val excerptReportStatus = ExcerptReportStatus.newBuilder().build()

      when(reportDao.getById(eq(request.reportId.get))).thenReturn(Future.successful(report.copy(paidReportId = None)))
      when(offerDao.getByCadastralNumber(eq(request.cadastralNumber)))
        .thenReturn(Future.successful(List(offer)))
      when(
        reportStatusManager.buildReportStatus(
          eq(offer): Offer,
          eq(report): Report,
          eq(ReportConversion.ForModeration): ReportConversion
        )
      ).thenReturn(excerptReportStatus)
      when(cadastrEventProducer.produceReportStatusChangedEvent(eq(offer), eq(excerptReportStatus)))
        .thenReturn(Future.unit)
      when(
        cadastrEventProducer
          .produceExcerptRequestStatusChangedEvent(?, eq(excerptId))
      ).thenReturn(Future.unit)

      deliveryStage.process(processingState).map { state =>
        verify(cadastrEventProducer).produceReportStatusChangedEvent(eq(offer), eq(excerptReportStatus))
        verify(cadastrEventProducer)
          .produceExcerptRequestStatusChangedEvent(?, eq(excerptId))
        assert(state.entry.status == RequestStatus.Delivered)
      }
    }

    "process erroneous request" in {
      val request = requestGen(statusOpt = Some(RequestStatus.Error)).next
      val excerptId = Some(ExcerptUtils.generateExcerptId())
      val processingState = ProcessingState(request).withWatcherContext(
        RequestWatcherContext(excerptService, excerptId = excerptId)
      )

      when(
        cadastrEventProducer
          .produceExcerptRequestStatusChangedEvent(?, eq(excerptId))
      ).thenReturn(Future.unit)

      deliveryStage.process(processingState).map { state =>
        verify(cadastrEventProducer)
          .produceExcerptRequestStatusChangedEvent(?, eq(excerptId))
        assert(state.entry.status == RequestStatus.Error)
      }
    }
  }
}
