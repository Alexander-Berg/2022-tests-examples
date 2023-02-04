package ru.auto.cabinet.api.v1.internal

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import com.google.protobuf.StringValue
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito._
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.ApiModel.{
  ClientIdsBalanceRequest,
  ClientIdsRequest,
  ClientIdsResponse,
  InvoiceRequestsResponse,
  Moderation
}
import ru.auto.cabinet.api.v1.{HandlerSpecTemplate, SecurityMocks}
import ru.auto.cabinet.dao.entities.BalanceRequest
import ru.auto.cabinet.service.{
  ClientModerationService,
  ClientService,
  DealerManagerService,
  InvoiceService
}
import ru.auto.cabinet.test.TestUtil.RichOngoingStub
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._

class ClientHandlerSpec extends FixtureAnyFlatSpec with HandlerSpecTemplate {
  private val auth = new SecurityMocks

  case class FixtureParam(
      clientService: ClientService,
      dealerManagerService: DealerManagerService,
      clientModerationService: ClientModerationService,
      invoiceService: InvoiceService,
      route: Route
  )

  override protected def withFixture(test: OneArgTest): Outcome = {
    val clientService: ClientService = mock[ClientService]
    val dealerManagerService: DealerManagerService = mock[DealerManagerService]
    val clientModerationService: ClientModerationService =
      mock[ClientModerationService]
    val invoiceService: InvoiceService = mock[InvoiceService]

    val route: Route = wrapRequestMock {
      new ClientHandler(
        clientService,
        dealerManagerService,
        clientModerationService,
        invoiceService).route
    }

    test(
      FixtureParam(
        clientService,
        dealerManagerService,
        clientModerationService,
        invoiceService,
        route
      ))
  }

  "POST /client/balance_ids" should "respond with empty message on empty request" in {
    f =>
      when(f.clientService.getClientBalanceIdsByOffice7(eq(Nil))(any()))
        .thenReturnF(ClientIdsResponse.getDefaultInstance)

      val entity = HttpEntity(ClientIdsRequest.getDefaultInstance.toByteArray)
      Post("/client/balance_ids").withEntity(
        entity) ~> auth.hdrRequest ~> f.route ~> check {
        status shouldBe OK
      }
  }

  "POST /client/balance_ids" should "respond with list of balance clients on provided ids" in {
    f =>
      val ids = List(1L)

      when(f.clientService.getClientBalanceIdsByOffice7(eq(ids))(any()))
        .thenReturnF(
          ClientIdsResponse
            .newBuilder()
            .addClientsInfo(
              ClientIdsResponse.ClientInfo.getDefaultInstance
            )
            .build())

      val entity = HttpEntity(
        ClientIdsRequest.newBuilder().addClientIds(1L).build().toByteArray)
      Post("/client/balance_ids").withEntity(
        entity) ~> auth.hdrRequest ~> f.route ~> check {
        status shouldBe OK
      }
  }

  "POST /client/by_balance_ids" should "respond with list of office7 clients on provided ids" in {
    f =>
      val balanceId = 1
      val agencyId = 1

      val reqIds = ClientIdsBalanceRequest.BalanceClientInfo
        .newBuilder()
        .setBalanceId(balanceId)
        .setAgencyId(agencyId)
        .build

      when(
        f.clientService.getClientBalanceIdsByBalance(eq(List(reqIds)))(any()))
        .thenReturnF(
          ClientIdsResponse
            .newBuilder()
            .addClientsInfo(
              ClientIdsResponse.ClientInfo.getDefaultInstance
            )
            .build())

      val entity = HttpEntity(
        ClientIdsBalanceRequest
          .newBuilder()
          .addBalanceIds(reqIds)
          .build()
          .toByteArray)

      Post("/client/by_balance_ids").withEntity(
        entity) ~> auth.hdrRequest ~> f.route ~> check {
        status shouldBe OK
      }
  }

  "GET /client/moderation/{client_id}" should "respond with moderation entity" in {
    f =>
      val clientId = 20101L

      val response =
        Moderation.newBuilder().setBanReasons(StringValue.of("Жулики")).build()

      when(f.clientModerationService.getModeration(eq(clientId))(any(), any()))
        .thenReturnF(response)

      Get(
        s"/client/moderation/$clientId") ~> auth.hdrRequest ~> f.route ~> check {
        status shouldBe OK
        responseAs[Moderation] shouldBe response
      }
  }

  "GET /client/{client_id}/invoice/request" should "respond with single entity" in {
    f =>
      val clientId = 1L
      when(f.invoiceService.getInvoiceRequests(eq(clientId), anyInt())(any()))
        .thenReturnF(Seq.empty[BalanceRequest])

      val expectedResponse = InvoiceRequestsResponse.getDefaultInstance

      Get(s"/client/$clientId/invoice/request") ~> auth.hdrRequest ~> f.route ~> check {
        status shouldBe OK
        responseAs[InvoiceRequestsResponse] shouldBe expectedResponse
      }
  }
}
