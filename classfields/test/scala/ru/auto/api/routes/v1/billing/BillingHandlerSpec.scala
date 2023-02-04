package ru.auto.api.routes.v1.billing

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Accept
import org.apache.http.client.utils.URIBuilder
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{ErrorCode, ErrorResponse}
import ru.auto.api.billing.BillingManager
import ru.auto.api.billing.v2.BillingModelV2.{InitPaymentResponse, PaymentStatusResponse, ProcessPaymentResponse, TiedCardsResponse}
import ru.auto.api.model.ModelGenerators.SessionResultGen
import ru.auto.api.model.gen.BankerModelGenerators.{TiedCardGenV2, TiedCardPatchGen}
import ru.auto.api.model.gen.BillingModelGenerators._
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain
import ru.auto.api.user.PaymentOuterClass.PaymentPage
import ru.auto.api.util.{Protobuf, Request}
import ru.yandex.vertis.banker.model.ApiModel

import scala.concurrent.Future

class BillingHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val billingManager: BillingManager = mock[BillingManager]

  val baseUrl = "/1.0/billing"

  "GET /{salesmanDomain}/payment" should {
    "correctly return payment status for domains" in {
      forAll(SessionResultGen, SalesmanDomainGen, TransactionIdGen, PaymentStatusResponseGen) {
        (session, domain, transactionId, paymentStatus) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(billingManager.getPaymentStatus(?, ?)(?)).thenReturnF(paymentStatus)
          val paymentURI = new URIBuilder(baseUrl + s"/$domain/payment")
          paymentURI.addParameter("ticket_id", transactionId.value)
          Get(paymentURI.toString) ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              val result = responseAs[String]
              withClue(result) {
                status shouldBe StatusCodes.OK
                val proto = Protobuf.fromJson[PaymentStatusResponse](result)
                proto shouldBe paymentStatus
              }
            }
      }
    }
  }

  "GET /{salesmanDomain}/tied-cards" should {
    "correctly return tied cards" in {
      forAll(SessionResultGen, SalesmanDomainGen, TiedCardsResponseGen) { (session, domain, tiedCardResponse) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(billingManager.getTiedCards(?, ?)(?)).thenReturnF(tiedCardResponse)
        val paymentURI = new URIBuilder(baseUrl + s"/$domain/tied-cards")
        Get(paymentURI.toString) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe StatusCodes.OK
              val proto = Protobuf.fromJson[TiedCardsResponse](result)
              proto shouldBe tiedCardResponse
            }
          }
      }
    }
  }

  "PUT /{salesmanDomain}/tied-cards" should {
    "correctly make card preferrable" in {
      forAll(SessionResultGen, SalesmanDomainGen, TiedCardPatchGen, TiedCardsResponseGen, TiedCardGenV2) {
        (session, domain, patch, response, card) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          stub(
            billingManager.updateCard(
              _: String,
              _: ApiModel.PaymentSystemId,
              _: ApiModel.PaymentMethod.Patch,
              _: SalesmanDomain
            )(_: Request)
          ) {
            case (_, _, `patch`, _, _) =>
              Future.successful(response)
          }
          val paymentURI = new URIBuilder(baseUrl + s"/$domain/tied-cards")
          paymentURI.addParameter("payment_system_id", card.getPsId.toString)
          paymentURI.addParameter("card_id", card.getId)
          Put(paymentURI.toString, patch) ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              val result = responseAs[String]
              withClue(result) {
                status shouldBe StatusCodes.OK
                val proto = Protobuf.fromJson[TiedCardsResponse](result)
                proto shouldBe response
              }
            }
      }
    }
  }

  "DELETE /{salesmanDomain}/tied-cards" should {
    "correctly return tied cards" in {
      forAll(SessionResultGen, SalesmanDomainGen, TiedCardGenV2, TiedCardsResponseGen) {
        (session, domain, cardToRemove, tiedCardResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(billingManager.removeCard(?, ?, ?)(?)).thenReturnF(tiedCardResponse)
          val paymentURI = new URIBuilder(baseUrl + s"/$domain/tied-cards")
          paymentURI.addParameter("card_id", cardToRemove.getId)
          paymentURI.addParameter("payment_system_id", cardToRemove.getPsId.toString)
          Delete(paymentURI.toString) ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              val result = responseAs[String]
              withClue(result) {
                status shouldBe StatusCodes.OK
                val proto = Protobuf.fromJson[TiedCardsResponse](result)
                proto shouldBe tiedCardResponse
              }
            }
      }
    }
  }

  "POST /{salesmanDomain}/payment/init" should {
    "correctly process payment initiation" in {
      forAll(SessionResultGen, SalesmanDomainGen, InitPaymentRandomRequestGen, InitPaymentResponseGen) {
        (session, domain, initPaymentRequest, initPaymentResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(billingManager.initPayment(?, ?)(?)).thenReturnF(initPaymentResponse)
          val paymentURI = new URIBuilder(baseUrl + s"/$domain/payment/init")
          Post(paymentURI.toString, initPaymentRequest) ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              val result = responseAs[String]
              withClue(result) {
                status shouldBe StatusCodes.OK
                val proto = Protobuf.fromJson[InitPaymentResponse](result)
                proto shouldBe initPaymentResponse
              }
            }
      }
    }
  }

  "POST /{salesmanDomain}/payment/process" should {
    "correctly process payment" in {
      forAll(SessionResultGen, SalesmanDomainGen, ProcessPaymentRequestGen, ProcessPaymentResponseGen) {
        (session, domain, processPaymentRequest, processPaymentResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(billingManager.processPayment(?, ?)(?)).thenReturnF(processPaymentResponse)
          val paymentURI = new URIBuilder(baseUrl + s"/$domain/payment/process")
          Post(paymentURI.toString, processPaymentRequest) ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              val result = responseAs[String]
              withClue(result) {
                status shouldBe StatusCodes.OK
                val proto = Protobuf.fromJson[ProcessPaymentResponse](result)
                proto shouldBe processPaymentResponse
              }
            }
      }
    }
  }

  "DELETE /schedules/qualified_trade/talkative_asylum/all_sale_fresh" should {
    "should return correct error" in {
      forAll(SessionResultGen) { session =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        val schedulesURI = new URIBuilder(baseUrl + "/schedules/qualified_trade/talkative_asylum/all_sale_fresh")
        Delete(schedulesURI.toString) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[ErrorResponse]
            withClue(result) {
              status shouldBe StatusCodes.BadRequest
              result.getError shouldBe ErrorCode.BAD_REQUEST
              result.getDetailedError should include(
                "Unknown category selector: [qualified_trade]. Known values: cars, moto, trucks, all"
              )
            }
          }
      }
    }
  }

  "DELETE /schedules/CARS/appropriate_commission/all_sale_fresh" should {
    "should return correct error" in {
      forAll(SessionResultGen) { session =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        val schedulesURI = new URIBuilder(baseUrl + "/schedules/CARS/appropriate_commission/all_sale_fresh")
        Delete(schedulesURI.toString) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[ErrorResponse]
            withClue(result) {
              status shouldBe StatusCodes.BadRequest
              result.getError shouldBe ErrorCode.BAD_REQUEST
              result.getDetailedError should include("Incorrect offer id: [appropriate_commission]")
            }
          }
      }
    }
  }

  "PUT /schedules/CARS/balanced_embarrassment/all_sale_fresh" should {
    "should return correct error" in {
      forAll(SessionResultGen) { session =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        val schedulesURI = new URIBuilder(baseUrl + "/schedules/CARS/balanced_embarrassment/all_sale_fresh")
        Put(schedulesURI.toString) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[ErrorResponse]
            withClue(result) {
              status shouldBe StatusCodes.BadRequest
              result.getError shouldBe ErrorCode.BAD_REQUEST
              result.getDetailedError should include("Incorrect offer id: [balanced_embarrassment]")
            }
          }
      }
    }
  }

  "PUT /schedules/early_hemisphere/1086637584-513028fd/all_sale_fresh" should {
    "should return correct error" in {
      forAll(SessionResultGen) { session =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        val schedulesURI = new URIBuilder(baseUrl + "/schedules/early_hemisphere/1086637584-513028fd/all_sale_fresh")
        Put(schedulesURI.toString) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[ErrorResponse]
            withClue(result) {
              status shouldBe StatusCodes.BadRequest
              result.getError shouldBe ErrorCode.BAD_REQUEST
              result.getDetailedError should include(
                "Unknown category selector: [early_hemisphere]. Known values: cars, moto, trucks, all"
              )
            }
          }
      }
    }
  }

  "GET /{salesmanDomain}/payment/history" should {
    "correctly return payments for domains" in {
      val pageSize = 10
      val pageNum = 1
      forAll(SessionResultGen, SalesmanDomainGen, paymentPageGen(pageSize, pageNum, 3)) { (session, domain, expected) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(billingManager.getPaymentsHistory(?, ?)(?)).thenReturnF(expected)
        val uri = new URIBuilder(baseUrl + s"/$domain/payment/history")
        uri.addParameter("page", pageNum.toString)
        uri.addParameter("page_size", pageSize.toString)
        Get(uri.toString) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe StatusCodes.OK
              val proto = Protobuf.fromJson[PaymentPage](result)
              proto shouldBe expected
            }
          }
      }
    }
  }

}
