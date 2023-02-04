package ru.auto.salesman.client.billing

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import ru.auto.salesman.client.billing.HttpBillingCampaignClientSpec._
import ru.auto.salesman.client.billing.model.{
  Page,
  SimpleDealerCampaign,
  SimpleDealerCampaignsResponse,
  SimpleOrder
}
import ru.auto.salesman.client.exceptions.CustomerNotFoundException
import ru.auto.salesman.test.TestAkkaComponents._
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

import scala.concurrent.Future

class HttpBillingCampaignClientSpec extends BaseSpec with IntegrationPropertyCheckConfig {

  private val httpBillingCampaignClient: HttpBillingCampaignClient =
    new HttpBillingCampaignClient("billing") {

      var firstRequest = true

      override def get(uri: Uri, headers: Seq[HttpHeader])(
          implicit system: ActorSystem
      ): Future[HttpResponse] =
        if (
          uri.path
            .toString()
            .equals("billing/api/1.x/service/autoru/customer/client/2/campaign")
        )
          Future.successful(customerNotFoundResponse)
        else if (firstRequest) {
          uri.path
            .toString() shouldBe "billing/api/1.x/service/autoru/customer/client/1/campaign"
          uri.rawQueryString.get shouldBe "pageSize=100&pageNum=0"

          firstRequest = false
          Future.successful(
            HttpResponse(
              entity = Marshal(responses.head).to[ResponseEntity].futureValue
            )
          )
        } else {
          uri.path
            .toString() shouldBe "billing/api/1.x/service/autoru/customer/client/1/campaign"
          uri.rawQueryString.get shouldBe "pageSize=100&pageNum=1"
          firstRequest = false
          Future.successful(
            HttpResponse(
              entity = Marshal(responses(1)).to[ResponseEntity].futureValue
            )
          )
        }
    }

  "HttpBillingCampaignClient" should {
    "getClientCampaigns() get campaigns from all pages" in {
      val campaigns = httpBillingCampaignClient
        .getClientCampaigns(balanceClientId, balanceAgencyId)
        .success
        .value

      campaigns shouldEqual responses.flatMap(_.values)
    }

    "fail getClientCampaigns() with ClientNotFoundException when 404 and code = CUSTOMER_NOT_FOUND" in {
      httpBillingCampaignClient
        .getClientCampaigns(2, balanceAgencyId)
        .failure
        .exception shouldBe an[CustomerNotFoundException]
    }
  }
}

object HttpBillingCampaignClientSpec {
  private val balanceClientId = 1
  private val balanceAgencyId = None

  private val responses = List(
    SimpleDealerCampaignsResponse(
      total = 101,
      Page(size = 100, number = 0),
      List(SimpleDealerCampaign("1", SimpleOrder(1)))
    ),
    SimpleDealerCampaignsResponse(
      total = 101,
      Page(size = 100, number = 1),
      List(SimpleDealerCampaign("2", SimpleOrder(2)))
    )
  )

  private val customerNotFoundResponse = HttpResponse(
    status = StatusCodes.NotFound,
    entity = HttpEntity(
      """{"code":"CUSTOMER_NOT_FOUND","message":"Customer [CustomerId(2,None)] not found"}"""
    )
  )
}
