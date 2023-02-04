package ru.auto.cabinet.api.v1

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Route.seal
import org.scalatest.{OneInstancePerTest, Outcome}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import ru.auto.cabinet.ApiModel.{
  ClientIdResponse,
  SalonCodeResponse,
  SalonInfoResponse,
  UpdateCustomerDiscountRequest,
  ManagerRecord => ProtoManagerRecord
}
import ru.auto.cabinet.Redemption.RedemptionForm
import ru.auto.cabinet.Redemption.RedemptionForm.{ClientInfo, DealerInfo}
import ru.auto.cabinet.api.v1.view.ClientView
import ru.auto.cabinet.dao.jdbc.{
  BalanceClientIdNotFound,
  BalanceInvoiceNotFound,
  ManagerInternalRecord,
  ManagerRecord,
  NotFoundError
}
import ru.auto.cabinet.model._
import ru.auto.cabinet.service.{
  ClientMarksService,
  ClientService,
  SalonInfoService
}
import ru.auto.cabinet.{environment, ApiModel}
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.test.TestUtil.RichOngoingStub

import java.time.OffsetDateTime

/** Specs [[ClientHandler]]
  */
class ClientHandlerSpec
    extends FixtureAnyFlatSpec
    with HandlerSpecTemplate
    with OneInstancePerTest {

  private val auth = new SecurityMocks

  import auth._

  private val origin = "test"

  val testClientProperties = ClientProperties(
    0,
    1,
    origin,
    ClientStatuses.Active,
    environment.now,
    "",
    Some("website.test"),
    "test@yandex.ru",
    Some("manager@yandex.ru"),
    None,
    Some(environment.now),
    multipostingEnabled = true,
    firstModerated = true,
    isAgent = false
  )

  val detailedDummy = DetailedClient(
    20101,
    Some("Test dealer"),
    isAgent = false,
    Some(1),
    Some("Test agency"),
    Some(2),
    Some("Test company"),
    testClientProperties
  )

  val dummy = Client(
    auth.client1Id,
    0,
    testClientProperties
  )

  val dummyCompany = Company(1, "Dummy company", OffsetDateTime.now())

  val manager = ManagerRecord(1, fio = Some("my manager"))

  val protoManager =
    ProtoManagerRecord.newBuilder().setId(1).setFio("my manager").build()

  val managerInternal = ManagerInternalRecord(id = 1, userId = 2)
  val protoManagerInternal = ProtoManagerRecord.newBuilder.setId(1).build()

  case class FixtureParam(
      clientService: ClientService,
      clientMarksService: ClientMarksService,
      salonInfoService: SalonInfoService,
      route: Route)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val clientService: ClientService = mock[ClientService]
    val clientMarksService: ClientMarksService = mock[ClientMarksService]
    val salonInfoService: SalonInfoService = mock[SalonInfoService]

    when(clientService.get(eq(auth.client1Id))(any())).thenReturnF(dummy)
    when(clientService.get(eq(auth.client2Id))(any())).thenReturnF(dummy)

    when(clientService.getClientByOrigin(eq(origin), eq(false))(any()))
      .thenReturnF(Some(dummy))
    when(clientService.getClientByOrigin(eq(origin), eq(true))(any()))
      .thenReturnF(Some(dummy))

    when(clientService.getManager(eq(auth.client1Id))(any()))
      .thenReturnF(Some(manager))
    when(clientService.getManagerInternal(eq(auth.client1Id))(any()))
      .thenReturnF(Some(managerInternal))

    val route =
      wrapRequestMock {
        new ClientHandler(
          clientService,
          clientMarksService,
          salonInfoService).route
      }

    test(
      FixtureParam(clientService, clientMarksService, salonInfoService, route))
  }

  "/client/{origin}" should "find client by origin" in { f =>
    Get(s"/client/$origin/") ~> seal(f.route) ~> check {
      import ClientView.unmarshaller
      responseAs[Client] should be(dummy)
    }
  }
  "/client/{clientId}/manager" should "return manager info" in { f =>
    Get(s"/client/${auth.client1Id}/manager/") ~> auth.headers1 ~> seal(
      f.route) ~> check {
      responseAs[ProtoManagerRecord] should be(protoManager)
    }
  }

  "/client/{clientId}/manager/internal" should "return manager info without auth" in {
    f =>
      Get(s"/client/${auth.client1Id}/manager/internal") ~> seal(
        f.route) ~> check {
        responseAs[ProtoManagerRecord] should be(protoManagerInternal)
      }
  }

  "/client" should "respond for correct authorization (not slash-ended)" in {
    f =>
      Get(s"/client/${auth.client1Id}/") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ApiModel.Client]
        response should be(dummy.asProto)
      }
  }

  "GET /client/{clientId}" should "respond for correct authorization (not slash-ended)" in {
    f =>
      Get(s"/client/${auth.client1Id}") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ApiModel.Client]
        response should be(dummy.asProto)
      }
  }

  "/client/detailed" should "respond for correct authorization (not slash-ended)" in {
    f =>
      when(f.clientService.getDetailed(eq(auth.client1Id))(any()))
        .thenReturnF(detailedDummy.asProto(None))

      Get(s"/client/${auth.client1Id}/detailed") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ApiModel.DetailedClient]
        response should be(detailedDummy.asProto(None))
      }
  }

  "GET /client/{clientId}" should "respond for correct authorization" in { f =>
    Get(s"/client/${auth.client1Id}/") ~> auth.headers1 ~> seal(
      f.route) ~> check {
      status shouldBe StatusCodes.OK
      val response = responseAs[ApiModel.Client]
      response should be(dummy.asProto)
    }

    Get(s"/client/${auth.client2Id}/") ~> auth.headers2 ~> seal(
      f.route) ~> check {
      status shouldBe StatusCodes.OK
      val response = responseAs[ApiModel.Client]
      response should be(dummy.asProto)
    }
  }

  "PUT /client/is_loyal" should "mark client as loyal" in { f =>
    when(
      f.clientService.updateLoyaltyStatus(eq(auth.client1Id), eq(true))(any()))
      .thenReturnF(())

    Put(s"/client/${auth.client1Id}/is_loyal") ~> auth.headers1 ~> seal(
      f.route) ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "DELETE /client/is_loyal" should "mark client as loyal" in { f =>
    when(
      f.clientService.updateLoyaltyStatus(eq(auth.client1Id), eq(false))(any()))
      .thenReturnF(())

    Delete(s"/client/${auth.client1Id}/is_loyal") ~> auth.headers1 ~> seal(
      f.route) ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  val mark = "MARK"
  val model = "MODEL"
  val km = 10
  val year = 10
  val price = 1000000
  val userName = "Vasiya"
  val telNumber = "+123"
  val clientProfileUri = "http://hello-world.ru/123"

  def testRedemptionForm(): RedemptionForm = {
    RedemptionForm
      .newBuilder()
      .setClient(
        ClientInfo
          .newBuilder()
          .setName(userName)
          .setPhoneNumber(telNumber)
      )
      .setDealerInfo(
        DealerInfo
          .newBuilder()
          .setProfileUrl(clientProfileUri)
      )
      .build()
  }

  "GET /overdraft/invoice" should "respond 404 on balance invoice not found" in {
    f =>
      when(
        f.clientService.getLastClientOverdraftInvoice(eq(auth.client1Id))(
          any()))
        .thenThrowF(new BalanceInvoiceNotFound("test"))

      Get(
        s"/client/${auth.client1Id}/overdraft/invoice") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe NotFound
      }
  }

  val updateCustomerDiscountRequest: HttpEntity.Strict = HttpEntity {
    UpdateCustomerDiscountRequest.newBuilder
      .setPercent(146)
      .build
      .toByteArray
  }

  "POST /client/{client_id}/discounts" should "fail with invalid percent" in {
    f =>
      Post(s"/client/${auth.client1Id}/discounts")
        .withEntity(updateCustomerDiscountRequest) ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe BadRequest
      }
  }

  "POST /company/{company_id}/discounts" should "fail with invalid percent" in {
    f =>
      Post(s"/company/${auth.client1Id}/discounts")
        .withEntity(updateCustomerDiscountRequest) ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe BadRequest
      }
  }

  "GET /client/[client_id]/company" should "find clients company" in { f =>
    val protoCompany = dummyCompany.asProto(Seq(dummy))

    when(f.clientService.getClientsCompany(any())(any()))
      .thenReturnF(protoCompany)

    Get(s"/client/${auth.client1Id}/company") ~> auth.headers1 ~> seal(
      f.route) ~> check {
      status shouldBe OK
      val response = responseAs[ApiModel.Company]
      response shouldBe protoCompany
    }
  }

  "GET /client/[client_id]/company" should "respond 404 if client or company is not found" in {
    f =>
      when(f.clientService.getClientsCompany(any())(any()))
        .thenThrowF(NotFoundError("Client or company is not found"))
      Get(s"/client/${auth.client1Id}/company") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe NotFound
      }
  }

  "POST /balance-client/{balance_id}/ya-balance-registrations" should "respond 200 save found balance client" in {
    f =>
      when(f.clientService.saveYaBalanceClient(eq(balanceId1))(any()))
        .thenReturnF(())

      Post(
        s"/balance-client/${auth.balanceId1}/ya-balance-registrations") ~> seal(
        f.route) ~> check {
        status shouldBe OK
      }
  }

  "POST /balance-client/{balance_id}/ya-balance-registrations" should "respond 404 if balance client is not found" in {
    f =>
      when(f.clientService.saveYaBalanceClient(eq(balanceId1))(any()))
        .thenThrowF(new BalanceClientIdNotFound(
          s"Balance Client not found for balance id: $client1Id"))

      Post(
        s"/balance-client/${auth.balanceId1}/ya-balance-registrations") ~> seal(
        f.route) ~> check {
        status shouldBe NotFound
      }
  }

  "GET /poi/{poi_id}/client_id" should "retrieve 200 get detailed client" in {
    f =>
      val dummyPoi = 0L
      val clientIdResponse: Long => ClientIdResponse = clientId =>
        ClientIdResponse
          .newBuilder()
          .setClientId(clientId)
          .build()

      when(f.clientService.getClientIdByPoiId(any())(any()))
        .thenReturnF(clientIdResponse(0L))

      Get(s"/poi/$dummyPoi/client_id") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe OK
        val response = responseAs[ClientIdResponse]
        response shouldBe clientIdResponse(0L)
      }
  }
  "GET /poi/{poi_id}/client_id" should "retrieve 400 if client couldn't conneted to poi" in {
    f =>
      val dummyPoi = 0L

      when(f.clientService.getClientIdByPoiId(any())(any()))
        .thenThrowF(NotFoundError("No rows found"))

      Get(s"/poi/$dummyPoi/client_id") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe NotFound
      }
  }

  "GET /client/{client_id}/salon/code" should "return code if exists" in { f =>
    val clientId = 68L
    val expectedCode = "evrosib_centr_sankt_peterburg_bmw"

    when(f.salonInfoService.salonCode(eq(clientId)))
      .thenReturn(Right(expectedCode))

    Get(s"/client/$clientId/salon/code") ~> seal(f.route) ~> check {
      status shouldBe OK
      responseAs[SalonCodeResponse].getSalonCode shouldBe expectedCode
    }
  }

  "GET /client/{client_id}/salon/code" should "return 404 if code doesn't exist" in {
    f =>
      val clientId = 20101L
      when(f.salonInfoService.salonCode(eq(clientId)))
        .thenReturn(Left(SalonInfoService.SalonCodeNotFound(clientId)))

      Get(s"/client/$clientId/salon/code") ~> seal(f.route) ~> check {
        status shouldBe NotFound
      }
  }

  "GET /client/{client_id}/salon/info" should "return SalonInfoResponse" in {
    f =>
      val clientId = 20101L
      val salonInfoResponse = SalonInfoResponse.getDefaultInstance
      when(f.salonInfoService.getSalonInfo(eq(clientId))(any()))
        .thenReturnF(Right(salonInfoResponse))

      Get(s"/client/$clientId/salon/info") ~> seal(f.route) ~> check {
        status shouldBe OK
        responseAs[SalonInfoResponse] shouldBe salonInfoResponse
      }
  }

  "PUT /client/{client_id}/agency/{agency_id}" should "update successfully" in {
    f =>
      val clientId = 20101L
      val agencyId = 123L

      when(
        f.clientService.updateClientAgency(eq(clientId), eq(agencyId))(any()))
        .thenReturnF(())

      Put(s"/client/$clientId/agency/$agencyId") ~> auth.headers1 ~> seal(
        f.route) ~> check {
        status shouldBe OK
      }
  }
}
