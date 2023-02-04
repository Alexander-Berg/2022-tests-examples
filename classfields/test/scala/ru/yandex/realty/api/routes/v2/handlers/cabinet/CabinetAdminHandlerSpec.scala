package ru.yandex.realty.api.routes.v2.handlers.cabinet

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.Slicing
import ru.yandex.realty.api.ProtoResponse._
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.routes.v2.cabinet.CabinetAdminHandlerImpl
import ru.yandex.realty.api.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.realty.clients.billing.BillingClient.ClientId
import ru.yandex.realty.clients.billing.gen.{BillingDomainGenerators, BillingGenerators}
import ru.yandex.realty.clients.billing.{BillingRequestContext, BillingRequestContextResolver, Client => BillingClient}
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.cabinet.CabinetConverters._
import ru.yandex.realty.managers.cabinet.CabinetManager
import ru.yandex.realty.model.billing.{BillingDomain, ClientQueryType}
import ru.yandex.realty.model.exception.ForbiddenException
import ru.yandex.realty.model.user.{PassportUser, UserRefGenerators}
import ru.yandex.realty.model.util.{Page, Slice}
import ru.yandex.realty.proto.billing._
import ru.yandex.realty.request.Request
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

import scala.languageFeature.postfixOps

@RunWith(classOf[JUnitRunner])
class CabinetAdminHandlerSpec
  extends HandlerSpecBase
  with PropertyChecks
  with UserRefGenerators
  with BillingDomainGenerators
  with BillingGenerators {

  override def routeUnderTest: Route = new CabinetAdminHandlerImpl(manager, resolver).route

  override protected val exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler
  override protected val rejectionHandler: RejectionHandler = ApiRejectionHandler.handler

  private val manager: CabinetManager = mock[CabinetManager]
  private val resolver: BillingRequestContextResolver = mock[BillingRequestContextResolver]
  private val forbidden = new ForbiddenException(None.orNull)
  private val oneItemPageQuery = Uri.Query("page" -> "0", "pageSize" -> "1")
  private val oneItemFirstPage = Page(0, 1)

  private val resolverMock = toMockFunction4(
    resolver.resolve(_: BillingDomain, _: Long, _: Option[ClientId])(_: Traced)
  )

  private val mockGetClientsList =
    toMockFunction5(manager.getClientsList(_: Long, _: BillingDomain, _: ClientQueryType, _: Slice)(_: Request))

  "cabinetHandler" when {
    "getClientsList" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in {
        forAll(agencyContextGen, clientQueryTypeGen) {
          (billingRequestContext: BillingRequestContext, queryType: ClientQueryType) =>
            inSequence {
              val uid = billingRequestContext.uid
              val billingDomain = billingRequestContext.billingDomain
              val user = PassportUser(uid)

              resolverMock
                .expects(billingDomain, uid, None, *)
                .anyNumberOfTimes()
                .returningF(billingRequestContext)

              mockGetClientsList
                .expects(uid, billingRequestContext.billingDomain, queryType, oneItemFirstPage, *)
                .throwingF(forbidden)

              Get(
                Uri(s"/cabinet/${billingDomain.toString}/clients")
                  .withQuery(oneItemPageQuery.+:("queryType" -> queryType.name))
              ).withUser(user) ~>
                route ~>
                check {
                  status should be(StatusCodes.Forbidden)
                }
            }
        }
      }

      "conclude with Bad Request for bad queryType" in {
        forAll(agencyContextGen) { billingRequestContext: BillingRequestContext =>
          inSequence {
            val uid = billingRequestContext.uid
            val billingDomain = billingRequestContext.billingDomain
            val user = PassportUser(uid)

            resolverMock
              .expects(billingDomain, uid, None, *)
              .anyNumberOfTimes()
              .returningF(billingRequestContext)

            Get(
              Uri(s"/cabinet/${billingDomain.toString}/clients")
                .withQuery(oneItemPageQuery.+:("queryType" -> "WRONG"))
            ).withUser(user) ~>
              route ~>
              check {
                status should be(StatusCodes.BadRequest)
              }
          }
        }
      }

      "conclude with some valid response for a valid queryType" in {
        forAll(agencyContextGen, clientQueryTypeGen, anyClientGen) {
          (billingRequestContext: BillingRequestContext, queryType: ClientQueryType, client: BillingClient) =>
            inSequence {
              val uid = billingRequestContext.uid
              val billingDomain = billingRequestContext.billingDomain
              val user = PassportUser(uid)
              val validResult: CabinetClientsResponse = {

                val clientBuilder = Client.newBuilder()
                clientBuilder.setId(client.id)
                clientBuilder.setProperties(clientToClientProperties(client))

                val clientsListBuilder = ClientsList
                  .newBuilder()
                  .addClientsInfo(ClientInfo.newBuilder().setClient(clientBuilder.build()).build())
                  .setSlicing(singlePageSlicing)

                CabinetClientsResponse
                  .newBuilder()
                  .setResponse(clientsListBuilder.build())
                  .build()
              }

              resolverMock
                .expects(billingDomain, uid, None, *)
                .anyNumberOfTimes()
                .returningF(billingRequestContext)

              mockGetClientsList
                .expects(uid, billingRequestContext.billingDomain, queryType, oneItemFirstPage, *)
                .returningF(validResult)

              Get(
                Uri(s"/cabinet/${billingDomain.toString}/clients")
                  .withQuery(oneItemPageQuery.+:("queryType" -> queryType.name))
              ).withUser(user) ~>
                route ~>
                check {
                  status should be(StatusCodes.OK)
                  entityAs[CabinetClientsResponse] should be(validResult)
                }
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

}
