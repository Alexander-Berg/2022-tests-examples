package ru.auto.api.services.billing

import java.time.LocalDate
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes.{Forbidden, NotFound, OK}
import ru.auto.api.calltracking.CalltrackingRequestModel.CallComplaintRequest
import ru.auto.api.exceptions.CustomerAccessForbidden
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.model.billing.BalanceId
import ru.auto.api.model.billing.vsbilling.{Balance2, Order, OverdraftResponse}
import ru.auto.api.services.billing.VsBillingTestUtils._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Resources
import ru.yandex.vertis.billing.model.proto.{Individual, RequisitesIdResponse, RequisitesProperties}

class DefaultVsBillingClientSpec extends HttpClientSpec with MockedHttpClient with TestRequestWithId {

  val client = new DefaultVsBillingClient(http)

  "DefaultVsBilling.getOrders()" should {

    "get dealer orders" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/customer/client/8168241/order?productKey=default")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/order_client_response.json")
      val res = client.getOrders(dealerBalanceId, agencyBalanceId = None).futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values shouldBe List(Order(dealerOrderId, Balance2(9095334909L)))
    }

    "get agency orders" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/customer/agency/7320375/client/32348772/order?productKey=default")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/order_agency_response.json")
      val res = client.getOrders(agencyDealerBalanceId, Some(agencyBalanceId)).futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values shouldBe List(Order(agencyOrderId, Balance2(9100000)))
    }

    "throw customer access forbidden" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/customer/client/8168241/order?productKey=default")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWith(Forbidden, "Forbidden")
      val res = client.getOrders(dealerBalanceId, agencyBalanceId = None).failed.futureValue
      res shouldBe a[CustomerAccessForbidden]
    }
  }

  "DefaultVsBilling.getOrderTransactions()" should {

    "get short dealer order transactions" in {
      http.expectUrl(
        GET,
        "/api/1.x/service/autoru/customer/client/8168241/order/30829/transactions" +
          "?from=2018-06-01&to=2018-06-19&transactionType=Incoming&nonNegative=true"
      )
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/order_transactions_response_client_short.json")
      val res = client.getOrderTransactions(dealerBalanceId, agencyBalanceId = None, dealerOrderId, params).futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values shouldBe dealerOrderTransactions
    }

    "get short dealer order transactions with alternative params" in {
      http.expectUrl(
        GET,
        "/api/1.x/service/autoru/customer/client/8168241/order/30829/transactions" +
          "?from=2017-05-06&to=2019-08-29&transactionType=Incoming&nonNegative=false"
      )
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/order_transactions_response_client_short.json")
      val res =
        client
          .getOrderTransactions(dealerBalanceId, agencyBalanceId = None, dealerOrderId, alternativeParams)
          .futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values shouldBe dealerOrderTransactions
    }

    "get short agency order transactions" in {
      http.expectUrl(
        GET,
        "/api/1.x/service/autoru/customer/agency/7320375/client/32348772/order/71286/transactions" +
          "?from=2018-06-01&to=2018-06-19&transactionType=Incoming&nonNegative=true"
      )
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/order_transactions_response_agency_short.json")
      val res =
        client.getOrderTransactions(agencyDealerBalanceId, Some(agencyBalanceId), agencyOrderId, params).futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values shouldBe agencyOrderTransactions
    }

    "get full dealer order transactions" in {
      http.expectUrl(
        GET,
        "/api/1.x/service/autoru/customer/client/36273879/order/82132/transactions" +
          "?from=2018-06-01&to=2018-06-19&pageNum=15&pageSize=3&transactionType=Withdraw&nonNegative=true"
      )
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/order_transactions_response_client_full.json")
      val res = client.getOrderTransactions(fullBalanceId, agencyBalanceId = None, fullOrderId, fullParams).futureValue
      res.total shouldBe fullTotal
      val page = res.page
      page.size shouldBe fullPageSize
      page.number shouldBe fullPageNumber
      res.values shouldBe fullOrderTransactions
    }

    "throw customer access forbidden" in {
      http.expectUrl(
        GET,
        "/api/1.x/service/autoru/customer/client/8168241/order/30829/transactions" +
          "?from=2018-06-01&to=2018-06-19&transactionType=Incoming&nonNegative=true"
      )
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWith(Forbidden, "Forbidden")
      val res =
        client.getOrderTransactions(dealerBalanceId, agencyBalanceId = None, dealerOrderId, params).failed.futureValue
      res shouldBe a[CustomerAccessForbidden]
    }
  }

  "DefaultVsBilling.getOverdraft()" should {

    "get dealer overdraft" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/client/notifyClient/8168241")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWithJsonFrom("/billing/get_overdraft_response.json")
      val res = client.getOverdraft(dealerBalanceId).futureValue
      res shouldBe Some {
        OverdraftResponse(
          BalanceId(8168241),
          21418000L,
          100L,
          Some(LocalDate.parse("2018-11-30")),
          overdraftBan = false
        )
      }
    }

    "get dealer overdraft with empty response" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/client/notifyClient/7777777")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWith(NotFound, "no content")
      val res = client.getOverdraft(BalanceId(7777777)).futureValue
      res shouldBe None
    }

    "throw customer access forbidden" in {
      http.expectUrl(GET, "/api/1.x/service/autoru/client/notifyClient/8168241")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.respondWith(Forbidden, "Forbidden")
      val res = client.getOverdraft(dealerBalanceId).failed.futureValue
      res shouldBe a[CustomerAccessForbidden]
    }
  }

  "DefaultVsBilling.callComplaint()" should {
    val complaintRequest = CallComplaintRequest
      .newBuilder()
      .setText("test")
      .setEmail("test_email")
      .build

    "build valid url for client with agency" in {

      http.expectUrl(
        POST,
        "/api/1.x/service/autoru/customer/agency/2/client/1/campaign/testCampaign/complain/call/testId"
      )
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectJson("""{"text":"test","email":"test_email"}""".stripMargin)
      http.respondWith(OK, "saved")
      client
        .makeCallComplaint(
          "autoru",
          "testId",
          "testCampaign",
          BalanceId(1),
          complaintRequest,
          "test-uid",
          Option(BalanceId(2))
        )
        .futureValue
    }

    "build valid url for client" in {

      http.expectUrl(POST, "/api/1.x/service/autoru/customer/client/1/campaign/testCampaign/complain/call/testId")
      http.expectHeader("X-Billing-User", "autoru:salesman")
      http.expectJson("""{"text":"test","email":"test_email"}""".stripMargin)
      http.respondWith(OK, "saved")
      client
        .makeCallComplaint(
          "autoru",
          "testId",
          "testCampaign",
          BalanceId(1),
          complaintRequest,
          "test-uid",
          None
        )
        .futureValue
    }
  }

  "DefaultVsBilling.createRequisites()" should {
    "send correct request" in {
      http.expectUrl(POST, "/api/1.x/service/autoru/requisites/client/777/requisites")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.expectHeader("X-Yandex-Operator-Uid", robotOperatorUid.toString)

      val expectedBody = Resources.toProto[RequisitesProperties]("/billing/requisites_individual_request.json")
      http.expectProto(expectedBody)

      http.respondWithProtoFrom[RequisitesIdResponse]("/billing/requisites_id_response.json")

      val requisites = RequisitesProperties
        .newBuilder()
        .setIndividual {
          Individual
            .newBuilder()
            .setFirstName("FirstName")
            .setMidName("MiddleName")
            .setLastName("LastName")
            .setPhone("70000000000")
            .setEmail("noreply@yandex-team.ru")
        }
        .build()

      val response = client.createRequisites(clientId = 777L, requisites).futureValue
      response.getId shouldBe 1234567
    }
  }

  "DefaultVsBillingClient.updateRequisites()" should {
    "update successfully" in {
      http.expectUrl(PUT, "/api/1.x/service/autoru/requisites/client/777/requisites/123")
      http.expectHeader("X-Yandex-Request-ID", testRequestId)
      http.expectHeader("X-Yandex-Operator-Uid", robotOperatorUid.toString)

      val expectedBody = Resources.toProto[RequisitesProperties]("/billing/requisites_individual_request.json")
      http.expectProto(expectedBody)

      http.respondWithProtoFrom[RequisitesIdResponse]("/billing/requisites_id_response.json")

      val requisites = RequisitesProperties
        .newBuilder()
        .setIndividual {
          Individual
            .newBuilder()
            .setFirstName("FirstName")
            .setMidName("MiddleName")
            .setLastName("LastName")
            .setPhone("70000000000")
            .setEmail("noreply@yandex-team.ru")
        }
        .build()

      val response = client.updateRequisites(clientId = 777L, 123, requisites).futureValue
      response.getId shouldBe 1234567
    }
  }
}
