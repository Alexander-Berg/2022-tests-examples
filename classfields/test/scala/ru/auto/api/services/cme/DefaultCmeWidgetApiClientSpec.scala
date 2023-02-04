package ru.auto.api.services.cme

import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import ru.auto.api.exceptions.{AuthenticationException, BadRequestDetailedException, OfferNotFoundException}
import ru.auto.api.model.OfferID
import ru.auto.api.services.cabinet.BalanceTestData.dealerRequest

class DefaultCmeWidgetApiClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with OptionValues {

  private val authToken = "authorization"
  private val client = new DefaultCmeWidgetApiClient(http, authToken)

  "DefaultCmeWidgetApiClient.getAppraisalWidgetTicket()" should {
    "return ticket response" in {
      http.expectUrl(POST, "/appraisal/initializations")
      http.respondWithJsonFrom("/cme/appraisal_initializations_response.json")

      http.expectHeader("Authorization", authToken)

      http.expectJson {
        """{"client":"autoru","request":{"offerID":"111-fff","price":2500000}}"""
      }

      val offerId = OfferID(111, Some("fff"))
      val price = 2500000

      val result = client.getAppraisalWidgetTicket(offerId, price)(dealerRequest).futureValue
      result shouldBe "test-ticket"
    }

    "throw 400 error" in {
      http.expectUrl(POST, "/appraisal/initializations")
      http.respondWithJsonFrom(BadRequest, "/cme/appraisal_initializations_badrequest_response.json")

      val offerId = OfferID(111, Some("fff"))
      val price = 2500000

      intercept[BadRequestDetailedException] {
        client.getAppraisalWidgetTicket(offerId, price)(dealerRequest).await
      }
    }

    "throw 401 error" in {
      http.expectUrl(POST, "/appraisal/initializations")
      http.respondWithStatus(Unauthorized)

      val offerId = OfferID(111, Some("fff"))
      val price = 2500000

      intercept[AuthenticationException] {
        client.getAppraisalWidgetTicket(offerId, price)(dealerRequest).await
      }
    }

    "throw 404 error" in {
      http.expectUrl(POST, "/appraisal/initializations")
      http.respondWithStatus(NotFound)

      val offerId = OfferID(111, Some("fff"))
      val price = 2500000

      intercept[OfferNotFoundException] {
        client.getAppraisalWidgetTicket(offerId, price)(dealerRequest).await
      }
    }
  }

}
