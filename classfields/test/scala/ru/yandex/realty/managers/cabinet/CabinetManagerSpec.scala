package ru.yandex.realty.managers.cabinet

import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.ProtoResponse.CabinetCampaignCallsResponse
import ru.yandex.realty.application.BillingComponents
import ru.yandex.realty.clients.billing.BillingClient.ClientId
import ru.yandex.realty.clients.billing._
import ru.yandex.realty.clients.billing.gen.{BillingDomainGenerators, BillingGenerators}
import ru.yandex.realty.clients.finstat.FinstatClient
import ru.yandex.realty.clients.{billing, BnbSearcherClient}
import ru.yandex.realty.features.FeatureStub
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.http.{HttpClient, HttpComponents, RequestAware}
import ru.yandex.realty.managers.cabinet.CabinetConverters._
import ru.yandex.realty.managers.cabinet.CabinetManagerTestComponents._
import ru.yandex.realty.model.billing.BillingDomain
import ru.yandex.realty.model.exception.ForbiddenException
import ru.yandex.realty.model.user.{PassportUser, UserRefGenerators}
import ru.yandex.realty.model.util.{Page, Slice, SliceToPageAdapter}
import ru.yandex.realty.phone.MaskPhonesService
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Properties
import ru.yandex.realty.{AsyncSpecBase, Slicing}
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class CabinetManagerSpec
  extends AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with UserRefGenerators
  with BillingDomainGenerators
  with BillingGenerators
  with RegionGraphTestComponents
  with CabinetManagerTestComponents {

  implicit private val generatorDrivenConfiguration: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 2, maxDiscardedFactor = 1.0, minSize = 4, sizeRange = 5)

  private val billingClient: billing.BillingClient = mock[billing.BillingClient]

  private class BillingComponentsWithHttpClient extends BillingComponents with HttpComponents with Properties {
    implicit override def ec: ExecutionContext = ???

    override val httpClient: HttpClient = mock[HttpClient]

    override def withBillingClient[A](billingDomain: BillingDomain)(callback: BillingClient => Future[A]): Future[A] =
      callback(billingClient)
  }

  private val billingComponents: BillingComponentsWithHttpClient = new BillingComponentsWithHttpClient()

  private type UIDType = String
  private type AgencyIdType = Option[ClientId]
  private type PageNumType = Option[Int]
  private type PageSizeType = Option[Int]
  private type ClientTypeType = String
  private type OrderId = Int
  private type SinceTimestampType = Instant
  private type TillTimestampType = Option[Instant]
  private type TransactionTypeOptionType = Option[OrderTransactionType.Value]
  private type CampaignId = String

  private val mockGetOrders = toMockFunction6(
    billingClient.getOrders(_: ClientId, _: UIDType, _: AgencyIdType, _: PageNumType, _: PageSizeType)(_: Traced)
  )

  private val mockGetUser =
    toMockFunction2(billingClient.getUser(_: UIDType)(_: Traced))

  private val mockGetBalanceClient = toMockFunction4(
    billingClient.getBalanceClient(_: ClientId, _: AgencyIdType, _: UIDType)(_: Traced)
  )

  private val mockGetAgencyClients = toMockFunction5(
    billingClient.getAgencyClients(_: UIDType, _: ClientId, _: PageNumType, _: PageSizeType)(_: Traced)
  )

  private val mockGetClientsByType = toMockFunction5(
    billingClient.getClientsByType(_: UIDType, _: ClientTypeType, _: PageNumType, _: PageSizeType)(_: Traced)
  )

  private val mockGetCustomer = toMockFunction4(
    billingClient.getCustomer(_: UIDType, _: ClientId, _: AgencyIdType)(_: Traced)
  )

  private val mockGetBindings =
    toMockFunction4(billingClient.getBindings(_: UIDType, _: ClientId, _: AgencyIdType)(_: Traced))

  private val mockGetCampaigns = toMockFunction6(
    billingClient.getCampaigns(_: UIDType, _: ClientId, _: AgencyIdType, _: PageNumType, _: PageSizeType)(_: Traced)
  )

  private val mockGetOrderTransactions = toMockFunction10(
    billingClient.getOrderTransactions(
      _: ClientId,
      _: OrderId,
      _: AgencyIdType,
      _: UIDType,
      _: SinceTimestampType,
      _: TillTimestampType,
      _: PageNumType,
      _: PageSizeType,
      _: TransactionTypeOptionType
    )(_: Traced)
  )

  private val mockGetOrderOutcome = toMockFunction8(
    billingClient.getOrderTransactionsOutcome(
      _: ClientId,
      _: OrderId,
      _: AgencyIdType,
      _: UIDType,
      _: SinceTimestampType,
      _: TillTimestampType,
      _: Boolean
    )(_: Traced)
  )

  private val mockGetCampaignCalls = toMockFunction9(
    billingClient.getCampaignCalls(
      _: UIDType,
      _: ClientId,
      _: AgencyIdType,
      _: CampaignId,
      _: SinceTimestampType,
      _: TillTimestampType,
      _: PageNumType,
      _: PageSizeType
    )(_: Traced)
  )

  val maskPhonesService = new MaskPhonesService(billingCampaignProvider, companiesProvider)

  val sitesService = mock[SitesGroupingService]
  val finstatClient = mock[FinstatClient]
  val bnbSearcherClient = mock[BnbSearcherClient]
  val modifiedCallsCache = mock[ModifiedCallsCache]

  val manager: DefaultCabinetManager =
    new DefaultCabinetManager(
      billingComponents,
      maskPhonesService,
      finstatClient,
      bnbSearcherClient,
      sitesService,
      regionGraphProvider,
      modifiedCallsCache,
      new FeatureStub(false)
    )

  private val forbidden = new ForbiddenException("")
  private val notFound = new NoSuchElementException("")

  val billingFirstPage: billing.Page = billing.Page(1, 0)
  val requestFirstPage: Slice = Page(0, 1)

  val singlePageSlicing: Slicing = Slicing
    .newBuilder()
    .setTotal(1)
    .setPage(
      ru.yandex.vertis.paging.Slice.Page
        .newBuilder()
        .setSize(1)
        .setNum(0)
        .build()
    )
    .build()

  "cabinetManager" when {
    "getOrdersList" should inSequence {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val client = billingRequestContext.customer.client

          mockGetOrders
            .expects(client.id, uid.toString, client.agencyId, Some(0), Some(1), *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getOrdersList(billingRequestContext, requestFirstPage)
            }

          val throwable = result.failed.futureValue
          throwable should be(forbidden)
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, ordersPageGen) { (billingRequestContext, ordersPageResponse) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val client = billingRequestContext.customer.client

          mockGetOrders
            .expects(client.id, uid.toString, client.agencyId, Some(0), Some(1), *)
            .returningF(ordersPageResponse)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getOrdersList(billingRequestContext, requestFirstPage)
            }

          val expected = toResponseOrders(ordersPageResponse)
          result.futureValue should be(expected)
        }
      }
    }

    "getUserInfo" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(billingRequestContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val billingDomain = billingRequestContext.billingDomain
          val user = PassportUser(uid)

          mockGetUser
            .expects(uid.toString, *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getUserInfo(uid, billingDomain)
            }

          result.failed.futureValue shouldBe forbidden
        }
      }

      "conclude with Not Found status for non-existent UID" in inSequence {
        forAll(billingRequestContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val billingDomain = billingRequestContext.billingDomain
          val user = PassportUser(uid)

          mockGetUser
            .expects(*, *)
            .returningF(None)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getUserInfo(uid, billingDomain)
            }

          result.failed.futureValue shouldBe a[NoSuchElementException]
        }
      }

      "conclude with some valid response for a valid UID" in inSequence {
        forAll(billingRequestContextGen, userGen) { (billingRequestContext, billingUser) =>
          val uid = billingRequestContext.uid
          val billingDomain = billingRequestContext.billingDomain
          val user = PassportUser(uid)

          val validResult = toResponse(uid, billingUser)

          mockGetUser
            .expects(uid.toString, *)
            .returningF(Some(billingUser))

          val result =
            withRequestContext(user) { implicit r =>
              manager.getUserInfo(uid, billingDomain)
            }

          result.futureValue shouldBe validResult
        }
      }

    }

    "getBalanceClientInfo" should {
      "conclude with some valid response for a valid client" in inSequence {
        forAll(anyContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val validResult = billingRequestContext match {
            case UnregisteredClientContext(_, _, client) =>
              toResponse(client)

            case CustomerContext(_, _, customer) =>
              toResponse(customer.client)

            case AgencyContext(_, _, client) =>
              toResponse(client)
          }

          val result =
            withRequestContext(user) { implicit r =>
              manager.getBalanceClientInfo(billingRequestContext)
            }

          result.futureValue should be(validResult)
        }
      }
    }

    "getAgencyClients" should inSequence {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(agencyContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val agency = billingRequestContext.agency

          mockGetAgencyClients
            .expects(uid.toString, agency.id, Some(0), Some(1), *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getAgencyClients(billingRequestContext, requestFirstPage)
            }

          val throwable = result.failed.futureValue
          throwable should be(forbidden)
        }
      }

      "conclude with empty response for non-existent agency" in inSequence {
        forAll(agencyContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val agency = billingRequestContext.agency

          mockGetAgencyClients
            .expects(uid.toString, agency.id, Some(0), Some(1), *)
            .throwingF(notFound)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getAgencyClients(billingRequestContext, requestFirstPage)
            }

          result.failed.futureValue shouldBe a[NoSuchElementException]
        }
      }

      "conclude with some valid response for a valid agency" in inSequence {
        forAll(agencyContextGen, anyCustomerGen) { (billingRequestContext, customer) =>
          val customersPageResponse = billing.CustomersPageResponse(0, billingFirstPage, Seq(customer))

          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val agency = billingRequestContext.agency

          mockGetAgencyClients
            .expects(uid.toString, agency.id, Some(0), Some(1), *)
            .returningF(customersPageResponse)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getAgencyClients(billingRequestContext, requestFirstPage)
            }

          val expected = toResponse(customersPageResponse, new SliceToPageAdapter(requestFirstPage))
          result.futureValue should be(expected)
        }
      }
    }

    "getClientsList" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(agencyContextGen, clientQueryTypeGen) { (billingRequestContext, queryType) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          mockGetClientsByType
            .expects(uidString, queryType.asBillingClientQueryType, Some(0), Some(1), *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getClientsList(uid, billingDomain, queryType, requestFirstPage)
            }

          result.failed.futureValue should be(forbidden)
        }
      }

      "conclude with some valid response for a valid queryType" in inSequence {
        forAll(agencyContextGen, agencyClientGen, clientQueryTypeGen) {
          (billingRequestContext, agencyClientProto, queryType) =>
            val uid = billingRequestContext.uid
            val user = PassportUser(uid)
            val agency = billingRequestContext.agency
            val agencyClient = agencyClientProto.copy(agencyId = Some(agency.id))

            val clientDescription = clientToClientDescription(agencyClient)
            val validResult = billing.ClientsPageResponse(0, billingFirstPage, Seq(clientDescription))

            val uidString = uid.toString
            val billingDomain = billingRequestContext.billingDomain
            mockGetClientsByType
              .expects(uidString, queryType.asBillingClientQueryType, Some(0), Some(1), *)
              .returningF(validResult)

            val result =
              withRequestContext(user) { implicit r =>
                manager.getClientsList(uid, billingDomain, queryType, requestFirstPage)
              }

            result.futureValue should be(toResponse(validResult, new SliceToPageAdapter(requestFirstPage)))
        }
      }
    }

    "getCustomerInfoDirectClient" should {
      "propagate customer as is from billingRequestContext" in inSequence {
        forAll(customerContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val customer = billingRequestContext.customer

          val result =
            withRequestContext(user) { implicit r =>
              manager.getCustomerInfo(billingRequestContext)
            }

          result.futureValue should be(toResponse(customer))
        }
      }

      "conclude with some valid response for a user with billing customer (either direct balance client" +
        " or an explicit client of an agency)" in inSequence {

        forAll(customerContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)
          val customer = billingRequestContext.customer
          val client = customer.client

          val expected = toResponse(customer)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getCustomerInfo(billingRequestContext)
            }

          result.futureValue should be(expected)
        }
      }
    }

    "getBindingsList" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client
          mockGetBindings
            .expects(uidString, client.id, client.agencyId, *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getBindingsList(billingRequestContext)
            }

          result.failed.futureValue should be(forbidden)
        }
      }

      "conclude with some valid response for a user with billing customer (either direct balance client" +
        " or an explicit client of an agency)" in inSequence {

        forAll(customerContextGen, bindingsGen) { (billingRequestContext, bindings) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client

          mockGetBindings
            .expects(uidString, client.id, client.agencyId, *)
            .returningF(bindings)

          val expected = toResponseBindings(bindings)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getBindingsList(billingRequestContext)
            }

          result.futureValue should be(expected)
        }
      }
    }

    "getCampaignsList" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen) { billingRequestContext =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client
          mockGetCampaigns
            .expects(uidString, client.id, client.agencyId, Some(0), Some(1), *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getCampaignsList(billingRequestContext, requestFirstPage)
            }

          result.failed.futureValue should be(forbidden)
        }
      }

      "conclude with some valid response for a user with billing customer (either direct balance client" +
        " or an explicit client of an agency)" in inSequence {

        forAll(customerContextGen, campaignsGen) { (billingRequestContext, campaigns) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client

          mockGetCampaigns
            .expects(uidString, client.id, client.agencyId, Some(0), Some(1), *)
            .returningF(campaigns)

          val expected = toResponseCampaigns(campaigns)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getCampaignsList(billingRequestContext, requestFirstPage)
            }

          result.futureValue should be(expected)
        }
      }
    }

    "getOrderTransactions" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen, orderIdGen, timespanGen, orderTransactionTypeOptionGen, orderTransactionsPageGen) {
          (billingRequestContext, orderId, timespan, transactionType, orderTransactionsPage) =>
            val uid = billingRequestContext.uid
            val user = PassportUser(uid)

            val uidString = uid.toString
            val billingDomain = billingRequestContext.billingDomain
            val client = billingRequestContext.customer.client
            val since = timespan.from
            val till = timespan.to

            mockGetOrderTransactions
              .expects(
                client.id,
                orderId,
                client.agencyId,
                uidString,
                since,
                till,
                Some(0),
                Some(1),
                transactionType,
                *
              )
              .throwingF(forbidden)

            val result =
              withRequestContext(user) { implicit r =>
                manager.getOrderTransactions(
                  billingRequestContext,
                  orderId,
                  transactionType,
                  timespan,
                  requestFirstPage
                )
              }

            result.failed.futureValue should be(forbidden)
        }
      }

      "conclude with some valid response for a user with billing customer (either direct balance client" +
        " or an explicit client of an agency)" in inSequence {

        forAll(customerContextGen, orderIdGen, timespanGen, orderTransactionTypeOptionGen, orderTransactionsPageGen) {
          (billingRequestContext, orderId, timespan, transactionType, orderTransactionsPage) =>
            val uid = billingRequestContext.uid
            val user = PassportUser(uid)

            val uidString = uid.toString
            val billingDomain = billingRequestContext.billingDomain
            val client = billingRequestContext.customer.client

            val since = timespan.from
            val till = timespan.to

            mockGetOrderTransactions
              .expects(
                client.id,
                orderId,
                client.agencyId,
                uidString,
                since,
                till,
                Some(0),
                Some(1),
                transactionType,
                *
              )
              .returningF(orderTransactionsPage)

            val expected = toResponseTransactions(orderTransactionsPage)

            val result =
              withRequestContext(user) { implicit r =>
                manager.getOrderTransactions(
                  billingRequestContext,
                  orderId,
                  transactionType,
                  timespan,
                  requestFirstPage
                )
              }

            result.futureValue should be(expected)
        }
      }

      "propagate comment field for rebate transaction type" in inSequence {
        def commented(comment: String)(input: OrderTransaction): OrderTransaction = input.copy(comment = Some(comment))

        forAll(
          customerContextGen,
          orderIdGen,
          timespanGen,
          orderTransactionsPageGen,
          readableString
        ) { (billingRequestContext, orderId, timespan, orderTransactionsPage, comment) =>
          val transactionType = OrderTransactionType.Rebate

          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client

          val since = timespan.from
          val till = timespan.to

          def replaceTransactionType: OrderTransaction => OrderTransaction = _.copy(`type` = transactionType)

          val transactionsFromBilling = orderTransactionsPage.copy(
            values = orderTransactionsPage.values
              .map(commented(comment))
              .map(replaceTransactionType)
          )

          val clientId = client.id
          val agencyId = client.agencyId
          mockGetOrderTransactions
            .expects(clientId, orderId, agencyId, uidString, since, till, Some(0), Some(1), Some(transactionType), *)
            .returningF(transactionsFromBilling)

          val expected = toResponseTransactions(transactionsFromBilling)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getOrderTransactions(
                billingRequestContext,
                orderId,
                Some(transactionType),
                timespan,
                requestFirstPage
              )
            }

          result.futureValue should be(expected)
        }
      }

      "refuse propagating comment field for non-rebate transaction types" in inSequence {
        def commented(comment: String)(input: OrderTransaction): OrderTransaction = input.copy(comment = Some(comment))
        def stripped(input: OrderTransaction): OrderTransaction = input.copy(comment = None)
        def replaceTransactionType(
          transactionTypeOption: Option[OrderTransactionType.Value]
        )(input: OrderTransaction): OrderTransaction = {
          input.copy(`type` = transactionTypeOption.getOrElse(OrderTransactionType.Overdraft))
        }

        forAll(
          customerContextGen,
          orderIdGen,
          timespanGen,
          orderTransactionTypeNonRebateOptionGen,
          orderTransactionsPageGen,
          readableString
        ) { (billingRequestContext, orderId, timespan, transactionType, orderTransactionsPage, comment) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client

          val since = timespan.from
          val till = timespan.to

          val clientId = client.id
          val agencyId = client.agencyId

          val transactionsFromBilling = orderTransactionsPage.copy(
            values = orderTransactionsPage.values
              .map(commented(comment))
              .map(replaceTransactionType(transactionType))
          )

          mockGetOrderTransactions
            .expects(clientId, orderId, agencyId, uidString, since, till, Some(0), Some(1), transactionType, *)
            .returningF(transactionsFromBilling)

          val transactionsStripped = transactionsFromBilling
            .copy(values = transactionsFromBilling.values.map(stripped))

          val expected = toResponseTransactions(transactionsStripped)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getOrderTransactions(billingRequestContext, orderId, transactionType, timespan, requestFirstPage)
            }

          result.futureValue should be(expected)
        }
      }

      "correctly process (notably convert) overdraft transactions" in inSequence {
        forAll(customerContextGen, orderIdGen, timespanGen, orderTransactionsPageGen) {
          (billingRequestContext, orderId, timespan, orderTransactionsPage) =>
            val uid = billingRequestContext.uid
            val user = PassportUser(uid)

            val uidString = uid.toString
            val billingDomain = billingRequestContext.billingDomain
            val client = billingRequestContext.customer.client

            val since = timespan.from
            val till = timespan.to

            val clientId = client.id
            val agencyIdOpt = client.agencyId
            val overdraft = Some(OrderTransactionType.Overdraft)
            mockGetOrderTransactions
              .expects(clientId, orderId, agencyIdOpt, uidString, since, till, Some(0), Some(1), overdraft, *)
              .returningF(orderTransactionsPage)

            val expected = toResponseTransactions(orderTransactionsPage)

            val result =
              withRequestContext(user) { implicit r =>
                manager.getOrderTransactions(billingRequestContext, orderId, overdraft, timespan, requestFirstPage)
              }

            result.futureValue should be(expected)
        }
      }

    }

    "getOrderOutcome" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(
          customerContextGen,
          orderIdGen,
          timespanGen,
          withdrawWithDetailsGen
        ) { (billingRequestContext, orderId, timespan, withdrawWithDetails) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client

          val since = timespan.from
          val till = timespan.to

          mockGetOrderOutcome
            .expects(client.id, orderId, client.agencyId, uidString, since, till, *, *)
            .throwingF(forbidden)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getOrderOutcome(billingRequestContext, orderId, timespan)
            }

          result.failed.futureValue should be(forbidden)
        }
      }

      "conclude with some valid response for a user with billing customer (either direct balance client" +
        " or an explicit client of an agency)" in inSequence {

        forAll(
          customerContextGen,
          orderIdGen,
          timespanGen,
          withdrawWithDetailsGen,
          orderOutcomeGen
        ) { (billingRequestContext, orderId, timespan, withdrawWithDetails, orderOutcome) =>
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          val uidString = uid.toString
          val billingDomain = billingRequestContext.billingDomain
          val client = billingRequestContext.customer.client

          val since = timespan.from
          val till = timespan.to

          mockGetOrderOutcome
            .expects(client.id, orderId, client.agencyId, uidString, since, till, *, *)
            .returningF(orderOutcome)

          val expected = toResponseOrderOutcome(orderOutcome)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getOrderOutcome(billingRequestContext, orderId, timespan)
            }

          result.futureValue should be(expected)
        }
      }
    }

    "getCampaignCalls" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen, campaignIdGen, timespanGen, campaignCallsPageGen) {
          (billingRequestContext, campaignId, timespan, campaignsCallPage) =>
            val uid = billingRequestContext.uid
            val user = PassportUser(uid)

            val uidString = uid.toString
            val billingDomain = billingRequestContext.billingDomain
            val client = billingRequestContext.customer.client

            val since = timespan.from
            val till = timespan.to

            mockGetCampaignCalls
              .expects(uidString, client.id, client.agencyId, campaignId, since, till, Some(0), Some(1), *)
              .throwingF(forbidden)

            val result =
              withRequestContext(user) { implicit r =>
                manager.getCampaignCalls(billingRequestContext, campaignId, timespan, requestFirstPage)
              }

            result.failed.futureValue should be(forbidden)
        }
      }

      "conclude with some valid response for a user with billing customer (either direct balance client" +
        " or an explicit client of an agency)" in inSequence {

        forAll(customerContextGen, campaignIdGen, timespanGen, campaignCallsPageGen) {
          (billingRequestContext, campaignId, timespan, campaignsCallPage) =>
            val uid = billingRequestContext.uid
            val user = PassportUser(uid)

            val uidString = uid.toString
            val billingDomain = billingRequestContext.billingDomain
            val client = billingRequestContext.customer.client

            val since = timespan.from
            val till = timespan.to

            mockGetCampaignCalls
              .expects(uidString, client.id, client.agencyId, campaignId, since, till, Some(0), Some(1), *)
              .returningF(campaignsCallPage)

            val expected = toResponseCampaignCalls(campaignsCallPage)

            val result =
              withRequestContext(user) { implicit r =>
                manager.getCampaignCalls(billingRequestContext, campaignId, timespan, requestFirstPage)
              }

            result.futureValue should be(expected)
        }
      }
    }

    "getCustomerCampaignCalls" should {
      "mask incoming phone numbers if campaign maskIncomingPhoneNumber is true" in inSequence {
        forAll(timespanGen, campaignCallsPageGen) { (timespan, campaignsCallPage) =>
          val uid = 124214252151L
          val user = PassportUser(uid)

          val uidString = uid.toString
          val clientId = 124214
          val since = timespan.from
          val till = timespan.to
          val campaignId = companyCampaignWithMaskIncomingPhoneId

          val expected = maskIncoming(toResponseCampaignCalls(campaignsCallPage))
          mockGetCampaignCalls
            .expects(uidString, clientId, None, campaignId, since, till, Some(0), Some(1), *)
            .returningF(campaignsCallPage)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getCustomerCampaignCalls(uid, clientId, None, campaignId, timespan, requestFirstPage)
            }

          result.futureValue should be(expected)
        }
      }

      "do not mask incoming phone numbers if campaign maskIncomingPhoneNumber is not true" in inSequence {
        forAll(timespanGen, campaignCallsPageGen) { (timespan, campaignsCallPage) =>
          val uid = 124214252151L
          val user = PassportUser(uid)

          val uidString = uid.toString
          val clientId = 124214
          val since = timespan.from
          val till = timespan.to
          val campaignId = companyCampaignWithoutMaskIncomingPhoneId

          val expected = toResponseCampaignCalls(campaignsCallPage)
          mockGetCampaignCalls
            .expects(uidString, clientId, None, campaignId, since, till, Some(0), Some(1), *)
            .returningF(campaignsCallPage)

          val result =
            withRequestContext(user) { implicit r =>
              manager.getCustomerCampaignCalls(uid, clientId, None, campaignId, timespan, requestFirstPage)
            }

          result.futureValue should be(expected)
        }
      }
    }

  }

  private def maskIncoming(response: CabinetCampaignCallsResponse): CabinetCampaignCallsResponse = {
    val b = response.toBuilder
    for {
      campaignCall <- b.getResponseBuilder.getCampaignCallsBuilderList.asScala
      callBuilder = campaignCall.getCallBuilder
      maskedIncoming = MaskPhonesService.mask(callBuilder.getIncoming)
    } callBuilder.setIncoming(maskedIncoming)
    b.build()
  }

  private def clientToClientDescription(agencyClient: billing.Client): ClientDescription = {
    require(agencyClient.agencyId.isDefined)
    billing.ClientDescription(agencyClient.id, agencyClient.agencyId, agencyClient, None, isAgencyInService = true)
  }
}
