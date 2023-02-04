package auto.common.clients.cabinet.testkit

import auto.common.clients.cabinet.Cabinet
import auto.common.clients.cabinet.model._
import ru.auto.amoyak.internal_service_model.AmoyakDto
import ru.auto.api.response_model.DealerAccountResponse
import ru.auto.cabinet.api_model._
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import common.zio.sttp.model.SttpError
import zio.{IO, Task, ZLayer}
import zio.test.mock.mockable

@mockable[Cabinet.Service]
object CabinetTest

object CabinetEmptyTest {

  val empty = ZLayer.succeed(new Cabinet.Service {

    override def getClientSubscriptionsByCategory(clientId: ClientId, category: String): Task[Seq[ClientSubscription]] =
      ???

    override def getClientBalanceIds(clientIds: Set[ClientId]): Task[Seq[ClientInfo]] =
      ???

    override def getClientByBalanceIds(balanceClientIds: Set[BalanceClient]): IO[CabinetException, Seq[ClientInfo]] =
      ???

    override def amoCreateClient(request: AmoCreateClientRequest): IO[CabinetException, AmoCreateClientResponse] =
      ???

    override def amoUpdateClient(request: AmoUpdateClientRequest): IO[CabinetException, Unit] =
      ???

    override def amoSyncClients(clientIds: Seq[ClientId]): IO[CabinetException, Unit] =
      ???

    override def amoGetClient(clientId: ClientId): IO[CabinetException, AmoyakDto] =
      ???

    override def amoGetCompany(companyId: CompanyId): IO[CabinetException, Company] =
      ???

    override def getStock(clientId: ClientId): IO[CabinetException, DealerStocks] =
      ???

    override def getClientAccount(clientId: ClientId): IO[CabinetException, DealerAccountResponse] =
      ???

    override def getInvoiceRequests(clientId: ClientId): IO[CabinetException, InvoiceRequestsResponse] =
      ???

    override def getModeration(client: ClientId): IO[CabinetException, Moderation] = ???

    override def getClientCompany(client: ClientId): IO[CabinetException, Option[Company]] = ???

    override def getClientIdByPoi(poiId: PoiId): IO[SttpError, ClientIdResponse] = ???

    override def getClientPoiProperties(clientId: ClientId): IO[SttpError, PoiProperties] = ???

    override def getDetailedClient(clientId: ClientId): IO[SttpError, DetailedClient] = ???

    override def getClientByOrigin(origin: Origin): IO[SttpError, Client] = ???

    override def createInvoice(
        clientId: ClientId,
        balancePersonId: BalancePersonId,
        quantity: BalanceClientId,
        invoiceType: InvoiceType): IO[CabinetException, Invoice] = ???

    override def getDetailedClients(request: DetailedClientRequest): IO[CabinetException, DetailedClientResponse] = ???

    override def getClientDiscounts(
        clientId: ClientId,
        product: Option[CustomerDiscount.Product]): IO[CabinetException, GetCustomerDiscountsResponse] = ???

    def getManager(clientId: ClientId): IO[CabinetException, ManagerRecord] = ???

    def internalSearchClients(request: FindClientsRequest): IO[CabinetException, FindClientsResponse] = ???
  })
}
