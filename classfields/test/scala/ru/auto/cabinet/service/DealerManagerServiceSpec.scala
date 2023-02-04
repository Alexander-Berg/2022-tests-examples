package ru.auto.cabinet.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.{AnyWordSpecLike => WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import ru.auto.api.ResponseModel.{
  DealerAccountResponse,
  DealerOverdraft,
  DealerPaymentActions
}
import ru.auto.cabinet.dao.entities.BalanceClient
import ru.auto.cabinet.dao.jdbc.{BalanceOrderClient, JdbcClientDao}
import ru.auto.cabinet.environment
import ru.auto.cabinet.model.billing.{OverdraftResponse, _}
import ru.auto.cabinet.model.{
  Client,
  ClientProperties,
  ClientStatuses,
  DealerUserRoleForAccount
}
import ru.auto.cabinet.service.billing.VsBillingClient
import ru.auto.cabinet.service.billing.util.toCents
import ru.auto.cabinet.service.dealer_stats.DealerStatsClient
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.test.TestUtil._
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.Protobuf._
import ru.auto.dealer_stats.proto.Rpc.DealerWeekAverageOutcomeResponse

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class DealerManagerServiceSpec
    extends WordSpecLike
    with Matchers
    with OptionValues
    with PropertyChecks
    with ScalaFutures {
  private def ?[T]: T = any()

  implicit private val instr: Instr = new EmptyInstr("test")
  implicit private val rc = Context.unknown

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  private val clientDao = mock[JdbcClientDao]
  private val invoiceService = mock[InvoiceService]
  private val clientService = mock[ClientService]
  private val dealerStatsClient = mock[DealerStatsClient]
  private val vsBillingClient = mock[VsBillingClient]

  private val dealerManager = new DealerManagerService(
    clientDao,
    invoiceService,
    clientService,
    dealerStatsClient,
    vsBillingClient
  )

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
  private val clientId = 1L
  private val accountId = 2L
  private val balanceClientId = 3L
  private val balanceAgencyId = 4L
  private val client = Client(clientId, 0, testClientProperties)

  private val balanceOrderClientWithoutAgency =
    BalanceOrderClient(accountId, Some(balanceClientId), None, None, None, None)

  private val balanceOrderClientWithAgency =
    BalanceOrderClient(
      accountId,
      Some(balanceClientId),
      Some(balanceAgencyId),
      None,
      None,
      None)

  private val balanceOrderClientWithZeroAccount =
    BalanceOrderClient(0L, Some(balanceClientId), None, None, None, None)

  private val dealerPaymentActions = DealerPaymentActions
    .newBuilder()
    .setCardPayment(true)
    .setInvoice(true)
    .setInvoiceRequest(true)
    .setOverdraft(true)
    .build()

  private val dealerAccountResponse = DealerAccountResponse
    .newBuilder()
    .setAccountId(accountId)
    .setDealerStatus(DealerAccountResponse.DealerStatus.ACTIVE)
    .setBalanceClientId(1)
    .setBalanceAgencyId(2)
    .setAverageOutcome(100)
    .setRestDays(10)
    .setBalance(2)
    .setPaymentActions(dealerPaymentActions)
    .build()

  private val returnedDealerStatsOutcome = DealerWeekAverageOutcomeResponse
    .newBuilder()
    .setOutcome(10000L)
    .build()

  def dealerOverdraft(invoiceId: Long = 0L, isAllowed: Boolean = true) =
    DealerOverdraft
      .newBuilder()
      .setInvoiceId(invoiceId)
      .setAllowed(isAllowed)
      .build()

  def balanceClientCreator(
      id: Long,
      balanceClientId: Long,
      balanceAgencyId: Option[Long]) =
    BalanceClient(
      id,
      Some(balanceClientId),
      balanceAgencyId,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None)

  private val orderResponse =
    OrdersResponse(
      1,
      Page(1, 1),
      List(Order(1, Balance2(toCents(dealerAccountResponse.getBalance)))))

  def overdraftResponse(
      limit: Long = 0L,
      spent: Long = 0L,
      isAllowed: Boolean) =
    OverdraftResponse(
      0L,
      limit,
      spent,
      Some(LocalDate.now().plusDays(7)),
      overdraftBan = !isAllowed)

  "DealerManager.getDealerAccount() for dealer" should {

    "get payment actions without overdraft" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)
      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)

      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue
      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions without overdraft for agency client" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)
      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue
      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)
      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(limit = 1L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(true)
        .build()
    }

    "get payment actions with overdraft and spent = 0 but not allowed in cabinet" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(200L, 100L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get overdraft with spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      val overdraftInfo = overdraftResponse(30000L, 10000L, isAllowed = true)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftInfo))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(invoiceId = 7777777L))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(invoiceId = 7777777L)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .setLimit(300L)
        .setSpent(100L)
        .setInvoiceId(7777777L)
        .setDeadline(overdraftInfo.deadline.get.toProtobufTimestamp)
        .setExpired(false)
        .build()
    }

    "get dealer response for client with accountId = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithZeroAccount)
      when(clientDao.get(clientId)).thenReturnF(client)

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Client)
        .futureValue

      val expectedPaymentActions = DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()

      response shouldBe DealerAccountResponse
        .newBuilder()
        .setAccountId(0)
        .setBalance(0L)
        .setBalanceClientId(
          balanceOrderClientWithoutAgency.balanceClientId.value)
        .setAverageOutcome(0)
        .setRestDays(0)
        .setDealerStatus(DealerAccountResponse.DealerStatus.ACTIVE)
        .setPaymentActions(expectedPaymentActions)
        .build()
    }
  }

  "DealerManager.getDealerAccount() for agency" should {

    "get payment actions without overdraft" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Agency)
        .futureValue
      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(limit = 1L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Agency)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(true)
        .setOverdraft(true)
        .build()
    }

    "get payment actions with overdraft and spent = 0 but not allowed in cabinet" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Agency)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(200L, 100L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Agency)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get overdraft with spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      val overdraftInfo = overdraftResponse(30000L, 10000L, isAllowed = true)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftInfo))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(invoiceId = 7777777L))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(invoiceId = 7777777L)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Agency)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .setLimit(300L)
        .setSpent(100L)
        .setInvoiceId(7777777L)
        .setDeadline(overdraftInfo.deadline.get.toProtobufTimestamp)
        .setExpired(false)
        .build()
    }
  }

  "DealerManager.getDealerAccount() for company" should {

    "get payment actions without overdraft" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Company)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions without overdraft for agency client" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Company)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(limit = 1L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Company)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(true)
        .build()
    }

    "get payment actions with overdraft and spent = 0 but not allowed in cabinet" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Company)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(200L, 100L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Company)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(true)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get overdraft with spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      val overdraftInfo = overdraftResponse(30000L, 10000L, isAllowed = true)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftInfo))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(invoiceId = 7777777L))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(invoiceId = 7777777L)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Company)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .setLimit(300L)
        .setSpent(100L)
        .setInvoiceId(7777777L)
        .setDeadline(overdraftInfo.deadline.get.toProtobufTimestamp)
        .setExpired(false)
        .build()
    }
  }

  "DealerManager.getDealerAccount() for manager" should {

    "get payment actions without overdraft" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Manager)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions without overdraft for agency client" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Manager)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(limit = 1L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Manager)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(true)
        .build()
    }

    "get payment actions with overdraft and spent = 0 but not allowed in cabinet" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Manager)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(0, 100L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Manager)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(true)
        .setInvoiceRequest(true)
        .setOverdraft(false)
        .build()
    }

    "get overdraft with spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      val overdraftInfo = overdraftResponse(30000L, 10000L, isAllowed = true)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftInfo))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(invoiceId = 7777777L))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(invoiceId = 7777777L)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Manager)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .setLimit(300L)
        .setSpent(100L)
        .setInvoiceId(7777777L)
        .setDeadline(overdraftInfo.deadline.get.toProtobufTimestamp)
        .setExpired(false)
        .build()
    }
  }

  "DealerManager.getDealerAccount() for moderation" should {

    "get payment actions without overdraft" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Moderation)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions without overdraft for agency client" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Moderation)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(limit = 1L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Moderation)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(true)
        .build()
    }

    "get payment actions with overdraft and spent = 0 but not allowed in cabinet" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Moderation)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(200L, 100L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Moderation)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get overdraft with spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      val overdraftInfo = overdraftResponse(30000L, 10000L, isAllowed = true)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftInfo))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(invoiceId = 7777777L))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(invoiceId = 7777777L)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Moderation)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .setLimit(300L)
        .setSpent(100L)
        .setInvoiceId(7777777L)
        .setDeadline(overdraftInfo.deadline.get.toProtobufTimestamp)
        .setExpired(false)
        .build()
    }
  }

  "DealerManager.getDealerAccount() for unknown role" should {

    "get payment actions without overdraft" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Unknown)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions without overdraft for agency client" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?)).thenReturnF(None)
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Unknown)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft.newBuilder().build()

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent = 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(limit = 1L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Unknown)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(true)
        .build()
    }

    "get payment actions with overdraft and spent = 0 but not allowed in cabinet" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(isAllowed = false))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(isAllowed = false)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Unknown)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get payment actions with overdraft and spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftResponse(200L, 100L, isAllowed = true)))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft())
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft()))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Unknown)
        .futureValue

      response.getPaymentActions shouldBe DealerPaymentActions
        .newBuilder()
        .setCardPayment(false)
        .setInvoice(false)
        .setInvoiceRequest(false)
        .setOverdraft(false)
        .build()
    }

    "get overdraft with spent > 0" in {
      when(invoiceService.getBalanceClientWithOrder(clientId))
        .thenReturnF(balanceOrderClientWithoutAgency)
      when(clientDao.get(clientId)).thenReturnF(client)
      when(vsBillingClient.getOrders(?, ?)(?)).thenReturnF(orderResponse)

      val overdraftInfo = overdraftResponse(30000L, 10000L, isAllowed = true)
      when(dealerStatsClient.getWeekAverageOutcome(?)(?, ?, ?, ?))
        .thenReturnF(returnedDealerStatsOutcome)
      when(vsBillingClient.getOverdraft(?)(?))
        .thenReturnF(Some(overdraftInfo))
      when(clientService.getClientOverdraft(?, ?)(?))
        .thenReturnF(dealerOverdraft(invoiceId = 7777777L))
      when(clientService.getLastClientOverdraftInvoice(?)(?))
        .thenReturnF(Some(dealerOverdraft(invoiceId = 7777777L)))

      val response = dealerManager
        .getDealerAccount(clientId, DealerUserRoleForAccount.Unknown)
        .futureValue

      response.getOverdraft shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .setLimit(300L)
        .setSpent(100L)
        .setInvoiceId(7777777L)
        .setDeadline(overdraftInfo.deadline.get.toProtobufTimestamp)
        .setExpired(false)
        .build()
    }
  }
}
