package ru.auto.salesman.api.v1.service.match_applications

import akka.http.scaladsl.model.StatusCodes
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.MatchApplication.{
  ErrorCodes,
  MatchApplicationCreateForm,
  MatchApplicationCreateForms,
  MatchApplicationCreateResponse
}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.view.{MatchApplicationsModelView, ProductPriceView}
import ru.auto.salesman.environment.IsoDateFormatter
import ru.auto.salesman.model.common.{PageModel, PagingModel}
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.MatchApplicationId
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{AutoruUser, ClientId, ProductId}
import ru.auto.salesman.service.billingcampaign.BillingCampaignService
import ru.auto.salesman.service.match_applications.MatchApplicationService
import ru.auto.salesman.service.match_applications.MatchApplicationService.{
  MatchApplicationsModel,
  MoreThanOneMatchApplicationFoundException,
  SalesmanMatchApplicationNotFoundException
}
import ru.auto.salesman.util.{DateTimeInterval, Page}
import ru.yandex.vertis.util.time.DateTimeUtil

import spray.json.{JsObject, JsString}

import java.util
import java.util.UUID
import scala.collection.JavaConverters._

class MatchApplicationHandlerSpec extends RoutingSpec {

  private val matchApplicationService = mock[MatchApplicationService]
  private val billingCampaignService = mock[BillingCampaignService]

  private val route = new MatchApplicationHandler(
    matchApplicationService,
    billingCampaignService
  ).route

  "MatchApplicationHandler" should {
    "create match application requests" in {
      val payload: MatchApplicationCreateForms =
        MatchApplicationCreateForms
          .newBuilder()
          .addAllForms(
            List(
              MatchApplicationCreateForm
                .newBuilder()
                .setClientOfferId("offer1")
                .setClientId(1)
                .setUserId(1)
                .setMatchApplicationId(
                  MatchApplicationId(UUID.randomUUID()).toString
                )
                .build(),
              MatchApplicationCreateForm
                .newBuilder()
                .setClientOfferId("offer3")
                .setClientId(2)
                .setUserId(2)
                .setMatchApplicationId(
                  MatchApplicationId(UUID.randomUUID()).toString
                )
                .build(),
              MatchApplicationCreateForm
                .newBuilder()
                .setClientOfferId("offer2")
                .setClientId(3)
                .setUserId(3)
                .setMatchApplicationId(
                  MatchApplicationId(UUID.randomUUID()).toString
                )
                .build()
            ).asJava
          )
          .build

      (matchApplicationService.create _)
        .expects(payload)
        .returningZ(None)

      Post("/", payload).withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        responseAs[
          MatchApplicationCreateResponse
        ] shouldBe MatchApplicationCreateResponse.getDefaultInstance
        status shouldBe StatusCodes.OK
      }
    }

    "return match application failed to be created when no billing campaign found" in {
      val payload: MatchApplicationCreateForms =
        MatchApplicationCreateForms
          .newBuilder()
          .addAllForms(
            List(
              MatchApplicationCreateForm
                .newBuilder()
                .setClientOfferId("offer1")
                .setClientId(1)
                .setUserId(1)
                .setMatchApplicationId(
                  MatchApplicationId(UUID.randomUUID()).toString
                )
                .build(),
              MatchApplicationCreateForm
                .newBuilder()
                .setClientOfferId("offer3")
                .setClientId(2)
                .setUserId(2)
                .setMatchApplicationId(
                  MatchApplicationId(UUID.randomUUID()).toString
                )
                .build(),
              MatchApplicationCreateForm
                .newBuilder()
                .setClientOfferId("offer2")
                .setClientId(3)
                .setUserId(3)
                .setMatchApplicationId(
                  MatchApplicationId(UUID.randomUUID()).toString
                )
                .build()
            ).asJava
          )
          .build

      val expectedResponse = MatchApplicationCreateResponse
        .newBuilder()
        .setErrorCode(ErrorCodes.NO_ACTIVE_BILLING_CAMPAIGN)
        .addAllClientsIds(util.Arrays.asList(1L, 2L, 3L))
        .build()

      (matchApplicationService.create _)
        .expects(payload)
        .returningZ(Some(expectedResponse))

      Post("/", payload).withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[MatchApplicationCreateResponse] shouldBe expectedResponse
      }
    }

    "response with 400 if validation fails" in {
      val emptyFormsPayload: MatchApplicationCreateForms =
        MatchApplicationCreateForms.newBuilder().build

      (matchApplicationService.create _)
        .expects(emptyFormsPayload)
        .never()

      Post("/", emptyFormsPayload).withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          MatchApplicationCreateResponse
        ] shouldBe MatchApplicationCreateResponse
          .newBuilder()
          .setErrorCode(ErrorCodes.VALIDATION_FAILED)
          .setMessage("Match applications list can`t be empty.")
          .build()
      }

      val invalidMatchApplicationIdPayload =
        MatchApplicationCreateForms
          .newBuilder()
          .addForms(
            MatchApplicationCreateForm
              .newBuilder()
              .setClientOfferId("offer2")
              .setClientId(3)
              .setMatchApplicationId("Not UUID")
              .build()
          )
          .build()

      Post("/", invalidMatchApplicationIdPayload) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          MatchApplicationCreateResponse
        ] shouldBe MatchApplicationCreateResponse
          .newBuilder()
          .setErrorCode(ErrorCodes.VALIDATION_FAILED)
          .setMessage("MatchApplicationCreateForm id must be UUID.")
          .build()
      }
    }

    "find match application requests" in {
      val response = MatchApplicationsModel(
        List(
          MatchApplicationCreateRequest(
            clientId = 1L,
            userId = AutoruUser(1L),
            MatchApplicationId(UUID.randomUUID()),
            AutoruOfferId("12345-123"),
            Category.CARS,
            Section.NEW,
            MatchApplicationCreateRequest.Statuses.New,
            DateTimeUtil.now(),
            isRead = false
          ),
          MatchApplicationCreateRequest(
            clientId = 2L,
            userId = AutoruUser(2L),
            MatchApplicationId(UUID.randomUUID()),
            AutoruOfferId("123456-123"),
            Category.CARS,
            Section.NEW,
            MatchApplicationCreateRequest.Statuses.New,
            DateTimeUtil.now(),
            isRead = false
          )
        ),
        totalCost = 10000,
        PagingModel(
          pageCount = 2,
          total = 2,
          page = PageModel(pageNum = 2, pageSize = 10)
        )
      )

      val interval = DateTimeInterval.daysFromCurrent(30)

      (matchApplicationService.find _)
        .expects(20101L, interval, Page(number = 2, size = 10))
        .returningZ(response)

      val dateFromString = interval.from.toString(IsoDateFormatter)
      val dateToString = interval.to.toString(IsoDateFormatter)

      Get(
        s"/client/20101?pageSize=10&pageNum=2&from=$dateFromString&to=$dateToString"
      ) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[MatchApplicationsModelView].asModel shouldBe response
        }
    }

    "get match-application product price" in {
      (billingCampaignService
        .calculateMatchApplicationProductCost(
          _: ProductId.MatchApplicationProduct,
          _: ClientId
        ))
        .expects(ProductId.MatchApplicationCarsNew, 20101L)
        .returningZ(500000)

      val productAlias = ProductId.MatchApplicationCarsNew.toString
      val url = s"/client/20101/price?product=$productAlias"

      Get(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[ProductPriceView]

        response.product shouldBe ProductId.MatchApplicationCarsNew
        response.price shouldBe 500000
      }
    }

    "fail price request on non match-application product" in {
      val productAlias = ProductId.Placement
      val url = s"/client/20101/price?product=$productAlias"

      Get(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "mark match application read" in {
      val uuid = "7f770f4b-b0db-4f5f-abd8-1d7ff344bf22"
      val clientId = 20101L
      (matchApplicationService.updateIsReadFlag _)
        .expects(clientId, MatchApplicationId(uuid), true)
        .returningZ(())

      val url = s"/$uuid/mark-read?clientId=$clientId&isRead=true"

      Put(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "fail with 404 if mark match application read without clientId provided" in {
      val uuid = "7f770f4b-b0db-4f5f-abd8-1d7ff344bf22"

      val url = s"/$uuid/mark-read"

      Put(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[
          String
        ] shouldBe "Request is missing required query parameter 'clientId'"
      }
    }

    "fail with 404 if mark match application read without isRead provided" in {
      val uuid = "7f770f4b-b0db-4f5f-abd8-1d7ff344bf22"
      val clientId = 20101L

      val url = s"/$uuid/mark-read?clientId=$clientId"

      Put(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[
          String
        ] shouldBe "Request is missing required query parameter 'isRead'"
      }
    }

    "fail with 404 if match application to mark was not found" in {
      val matchApplicationId =
        MatchApplicationId("7f770f4b-b0db-4f5f-abd8-1d7ff344bf22")
      val clientId = 20101L
      (matchApplicationService.updateIsReadFlag _)
        .expects(clientId, matchApplicationId, true)
        .throwingZ(
          SalesmanMatchApplicationNotFoundException(
            matchApplicationId,
            clientId
          )
        )

      val url = s"/$matchApplicationId/mark-read?clientId=$clientId&isRead=true"

      Put(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[JsObject] shouldBe JsObject(
          "error" -> JsString(
            "Match application with id [7f770f4b-b0db-4f5f-abd8-1d7ff344bf22] and client id [20101] not found in salesman."
          )
        )
      }
    }

    "fail with 500 if more that one match application was found" in {
      val matchApplicationId =
        MatchApplicationId("7f770f4b-b0db-4f5f-abd8-1d7ff344bf22")
      val clientId = 20101L
      (matchApplicationService.updateIsReadFlag _)
        .expects(clientId, matchApplicationId, false)
        .throwingZ(
          MoreThanOneMatchApplicationFoundException(
            matchApplicationId,
            clientId
          )
        )

      val url =
        s"/$matchApplicationId/mark-read?clientId=$clientId&isRead=false"

      Put(url).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.InternalServerError
        responseAs[JsObject] shouldBe JsObject(
          "error" -> JsString(
            "More than one match application found by id [7f770f4b-b0db-4f5f-abd8-1d7ff344bf22] and client id [20101]."
          )
        )
      }
    }
  }

}
