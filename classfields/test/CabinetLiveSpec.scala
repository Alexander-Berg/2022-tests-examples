package auto.common.clients.cabinet.test

import auto.common.clients.cabinet.Cabinet.Cabinet
import auto.common.clients.cabinet.model.{BalanceClient, ClientSubscription, Invoice, InvoiceType}
import auto.common.clients.cabinet.{Cabinet, CabinetLive}
import common.zio.sttp.endpoint.Endpoint
import common.zio.uuid.UUID
import io.circe.syntax._
import ru.auto.amoyak.internal_service_model.AmoyakDto
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import ru.auto.cabinet.api_model._
import common.zio.logging.Logging
import common.zio.sttp.Sttp
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.{MediaType, Method}
import zio._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object CabinetLiveSpec extends DefaultRunnableSpec { self =>

  val category = "booking"
  val clientId = 20101L
  val companyId = 188L
  val poiId = 0L
  val agencyId = 1L

  val subscriptionResponse = Seq(ClientSubscription(20101, "booking", "test@test.ru"))
  val clientIdsResponse = ClientIdsResponse(Seq(ClientInfo(clientId, agencyId, clientId, true)))
  val amoClientResponse = AmoyakDto.defaultInstance
  val amoCompanyResponse = Company.defaultInstance

  val createInvoiceResponse = Invoice(123, MediaType.ApplicationPdf, "invoice.pdf", "http://invoice.pdf")

  val managerResponse = ManagerRecord(id = 1)

  val GetClientSubscriptionsByCategoryPath =
    "api/1.x/subscriptions/no-auth/client/20101/category/booking".split('/').toList

  val GetClientPoiPropertiesPath = s"api/1.x/client/$clientId/poi_properties".split('/').toList

  val GetDetailedClientPath = s"api/1.x/client/$clientId/detailed".split('/').toList

  val GetClientIdByPoiPath = s"api/1.x/poi/$poiId/client_id".split('/').toList

  val GetClientBalanceIdsPath =
    "api/1.x/internal/client/balance_ids".split('/').toList

  val GetClientByBalanceIdsPath =
    "api/1.x/internal/client/by_balance_ids".split('/').toList

  val UpdateClientPath =
    "api/1.x/internal/amo/client".split('/').toList

  val GetFullClientPath = s"api/1.x/internal/amo/client/$clientId".split('/').toList

  val GetFullCompanyPath = s"api/1.x/internal/amo/company/$companyId".split('/').toList

  val GetInvoiceRequestsPath = s"api/1.x/internal/client/$clientId/invoice/request".split('/').toList

  val CreateInvoicePath = s"api/1.x/client/$clientId/invoice".split('/').toList

  val GetDetailedClientsPath = "api/1.x/client/detailed/batch".split('/').toList

  val GetClientDiscountsPath = s"api/1.x/client/$clientId/discounts".split('/').toList
  val GetClientDiscountsParams = Map("product" -> "VAS")

  val GetManagerPath = s"api/1.x/client/$clientId/manager/internal".split('/').toList

  private val responseStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r if r.uri.path.equals(GetClientSubscriptionsByCategoryPath) && r.method == Method.GET =>
      Response.ok(subscriptionResponse.asJson.toString())
    case r if r.uri.path.equals(GetFullClientPath) && r.method == Method.GET =>
      Response.ok(amoClientResponse.toByteArray)
    case r if r.uri.path.equals(GetFullCompanyPath) && r.method == Method.GET =>
      Response.ok(amoCompanyResponse.toByteArray)
    case r if r.uri.path.equals(GetClientBalanceIdsPath) && r.method == Method.POST =>
      Response.ok(clientIdsResponse.toByteArray)
    case r if r.uri.path.equals(GetClientByBalanceIdsPath) && r.method == Method.POST =>
      Response.ok(clientIdsResponse.toByteArray)
    case r if r.uri.path.equals(UpdateClientPath) && r.method == Method.PUT =>
      Response.ok(())
    case r if r.uri.path.equals(GetInvoiceRequestsPath) && r.method == Method.GET =>
      Response.ok(InvoiceRequestsResponse.defaultInstance.toByteArray)
    case r if r.uri.path.equals(GetClientPoiPropertiesPath) && r.method == Method.GET =>
      Response.ok(PoiProperties.defaultInstance.toByteArray)
    case r if r.uri.path.equals(GetDetailedClientPath) && r.method == Method.GET =>
      Response.ok(DetailedClient.defaultInstance.toByteArray)
    case r if r.uri.path.equals(GetClientIdByPoiPath) && r.method == Method.GET =>
      Response.ok(ClientIdResponse.defaultInstance.toByteArray)
    case r if r.uri.path.equals(CreateInvoicePath) && r.method == Method.POST =>
      Response.ok(createInvoiceResponse.asJson.toString)
    case r if r.uri.path.equals(GetDetailedClientsPath) && r.method == Method.POST =>
      Response.ok(DetailedClientResponse.defaultInstance.toByteArray)
    case r
        if r.uri.path.equals(
          GetClientDiscountsPath
        ) && r.uri.params.toMap == GetClientDiscountsParams && r.method == Method.GET =>
      Response.ok(GetCustomerDiscountsResponse.defaultInstance.toByteArray)
    case r if r.uri.path.equals(GetClientDiscountsPath) && r.uri.params.toMap.isEmpty && r.method == Method.GET =>
      Response.ok(GetCustomerDiscountsResponse.defaultInstance.addDiscounts(CustomerDiscount()).toByteArray)
    case r if r.uri.path.equals(GetManagerPath) && r.method == Method.GET =>
      Response.ok(ManagerRecord.defaultInstance.toByteArray)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("CabinetLive")(
      testM("return poi properties of client by client_id")(
        for {
          response <- Cabinet.getClientPoiProperties(clientId)
        } yield assert(response)(equalTo(PoiProperties.defaultInstance))
      ),
      testM("return detailed client by client_id")(
        for {
          response <- Cabinet.getDetailedClient(clientId)
        } yield assert(response)(equalTo(DetailedClient.defaultInstance))
      ),
      testM("return client_id by poi_id")(
        for {
          response <- Cabinet.getClientIdByPoi(poiId)
        } yield assert(response)(equalTo(ClientIdResponse.defaultInstance))
      ),
      testM("return list of subscriptions")(
        for {
          response <- Cabinet.getClientSubscriptionsByCategory(clientId, category)
        } yield assert(response)(equalTo(self.subscriptionResponse))
      ),
      testM("get ids by clientId")(
        for {
          response <- Cabinet.getClientBalanceIds(Set(clientId))
        } yield assert(response)(equalTo(self.clientIdsResponse.clientsInfo))
      ),
      testM("get all ids by balance ids")(
        for {
          response <- Cabinet.getClientByBalanceIds(Set(BalanceClient(clientId, agencyId)))
        } yield assert(response)(equalTo(self.clientIdsResponse.clientsInfo))
      ),
      testM("update client")(
        for {
          response <- Cabinet.amoUpdateClient(AmoUpdateClientRequest(clientId))
        } yield assert(response)(isUnit)
      ),
      testM("get full client")(
        for {
          response <- Cabinet.amoGetClient(clientId)
        } yield assert(response)(equalTo(AmoyakDto.defaultInstance))
      ),
      testM("get full company")(
        for {
          response <- Cabinet.amoGetCompany(companyId)
        } yield assert(response)(equalTo(Company.defaultInstance))
      ),
      testM("get invoice requests")(
        for {
          response <- Cabinet.getInvoiceRequests(clientId)
        } yield assert(response)(equalTo(InvoiceRequestsResponse.defaultInstance))
      ),
      testM("create invoice")(
        for {
          response <- Cabinet.createInvoice(clientId, 1, 100, InvoiceType.Overdraft)
        } yield assert(response)(equalTo(createInvoiceResponse))
      ),
      testM("get detailed clients")(
        for {
          response <- Cabinet.getDetailedClients(DetailedClientRequest(clientIds = Seq(clientId)))
        } yield assert(response)(equalTo(DetailedClientResponse.defaultInstance))
      ),
      testM("get client discounts")(
        for {
          response <- Cabinet.getClientDiscounts(clientId, product = None)
        } yield assert(response)(equalTo(GetCustomerDiscountsResponse.defaultInstance.addDiscounts(CustomerDiscount())))
      ),
      testM("get client discounts for product")(
        for {
          response <- Cabinet.getClientDiscounts(clientId, product = Some(CustomerDiscount.Product.VAS))
        } yield assert(response)(equalTo(GetCustomerDiscountsResponse.defaultInstance))
      ),
      testM("get client manager")(
        for {
          response <- Cabinet.getManager(clientId)
        } yield assert(response)(equalTo(ManagerRecord.defaultInstance))
      )
    ).provideCustomLayer(createEnvironment(responseStub))
  }

  def createEnvironment(sttpBackendStub: Sttp.ZioSttpBackendStub): ZLayer[Any, Nothing, Cabinet] = {

    (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub)) ++ UUID.live ++ Logging.live >>>
      ZLayer.fromServices[Endpoint, Sttp.Service, UUID.Service, Logging.Service, Cabinet.Service](
        new CabinetLive(_, _, _, _)
      )
  }

}
