package ru.auto.cabinet.api.v1

import java.time.OffsetDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.ScalacheckShapeless._
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import ru.auto.cabinet.test.TestUtil._
import ru.auto.cabinet.api.v1.view.InvoiceMarshaller._
import ru.auto.cabinet.api.v1.view.{InvoiceParams, InvoiceRequestsResult}
import ru.auto.cabinet.dao.entities.BalanceRequest
import ru.auto.cabinet.dao.jdbc.BalanceOrderClient
import ru.auto.cabinet.model.BalanceInvoiceTypes.BalanceInvoiceType
import ru.auto.cabinet.model.{ClientId, RegionId}
import ru.auto.cabinet.service.{
  BalanceOrderService,
  InvoiceService,
  PaymentRequestParams
}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions, when}
import org.mockito.ArgumentMatchers.{anyInt, anyLong}
import org.scalatest.Outcome
import ru.auto.cabinet.remote.impl.{InvoicePdfLink, PaymentResponse}
import spray.json._

class InvoiceHandlerSpec
    extends FixtureAnyFlatSpec
    with HandlerSpecTemplate
    with PropertyChecks
    with SprayJsonSupport {

  private val auth = new SecurityMocks

  import auth._

  private val clientId = client1.clientId

  implicit val arbPosLong: Arbitrary[Long] = Arbitrary(Gen.posNum[Long])

  case class FixtureParam(
      invoiceService: InvoiceService,
      balanceOrderService: BalanceOrderService,
      route: Route)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val invoiceService = mock[InvoiceService]
    val balanceOrderService = mock[BalanceOrderService]

    val route: Route =
      wrapRequestMock {
        new InvoiceHandler(invoiceService, balanceOrderService).route
      }

    test(FixtureParam(invoiceService, balanceOrderService, route))
  }

  "GET /invoice/client" should "respond with client" in { f =>
    forAll { (balanceClient: BalanceOrderClient) =>
      reset(f.invoiceService)
      when(f.invoiceService.getBalanceClientWithOrder(any())(any()))
        .thenReturnF(balanceClient)
      Get(s"/client/$clientId/invoice/client") ~> auth.headers1 ~> f.route ~> check {
        responseAs[BalanceOrderClient] shouldBe balanceClient
        verify(f.invoiceService).getBalanceClientWithOrder(eq(clientId))(any())
        verifyNoMoreInteractions(f.invoiceService)
      }
    }
  }

  it should "respond with balance id client fields" in { f =>
    forAll { (balanceClientId: RegionId, balanceAgencyId: RegionId) =>
      val balanceClient = BalanceOrderClient(
        1,
        balanceClientId = Some(balanceClientId),
        balanceAgencyId = Some(balanceAgencyId),
        regionId = None,
        name = None,
        contractId = None
      )
      reset(f.invoiceService)
      when(f.invoiceService.getBalanceClientWithOrder(anyLong)(any()))
        .thenReturnF(balanceClient)
      Get(s"/client/$clientId/invoice/client") ~> auth.headers1 ~> f.route ~> check {
        val response = responseAs[String].parseJson.asJsObject.fields
        response("balanceClientId").toString shouldBe balanceClientId.toString
        response("balanceAgencyId").toString shouldBe balanceAgencyId.toString
        response.get("regionId") shouldBe None
        response.get("name") shouldBe None
        response.get("contractId") shouldBe None
        verify(f.invoiceService).getBalanceClientWithOrder(eq(clientId))(any())
        verifyNoMoreInteractions(f.invoiceService)
      }
    }
  }

  it should "respond with all fields" in { f =>
    forAll {
      (
          balanceClientId: ClientId,
          balanceAgencyId: ClientId,
          regionId: RegionId,
          name: String,
          contractId: Long) =>
        val balanceClient = BalanceOrderClient(
          1,
          balanceClientId = Some(balanceClientId),
          balanceAgencyId = Some(balanceAgencyId),
          regionId = Some(regionId),
          name = Some(name),
          contractId = Some(contractId)
        )
        reset(f.invoiceService)
        when(f.invoiceService.getBalanceClientWithOrder(anyLong)(any()))
          .thenReturnF(balanceClient)
        Get(s"/client/$clientId/invoice/client") ~> auth.headers1 ~> f.route ~> check {
          val response = responseAs[String].parseJson.asJsObject.fields
          response("balanceClientId")
            .convertTo[ClientId] shouldBe balanceClientId
          response("balanceAgencyId")
            .convertTo[ClientId] shouldBe balanceAgencyId
          response("regionId").convertTo[RegionId] shouldBe regionId
          response("name").convertTo[String] shouldBe name
          response("contractId").convertTo[Long] shouldBe contractId
          verify(f.invoiceService).getBalanceClientWithOrder(eq(clientId))(
            any())
          verifyNoMoreInteractions(f.invoiceService)
        }
    }
  }

  it should "respond with client to agency" in { f =>
    forAll { (balanceClient: BalanceOrderClient) =>
      reset(f.invoiceService)
      when(f.invoiceService.getBalanceClientWithOrder(anyLong)(any()))
        .thenReturnF(balanceClient)
      Get(
        s"/client/$agencyClientId/invoice/client") ~> auth.agentHeaders ~> f.route ~> check {
        responseAs[BalanceOrderClient] shouldBe balanceClient
        verify(f.invoiceService).getBalanceClientWithOrder(eq(agencyClientId))(
          any())
        verifyNoMoreInteractions(f.invoiceService)
      }
    }
  }

  "GET /invoice" should "respond with single element seq" in { f =>
    forAll { clientId: Long =>
      val balanceRequest = BalanceRequest(
        id = 0L,
        requestId = 0L,
        clientId = clientId,
        agencyId = Some(0L),
        createDate = Some(OffsetDateTime.now()),
        url = Some(""),
        userId = Some(0L),
        totalSum = Some(0L)
      )
      when(f.invoiceService.getInvoiceRequests(anyLong, anyInt)(any()))
        .thenReturnF(Seq(balanceRequest))

      Get(
        s"/client/$clientId/invoice/request") ~> auth.adminHeaders ~> f.route ~> check {
        responseAs[InvoiceRequestsResult] shouldBe InvoiceRequestsResult(
          Seq(balanceRequest))
      }
    }
  }

  "POST /invoice" should "respond 200" in { f =>
    forAll {
      (
          params: InvoiceParams,
          invoice: InvoicePdfLink,
          invoiceType: BalanceInvoiceType) =>
        reset(f.invoiceService)

        when(
          f.invoiceService.generateInvoice(anyLong, anyLong, anyLong, any())(
            any()))
          .thenReturnF(invoice)
        val entity = params.toJson.prettyPrint
        Post(s"/client/$clientId/invoice?type=$invoiceType")
          .withEntity(`application/json`, entity)
          .withHeaders(RawHeader("X-Request-ID", "test")) ~>
          auth.headers1 ~> f.route ~> check {
            status shouldBe OK
            responseAs[JsValue].convertTo[InvoicePdfLink] shouldBe invoice
            verify(f.invoiceService).generateInvoice(
              eq(params.quantity),
              eq(params.balancePersonId),
              eq(clientId),
              eq(invoiceType)
            )(any())
            verifyNoMoreInteractions(f.invoiceService)
          }
    }
  }

  "GET /invoice/pdf" should "respond 200" in { f =>
    forAll { (invoiceId: Long, invoice: InvoicePdfLink) =>
      reset(f.invoiceService)
      when(f.invoiceService.getInvoicePdf(anyLong, anyLong)(any()))
        .thenReturnF(invoice)
      Get(s"/client/$clientId/invoice/$invoiceId/pdf")
        .withHeaders(RawHeader("X-Request-ID", "test")) ~>
        auth.headers1 ~> f.route ~> check {
          status shouldBe OK
          responseAs[JsValue].convertTo[InvoicePdfLink] shouldBe invoice
          verify(f.invoiceService).getInvoicePdf(eq(clientId), eq(invoiceId))(
            any())
          verifyNoMoreInteractions(f.invoiceService)
        }
    }
  }

  "POST /payrequest" should "respond 200" in { f =>
    forAll { (params: PaymentRequestParams, response: PaymentResponse) =>
      reset(f.invoiceService)

      when(f.invoiceService.payRequest(any(), anyLong)(any()))
        .thenReturnF(response)
      val entity = params.toJson.prettyPrint
      Post(s"/client/$clientId/payrequest")
        .withEntity(`application/json`, entity)
        .withHeaders(RawHeader("X-Request-ID", "test")) ~>
        auth.headers1 ~> f.route ~> check {
          status shouldBe OK
          responseAs[JsValue].convertTo[PaymentResponse] shouldBe response
          verify(f.invoiceService).payRequest(eq(params), eq(clientId))(any())
          verifyNoMoreInteractions(f.invoiceService)
        }
    }
  }
}
