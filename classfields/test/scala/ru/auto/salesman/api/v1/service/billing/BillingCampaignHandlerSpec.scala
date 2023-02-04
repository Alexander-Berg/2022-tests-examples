package ru.auto.salesman.api.v1.service.billing

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{StatusCodes, Uri}
import auto.indexing.CampaignByClientOuterClass.CampaignHeaderList
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.v1.service.billing_campaign.BillingCampaignHandler
import ru.auto.salesman.exceptions.ClientNotFoundException
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.service.billingcampaign.BillingCampaignService.CallInfo
import ru.auto.salesman.service.billingcampaign.{
  CallCampaignNotFoundException,
  PaidCallDisabledException,
  ProductCampaignNotFoundException
}
import ru.yandex.vertis.billing.Model.CampaignHeader
import ru.yandex.vertis.protobuf.ProtobufUtils
import spray.json.{JsNumber, JsObject}

import scala.collection.JavaConverters._

class BillingCampaignHandlerSpec extends RoutingSpec {

  import BillingTestData._

  private val route = new BillingCampaignHandler(
    BillingTestData.billingCampaignService
  ).route

  "GET /call/client/{clientId}" should {
    "get campaign" in {
      (billingCampaignService.getCallCampaign _)
        .expects(*)
        .returningZ(campaignHeader)

      val uri = Uri(s"/call/client/$ClientId")

      Get(uri.toString())
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 404 with json body when can't resolve client" in {
      (billingCampaignService.getCallCampaign _)
        .expects(*)
        .throwingZ(ClientNotFoundException(1L))
      Get("/call/client/1").withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.NotFound
          responseAs[JsObject]
        }
    }
  }

  "GET /call:cars:used/client/{clientId}" should {
    val Product = ProductId.withName("call:cars:used")

    "get campaign" in {
      (billingCampaignService
        .getProductCampaign(_: Long, _: ProductId))
        .expects(ClientId, Product)
        .returningZ(campaignHeader)

      Get(s"/call:cars:used/client/$ClientId")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 404 with json body when can't resolve campaign" in {
      (billingCampaignService
        .getProductCampaign(_: Long, _: ProductId))
        .expects(ClientId, Product)
        .throwingZ(ClientNotFoundException(ClientId))

      Get(s"/call:cars:used/client/$ClientId").withHeaders(
        RequestIdentityHeaders
      ) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.NotFound
          responseAs[JsObject]
        }
    }

    "return 404 with json body when can't resolve client" in {
      (billingCampaignService
        .getProductCampaign(_: Long, _: ProductId))
        .expects(ClientId, Product)
        .throwingZ(ProductCampaignNotFoundException(Product, ClientId))

      Get(s"/call:cars:used/client/$ClientId").withHeaders(
        RequestIdentityHeaders
      ) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.NotFound
          responseAs[JsObject]
        }
    }
  }

  "GET /match-applications/client/{clientId}" should {
    "get campaigns" in {
      val response = CampaignHeaderList
        .newBuilder()
        .addAllCampaignHeaders(List(campaignHeader, campaignHeader).asJava)
        .build()

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(*)
        .returningZ(Some(response))

      val uri = Uri(s"/match-applications/client/$ClientId")

      Get(uri.toString())
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[CampaignHeaderList] shouldBe response
      }
    }

    "return 404 if campaign is missing" in {
      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(*)
        .returningZ(None)

      val uri = Uri(s"/match-applications/client/$ClientId")

      Get(uri.toString())
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[JsObject]
      }
    }
  }

  "PUT /call/client/{clientId}" should {

    "update campaign" in {

      (billingCampaignService.updateCallCarsNewCampaign _)
        .expects(*)
        .returningZ(campaignHeader)

      val uri = Uri(s"/call/client/$ClientId").withQuery(
        Query(
          "dayLimit" -> "1000000",
          "weekLimit" -> "5000000",
          "depositFixed" -> "150000",
          "enabled" -> "true",
          "costPerCall" -> "600"
        )
      )

      Put(uri.toString())
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val response = ProtobufUtils.fromJson(
          CampaignHeader.getDefaultInstance,
          responseAs[String]
        )
        response shouldBe campaignHeader
      }
    }

    "return 404 if campaign is missing" in {
      (billingCampaignService.updateCallCarsNewCampaign _)
        .expects(*)
        .throwingZ(CallCampaignNotFoundException(1L))
      val uri = Uri(s"/call/client/$ClientId").withQuery(
        Query(
          "dayLimit" -> "1000000",
          "weekLimit" -> "5000000",
          "depositCoeff" -> "5000000",
          "enabled" -> "true",
          "costPerCall" -> "600",
          "createNew" -> "false"
        )
      )
      Put(uri.toString())
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "GET /call/client/{clientId}/info" should {
    "return 200" in {
      (billingCampaignService.getCallInfo _)
        .expects(*)
        .returningZ(CallInfo(30000, 3))
      Get("/call/client/1/info").withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~>
        check {
          status shouldBe StatusCodes.OK
          val result = responseAs[JsObject].fields
          result shouldBe Map(
            "callCost" -> JsNumber("30000"),
            "depositCoefficient" -> JsNumber(3)
          )
        }
    }

    "return 404 when client not found" in {
      (billingCampaignService.getCallInfo _)
        .expects(*)
        .throwingZ(ClientNotFoundException(1L))
      Get("/call/client/1/info").withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~>
        check {
          status shouldBe StatusCodes.NotFound
        }
    }

    "return 403 when calls aren't available for client" in {
      (billingCampaignService.getCallInfo _)
        .expects(*)
        .throwingZ(PaidCallDisabledException(1L))
      Get("/call/client/1/info").withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }
  }

  implicit def actorRefFactory: ActorSystem = system
}
