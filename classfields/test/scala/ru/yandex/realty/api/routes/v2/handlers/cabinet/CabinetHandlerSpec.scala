package ru.yandex.realty.api.routes.v2.handlers.cabinet

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.Slicing
import ru.yandex.realty.api.ProtoResponse._
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.routes.v2.cabinet.CabinetHandlerImpl
import ru.yandex.realty.api.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.realty.clients.billing.BillingClient.ClientId
import ru.yandex.realty.clients.billing.gen.{BillingDomainGenerators, BillingGenerators}
import ru.yandex.realty.clients.billing.{BillingRequestContext, BillingRequestContextResolver, Client => BillingClient}
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.cabinet.CabinetConverters._
import ru.yandex.realty.managers.cabinet.CabinetManager
import ru.yandex.realty.clients.billing.OrderTransactionType
import ru.yandex.realty.model.billing
import ru.yandex.realty.model.billing.{BillingDomain, BillingDomains}
import ru.yandex.realty.model.exception.ForbiddenException
import ru.yandex.realty.model.user.{PassportUser, UserRefGenerators}
import ru.yandex.realty.model.util.{Page, Slice}
import ru.yandex.realty.proto.billing._
import ru.yandex.realty.request.Request
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

import scala.languageFeature.postfixOps

@RunWith(classOf[JUnitRunner])
class CabinetHandlerSpec
  extends HandlerSpecBase
  with PropertyChecks
  with UserRefGenerators
  with BasicDirectives
  with ScalatestRouteTest
  with BillingDomainGenerators
  with BillingGenerators {

  override protected val exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler
  override protected val rejectionHandler: RejectionHandler = ApiRejectionHandler.handler

  implicit val excHandler: ExceptionHandler = exceptionHandler
  implicit val rejHandler: RejectionHandler = rejectionHandler

  override def routeUnderTest: Route = Route.seal(new CabinetHandlerImpl(manager, resolver).route)

  private val manager: CabinetManager = mock[CabinetManager]
  private val resolver: BillingRequestContextResolver = mock[BillingRequestContextResolver]
  private val forbidden = new ForbiddenException(None.orNull)
  private val notFound = new NoSuchElementException()
  private val oneItemPageQuery = Uri.Query("page" -> "0", "pageSize" -> "1")
  private val oneItemFirstPage = Page(0, 1)

  private val resolverMock = toMockFunction4(
    resolver.resolve(_: BillingDomain, _: Long, _: Option[ClientId])(_: Traced)
  )

  private val mockGetOrdersList =
    toMockFunction3(manager.getOrdersList(_: BillingRequestContext, _: Slice)(_: Request))

  private val mockGetUserInfo = toMockFunction3(manager.getUserInfo(_: Long, _: BillingDomain)(_: Request))

  private val mockGetBalanceClientInfo =
    toMockFunction2(manager.getBalanceClientInfo(_: BillingRequestContext)(_: Request))

  private val mockGetAgencyClients =
    toMockFunction3(manager.getAgencyClients(_: BillingRequestContext, _: Slice)(_: Request))

  private val mockGetCustomerInfo =
    toMockFunction2(manager.getCustomerInfo(_: BillingRequestContext)(_: Request))

  private val mockGetBindingsList =
    toMockFunction2(manager.getBindingsList(_: BillingRequestContext)(_: Request))

  private val mockGetCampaignsList =
    toMockFunction3(manager.getCampaignsList(_: BillingRequestContext, _: Slice)(_: Request))

  private val mockGetOrderTransactions = toMockFunction6(
    manager.getOrderTransactions(
      _: BillingRequestContext,
      _: Int,
      _: Option[OrderTransactionType.Value],
      _: billing.Timespan,
      _: Slice
    )(_: Request)
  )

  private val mockGetOrderOutcome = toMockFunction4(
    manager.getOrderOutcome(_: BillingRequestContext, _: Int, _: billing.Timespan)(_: Request)
  )

  private val mockGetCampaignCalls = toMockFunction5(
    manager.getCampaignCalls(_: BillingRequestContext, _: String, _: billing.Timespan, _: Slice)(_: Request)
  )

  "cabinetHandler" when {
    "ordersListDirectClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen) { customerContext =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetOrdersList
            .expects(customerContext, oneItemFirstPage, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/orders")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, ordersPageGen) { (customerContext, result) =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetOrdersList
            .expects(customerContext, oneItemFirstPage, *)
            .returningF(toResponseOrders(result))

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/orders")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetOrdersResponse] should be(toResponseOrders(result))
            }
        }
      }
    }

    "ordersListAgencyClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in {
        forAll(agencyContextGen, directClientGen) { (agencyContext, directClient) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          mockGetOrdersList
            .expects(agencyContext, oneItemFirstPage, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/orders")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in {
        forAll(agencyContextGen, directClientGen, ordersPageGen) { (agencyContext, directClient, result) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          mockGetOrdersList
            .expects(agencyContext, oneItemFirstPage, *)
            .returningF(toResponseOrders(result))

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/orders")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetOrdersResponse] should be(toResponseOrders(result))
            }
        }
      }
    }

    "userInfo" should {
      "propagate missing/inapt authorization as Forbidden status" in {
        forAll(billingDomainGen, passportUserGen) { (billingDomain, user) =>
          inSequence {
            val uid = user.uid
            mockGetUserInfo
              .expects(uid, billingDomain, *)
              .throwingF(forbidden)

            Get(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}").acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
          }
        }
      }

      "conclude with Not Found status for non-existent UID" in {
        forAll(billingDomainGen, passportUserGen) { (billingDomain, user) =>
          inSequence {
            val uid = user.uid
            mockGetUserInfo
              .expects(uid, billingDomain, *)
              .throwingF(notFound)

            Get(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}").acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.NotFound)
              }
          }
        }
      }

      "conclude with some valid response for a valid UID" in {
        forAll(billingDomainGen, passportUserGen, anyCustomerGen) { (billingDomain, user, customer) =>
          inSequence {
            val validResult: CabinetUserInfoResponse = {
              val resultBuilder = CabinetUserInfoResponse.newBuilder()
              val userInfoBuilder = resultBuilder.getResponseBuilder
              userInfoBuilder.setUserId(user.uid)
              userInfoBuilder.setRole(Role.ROLE_REGULAR_USER)

              userInfoBuilder.setCustomers(CustomersList.newBuilder().addCustomers(convert(customer)))
              resultBuilder.build()
            }

            mockGetUserInfo
              .expects(user.uid, billingDomain, *)
              .returningF(validResult)

            Get(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}").acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetUserInfoResponse] should be(validResult)
              }
          }
        }
      }
    }

    "balanceClientInfo" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in {
        forAll(billingDomainGen, passportUserGen, billingRequestContextGen, clientIdForAgencyMembersGen) {
          (billingDomain, user, billingRequestContext, clientIdOption) =>
            inSequence {
              val uid = user.uid

              resolverMock
                .expects(billingDomain, uid, clientIdOption, *)
                .anyNumberOfTimes()
                .returningF(billingRequestContext)

              mockGetBalanceClientInfo
                .expects(billingRequestContext, *)
                .throwingF(forbidden)

              Get(
                Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/client")
                  .withQuery(Uri.Query(asQueryParam(clientIdOption)))
              ).acceptingProto
                .withUser(user) ~>
                route ~>
                check {
                  status should be(StatusCodes.Forbidden)
                }
            }
        }
      }

      "conclude with Not Found status for non-existent client" in {
        forAll(billingDomainGen, passportUserGen, billingRequestContextGen, clientIdForAgencyMembersGen) {
          (billingDomain, user, billingRequestContext, clientIdOption) =>
            inSequence {
              val uid = user.uid

              resolverMock
                .expects(billingDomain, uid, clientIdOption, *)
                .anyNumberOfTimes()
                .returningF(billingRequestContext)

              mockGetBalanceClientInfo
                .expects(billingRequestContext, *)
                .throwingF(notFound)

              Get(
                Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/client")
                  .withQuery(Uri.Query(asQueryParam(clientIdOption)))
              ).acceptingProto
                .withUser(user) ~>
                route ~>
                check {
                  status should be(StatusCodes.NotFound)
                }
            }
        }
      }

      "conclude with some valid response for a valid client" in {
        forAll(billingDomainGen, passportUserGen, billingRequestContextGen, clientIdForAgencyMembersGen) {
          (billingDomain, user, billingRequestContext, clientIdOption) =>
            inSequence {
              val uid = user.uid

              resolverMock
                .expects(billingDomain, uid, clientIdOption, *)
                .anyNumberOfTimes()
                .returningF(billingRequestContext)

              mockGetBalanceClientInfo
                .expects(billingRequestContext, *)
                .returningF(CabinetClientResponse.getDefaultInstance)

              Get(
                Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/client")
                  .withQuery(Uri.Query(asQueryParam(clientIdOption)))
              ).acceptingProto
                .withUser(user) ~>
                route ~>
                check {
                  status should be(StatusCodes.OK)
                  entityAs[CabinetClientResponse] should be(CabinetClientResponse.getDefaultInstance)
                }
            }
        }
      }
    }

    "agencyClients" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(billingRequestContextGen, clientIdForAgencyMembersGen) { (billingRequestContext, clientIdOption) =>
          val billingDomain = billingRequestContext.billingDomain
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          resolverMock
            .expects(billingDomain, uid, clientIdOption, *)
            .anyNumberOfTimes()
            .returningF(billingRequestContext)

          mockGetAgencyClients
            .expects(billingRequestContext, oneItemFirstPage, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/agency/clients")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(clientIdOption)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with Not Found status for non-existent agency" in inSequence {
        forAll(billingRequestContextGen, clientIdForAgencyMembersGen) { (billingRequestContext, clientIdOption) =>
          val billingDomain = billingRequestContext.billingDomain
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          resolverMock
            .expects(billingDomain, uid, clientIdOption, *)
            .anyNumberOfTimes()
            .returningF(billingRequestContext)

          mockGetAgencyClients
            .expects(billingRequestContext, oneItemFirstPage, *)
            .throwingF(notFound)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/agency/clients")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(clientIdOption)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.NotFound)
            }
        }
      }

      "conclude with some valid response for a valid agency" in inSequence {
        val trivialResult = CabinetAgencyClientsResponse.getDefaultInstance

        forAll(billingRequestContextGen, clientIdForAgencyMembersGen) { (billingRequestContext, clientIdOption) =>
          val billingDomain = billingRequestContext.billingDomain
          val uid = billingRequestContext.uid
          val user = PassportUser(uid)

          resolverMock
            .expects(billingDomain, uid, clientIdOption, *)
            .anyNumberOfTimes()
            .returningF(billingRequestContext)

          mockGetAgencyClients
            .expects(billingRequestContext, oneItemFirstPage, *)
            .returningF(trivialResult)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/agency/clients")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(clientIdOption)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetAgencyClientsResponse] shouldBe trivialResult
            }
        }
      }
    }

    "customerInfoDirectClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(billingRequestContextGen) { billingRequestContext: BillingRequestContext =>
          val uid = billingRequestContext.uid
          val billingDomain = billingRequestContext.billingDomain
          val user = PassportUser(uid)

          resolverMock
            .expects(billingDomain, uid, None, *)
            .anyNumberOfTimes()
            .returningF(billingRequestContext)

          mockGetCustomerInfo
            .expects(billingRequestContext, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/customer")
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with Not Found status for non-existent client" in inSequence {
        forAll(billingRequestContextGen) { billingRequestContext: BillingRequestContext =>
          val uid = billingRequestContext.uid
          val billingDomain = billingRequestContext.billingDomain
          val user = PassportUser(uid)

          resolverMock
            .expects(billingDomain, uid, None, *)
            .anyNumberOfTimes()
            .returningF(billingRequestContext)

          mockGetCustomerInfo
            .expects(billingRequestContext, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/customer")
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        val trivialResponse = CabinetCustomerInfoResponse.getDefaultInstance
        forAll(billingRequestContextGen) { billingRequestContext: BillingRequestContext =>
          val uid = billingRequestContext.uid
          val billingDomain = billingRequestContext.billingDomain
          val user = PassportUser(uid)

          resolverMock
            .expects(billingDomain, uid, None, *)
            .anyNumberOfTimes()
            .returningF(billingRequestContext)

          mockGetCustomerInfo
            .expects(billingRequestContext, *)
            .returningF(trivialResponse)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/customer")
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetCustomerInfoResponse] should be(trivialResponse)
            }
        }
      }
    }

    "customerInfoAgencyClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(billingRequestContextGen, agencyClientGen) {
          (billingRequestContext: BillingRequestContext, client: BillingClient) =>
            val uid = billingRequestContext.uid
            val billingDomain = billingRequestContext.billingDomain
            val user = PassportUser(uid)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(billingRequestContext)

            mockGetCustomerInfo
              .expects(billingRequestContext, *)
              .throwingF(forbidden)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/customer")
                .withQuery(Uri.Query(asQueryParam(client.agencyId)))
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
        }
      }

      "conclude with Not Found status for non-existent client" in inSequence {
        forAll(billingRequestContextGen, directClientGen) {
          (billingRequestContext: BillingRequestContext, client: BillingClient) =>
            val uid = billingRequestContext.uid
            val billingDomain = billingRequestContext.billingDomain
            val user = PassportUser(uid)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(billingRequestContext)

            mockGetCustomerInfo
              .expects(billingRequestContext, *)
              .throwingF(forbidden)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/customer")
                .withQuery(Uri.Query(asQueryParam(client.agencyId)))
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        val trivialResponse = CabinetCustomerInfoResponse.getDefaultInstance
        forAll(billingRequestContextGen, directClientGen) {
          (billingRequestContext: BillingRequestContext, client: BillingClient) =>
            val uid = billingRequestContext.uid
            val billingDomain = billingRequestContext.billingDomain
            val user = PassportUser(uid)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(billingRequestContext)

            mockGetCustomerInfo
              .expects(billingRequestContext, *)
              .returningF(trivialResponse)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/customer")
                .withQuery(Uri.Query(asQueryParam(client.agencyId)))
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetCustomerInfoResponse] should be(trivialResponse)
              }
        }
      }
    }

    "bindingsListDirectClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen) { customerContext =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetBindingsList
            .expects(customerContext, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/bindings")
              .withQuery(Uri.Query(asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, bindingsGen) { (customerContext, result) =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetBindingsList
            .expects(customerContext, *)
            .returningF(toResponseBindings(result))

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/bindings")
              .withQuery(Uri.Query(asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetCampaignBindingsResponse] should be(toResponseBindings(result))
            }
        }
      }
    }

    "bindingsListAgencyClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in {
        forAll(agencyContextGen, directClientGen) { (agencyContext, directClient) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          mockGetBindingsList
            .expects(agencyContext, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/bindings")
              .withQuery(Uri.Query(asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in {
        forAll(agencyContextGen, directClientGen, bindingsGen) { (agencyContext, directClient, result) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          mockGetBindingsList
            .expects(agencyContext, *)
            .returningF(toResponseBindings(result))

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/bindings")
              .withQuery(Uri.Query(asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetCampaignBindingsResponse] should be(toResponseBindings(result))
            }
        }
      }
    }

    "campaignsListDirectClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen) { customerContext =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetCampaignsList
            .expects(customerContext, oneItemFirstPage, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaigns")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, campaignsGen) { (customerContext, result) =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetCampaignsList
            .expects(customerContext, oneItemFirstPage, *)
            .returningF(toResponseCampaigns(result))

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaigns")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetCampaignsResponse] should be(toResponseCampaigns(result))
            }
        }
      }
    }

    "campaignsListAgencyClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in {
        forAll(agencyContextGen, directClientGen) { (agencyContext, directClient) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          mockGetCampaignsList
            .expects(agencyContext, oneItemFirstPage, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaigns")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in {
        forAll(agencyContextGen, directClientGen, campaignsGen) { (agencyContext, directClient, result) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          mockGetCampaignsList
            .expects(agencyContext, oneItemFirstPage, *)
            .returningF(toResponseCampaigns(result))

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaigns")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetCampaignsResponse] should be(toResponseCampaigns(result))
            }
        }
      }
    }

    "orderTransactionsDirectClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen, orderIdGen, orderTransactionTypeOptionGen, timespanGen) {
          (customerContext, orderId, transactionType, timespan) =>
            val uid = customerContext.uid
            val billingDomain = customerContext.billingDomain
            val user = PassportUser(uid)
            val client = customerContext.customer.client

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(customerContext)

            mockGetOrderTransactions
              .expects(customerContext, orderId, transactionType, timespan, oneItemFirstPage, *)
              .throwingF(forbidden)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/transactions")
                .withQuery(
                  Uri.Query(
                    oneItemPageQuery.toMap ++
                      asQueryParams(timespan) ++
                      asQueryParam("transactionType", transactionType) ++
                      asQueryParam(client.agencyId)
                  )
                )
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, orderIdGen, orderTransactionTypeOptionGen, timespanGen, orderTransactionsPageGen) {
          (customerContext, orderId, transactionType, timespan, orderTransactionsPage) =>
            val uid = customerContext.uid
            val billingDomain = customerContext.billingDomain
            val user = PassportUser(uid)
            val client = customerContext.customer.client

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(customerContext)

            val expected = toResponseTransactions(orderTransactionsPage)
            mockGetOrderTransactions
              .expects(customerContext, orderId, transactionType, timespan, oneItemFirstPage, *)
              .returningF(expected)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/transactions")
                .withQuery(
                  Uri.Query(
                    oneItemPageQuery.toMap ++
                      asQueryParams(timespan) ++
                      asQueryParam("transactionType", transactionType) ++
                      asQueryParam(client.agencyId)
                  )
                )
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetOrderTransactionsResponse] should be(expected)
              }
        }
      }
    }

    "orderTransactionsAgencyClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(agencyContextGen, directClientGen, orderIdGen, orderTransactionTypeOptionGen, timespanGen) {
          (agencyContext, directClient, orderId, transactionType, timespan) =>
            val uid = agencyContext.uid
            val billingDomain = agencyContext.billingDomain
            val user = PassportUser(uid)
            val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(agencyContext)

            mockGetOrderTransactions
              .expects(agencyContext, orderId, transactionType, timespan, oneItemFirstPage, *)
              .throwingF(forbidden)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/transactions")
                .withQuery(
                  Uri.Query(
                    oneItemPageQuery.toMap ++
                      asQueryParams(timespan) ++
                      asQueryParam("transactionType", transactionType) ++
                      asQueryParam(client.agencyId)
                  )
                )
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(
          agencyContextGen,
          directClientGen,
          orderIdGen,
          orderTransactionTypeOptionGen,
          timespanGen,
          orderTransactionsPageGen
        ) { (agencyContext, directClient, orderId, transactionType, timespan, orderTransactionsPage) =>
          val uid = agencyContext.uid
          val billingDomain = agencyContext.billingDomain
          val user = PassportUser(uid)
          val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(agencyContext)

          val expected = toResponseTransactions(orderTransactionsPage)
          mockGetOrderTransactions
            .expects(agencyContext, orderId, transactionType, timespan, oneItemFirstPage, *)
            .returningF(expected)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/transactions")
              .withQuery(
                Uri.Query(
                  oneItemPageQuery.toMap ++
                    asQueryParams(timespan) ++
                    asQueryParam("transactionType", transactionType) ++
                    asQueryParam(client.agencyId)
                )
              )
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
              entityAs[CabinetOrderTransactionsResponse] should be(expected)
            }
        }
      }
    }

    "orderOutcomeDirectClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen, orderIdGen, timespanGen) { (customerContext, orderId, timespan) =>
          val uid = customerContext.uid
          val billingDomain = customerContext.billingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(customerContext)

          mockGetOrderOutcome
            .expects(customerContext, orderId, timespan, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/outcome")
              .withQuery(Uri.Query(asQueryParams(timespan) ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, orderIdGen, timespanGen, orderOutcomeGen) {
          (customerContext, orderId, timespan, orderOutcome) =>
            val uid = customerContext.uid
            val billingDomain = customerContext.billingDomain
            val user = PassportUser(uid)
            val client = customerContext.customer.client

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(customerContext)

            val expected = toResponseOrderOutcome(orderOutcome)
            mockGetOrderOutcome
              .expects(customerContext, orderId, timespan, *)
              .returningF(expected)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/outcome")
                .withQuery(Uri.Query(asQueryParams(timespan) ++ asQueryParam(client.agencyId)))
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetOrderOutcomeResponse] should be(expected)
              }
        }
      }
    }

    "orderOutcomeAgencyClient" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(agencyContextGen, directClientGen, orderIdGen, timespanGen) {
          (agencyContext, directClient, orderId, timespan) =>
            val uid = agencyContext.uid
            val billingDomain = agencyContext.billingDomain
            val user = PassportUser(uid)
            val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(agencyContext)

            mockGetOrderOutcome
              .expects(agencyContext, orderId, timespan, *)
              .throwingF(forbidden)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/outcome")
                .withQuery(Uri.Query(asQueryParams(timespan) ++ asQueryParam(client.agencyId)))
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(agencyContextGen, directClientGen, orderIdGen, timespanGen, orderOutcomeGen) {
          (agencyContext, directClient, orderId, timespan, orderOutcome) =>
            val uid = agencyContext.uid
            val billingDomain = agencyContext.billingDomain
            val user = PassportUser(uid)
            val client = directClient.copy(agencyId = Some(agencyContext.agency.id))

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(agencyContext)

            val expected = toResponseOrderOutcome(orderOutcome)
            mockGetOrderOutcome
              .expects(agencyContext, orderId, timespan, *)
              .returningF(expected)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/order/$orderId/outcome")
                .withQuery(Uri.Query(asQueryParams(timespan) ++ asQueryParam(client.agencyId)))
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetOrderOutcomeResponse] should be(expected)
              }
        }
      }
    }

    "campaignCallsDirectClient for calls billing-domain" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(customerContextGen, campaignIdGen, timespanGen) { (customerContext, campaignId, timespan) =>
          val uid = customerContext.uid
          val billingDomain = BillingDomains.CallsBillingDomain
          val user = PassportUser(uid)
          val client = customerContext.customer.client
          val context = customerContext.copy(billingDomain = billingDomain)

          resolverMock
            .expects(billingDomain, uid, client.agencyId, *)
            .anyNumberOfTimes()
            .returningF(context)

          mockGetCampaignCalls
            .expects(context, campaignId, timespan, oneItemFirstPage, *)
            .throwingF(forbidden)

          Get(
            Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaign/$campaignId/calls")
              .withQuery(Uri.Query(oneItemPageQuery.toMap ++ asQueryParams(timespan) ++ asQueryParam(client.agencyId)))
          ).acceptingProto
            .withUser(user) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, campaignIdGen, timespanGen, campaignCallsPageGen) {
          (customerContext, campaignId, timespan, campaignCallsPage) =>
            val uid = customerContext.uid
            val billingDomain = BillingDomains.CallsBillingDomain
            val user = PassportUser(uid)
            val client = customerContext.customer.client
            val context = customerContext.copy(billingDomain = billingDomain)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(context)

            val expected = toResponseCampaignCalls(campaignCallsPage)
            mockGetCampaignCalls
              .expects(context, campaignId, timespan, oneItemFirstPage, *)
              .returningF(expected)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaign/$campaignId/calls")
                .withQuery(
                  Uri.Query(oneItemPageQuery.toMap ++ asQueryParams(timespan) ++ asQueryParam(client.agencyId))
                )
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetCampaignCallsResponse] should be(expected)
              }
        }
      }
    }

    "campaignCallsAgencyClient for calls billing-domain" should {
      "conclude with BadRequest for an unauthorized calls request into wrong billing-domain (offers)" in inSequence {
        forAll(agencyContextGen, directClientGen, campaignIdGen, timespanGen) {
          (agencyContext, directClient, campaignId, timespan) =>
            val uid = agencyContext.uid
            val billingDomain = BillingDomains.CallsBillingDomain
            val user = PassportUser(uid)
            val client = directClient.copy(agencyId = Some(agencyContext.agency.id))
            val context = agencyContext.copy(billingDomain = billingDomain)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(context)

            mockGetCampaignCalls
              .expects(context, campaignId, timespan, oneItemFirstPage, *)
              .throwingF(forbidden)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaign/$campaignId/calls")
                .withQuery(
                  Uri.Query(oneItemPageQuery.toMap ++ asQueryParams(timespan) ++ asQueryParam(client.agencyId))
                )
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.Forbidden)
              }
        }
      }

      "conclude with some valid response for a valid client" in inSequence {
        forAll(agencyContextGen, directClientGen, campaignIdGen, timespanGen, campaignCallsPageGen) {
          (agencyContext, directClient, campaignId, timespan, campaignCallsPage) =>
            val uid = agencyContext.uid
            val billingDomain = BillingDomains.CallsBillingDomain
            val user = PassportUser(uid)
            val client = directClient.copy(agencyId = Some(agencyContext.agency.id))
            val context = agencyContext.copy(billingDomain = billingDomain)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(context)

            val expected = toResponseCampaignCalls(campaignCallsPage)
            mockGetCampaignCalls
              .expects(context, campaignId, timespan, oneItemFirstPage, *)
              .returningF(expected)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaign/$campaignId/calls")
                .withQuery(
                  Uri.Query(oneItemPageQuery.toMap ++ asQueryParams(timespan) ++ asQueryParam(client.agencyId))
                )
            ).acceptingProto
              .withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CabinetCampaignCallsResponse] should be(expected)
              }
        }
      }
    }

    "campaignCallsDirectClient for offers billing-domain" should {
      "conclude with some valid response for a valid client" in inSequence {
        forAll(customerContextGen, campaignIdGen, timespanGen, campaignCallsPageGen) {
          (customerContext, campaignId, timespan, campaignCallsPage) =>
            val uid = customerContext.uid
            val billingDomain = BillingDomains.OffersBillingDomain
            val user = PassportUser(uid)
            val client = customerContext.customer.client
            val context = customerContext.copy(billingDomain = billingDomain)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(context)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaign/$campaignId/calls")
                .withQuery(
                  Uri.Query(oneItemPageQuery.toMap ++ asQueryParams(timespan) ++ asQueryParam(client.agencyId))
                )
            ).acceptingProto
              .withUser(user) ~>
              Route.seal(route) ~>
              check {
                status should be(StatusCodes.BadRequest)
              }
        }
      }
    }

    "campaignCallsAgencyClient for offers billing-domain" should {
      "conclude with BadRequest for a calls request valid but into wrong billing-domain (offers)" in inSequence {
        forAll(agencyContextGen, directClientGen, campaignIdGen, timespanGen) {
          (agencyContext, directClient, campaignId, timespan) =>
            val uid = agencyContext.uid
            val billingDomain = BillingDomains.OffersBillingDomain
            val user = PassportUser(uid)
            val client = directClient.copy(agencyId = Some(agencyContext.agency.id))
            val context = agencyContext.copy(billingDomain = billingDomain)

            resolverMock
              .expects(billingDomain, uid, client.agencyId, *)
              .anyNumberOfTimes()
              .returningF(context)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/user/${user.toPlain}/campaign/$campaignId/calls")
                .withQuery(
                  Uri.Query(oneItemPageQuery.toMap ++ asQueryParams(timespan) ++ asQueryParam(client.agencyId))
                )
            ).acceptingProto
              .withUser(user) ~>
              Route.seal(route) ~>
              check {
                status should be(StatusCodes.BadRequest)
              }
        }
      }
    }
  }

  private val singlePageSlicing: Slicing = {
    val slicingBuilder = Slicing
      .newBuilder()
      .setTotal(1)
      .setPage(ru.yandex.vertis.paging.Slice.Page.newBuilder().setSize(1).setNum(0).build())

    slicingBuilder.build()
  }

  private def asQueryParam(clientIdOption: Option[ClientId]): Map[String, String] = {
    clientIdOption.map(v => "client" -> v.toString).toMap
  }

  private def asQueryParams(timespan: billing.Timespan): Map[String, String] = {
    val since = timespan.from
    val till = timespan.to
    val printer = ISODateTimeFormat.dateTime()
    Map("since" -> printer.print(since)) ++ till.map(t => "till" -> printer.print(t)).toMap
  }

  private def asQueryParam[A](paramName: String, paramValue: A): Map[String, String] = {
    Map(paramName -> String.valueOf(paramValue))
  }

  private def asQueryParam[A](paramName: String, paramValueOption: Option[A]): Map[String, String] = {
    paramValueOption.map(value => paramName -> String.valueOf(value)).toMap
  }

}
