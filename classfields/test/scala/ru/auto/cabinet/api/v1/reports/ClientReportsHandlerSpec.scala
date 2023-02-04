package ru.auto.cabinet.api.v1.reports

import akka.actor.ActorSystem
import org.mockito.Mockito.when
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.api.ResponseModel.ExpensesReportResponse
import ru.auto.api.ResponseModel.ExpensesReportResponse.ProcessStatus
import ru.auto.cabinet.api.v1.{HandlerSpecTemplate, SecurityMocks}
import ru.auto.cabinet.service.ClientReportsService
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future.successful

/** Тестируем [[ClientReportsHandler]].
  */
class ClientReportsHandlerSpec
    extends FlatSpec
    with HandlerSpecTemplate
    with ProtobufSupport {

  implicit private val as: ActorSystem = ActorSystem()
  implicit private val ec: ExecutionContextExecutor = as.dispatcher
  private val clientServiceMock = mock[ClientReportsService]
  private val mdsReportManager = mock[MdsReportManager]
  private val auth = new SecurityMocks

  import auth._

  when(
    mdsReportManager.expensesReport(
      any(),
      any(),
      any(),
      eq(12)
    )(any())
  ).thenReturn(
    successful(
      ExpensesReportResponse
        .newBuilder()
        .setProcessStatus(ProcessStatus.WILL_BE_SENT_TO_EMAIL)
        .build()
    )
  )

  val route = wrapRequestMock(
    new ClientReportsHandler(clientServiceMock, mdsReportManager).route)

  "ClientReportsHandler" should "should be able to form expenses report" in {
    Get(
      s"/reports/offers-activations/client/$client1Id/link?" +
        s"fromDate=2018-01-01&toDate=2018-03-03&collectorTimeoutSeconds=12") ~>
      headers1 ~>
      route ~> check {
        val resp = responseAs[ExpensesReportResponse]
        resp.getProcessStatus == ExpensesReportResponse.ProcessStatus.WILL_BE_SENT_TO_EMAIL
      }
  }
}
