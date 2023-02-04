package ru.yandex.realty.clients.billing

import akka.http.scaladsl.model.HttpMethods
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, Instant}
import org.junit.runner.RunWith
import org.scalacheck.ShrinkLowPriority
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.billing.gen.BillingGenerators
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.model.offer.OfferCampaignType
import ru.yandex.vertis.generators.BasicGenerators

@RunWith(classOf[JUnitRunner])
class BillingClientImplSpec
  extends AsyncSpecBase
  with BillingGenerators
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with BasicGenerators
  with ShrinkLowPriority {

  private val client = new BillingClientImpl(httpService, "default")

  "BillingClientImpl" when {
    "get order transactions outcome" should {
      val orderId = 1761

      def coverGetOrderTransactionsOutcomeWith(
        fullWithdrawDetails: Boolean = false
      )(test: CustomerContext => Unit): Unit = {
        val context = customerContextGen.next
        val clientId = context.customer.client.id
        httpClient.expect(
          HttpMethods.GET,
          s"/api/1.x/service/default/customer/client/$clientId/order/$orderId/outcome" +
            s"?withdrawWithDetails=$fullWithdrawDetails&from=2018-12-31T21:00:00.000Z"
        )

        test(context)
      }

      "succeed with full details on withdrawal" in {
        coverGetOrderTransactionsOutcomeWith(fullWithdrawDetails = true) { context =>
          httpClient.respondWithJsonFrom("/billing/order_1761_outcome_fullWithdrawDetails.json")
          val from = DateTime.parse("2019-01-01")
          val response = client
            .getOrderTransactionsOutcome(
              context.customer.client.id,
              orderId,
              None,
              context.uid.toString,
              from,
              withdrawWithDetails = true
            )
            .futureValue
          response match {
            case OrderTransactionsOutcome(rebate, correction, income, withdraw, overdraft, withdrawDetails) =>
              withdrawDetails.exists(_.nonEmpty) shouldBe true
              rebate shouldBe 0
              income shouldBe 2770000
              withdraw shouldBe 2770000
              overdraft shouldBe 3895000
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }

      "succeed for basic case" in {
        coverGetOrderTransactionsOutcomeWith() { context =>
          httpClient.respondWithJsonFrom("/billing/order_1761_outcome_noWithdrawDetails.json")
          val from = DateTime.parse("2019-01-01")
          val response = client
            .getOrderTransactionsOutcome(
              context.customer.client.id,
              orderId,
              None,
              context.uid.toString,
              from
            )
            .futureValue
          response match {
            case OrderTransactionsOutcome(rebate, correction, income, withdraw, overdraft, withdrawDetails) =>
              withdrawDetails.forall(_.isEmpty) shouldBe true
              rebate shouldBe 0
              income shouldBe 0
              withdraw shouldBe 0
              overdraft shouldBe 350000
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }
    }

    "get campaigns" should {
      def coverGetCampaignsWith(test: CustomerContext => Unit): Unit = {
        val context = customerContextGen.next
        val clientId = context.customer.client.id
        httpClient.expect(HttpMethods.GET, s"/api/1.x/service/default/customer/client/$clientId/campaign")

        test(context)
      }

      "succeed with PLACEMENT on placement product" in {
        coverGetCampaignsWith { context =>
          httpClient.respondWithJsonFrom("/billing/placement_product.json")
          val response = client.getCampaigns(context.uid.toString, context.customer.client.id).futureValue
          response match {
            case CampaignsPageResponse(_, _, values) =>
              values.exists(_.`type` == OfferCampaignType.PLACEMENT) shouldBe true
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }

      "succeed with PLACEMENT on custom product with placement as its id" in {
        coverGetCampaignsWith { context =>
          httpClient.respondWithJsonFrom("/billing/custom_product_placement_id.json")
          val response = client.getCampaigns(context.uid.toString, context.customer.client.id).futureValue
          response match {
            case CampaignsPageResponse(_, _, values) =>
              values.exists(_.`type` == OfferCampaignType.PLACEMENT) shouldBe true
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }
    }

    "get campaign calls" should {
      def coverGetCampaignCallsWith(test: CustomerContext => Unit): Unit = {
        val context = customerContextGen.next
        val clientId = context.customer.client.id
        httpClient.expect(
          HttpMethods.GET,
          s"/api/1.x/service/default/customer/client/$clientId/campaign/wellKnown/calls?from=2007-09-30T13:03:26.245Z"
        )

        test(context)
      }

      "succeed for 79d0a6ea-4aec-4786-a464-3994edf33dd5 campaign" in {
        coverGetCampaignCallsWith { context =>
          httpClient.respondWithJsonFrom("/billing/campaign_calls_79d0a6ea4aec4786a4643994edf33dd5.json")
          val response = client
            .getCampaignCalls(
              context.uid.toString,
              context.customer.client.id,
              None,
              "wellKnown",
              Instant.parse("2007-09-30T13:03:26.245Z")
            )
            .futureValue

          response match {
            case CampaignCallsPageResponse(_, _, values) =>
              values.exists(_.call.incoming.contains("+79998898367")) shouldBe true
              values.exists {
                _.call.timestamp.toString(ISODateTimeFormat.dateTime()) == "2018-10-01T15:36:48.000+03:00"
              } shouldBe true
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }

      "(manual:Fail coverage) succeed for de346ac1-19ee-45f3-83a3-cd28038b6d63 campaign" in {
        coverGetCampaignCallsWith { context =>
          httpClient.respondWithJsonFrom("/billing/campaign_calls_de346ac119ee45f383a3cd28038b6d63.json")
          val response = client
            .getCampaignCalls(
              context.uid.toString,
              context.customer.client.id,
              None,
              "wellKnown",
              Instant.parse("2007-09-30T13:03:26.245Z")
            )
            .futureValue

          response match {
            case CampaignCallsPageResponse(_, _, values) =>
              values.exists(_.manual.contains(CallResolutionStatus.Fail)) shouldBe true
              values.exists(_.call.incoming.contains("+74997533408")) shouldBe true
              values.exists {
                _.call.timestamp.toString(ISODateTimeFormat.dateTime()) == "2018-07-02T11:47:03.000+03:00"
              } shouldBe true
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }

      "(manual:Pass coverage) succeed for aad782df-2bf9-4230-b2c9-63129232c12b campaign" in {
        coverGetCampaignCallsWith { context =>
          httpClient.respondWithJsonFrom("/billing/campaign_calls_aad782df2bf94230b2c963129232c12b.json")
          val response = client
            .getCampaignCalls(
              context.uid.toString,
              context.customer.client.id,
              None,
              "wellKnown",
              Instant.parse("2007-09-30T13:03:26.245Z")
            )
            .futureValue

          response match {
            case CampaignCallsPageResponse(_, _, values) =>
              values.exists(_.manual.contains(CallResolutionStatus.Pass)) shouldBe true
              values.exists(_.call.incoming.contains("+78127015076")) shouldBe true
              values.exists {
                _.call.timestamp.toString(ISODateTimeFormat.dateTime()) == "2018-12-22T13:18:20.000+03:00"
              } shouldBe true
            case _ =>
              fail(s"Unexpected response [$response]")
          }
        }
      }
    }
  }
}
