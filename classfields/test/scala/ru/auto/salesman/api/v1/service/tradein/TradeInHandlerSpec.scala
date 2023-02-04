package ru.auto.salesman.api.v1.service.tradein

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm._
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.view.common.{PageModelView, PagingModelView}
import ru.auto.salesman.api.view.tradein.{
  SectionRecordsAvailableView,
  TradeInRequestCoreListingView,
  TradeInRequestCoreView
}
import ru.auto.salesman.model.TradeInRequest
import ru.auto.salesman.service.async.AsyncTradeInService
import ru.auto.salesman.service.tradein.TradeInService.UnsupportedTradeInCategorySectionException

class TradeInHandlerSpec extends RoutingSpec {

  val asyncTradeInServiceMock = mock[AsyncTradeInService]

  private val route = new TradeInHandler(asyncTradeInServiceMock).route

  "POST /trade-in/client/{clientId}" should {
    val uri = "/client/123"
    val body = TradeInRequestForm
      .newBuilder()
      .setUserInfo(
        UserInfo.newBuilder().setPhoneNumber("89175576921").setName("Vova")
      )
      .setClientInfo(ClientInfo.newBuilder().setClientId(123))
      .setClientOfferInfo(
        OfferInfo
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.NEW)
          .setOfferId("123asdasd-32123")
          .setDescription(
            OfferDescription
              .newBuilder()
              .setMark("Kia")
              .setModel("Rio")
              .setYear(2001)
              .setLicensePlate("2")
              .setVin("3")
              .setMileage(2000)
              .setPrice(50000)
          )
      )
      .setUserOfferInfo(
        OfferInfo
          .newBuilder()
          .setOfferId("12345-123")
          .setCategory(Category.CARS)
          .setDescription(
            OfferDescription
              .newBuilder()
              .setMark("Volvo")
              .setModel("x1")
              .setYear(1998)
              .setLicensePlate("123")
              .setVin("123")
              .setMileage(1998)
              .setPrice(10000)
          )
      )
      .build()

    "return 200 for full request" in {
      val expectedModel = TradeInRequest(123L, body)
      val expectedArg = argThat { expected: TradeInRequest =>
        expected.copy(createDate = expectedModel.createDate) == expectedModel
      }
      (asyncTradeInServiceMock.insert _)
        .expects(expectedArg)
        .returningF(())
      Post(uri, HttpEntity(body.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 200 if client offerId and description are missing" in {
      val bodyWithoutClientOffer = body.toBuilder
        .setClientOfferInfo(
          OfferInfo
            .newBuilder()
            .setCategory(Category.CARS)
            .setSection(Section.NEW)
        )
        .build()
      val expectedModel = TradeInRequest(123L, bodyWithoutClientOffer)
      val expectedArg = argThat { expected: TradeInRequest =>
        expected.copy(createDate = expectedModel.createDate) == expectedModel
      }
      (asyncTradeInServiceMock.insert _)
        .expects(expectedArg)
        .returningF(())
      Post(uri, HttpEntity(bodyWithoutClientOffer.toByteArray)) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 404 if invalid category/section" in {
      val motoTradeInForm = body.toBuilder
        .setClientOfferInfo(
          body.getClientOfferInfo.toBuilder.setCategory(Category.MOTO)
        )
        .build()
      val expectedModel = TradeInRequest(123L, motoTradeInForm)
      val expectedArg = argThat { expected: TradeInRequest =>
        expected.copy(createDate = expectedModel.createDate) == expectedModel
      }
      (asyncTradeInServiceMock.insert _)
        .expects(expectedArg)
        .throwing(
          UnsupportedTradeInCategorySectionException(
            Category.MOTO.toString,
            Section.NEW.toString
          )
        )
      Post(uri, HttpEntity(motoTradeInForm.toByteArray)) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return 400 if clientId is missing" in {
      val bodyWithMissingClientId =
        body.toBuilder.setClientInfo(ClientInfo.newBuilder()).build()
      Post(uri, HttpEntity(bodyWithMissingClientId.toByteArray)) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe "Client identifier required."
      }
    }

    "return 400 if user offer id is missing" in {
      val bodyWithMissingClientId =
        body.toBuilder.setUserOfferInfo(OfferInfo.newBuilder()).build()
      Post(uri, HttpEntity(bodyWithMissingClientId.toByteArray)) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe "User offer id required."
      }
    }
  }

  "Get /trade-in/client/{clientId}" should {
    val clientId = 1L
    val fromDate = DateTime.parse("2019-01-01")
    val toDate = DateTime.parse("2019-02-01")
    val section = Some(Section.NEW)
    val pageNum = 1
    val pageSize = 10

    val expectedResponse = {
      val request = TradeInRequestCoreView(
        1L,
        clientId,
        Some("1234-567"),
        Some(12345),
        "89175586983",
        None,
        Some("7654-321"),
        10000,
        Section.NEW.toString,
        DateTime.parse("2019-01-01")
      )
      val recordsWithSections = Seq(
        SectionRecordsAvailableView(
          Section.NEW.toString,
          available = true
        ),
        SectionRecordsAvailableView(
          Section.USED.toString,
          available = false
        )
      )
      val paging = PagingModelView(1, 1, PageModelView(1, 10))

      TradeInRequestCoreListingView(
        Seq(request),
        recordsWithSections,
        10000,
        paging
      )
    }
    val uri =
      "/client/1?from=2019-01-01&to=2019-02-01&pageNum=1&pageSize=10&section=NEW"

    "return trade-in requests" in {
      (asyncTradeInServiceMock.find _)
        .expects(
          clientId,
          fromDate,
          toDate.plusDays(1).minusMillis(1),
          pageNum,
          pageSize,
          section
        )
        .returningF(expectedResponse.asModel)
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[TradeInRequestCoreListingView]
        response shouldBe expectedResponse
      }
    }
  }

}
