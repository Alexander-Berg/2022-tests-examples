package ru.yandex.vertis.billing.integration.test.mocks

import ru.yandex.vertis.billing.balance.model
import ru.yandex.vertis.billing.balance.model.{
  Balance,
  CampaignSpending,
  CampaignSpendingResult,
  Client,
  ClientRequest,
  ClientUser,
  InvoiceId,
  InvoiceRequest,
  NotificationUrlChangeRequest,
  NotifyOrder2,
  OperatorId,
  OrderRequest,
  OrderResult,
  PaymentRequest,
  PaymentRequestResult,
  Person,
  PersonId,
  PersonRequest,
  RequestChoices,
  UserId
}
import ru.yandex.vertis.billing.balance.xmlrpc.model.Value
import ru.yandex.vertis.billing.model_core.ClientId

import scala.util.Try

class TestBalance extends Balance {

  def updateNotificationUrl(request: NotificationUrlChangeRequest): Try[Unit] = ???

  def createRequest(
      clientId: ClientId,
      paymentRequest: PaymentRequest
    )(implicit operator: OperatorId): Try[PaymentRequestResult] = ???

  def createClient(request: ClientRequest)(implicit operator: OperatorId): Try[ClientId] = ???
  def createUserClientAssociation(clientId: ClientId, clientUid: UserId)(implicit operator: OperatorId): Try[Unit] = ???
  def removeUserClientAssociation(clientId: ClientId, clientUid: UserId)(implicit operator: OperatorId): Try[Unit] = ???
  def updateCampaigns(campaignSpendings: Iterable[CampaignSpending]): Try[Iterable[CampaignSpendingResult]] = ???
  def createOrUpdateOrdersBatch(orders: Seq[model.Order])(implicit operator: OperatorId): Try[Seq[OrderResult]] = ???
  def getClientsByIdBatch(ids: Iterable[ClientId]): Try[Iterable[Client]] = ???
  def listClientPassports(id: ClientId)(implicit operator: OperatorId): Try[Iterable[ClientUser]] = ???
  def getPassportByLogin(login: String)(implicit operator: OperatorId): Try[Option[ClientUser]] = ???
  def getPassportByUid(uid: String)(implicit operator: OperatorId): Try[Option[ClientUser]] = ???
  def getOrdersInfo(requests: Iterable[OrderRequest]): Try[Iterable[NotifyOrder2]] = ???
  def createPerson(request: PersonRequest)(implicit operator: OperatorId): Try[PersonId] = ???
  def getClientPersons(clientId: ClientId, isPartner: Boolean): Try[Iterable[Person]] = ???
  def createInvoice(ir: InvoiceRequest)(implicit operatorId: OperatorId): Try[InvoiceId] = ???
  def getRequestChoices(request: RequestChoices): Try[Value] = ???
}
